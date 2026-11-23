package com.example.domain

import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType
import java.util.Locale
import kotlin.math.abs

/**
 * True Transfer Learning Engine:
 * Selects expressions the learner has ALREADY studied/learned and places them inside
 * brand-new, unseen conversational settings, workplace situations, and paraphrase contexts
 * that are strictly distinct from the original lesson dialogue.
 */
object TransferScenarioEngine {

    private data class UnseenSetting(
        val domainLabel: String,
        val speakerA: String,
        val speakerB: String,
        val leadLine: String,
        val responseWithBlank: (meaningLower: String, registerLabel: String) -> String
    )

    private val unseenSettings = listOf(
        UnseenSetting(
            domainLabel = "Podcast Debate",
            speakerA = "Host (Tessa)",
            speakerB = "Guest (Devon)",
            leadLine = "Listeners wrote in asking why last week's announcement sparked such a big reaction.",
            responseWithBlank = { meaning, _ ->
                "Honestly, the whole panel felt that way—if you want to capture \"$meaning\" without rambling, you'd just say ________."
            }
        ),
        UnseenSetting(
            domainLabel = "Client Sync",
            speakerA = "Priya",
            speakerB = "Marcus",
            leadLine = "Before we jump on the call with the client team, how should we summarize this trade-off?",
            responseWithBlank = { meaning, reg ->
                "Keep it $reg and clear—use ________ so they immediately get the idea of $meaning."
            }
        ),
        UnseenSetting(
            domainLabel = "Coffee Shop Catchup",
            speakerA = "Chloe",
            speakerB = "Sam",
            leadLine = "Wait, tell me the real version of what happened after you left the meeting!",
            responseWithBlank = { meaning, _ ->
                "Picture this: everyone paused, and I realized the only phrase that nailed \"$meaning\" was ________."
            }
        ),
        UnseenSetting(
            domainLabel = "Slack Thread",
            speakerA = "Noah (#product-ops)",
            speakerB = "Elena (#product-ops)",
            leadLine = "Quick wording check before I post the update to the whole org—does this sound right?",
            responseWithBlank = { meaning, reg ->
                "Super close! Swap that clunky sentence for ________—it's much more $reg for conveying $meaning."
            }
        ),
        UnseenSetting(
            domainLabel = "Book Club Night",
            speakerA = "Nora",
            speakerB = "Liam",
            leadLine = "What was your biggest takeaway from the final chapter? The ending caught me off guard.",
            responseWithBlank = { meaning, _ ->
                "Same here. To sum up that feeling of $meaning in one natural expression, I'd call it ________."
            }
        ),
        UnseenSetting(
            domainLabel = "Dinner Banter",
            speakerA = "Ava",
            speakerB = "Owen",
            leadLine = "My sister asked me how your new job is going—give me the unfiltered version.",
            responseWithBlank = { meaning, reg ->
                "Let's just say if you need a $reg expression for $meaning, ________ sums up my whole month."
            }
        ),
        UnseenSetting(
            domainLabel = "Editorial Review",
            speakerA = "Rachel",
            speakerB = "Caleb",
            leadLine = "This paragraph feels a little wordy right before the conclusion—any tighter phrasing?",
            responseWithBlank = { meaning, _ ->
                "Trim the filler and drop in ________ right there—it nails $meaning with way sharper rhythm."
            }
        ),
        UnseenSetting(
            domainLabel = "Team Retro",
            speakerA = "Hannah",
            speakerB = "Jordan",
            leadLine = "Looking back at the launch sprint, what's the one headline lesson we should carry forward?",
            responseWithBlank = { meaning, reg ->
                "In a $reg tone, ________ captures $meaning better than a three-bullet slide ever could."
            }
        )
    )

    private val freshRealWorldRoleplays = listOf(
        "You're debriefing a colleague after a tense stakeholder review" to "Workplace Debrief",
        "You're chatting with a close friend while grabbing weekend coffee" to "Weekend Coffee",
        "You're recording a voice note to recap a surprising plot twist" to "Voice Note Recap",
        "You're editing an op-ed draft with a senior editor on deadline" to "Newsroom Edit",
        "You're replying in a lively group chat about a sudden change of plans" to "Group Chat",
        "You're explaining a subtle pattern during a live Q&A panel" to "Live Q&A Panel"
    )

    /**
     * Returns expressions the user has already engaged with (completed lesson, quizzed, or saved).
     */
    fun getLearnedPhrases(
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        categoryFilter: CategoryFilter = CategoryFilter.ALL
    ): List<PhraseEntry> {
        val learnedIds = progressMap.values
            .filter { it.lessonCompleted || it.attemptCount > 0L || it.reviewCount > 0L || it.bookmarked }
            .map { it.phraseId }
            .toSet()

        if (learnedIds.isEmpty()) return emptyList()

        val learnedAll = allPhrases.filter { it.id in learnedIds }
        val filtered = learnedAll.filter { categoryFilter.matches(it.category) }
        return if (filtered.isNotEmpty()) filtered else learnedAll
    }

    /**
     * Builds True Transfer questions strictly from the user's already-learned vocabulary,
     * placing every learned word in a brand-new unseen context distinct from its original lesson.
     */
    fun buildTransferQuestionsForLearned(
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        categoryFilter: CategoryFilter = CategoryFilter.ALL,
        focusPhraseIds: List<String> = emptyList(),
        questionCount: Int = 5
    ): List<QuizQuestion> {
        val learned = getLearnedPhrases(allPhrases, progressMap, categoryFilter)
        if (learned.isEmpty()) return emptyList()

        val focusSet = focusPhraseIds.toSet()
        // Prioritize focus phrases + recently studied/missed phrases, then shuffle for fresh variety
        val prioritized = learned.sortedWith(
            compareByDescending<PhraseEntry> { it.id in focusSet }
                .thenByDescending { progressMap[it.id]?.missCount ?: 0L }
                .thenBy { progressMap[it.id]?.reviewCount ?: 0L }
        )
        val candidatePool = (prioritized.take(questionCount * 2).shuffled() + prioritized)
            .distinctBy { it.id }

        val selected = candidatePool.take(questionCount.coerceAtMost(candidatePool.size))
        return buildQuestionsFromPool(selected, allPhrases)
    }

    /**
     * Explicit starter preview (only used when user has 0 learned words and taps "Try Starter Preview").
     */
    fun buildStarterPreview(
        allPhrases: List<PhraseEntry>,
        categoryFilter: CategoryFilter = CategoryFilter.ALL,
        questionCount: Int = 5
    ): List<QuizQuestion> {
        val pool = allPhrases.filter { categoryFilter.matches(it.category) }.ifEmpty { allPhrases }
        val sample = pool.take(40).shuffled().take(questionCount.coerceAtMost(pool.size))
        return buildQuestionsFromPool(sample, allPhrases)
    }

    private fun buildQuestionsFromPool(
        targets: List<PhraseEntry>,
        allPhrases: List<PhraseEntry>
    ): List<QuizQuestion> {
        val seedBase = (System.currentTimeMillis() / 1000L).toInt()
        val typesCycle = listOf(
            QuizQuestionType.CLOZE,
            QuizQuestionType.REGISTER,
            QuizQuestionType.MEANING
        )

        return targets.mapIndexed { idx, target ->
            val qType = typesCycle[idx % typesCycle.size]
            buildSingleTransferQuestion(
                target = target,
                allPhrases = allPhrases,
                type = qType,
                seed = abs(target.id.hashCode() + seedBase + idx * 31),
                idx = idx
            )
        }
    }

    private fun buildSingleTransferQuestion(
        target: PhraseEntry,
        allPhrases: List<PhraseEntry>,
        type: QuizQuestionType,
        seed: Int,
        idx: Int
    ): QuizQuestion {
        val sameCategory = allPhrases.filter { it.id != target.id && it.category == target.category }
        val distractorPool = if (sameCategory.size >= 3) sameCategory else allPhrases.filter { it.id != target.id }
        val distractors = distractorPool.shuffled().take(3)

        val cleanDef = target.definition.trim().trimEnd('.')
        val defLower = cleanDef.replaceFirstChar { it.lowercase(Locale.US) }
        val regLower = target.register.label.lowercase(Locale.US)

        return when (type) {
            QuizQuestionType.CLOZE -> {
                val setting = unseenSettings[seed % unseenSettings.size]
                val snippet = buildString {
                    append("${setting.speakerA}: \"${setting.leadLine}\"\n")
                    append("${setting.speakerB}: \"${setting.responseWithBlank(defLower, regLower)}\"")
                }
                val options = (distractors + target).shuffled()
                val correctIndex = options.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "xfer_cloze_${target.id}_${idx}_$seed",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.CLOZE,
                    prompt = "Apply your learned vocabulary in this brand-new conversation:",
                    contextSnippet = snippet,
                    options = options.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "Transfer match: \"${target.term}\" ($regLower) means $defLower. ${target.nuanceSummary}",
                    isTransferQuestion = true,
                    transferDomainLabel = "New Context • ${setting.domainLabel}"
                )
            }

            QuizQuestionType.REGISTER -> {
                val (situation, domainName) = freshRealWorldRoleplays[seed % freshRealWorldRoleplays.size]
                val synHint = target.synonyms.firstOrNull()?.term
                val snippet = buildString {
                    append("Scenario: $situation. ")
                    append("You need a $regLower expression that conveys \"$defLower\"")
                    if (!synHint.isNullOrBlank()) {
                        append(" (similar in spirit to \"$synHint\", with sharper American nuance).")
                    } else {
                        append(".")
                    }
                }
                val options = (distractors + target).shuffled()
                val correctIndex = options.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "xfer_reg_${target.id}_${idx}_$seed",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.REGISTER,
                    prompt = "Which expression you've learned transfers best to this new situation?",
                    contextSnippet = snippet,
                    options = options.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "\"${target.term}\" fits ${target.register.label} contexts cleanly: $cleanDef.",
                    isTransferQuestion = true,
                    transferDomainLabel = "New Context • $domainName"
                )
            }

            QuizQuestionType.MEANING -> {
                val (situation, domainName) = freshRealWorldRoleplays[(seed + 3) % freshRealWorldRoleplays.size]
                val freshSentence = buildFreshContextLineForMeaning(target, situation, seed)
                val optionDefs = (distractors.map { it.definition } + target.definition).shuffled()
                val correctIndex = optionDefs.indexOf(target.definition).coerceAtLeast(0)
                QuizQuestion(
                    id = "xfer_mean_${target.id}_${idx}_$seed",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.MEANING,
                    prompt = "In this unfamiliar setting, what does \"${target.term}\" signal?",
                    contextSnippet = freshSentence,
                    options = optionDefs,
                    correctIndex = correctIndex,
                    explanation = "In this new context, \"${target.term}\" still conveys: $cleanDef. ${target.nuanceSummary}",
                    isTransferQuestion = true,
                    transferDomainLabel = "New Context • $domainName"
                )
            }
        }
    }

    private fun buildFreshContextLineForMeaning(
        entry: PhraseEntry,
        situation: String,
        seed: Int
    ): String {
        val term = entry.term
        val contextTemplates = when (entry.category) {
            PhraseCategory.GRE_VOCAB -> listOf(
                "$situation:\n\"Honestly, watching how that played out felt unmistakably $term—nobody expected it.\"",
                "$situation:\n\"Wait until you hear their reaction; the whole exchange was shaped by $term.\""
            )
            PhraseCategory.WORKPLACE -> listOf(
                "$situation:\n\"Before we sign off on Q4, let's make sure we don't overlook $term.\"",
                "$situation:\n\"Her manager brought up \"$term\" during the debrief, and the whole room nodded.\""
            )
            else -> listOf(
                "$situation:\n\"No kidding—right in the middle of the story, she dropped \"$term\" and everyone laughed.\"",
                "$situation:\n\"He turned to me afterward and said, 'Yeah, \"$term\" pretty much covers it.'\""
            )
        }
        return contextTemplates[seed % contextTemplates.size]
    }
}
