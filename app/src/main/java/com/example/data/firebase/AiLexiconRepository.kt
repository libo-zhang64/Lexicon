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
import com.google.firebase.ai.GenerativeModel
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

    // Lean, compact schema for fast single-expression lookup
    private val phraseSchema = Schema.obj(
        properties = mapOf(
            "term" to Schema.string("Canonical US expression"),
            "ipa" to Schema.string("US IPA in slashes"),
            "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE", "GRE_VOCAB")),
            "register" to Schema.enumeration(listOf("CASUAL", "CONVERSATIONAL", "PROFESSIONAL", "FORMAL")),
            "cefrLevel" to Schema.enumeration(listOf("B2", "C1", "C2")),
            "definition" to Schema.string("1 concise plain-English sentence"),
            "nuanceSummary" to Schema.string("1 concise sentence on tone and register"),
            "speakerA" to Schema.string("First speaker name"),
            "lineA" to Schema.string("Natural US conversation opener"),
            "speakerB" to Schema.string("Second speaker name"),
            "lineB" to Schema.string("Natural US reply using the expression verbatim"),
            "etymology" to Schema.string("1 brief origin sentence"),
            "synonyms" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string("Near-synonym"),
                        "diff" to Schema.string("Brief tone contrast")
                    )
                ),
                description = "2 near-synonyms"
            ),
            "antonyms" to Schema.array(items = Schema.string(), description = "2 antonyms"),
            "usageNotes" to Schema.array(items = Schema.string(), description = "2 concise usage notes")
        )
    )

    // Lean quiz schema (used for both custom quizzes and Transfer AI Boost)
    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "phraseTerm" to Schema.string("Target expression"),
                        "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE", "GRE_VOCAB")),
                        "questionType" to Schema.enumeration(listOf("CLOZE", "REGISTER", "MEANING")),
                        "settingLabel" to Schema.string("Short 2-word context label, e.g. 'Client Call'"),
                        "prompt" to Schema.string("Concise question stem"),
                        "contextSnippet" to Schema.string("Short 2-line realistic US dialogue or scenario"),
                        "options" to Schema.array(items = Schema.string(), description = "4 short choices"),
                        "correctIndex" to Schema.integer("Index 0..3"),
                        "explanation" to Schema.string("1 concise sentence explaining why")
                    )
                ),
                description = "4 to 5 concise multiple-choice questions"
            )
        )
    )

    // Ultra-lean flat schema for Text Vocabulary Extraction:
    // Avoids deeply nested per-item arrays to slash token volume and eliminate timeouts.
    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "passageLevel" to Schema.string("Short label, e.g. 'C1 Essay'"),
            "passageSummary" to Schema.string("1 concise sentence summarizing vocabulary level"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "term" to Schema.string("Exact strong word/phrase from text"),
                        "ipa" to Schema.string("US IPA"),
                        "category" to Schema.enumeration(listOf("SLANG", "IDIOM", "PHRASAL_VERB", "WORKPLACE", "GRE_VOCAB")),
                        "register" to Schema.enumeration(listOf("CASUAL", "CONVERSATIONAL", "PROFESSIONAL", "FORMAL")),
                        "cefrLevel" to Schema.enumeration(listOf("B2", "C1", "C2")),
                        "definition" to Schema.string("1 concise definition sentence"),
                        "nuanceSummary" to Schema.string("1 short tone/register note"),
                        "sourceExcerpt" to Schema.string("Short quote (<=16 words) from user text"),
                        "upgradeReason" to Schema.string("1 short sentence why it strengthens vocab"),
                        "dialogueA" to Schema.string("Short conversational prompt"),
                        "dialogueB" to Schema.string("Natural US reply using term"),
                        "synonymsCsv" to Schema.string("2 near-synonyms comma-separated"),
                        "antonym" to Schema.string("1 contrasting word")
                    )
                ),
                description = "5 to 6 highest-value words or phrases from the text"
            )
        )
    )

    private val phraseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.55f
                maxOutputTokens = 1400
                responseMimeType = "application/json"
                responseSchema = phraseSchema
            }
        )
    }

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.6f
                maxOutputTokens = 1800
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 2200
                responseMimeType = "application/json"
                responseSchema = extractionSchema
            }
        )
    }

    private suspend fun streamJsonPayload(
        model: GenerativeModel,
        prompt: String,
        timeoutMs: Long = 55_000L,
        onChunkProgress: (charsReceived: Int) -> Unit = {}
    ): String {
        val builder = StringBuilder(2048)
        withTimeout(timeoutMs) {
            model.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text
                if (!text.isNullOrEmpty()) {
                    builder.append(text)
                    onChunkProgress(builder.length)
                }
            }
        }
        return builder.toString().trim()
    }

    suspend fun explainPhrase(
        query: String,
        onProgress: (status: String, charsReceived: Int) -> Unit = { _, _ -> }
    ): Result<PhraseEntry> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter an American English word or phrase to look up."))
        }
        return try {
            val prompt = """
                Lexicon entry for American English expression: "$cleaned".
                Be crisp and concise (1 short sentence per field).
                Use natural everyday US speakers in lineA/lineB with "$cleaned" spoken organically in lineB.
            """.trimIndent()

            onProgress("Connecting to Gemini 3.8 Flash...", 0)
            val rawJson = streamJsonPayload(phraseModel, prompt) { chars ->
                val stage = when {
                    chars < 150 -> "Defining \"$cleaned\"..."
                    chars < 450 -> "Writing real US conversation..."
                    else -> "Adding synonyms & usage notes..."
                }
                onProgress(stage, chars)
            }
            if (rawJson.isBlank()) {
                return Result.failure(IllegalStateException("AI returned an empty response. Please try again."))
            }

            val json = JSONObject(rawJson)
            val term = json.optString("term", cleaned).ifBlank { cleaned }
            val speakerA = json.optString("speakerA", "Maya").ifBlank { "Maya" }
            val lineA = json.optString("lineA", "").trim()
            val speakerB = json.optString("speakerB", "Jordan").ifBlank { "Jordan" }
            val lineB = json.optString("lineB", "").trim()
            val dialogue = buildList {
                if (lineA.isNotEmpty()) add(DialogueTurn(speakerA, lineA))
                if (lineB.isNotEmpty()) add(DialogueTurn(speakerB, lineB))
            }

            val synArr = json.optJSONArray("synonyms")
            val synonyms = buildList {
                if (synArr != null) {
                    for (i in 0 until synArr.length()) {
                        val obj = synArr.optJSONObject(i) ?: continue
                        val synTerm = obj.optString("term").trim()
                        val diff = obj.optString("diff").trim()
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
            Result.failure(Exception("Phrase lookup timed out. Please check your connection and try again."))
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
        categoryFilter: CategoryFilter,
        onProgress: (status: String, charsReceived: Int) -> Unit = { _, _ -> }
    ): Result<List<QuizQuestion>> {
        return try {
            val weakClause = if (weakTerms.isNotEmpty()) {
                "Prioritize: ${weakTerms.take(5).joinToString(", ")}."
            } else {
                "Pick varied B2-C2 US expressions."
            }
            val categoryClause = if (categoryFilter == CategoryFilter.ALL) {
                "Mix slang, idioms, phrasal verbs, workplace, and GRE words."
            } else {
                "Category: ${categoryFilter.label}."
            }
            val topicClause = if (focusTopic.isNotBlank()) "Theme: \"$focusTopic\"." else "Modern everyday & workplace US dialogue."

            val prompt = """
                Create 5 concise B2-C2 American English multiple-choice questions.
                $categoryClause $weakClause $topicClause
                Mix CLOZE (use ________), REGISTER, and MEANING. Keep contexts brief (2 lines) and explanations 1 sentence.
            """.trimIndent()

            onProgress("Streaming personalized quiz...", 0)
            val rawJson = streamJsonPayload(quizModel, prompt) { chars ->
                val countEst = (chars / 260).coerceIn(1, 5)
                onProgress("Drafting question $countEst of 5...", chars)
            }
            parseQuizResponse(rawJson, isTransfer = false, idLookup = emptyMap())
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI quiz generation timed out", e)
            Result.failure(Exception("Quiz generation timed out. Tap retry to try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI quiz generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not generate AI quiz right now."))
        }
    }

    /**
     * AI Fresh Conversation Boost for True Transfer Learning:
     * Generates brand-new unseen multi-turn US conversations testing the user's ALREADY-LEARNED words.
     */
    suspend fun generateTransferBoostQuiz(
        learnedPhrases: List<PhraseEntry>,
        categoryFilter: CategoryFilter,
        onProgress: (status: String, charsReceived: Int) -> Unit = { _, _ -> }
    ): Result<List<QuizQuestion>> {
        if (learnedPhrases.isEmpty()) {
            return Result.failure(IllegalArgumentException("Learn at least one phrase first to generate AI Transfer scenarios."))
        }
        return try {
            val selected = learnedPhrases.shuffled().take(5)
            val idLookup = selected.associateBy { it.term.lowercase() }
            val wordBriefs = selected.joinToString("\n") { p ->
                "- ${p.term} (${p.category.name}, ${p.register.label}): ${p.definition.take(80)}"
            }
            val prompt = """
                Create ${selected.size} concise TRANSFER LEARNING multiple-choice questions testing ONLY these exact expressions the learner ALREADY studied:
                $wordBriefs
                Rules:
                - Place each expression in a BRAND-NEW realistic American conversation (podcast, coffee chat, client call, book club, dinner banter, newsroom) — never use textbook definitions as dialogue.
                - Mix CLOZE (blank out the target word with ________), REGISTER, and MEANING.
                - Keep dialogue snippets to 2 crisp lines and explanations to 1 concise sentence.
                - Set phraseTerm to the EXACT target expression from the list above.
            """.trimIndent()

            onProgress("Crafting fresh AI conversations for your learned words...", 0)
            val rawJson = streamJsonPayload(quizModel, prompt) { chars ->
                val est = (chars / 250).coerceIn(1, selected.size)
                onProgress("Writing unseen scenario $est of ${selected.size}...", chars)
            }
            parseQuizResponse(rawJson, isTransfer = true, idLookup = idLookup)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI transfer boost timed out", e)
            Result.failure(Exception("AI Transfer Boost timed out. You can use Instant Transfer or retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI transfer boost failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not generate AI Transfer scenarios right now."))
        }
    }

    private fun parseQuizResponse(
        rawJson: String,
        isTransfer: Boolean,
        idLookup: Map<String, PhraseEntry>
    ): Result<List<QuizQuestion>> {
        if (rawJson.isBlank()) {
            return Result.failure(IllegalStateException("AI returned an empty quiz. Please try again."))
        }
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
                val matchedEntry = idLookup[term.lowercase()]
                val setting = q.optString("settingLabel", "").trim()
                add(
                    QuizQuestion(
                        id = "ai_q_${i}_${System.currentTimeMillis()}",
                        phraseId = matchedEntry?.id ?: sanitizePhraseId(term),
                        phraseTerm = matchedEntry?.term ?: term,
                        category = matchedEntry?.category ?: PhraseCategory.fromRaw(q.optString("category")),
                        questionType = QuizQuestionType.fromRaw(q.optString("questionType")),
                        prompt = promptText,
                        contextSnippet = q.optString("contextSnippet").trim().takeIf { it.isNotEmpty() },
                        options = opts,
                        correctIndex = correctIdx,
                        explanation = q.optString("explanation", "").trim(),
                        isTransferQuestion = isTransfer,
                        transferDomainLabel = if (isTransfer) {
                            if (setting.isNotBlank()) "AI Fresh Context • $setting" else "AI Fresh Context"
                        } else null
                    )
                )
            }
        }
        return if (parsed.isEmpty()) {
            Result.failure(IllegalStateException("Could not build valid quiz questions. Please retry."))
        } else {
            Result.success(parsed)
        }
    }

    suspend fun extractVocabularyFromText(
        rawText: String,
        focus: TextExtractionFocus = TextExtractionFocus.BALANCED,
        onProgress: (status: String, charsReceived: Int) -> Unit = { _, _ -> }
    ): Result<TextExtractionResult> {
        val cleaned = rawText.trim().take(4_500)
        if (cleaned.length < 25) {
            return Result.failure(IllegalArgumentException("Upload or paste at least a short paragraph (25+ characters) to extract vocabulary."))
        }
        return try {
            val prompt = """
                Extract 5 to 6 of the strongest B2-C2/GRE words or phrases directly from the text below.
                Focus: ${focus.promptHint}
                Rules: Every term MUST appear in the text. Skip basic words. Keep every field crisp and concise (1 short sentence max). Make dialogueA/dialogueB sound like natural everyday American speech.

                TEXT:
                $cleaned
            """.trimIndent()

            onProgress("Scanning passage with Gemini 3.8 Flash...", 0)
            val rawJson = streamJsonPayload(extractionModel, prompt) { chars ->
                val stage = when {
                    chars < 220 -> "Identifying high-impact vocabulary..."
                    chars < 700 -> "Extracting excerpts & nuances..."
                    else -> "Finishing examples & definitions..."
                }
                onProgress(stage, chars)
            }
            if (rawJson.isBlank()) {
                return Result.failure(IllegalStateException("AI returned an empty analysis. Please try again."))
            }

            val root = JSONObject(rawJson)
            val passageLevel = root.optString("passageLevel", "Advanced American English").trim()
            val passageSummary = root.optString(
                "passageSummary",
                "High-leverage expressions extracted from your passage."
            ).trim()
            val arr = root.optJSONArray("items")
                ?: return Result.failure(IllegalStateException("No vocabulary items could be extracted from this text."))

            val speakerPairs = listOf("Maya" to "Jordan", "Sam" to "Priya", "Chloe" to "Marcus", "Nora" to "Liam")
            val extracted = buildList {
                val seen = HashSet<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val term = obj.optString("term").trim()
                    if (term.isEmpty()) continue
                    val id = sanitizePhraseId(term)
                    if (!seen.add(id)) continue

                    val (spA, spB) = speakerPairs[i % speakerPairs.size]
                    val lineA = obj.optString("dialogueA", "").trim()
                    val lineB = obj.optString("dialogueB", "").trim()
                    val dialogue = buildList {
                        if (lineA.isNotEmpty()) add(DialogueTurn(spA, lineA))
                        if (lineB.isNotEmpty()) add(DialogueTurn(spB, lineB))
                    }

                    val synonyms = obj.optString("synonymsCsv", "")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .take(3)
                        .mapIndexed { idx, s ->
                            SynonymNuance(
                                term = s,
                                nuanceDifference = if (idx == 0) "Close substitute in similar contexts." else "Alternative phrasing with slightly different tone."
                            )
                        }
                    val antonym = obj.optString("antonym", "").trim()
                    val antonyms = if (antonym.isNotEmpty()) listOf(antonym) else emptyList()
                    val register = PhraseRegister.fromRaw(obj.optString("register"))
                    val upgradeReason = obj.optString("upgradeReason", "").trim()
                    val nuance = obj.optString("nuanceSummary", "").trim()

                    val entry = PhraseEntry(
                        id = id,
                        term = term,
                        ipa = obj.optString("ipa", "").trim(),
                        category = PhraseCategory.fromRaw(obj.optString("category")),
                        register = register,
                        cefrLevel = CefrLevel.fromRaw(obj.optString("cefrLevel")),
                        definition = obj.optString("definition", "").trim().ifBlank { "High-value English expression." },
                        nuanceSummary = nuance,
                        dialogue = dialogue,
                        etymology = nuance.ifBlank { "Extracted from your reading passage for active B2-C2 mastery." },
                        synonyms = synonyms,
                        antonyms = antonyms,
                        usageNotes = listOfNotNull(
                            upgradeReason.takeIf { it.isNotBlank() },
                            "Register: ${register.label} — ${register.toneHint}."
                        ),
                        source = PhraseSource.AI_GENERATED
                    )

                    add(
                        ExtractedVocabItem(
                            entry = entry,
                            sourceExcerpt = obj.optString("sourceExcerpt", "").trim(),
                            upgradeReason = upgradeReason
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
            Result.failure(Exception("Text analysis timed out. Try a shorter excerpt or tap Try Again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text vocabulary extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not analyze the text right now."))
        }
    }
}
