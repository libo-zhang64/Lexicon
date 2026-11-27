package com.example.domain

import com.example.data.catalog.NaturalDialogueSynthesizer
import com.example.data.model.CategoryFilter
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
 * that are strictly distinct from the original lesson dialogue—using genuine spoken English
 * with zero meta-linguistic filler.
 */
object TransferScenarioEngine {

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
        val distractors = CuratedQuizGenerator.selectSafeDistractors(target, allPhrases, count = 3)
        val synTerms = target.synonyms.map { it.term }

        val cleanDef = target.definition.trim().trimEnd('.')
        val defLower = cleanDef.replaceFirstChar { it.lowercase(Locale.US) }
        val regLower = target.register.label.lowercase(Locale.US)

        return when (type) {
            QuizQuestionType.CLOZE -> {
                val scene = NaturalDialogueSynthesizer.buildTransferScene(
                    category = target.category,
                    term = target.term,
                    definition = target.definition,
                    seed = seed,
                    synonyms = synTerms
                )
                val snippet = "${scene.speakerA}: \"${scene.leadLine}\"\n${scene.speakerB}: \"${scene.replyWithBlank}\""
                val options = (distractors + target).shuffled()
                val correctIndex = options.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "xfer_cloze_${target.id}_${idx}_$seed",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.CLOZE,
                    prompt = "Fill in the blank with the expression meaning \"$defLower\":",
                    contextSnippet = snippet,
                    options = options.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "\"${target.term}\" ($regLower) means $defLower. ${target.nuanceSummary}",
                    isTransferQuestion = true,
                    transferDomainLabel = "New Context • ${scene.domainLabel}"
                )
            }

            QuizQuestionType.REGISTER -> {
                val scene = NaturalDialogueSynthesizer.buildTransferScene(
                    category = target.category,
                    term = target.term,
                    definition = target.definition,
                    seed = seed + 5,
                    synonyms = synTerms
                )
                val snippet = "${scene.speakerA}: \"${scene.leadLine}\"\n${scene.speakerB}: \"${scene.replyWithBlank}\""
                val options = (distractors + target).shuffled()
                val correctIndex = options.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "xfer_reg_${target.id}_${idx}_$seed",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.REGISTER,
                    prompt = "Pick the $regLower expression meaning \"$defLower\" that fits ${scene.speakerB}'s reply:",
                    contextSnippet = snippet,
                    options = options.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "\"${target.term}\" fits ${target.register.label} contexts cleanly: $cleanDef.",
                    isTransferQuestion = true,
                    transferDomainLabel = "New Context • ${scene.domainLabel}"
                )
            }

            QuizQuestionType.MEANING -> {
                val scene = NaturalDialogueSynthesizer.buildTransferScene(
                    category = target.category,
                    term = target.term,
                    definition = target.definition,
                    seed = seed + 3,
                    synonyms = synTerms
                )
                val snippet = "${scene.speakerA}: \"${scene.leadLine}\"\n${scene.speakerB}: \"${scene.replyWithTarget}\""
                val optionDefs = (distractors.map { it.definition } + target.definition).shuffled()
                val correctIndex = optionDefs.indexOf(target.definition).coerceAtLeast(0)
                QuizQuestion(
                    id = "xfer_mean_${target.id}_${idx}_$seed",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.MEANING,
                    prompt = "What does \"${target.term}\" mean in this conversation?",
                    contextSnippet = snippet,
                    options = optionDefs,
                    correctIndex = correctIndex,
                    explanation = "\"${target.term}\" means: $cleanDef. ${target.nuanceSummary}",
                    isTransferQuestion = true,
                    transferDomainLabel = "New Context • ${scene.domainLabel}"
                )
            }
        }
    }
}
