package com.example.domain

import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType
import java.util.Locale

object CuratedQuizGenerator {

    fun buildQuiz(
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        categoryFilter: CategoryFilter = CategoryFilter.ALL,
        focusPhraseIds: List<String> = emptyList(),
        questionCount: Int = 5
    ): List<QuizQuestion> {
        if (allPhrases.isEmpty()) return emptyList()

        val filteredPool = allPhrases.filter { categoryFilter.matches(it.category) }
            .ifEmpty { allPhrases }

        val focusSet = focusPhraseIds.toSet()
        val prioritized = filteredPool.sortedWith(
            compareByDescending<PhraseEntry> { it.id in focusSet }
                .thenByDescending { progressMap[it.id]?.missCount ?: 0L }
                .thenBy { progressMap[it.id]?.masteryLevel ?: 0L }
                .thenByDescending { if (progressMap[it.id]?.bookmarked == true) 1 else 0 }
        )

        val selected = prioritized.take(questionCount.coerceAtMost(filteredPool.size))
        val typesCycle = listOf(
            QuizQuestionType.CLOZE,
            QuizQuestionType.REGISTER,
            QuizQuestionType.MEANING
        )

        return selected.mapIndexed { index, target ->
            val qType = typesCycle[index % typesCycle.size]
            buildQuestionForPhrase(target, allPhrases, qType, index)
        }
    }

    fun buildSinglePhraseMiniQuiz(
        target: PhraseEntry,
        allPhrases: List<PhraseEntry>
    ): List<QuizQuestion> {
        return listOf(
            buildQuestionForPhrase(target, allPhrases, QuizQuestionType.CLOZE, 0),
            buildQuestionForPhrase(target, allPhrases, QuizQuestionType.REGISTER, 1),
            buildQuestionForPhrase(target, allPhrases, QuizQuestionType.MEANING, 2)
        )
    }

    private fun buildQuestionForPhrase(
        target: PhraseEntry,
        allPhrases: List<PhraseEntry>,
        type: QuizQuestionType,
        idx: Int
    ): QuizQuestion {
        val distractors = allPhrases
            .filter { it.id != target.id }
            .sortedByDescending { if (it.category == target.category) 1 else 0 }
            .shuffled()
            .take(3)

        return when (type) {
            QuizQuestionType.CLOZE -> {
                val clozeSnippet = buildClozeDialogue(target)
                val optionEntries = (distractors + target).shuffled()
                val correctIndex = optionEntries.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "q_cloze_${target.id}_$idx",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.CLOZE,
                    prompt = "Which American expression naturally completes this conversation?",
                    contextSnippet = clozeSnippet,
                    options = optionEntries.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "'${target.term}' means: ${target.definition} ${target.nuanceSummary}"
                )
            }

            QuizQuestionType.REGISTER -> {
                val scenarioHint = target.nuanceSummary.ifBlank { target.definition }
                val optionEntries = (distractors + target).shuffled()
                val correctIndex = optionEntries.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "q_reg_${target.id}_$idx",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.REGISTER,
                    prompt = "Select the expression that best matches this ${target.register.label.lowercase(Locale.US)} nuance:",
                    contextSnippet = "\"$scenarioHint\"",
                    options = optionEntries.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "'${target.term}' (${target.register.label} • ${target.cefrLevel.name}): ${target.usageNotes.firstOrNull() ?: target.definition}"
                )
            }

            QuizQuestionType.MEANING -> {
                val optionDefs = (distractors.map { it.definition } + target.definition).shuffled()
                val correctIndex = optionDefs.indexOf(target.definition).coerceAtLeast(0)
                val synHint = target.synonyms.firstOrNull()?.let { "Closest synonym: '${it.term}'." }.orEmpty()
                QuizQuestion(
                    id = "q_mean_${target.id}_$idx",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.MEANING,
                    prompt = "What is the most accurate meaning of \"${target.term}\" in American English?",
                    contextSnippet = "Register: ${target.register.label} • ${target.category.label}",
                    options = optionDefs,
                    correctIndex = correctIndex,
                    explanation = "${target.definition} $synHint".trim()
                )
            }
        }
    }

    private fun buildClozeDialogue(entry: PhraseEntry): String {
        val baseTerm = entry.term
            .replace(Regex("\\([^)]*\\)"), "")
            .trim()
        val keywords = baseTerm.split(Regex("\\s+")).filter { it.length >= 3 }

        val lines = entry.dialogue.map { turn ->
            var masked = turn.line
            if (baseTerm.isNotBlank() && masked.contains(baseTerm, ignoreCase = true)) {
                masked = masked.replace(Regex(Regex.escape(baseTerm), RegexOption.IGNORE_CASE), "________")
            } else if (keywords.isNotEmpty()) {
                val regexPattern = keywords.joinToString(".{0,18}") { Regex.escape(it.take(maxOf(3, it.length - 2))) + "[a-zA-Z'-]*" }
                val match = Regex(regexPattern, RegexOption.IGNORE_CASE).find(masked)
                if (match != null) {
                    masked = masked.replaceRange(match.range, "________")
                }
            }
            "${turn.speaker}: \"$masked\""
        }

        val combined = lines.joinToString("\n")
        return if (combined.contains("________")) {
            combined
        } else {
            "Context: Someone wants to express \"${entry.definition.replaceFirstChar { it.lowercase(Locale.US) }}\"\nLine: \"Honestly, I think we should ________ here.\""
        }
    }
}
