package com.example.data.firebase

import android.util.Log
import com.example.data.model.CategoryFilter
import com.example.data.model.CefrLevel
import com.example.data.model.DialogueTurn
import com.example.data.model.ExtractedVocabItem
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseRegister
import com.example.data.model.PhraseSource
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType
import com.example.data.model.SynonymNuance
import com.example.data.model.TextExtractionFocus
import com.example.data.model.TextExtractionResult
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
            "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE", "GRE_VOCAB")),
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
                        "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE", "GRE_VOCAB")),
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

    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "passageLevel" to Schema.string("Short reading level & tone label, e.g., 'C1 Analytical Essay' or 'B2 Workplace Memo'"),
            "passageSummary" to Schema.string("One concise sentence summarizing the text's vocabulary profile and what learners will gain"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string("Exact high-value word or phrase extracted from the text (in canonical dictionary form)"),
                        "ipa" to Schema.string("General American IPA pronunciation inside slashes"),
                        "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE", "GRE_VOCAB")),
                        "register" to Schema.enumeration(listOf("CASUAL", "CONVERSATIONAL", "PROFESSIONAL", "FORMAL")),
                        "cefrLevel" to Schema.enumeration(listOf("B2", "C1", "C2")),
                        "definition" to Schema.string("Clear, precise definition for intermediate-to-advanced learners"),
                        "nuanceSummary" to Schema.string("Subtle pragmatic tone, register, or usage nuance"),
                        "sourceExcerpt" to Schema.string("The exact short sentence or clause from the uploaded text where this word or phrase appears"),
                        "upgradeReason" to Schema.string("One concise sentence explaining why mastering this expression strengthens the user's vocabulary"),
                        "dialogue" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "speaker" to Schema.string("First name of speaker"),
                                    "line" to Schema.string("Natural American English sentence using the term")
                                )
                            ),
                            description = "2-turn or 3-turn realistic American dialogue using the extracted term"
                        ),
                        "etymology" to Schema.string("Brief origin story or memorable root breakdown"),
                        "synonyms" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "term" to Schema.string("Near-synonym"),
                                    "nuanceDifference" to Schema.string("How its tone or meaning differs")
                                )
                            ),
                            description = "2 near-synonyms with nuance contrasts"
                        ),
                        "antonyms" to Schema.array(
                            items = Schema.string(),
                            description = "1 to 2 contrasting words or phrases"
                        ),
                        "usageNotes" to Schema.array(
                            items = Schema.string(),
                            description = "1 to 2 practical learner usage notes"
                        )
                    )
                ),
                description = "6 to 10 strongest vocabulary words and phrases extracted directly from the uploaded text"
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

    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = extractionSchema
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
                "Mix Daily Slang, Idioms, Complex Phrasal Verbs, Workplace Nuance, and GRE Vocabulary."
            } else {
                "Focus primarily on the ${categoryFilter.label} category."
            }
            val topicClause = if (focusTopic.isNotBlank()) {
                "Scenario theme requested by learner: \"$focusTopic\"."
            } else {
                "Use authentic modern American daily life, academic, and workplace dialogues."
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

    suspend fun extractVocabularyFromText(
        rawText: String,
        focus: TextExtractionFocus = TextExtractionFocus.BALANCED
    ): Result<TextExtractionResult> {
        val cleaned = rawText.trim().take(12_000)
        if (cleaned.length < 25) {
            return Result.failure(IllegalArgumentException("Upload or paste at least a short paragraph (25+ characters) to extract vocabulary."))
        }
        return try {
            val prompt = """
                You are an expert American English lexicographer and GRE/executive vocabulary coach.
                Analyze the following user-provided text and extract 6 to 10 of the strongest, highest-leverage words and phrases that actually appear in the text and will genuinely elevate an intermediate-to-advanced (B2-C2) learner's vocabulary.
                Focus preference: ${focus.promptHint}
                Rules:
                - Every extracted term MUST come directly from the provided text (never invent words not in the text).
                - Skip basic everyday words (e.g., 'important', 'good', 'make'). Select precise GRE words, crisp verbs, idioms, phrasal verbs, or nuanced workplace phrases.
                - Include the exact short sourceExcerpt sentence/clause from the user's text where each term appears.
                - Provide General American IPA, accurate category (SLANG, IDIOM, PHRASAL_VERB, WORKPLACE, or GRE_VOCAB), register, CEFR level, clear definition, upgradeReason, realistic 2-3 turn American dialogue, brief etymology, 2 synonyms with nuance contrasts, antonyms, and practical usage notes.

                USER TEXT:
                $cleaned
            """.trimIndent()

            val response = withTimeout(60_000L) {
                extractionModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty analysis. Please try again."))

            val root = JSONObject(rawJson)
            val passageLevel = root.optString("passageLevel", "Advanced American English").trim()
            val passageSummary = root.optString(
                "passageSummary",
                "High-leverage expressions extracted from your passage."
            ).trim()
            val arr = root.optJSONArray("items")
                ?: return Result.failure(IllegalStateException("No vocabulary items could be extracted from this text."))

            val extracted = buildList {
                val seen = HashSet<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val term = obj.optString("term").trim()
                    if (term.isEmpty()) continue
                    val id = sanitizePhraseId(term)
                    if (!seen.add(id)) continue

                    val dialogueArr = obj.optJSONArray("dialogue")
                    val dialogue = buildList {
                        if (dialogueArr != null) {
                            for (d in 0 until dialogueArr.length()) {
                                val turn = dialogueArr.optJSONObject(d) ?: continue
                                val speaker = turn.optString("speaker").trim()
                                val line = turn.optString("line").trim()
                                if (speaker.isNotEmpty() && line.isNotEmpty()) {
                                    add(DialogueTurn(speaker, line))
                                }
                            }
                        }
                    }

                    val synArr = obj.optJSONArray("synonyms")
                    val synonyms = buildList {
                        if (synArr != null) {
                            for (s in 0 until synArr.length()) {
                                val synObj = synArr.optJSONObject(s) ?: continue
                                val synTerm = synObj.optString("term").trim()
                                val diff = synObj.optString("nuanceDifference").trim()
                                if (synTerm.isNotEmpty()) add(SynonymNuance(synTerm, diff))
                            }
                        }
                    }

                    val antArr = obj.optJSONArray("antonyms")
                    val antonyms = buildList {
                        if (antArr != null) {
                            for (a in 0 until antArr.length()) {
                                val v = antArr.optString(a).trim()
                                if (v.isNotEmpty()) add(v)
                            }
                        }
                    }

                    val notesArr = obj.optJSONArray("usageNotes")
                    val usageNotes = buildList {
                        if (notesArr != null) {
                            for (n in 0 until notesArr.length()) {
                                val v = notesArr.optString(n).trim()
                                if (v.isNotEmpty()) add(v)
                            }
                        }
                    }

                    val entry = PhraseEntry(
                        id = id,
                        term = term,
                        ipa = obj.optString("ipa", "").trim(),
                        category = PhraseCategory.fromRaw(obj.optString("category")),
                        register = PhraseRegister.fromRaw(obj.optString("register")),
                        cefrLevel = CefrLevel.fromRaw(obj.optString("cefrLevel")),
                        definition = obj.optString("definition", "").trim().ifBlank { "High-value English expression." },
                        nuanceSummary = obj.optString("nuanceSummary", "").trim(),
                        dialogue = dialogue,
                        etymology = obj.optString("etymology", "").trim(),
                        synonyms = synonyms,
                        antonyms = antonyms,
                        usageNotes = usageNotes,
                        source = PhraseSource.AI_GENERATED
                    )

                    add(
                        ExtractedVocabItem(
                            entry = entry,
                            sourceExcerpt = obj.optString("sourceExcerpt", "").trim(),
                            upgradeReason = obj.optString("upgradeReason", "").trim()
                        )
                    )
                }
            }

            if (extracted.isEmpty()) {
                Result.failure(IllegalStateException("No distinct advanced vocabulary words were found. Try a longer or richer passage."))
            } else {
                Result.success(
                    TextExtractionResult(
                        passageLevel = passageLevel,
                        passageSummary = passageSummary,
                        items = extracted
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Text vocabulary extraction timed out", e)
            Result.failure(Exception("Text analysis timed out after 60 seconds. Try a slightly shorter passage."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text vocabulary extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not analyze the text right now."))
        }
    }
}
