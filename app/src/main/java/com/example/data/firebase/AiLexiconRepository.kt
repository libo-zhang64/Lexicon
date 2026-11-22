package com.example.data.firebase

import android.util.Log
import com.example.data.model.CategoryFilter
import com.example.data.model.CefrLevel
import com.example.data.model.DialogueTurn
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseRegister
import com.example.data.model.PhraseSource
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType
import com.example.data.model.SynonymNuance
import com.example.data.model.sanitizePhraseId
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiLexiconRepository"
private const val MODEL_NAME = "gemini-3.8-flash"

class AiLexiconRepository {

    private val phraseSchema = Schema.obj(
        properties = mapOf(
            "term" to Schema.string("Canonical American English expression"),
            "ipa" to Schema.string("General American IPA pronunciation inside slashes"),
            "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE")),
            "register" to Schema.enumeration(listOf("CASUAL", "CONVERSATIONAL", "PROFESSIONAL", "FORMAL")),
            "cefrLevel" to Schema.enumeration(listOf("B2", "C1", "C2")),
            "definition" to Schema.string("Clear plain-English definition for B2-C2 learners"),
            "nuanceSummary" to Schema.string("Subtle pragmatic tone and register guidance"),
            "dialogue" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "speaker" to Schema.string("First name of speaker"),
                        "line" to Schema.string("Natural American English dialogue turn")
                    )
                ),
                description = "3-turn real-world American conversation using the target phrase naturally"
            ),
            "etymology" to Schema.string("Concise, accurate historical or cultural origin story"),
            "synonyms" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string("Related expression"),
                        "nuanceDifference" to Schema.string("How its tone or meaning differs from the headword")
                    )
                ),
                description = "2 to 3 near-synonyms with nuance contrasts"
            ),
            "antonyms" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 contrasting expressions"
            ),
            "usageNotes" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 advanced learner notes on collocations, grammar, formality, or US vs UK differences"
            )
        )
    )

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "phraseTerm" to Schema.string("Target American English expression being tested"),
                        "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE")),
                        "questionType" to Schema.enumeration(listOf("CLOZE", "REGISTER", "MEANING")),
                        "prompt" to Schema.string("Clear question stem for a B2-C2 learner"),
                        "contextSnippet" to Schema.string("Short realistic American dialogue or workplace scenario"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 plausible answer choices"
                        ),
                        "correctIndex" to Schema.integer("Zero-based index (0 to 3) of the single best answer"),
                        "explanation" to Schema.string("Concise editorial explanation of why the answer is right and why others miss the nuance")
                    )
                ),
                description = "Exactly 5 varied multiple-choice questions"
            )
        )
    )

    private val phraseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = phraseSchema
            }
        )
    }

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    suspend fun explainPhrase(query: String): Result<PhraseEntry> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter an American English word or phrase to look up."))
        }
        return try {
            val prompt = """
                You are an expert American English lexicographer teaching upper-intermediate to advanced learners (B2-C2).
                Create a rich, accurate, publication-grade lexicon entry for the American English expression: "$cleaned".
                Ensure:
                - General American IPA pronunciation (e.g., /ˌloʊˈkiː/).
                - A realistic 3-turn everyday or workplace American dialogue that naturally uses "$cleaned" verbatim.
                - An authentic etymology / origin story.
                - 2-3 synonyms with exact nuance differences, 2-3 antonyms, and 2-3 advanced usage/register notes (mention US vs UK differences if applicable).
            """.trimIndent()

            val response = withTimeout(60_000L) {
                phraseModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty response. Please try again."))

            val json = JSONObject(rawJson)
            val term = json.optString("term", cleaned).ifBlank { cleaned }
            val dialogueArr = json.optJSONArray("dialogue")
            val dialogue = buildList {
                if (dialogueArr != null) {
                    for (i in 0 until dialogueArr.length()) {
                        val obj = dialogueArr.optJSONObject(i) ?: continue
                        val speaker = obj.optString("speaker").trim()
                        val line = obj.optString("line").trim()
                        if (speaker.isNotEmpty() && line.isNotEmpty()) {
                            add(DialogueTurn(speaker, line))
                        }
                    }
                }
            }
            val synArr = json.optJSONArray("synonyms")
            val synonyms = buildList {
                if (synArr != null) {
                    for (i in 0 until synArr.length()) {
                        val obj = synArr.optJSONObject(i) ?: continue
                        val synTerm = obj.optString("term").trim()
                        val diff = obj.optString("nuanceDifference").trim()
                        if (synTerm.isNotEmpty()) add(SynonymNuance(synTerm, diff))
                    }
                }
            }
            val antArr = json.optJSONArray("antonyms")
            val antonyms = buildList {
                if (antArr != null) {
                    for (i in 0 until antArr.length()) {
                        val item = antArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            val notesArr = json.optJSONArray("usageNotes")
            val usageNotes = buildList {
                if (notesArr != null) {
                    for (i in 0 until notesArr.length()) {
                        val item = notesArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }

            Result.success(
                PhraseEntry(
                    id = sanitizePhraseId(term),
                    term = term,
                    ipa = json.optString("ipa", ""),
                    category = PhraseCategory.fromRaw(json.optString("category")),
                    register = PhraseRegister.fromRaw(json.optString("register")),
                    cefrLevel = CefrLevel.fromRaw(json.optString("cefrLevel")),
                    definition = json.optString("definition", "American English expression."),
                    nuanceSummary = json.optString("nuanceSummary", ""),
                    dialogue = dialogue,
                    etymology = json.optString("etymology", ""),
                    synonyms = synonyms,
                    antonyms = antonyms,
                    usageNotes = usageNotes,
                    source = PhraseSource.AI_GENERATED
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI phrase explanation timed out", e)
            Result.failure(Exception("Phrase lookup timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI phrase explanation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not explain phrase right now."))
        }
    }

    suspend fun generateCustomQuiz(
        focusTopic: String,
        weakTerms: List<String>,
        categoryFilter: CategoryFilter
    ): Result<List<QuizQuestion>> {
        return try {
            val weakClause = if (weakTerms.isNotEmpty()) {
                "Prioritize testing and reinforcing these expressions the learner recently missed or is practicing: ${weakTerms.take(6).joinToString(", ")}."
            } else {
                "Choose high-value B2-C2 American English expressions."
            }
            val categoryClause = if (categoryFilter == CategoryFilter.ALL) {
                "Mix Daily Slang, Idioms, Complex Phrasal Verbs, and Workplace Nuance."
            } else {
                "Focus primarily on the ${categoryFilter.label} category."
            }
            val topicClause = if (focusTopic.isNotBlank()) {
                "Scenario theme requested by learner: \"$focusTopic\"."
            } else {
                "Use authentic modern American daily life and workplace dialogues."
            }

            val prompt = """
                Create exactly 5 challenging multiple-choice quiz questions for an intermediate-to-advanced (B2-C2) American English learner.
                $categoryClause
                $weakClause
                $topicClause
                Mix questionType across CLOZE (fill-in-the-blank dialogue with ____), REGISTER (picking the most natural/appropriate tone for a scenario), and MEANING (nuance/synonym precision).
                Each question MUST have exactly 4 distinct options and a valid correctIndex (0..3).
            """.trimIndent()

            val response = withTimeout(60_000L) {
                quizModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty quiz. Please try again."))

            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("questions")
                ?: return Result.failure(IllegalStateException("Quiz response did not contain questions."))

            val parsed = buildList {
                for (i in 0 until arr.length()) {
                    val q = arr.optJSONObject(i) ?: continue
                    val term = q.optString("phraseTerm").trim().ifBlank { "American phrase" }
                    val optsArr = q.optJSONArray("options") ?: continue
                    val opts = (0 until optsArr.length()).map { optsArr.optString(it).trim() }.filter { it.isNotEmpty() }
                    if (opts.size < 2) continue
                    val correctIdx = q.optInt("correctIndex", 0).coerceIn(0, opts.lastIndex)
                    val promptText = q.optString("prompt").trim()
                    if (promptText.isEmpty()) continue
                    add(
                        QuizQuestion(
                            id = "ai_q_${i}_${System.currentTimeMillis()}",
                            phraseId = sanitizePhraseId(term),
                            phraseTerm = term,
                            category = PhraseCategory.fromRaw(q.optString("category")),
                            questionType = QuizQuestionType.fromRaw(q.optString("questionType")),
                            prompt = promptText,
                            contextSnippet = q.optString("contextSnippet").trim().takeIf { it.isNotEmpty() },
                            options = opts,
                            correctIndex = correctIdx,
                            explanation = q.optString("explanation", "").trim()
                        )
                    )
                }
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("Could not build valid quiz questions. Please retry."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI quiz generation timed out", e)
            Result.failure(Exception("Quiz generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI quiz generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not generate AI quiz right now."))
        }
    }
}
