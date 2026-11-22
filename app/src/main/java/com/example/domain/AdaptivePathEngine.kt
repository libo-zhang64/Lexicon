package com.example.domain

import com.example.data.model.CategoryFilter
import com.example.data.model.GoalCadence
import com.example.data.model.GoalMetricType
import com.example.data.model.LearningGoalDoc
import com.example.data.model.PeriodKeys
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizMode
import com.example.data.model.QuizQuestionType
import com.example.data.model.QuizSessionDoc

data class GoalProgressView(
    val goal: LearningGoalDoc,
    val currentCount: Int,
    val targetCount: Int,
    val fraction: Float,
    val isCompleted: Boolean,
    val periodSubtitle: String
)

data class CategoryPerformance(
    val category: PhraseCategory,
    val totalInCatalog: Int,
    val lessonsCompleted: Int,
    val masteredCount: Int,
    val attempts: Int,
    val misses: Int,
    val accuracyPercent: Int?
)

data class SkillMistakeArea(
    val questionType: QuizQuestionType,
    val missCount: Int,
    val coachingTip: String
)

data class MissedPhraseStat(
    val phrase: PhraseEntry,
    val missCount: Int,
    val masteryLevel: Int
)

sealed interface ReviewAction {
    data class OpenPhrase(val phraseId: String) : ReviewAction
    data class LaunchTargetedQuiz(
        val mode: QuizMode,
        val category: CategoryFilter,
        val focusPhraseIds: List<String>
    ) : ReviewAction
}

data class ReviewRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val actionLabel: String,
    val action: ReviewAction
)

data class LearningPathInsights(
    val activeGoals: List<GoalProgressView>,
    val categoryPerformance: List<CategoryPerformance>,
    val skillMistakeAreas: List<SkillMistakeArea>,
    val frequentMistakePhrases: List<MissedPhraseStat>,
    val recommendations: List<ReviewRecommendation>,
    val totalLessonsCompleted: Int,
    val totalMastered: Int,
    val averageQuizScorePercent: Int?,
    val totalQuizzesCompleted: Int
)

object AdaptivePathEngine {

    fun computeInsights(
        allPhrases: List<PhraseEntry>,
        goals: List<LearningGoalDoc>,
        progressMap: Map<String, PhraseProgressDoc>,
        sessions: List<QuizSessionDoc>,
        todayDay: String = PeriodKeys.todayDayKey(),
        currentWeek: String = PeriodKeys.currentWeekKey()
    ): LearningPathInsights {
        val phraseById = allPhrases.associateBy { it.id }

        // 1. Evaluate active daily/weekly goals deterministically for current period
        val goalViews = goals.filter { it.active }.map { goal ->
            val count = computeGoalCurrentCount(goal, progressMap, sessions, todayDay, currentWeek)
            val target = goal.targetCount.toInt().coerceAtLeast(1)
            GoalProgressView(
                goal = goal,
                currentCount = count,
                targetCount = target,
                fraction = (count.toFloat() / target.toFloat()).coerceIn(0f, 1f),
                isCompleted = count >= target,
                periodSubtitle = "${goal.cadenceEnum.periodLabel} • ${goal.categoryEnum.label}"
            )
        }

        // 2. Category performance breakdown
        val catPerf = PhraseCategory.entries.map { cat ->
            val catPhrases = allPhrases.filter { it.category == cat }
            val catProgress = catPhrases.mapNotNull { progressMap[it.id] }
            val lessons = catProgress.count { it.lessonCompleted }
            val mastered = catProgress.count { it.masteryLevel >= 3L }
            val attempts = catProgress.sumOf { it.attemptCount.toInt() }
            val correct = catProgress.sumOf { it.correctCount.toInt() }
            val misses = catProgress.sumOf { it.missCount.toInt() }
            val accuracy = if (attempts > 0) ((correct * 100f) / attempts).toInt().coerceIn(0, 100) else null
            CategoryPerformance(
                category = cat,
                totalInCatalog = catPhrases.size,
                lessonsCompleted = lessons,
                masteredCount = mastered,
                attempts = attempts,
                misses = misses,
                accuracyPercent = accuracy
            )
        }

        // 3. Mistake patterns by skill type
        val clozeMisses = progressMap.values.sumOf { it.missedClozeCount.toInt() }
        val registerMisses = progressMap.values.sumOf { it.missedRegisterCount.toInt() }
        val meaningMisses = progressMap.values.sumOf { it.missedMeaningCount.toInt() }
        val skillAreas = listOf(
            SkillMistakeArea(
                questionType = QuizQuestionType.CLOZE,
                missCount = clozeMisses,
                coachingTip = "Focus on how phrases fit naturally inside spoken multi-turn dialogues."
            ),
            SkillMistakeArea(
                questionType = QuizQuestionType.REGISTER,
                missCount = registerMisses,
                coachingTip = "Check formality & US workplace nuance notes before choosing between casual and executive phrasing."
            ),
            SkillMistakeArea(
                questionType = QuizQuestionType.MEANING,
                missCount = meaningMisses,
                coachingTip = "Compare near-synonyms and contrast pairs in each entry's Nuance section."
            )
        ).filter { it.missCount > 0 }.sortedByDescending { it.missCount }

        // 4. Most frequently missed phrases
        val missedPhrases = progressMap.values
            .filter { it.missCount > 0L }
            .sortedWith(compareByDescending<PhraseProgressDoc> { it.missCount }.thenBy { it.masteryLevel })
            .mapNotNull { prog ->
                val entry = phraseById[prog.phraseId] ?: return@mapNotNull null
                MissedPhraseStat(
                    phrase = entry,
                    missCount = prog.missCount.toInt(),
                    masteryLevel = prog.masteryLevel.toInt()
                )
            }
            .take(5)

        // 5. Build prioritized review recommendations
        val recommendations = buildList {
            // Priority A: High-miss phrases targeted drill
            if (missedPhrases.isNotEmpty()) {
                val topTerms = missedPhrases.take(2).joinToString(" & ") { "'${it.phrase.term}'" }
                add(
                    ReviewRecommendation(
                        id = "rec_weak_phrases",
                        title = "Reinforce Missed Expressions",
                        subtitle = "You've missed $topTerms recently. A quick 5-question drill will lock them in.",
                        badge = "Weak Spot",
                        actionLabel = "Practice Weak Spots",
                        action = ReviewAction.LaunchTargetedQuiz(
                            mode = QuizMode.SMART_REVIEW,
                            category = CategoryFilter.ALL,
                            focusPhraseIds = missedPhrases.map { it.phrase.id }
                        )
                    )
                )
            }

            // Priority B: Lowest accuracy category with misses
            val weakestCategory = catPerf
                .filter { it.attempts >= 2 && (it.accuracyPercent ?: 100) < 85 }
                .minByOrNull { it.accuracyPercent ?: 100 }
            if (weakestCategory != null) {
                val focusIds = allPhrases
                    .filter { it.category == weakestCategory.category }
                    .sortedByDescending { progressMap[it.id]?.missCount ?: 0L }
                    .take(5)
                    .map { it.id }
                add(
                    ReviewRecommendation(
                        id = "rec_weak_cat_${weakestCategory.category.name}",
                        title = "Sharpen ${weakestCategory.category.label}",
                        subtitle = "${weakestCategory.accuracyPercent}% quiz accuracy so far (${weakestCategory.misses} misses). Review register & dialogues.",
                        badge = "Needs Review",
                        actionLabel = "Drill ${weakestCategory.category.shortLabel}",
                        action = ReviewAction.LaunchTargetedQuiz(
                            mode = QuizMode.CATEGORY_DRILL,
                            category = CategoryFilter.fromCategory(weakestCategory.category),
                            focusPhraseIds = focusIds
                        )
                    )
                )
            }

            // Priority C: Incomplete active goal nudge
            val incompleteGoal = goalViews.firstOrNull { !it.isCompleted }
            if (incompleteGoal != null) {
                val remaining = (incompleteGoal.targetCount - incompleteGoal.currentCount).coerceAtLeast(1)
                val catFilter = incompleteGoal.goal.categoryEnum
                val candidatePhrase = allPhrases.firstOrNull { entry ->
                    catFilter.matches(entry.category) && (progressMap[entry.id]?.lessonCompleted != true)
                }
                if (incompleteGoal.goal.metricEnum == GoalMetricType.LEARN_NEW && candidatePhrase != null) {
                    add(
                        ReviewRecommendation(
                            id = "rec_goal_${incompleteGoal.goal.id}",
                            title = "Advance Goal: ${incompleteGoal.goal.title}",
                            subtitle = "$remaining more to finish ${incompleteGoal.goal.cadenceEnum.periodLabel.lowercase()}. Start with '${candidatePhrase.term}'.",
                            badge = "Goal Step",
                            actionLabel = "Read '${candidatePhrase.term}'",
                            action = ReviewAction.OpenPhrase(candidatePhrase.id)
                        )
                    )
                } else {
                    val focusIds = allPhrases
                        .filter { catFilter.matches(it.category) }
                        .sortedBy { progressMap[it.id]?.masteryLevel ?: 0L }
                        .take(5)
                        .map { it.id }
                    add(
                        ReviewRecommendation(
                            id = "rec_goal_quiz_${incompleteGoal.goal.id}",
                            title = "Advance Goal: ${incompleteGoal.goal.title}",
                            subtitle = "$remaining left for ${incompleteGoal.goal.cadenceEnum.periodLabel.lowercase()}. Run a targeted drill to make progress.",
                            badge = "Goal Drill",
                            actionLabel = "Start Goal Drill",
                            action = ReviewAction.LaunchTargetedQuiz(
                                mode = QuizMode.GOAL_DRILL,
                                category = catFilter,
                                focusPhraseIds = focusIds
                            )
                        )
                    )
                }
            }

            // Priority D: Stale or unmastered bookmarked phrase
            val reviewCandidate = allPhrases.firstOrNull { entry ->
                val p = progressMap[entry.id]
                p != null && p.masteryLevel in 1L..2L && PeriodKeys.daysBetween(p.lastReviewedDay, todayDay) >= 3
            } ?: allPhrases.firstOrNull { entry ->
                val p = progressMap[entry.id]
                p?.bookmarked == true && p.masteryLevel < 3L
            }
            if (reviewCandidate != null) {
                add(
                    ReviewRecommendation(
                        id = "rec_refresh_${reviewCandidate.id}",
                        title = "Refresh '${reviewCandidate.term}'",
                        subtitle = "Revisit its dialogue & etymology to move it closer to Mastered.",
                        badge = "Spaced Review",
                        actionLabel = "Open Lesson",
                        action = ReviewAction.OpenPhrase(reviewCandidate.id)
                    )
                )
            }

            // Fallback starter recommendation for brand-new learners
            if (isEmpty()) {
                val starter = allPhrases.firstOrNull { it.id == "table_a_discussion" } ?: allPhrases.firstOrNull()
                if (starter != null) {
                    add(
                        ReviewRecommendation(
                            id = "rec_starter",
                            title = "Start with a Classic US Workplace Trap",
                            subtitle = "Learn why '${starter.term}' means the opposite in American vs. British meetings.",
                            badge = "Recommended",
                            actionLabel = "Open Lesson",
                            action = ReviewAction.OpenPhrase(starter.id)
                        )
                    )
                }
            }
        }.take(5)

        val totalLessons = progressMap.values.count { it.lessonCompleted }
        val totalMastered = progressMap.values.count { it.masteryLevel >= 3L }
        val totalQ = sessions.sumOf { it.totalQuestions.toInt() }
        val totalCorrect = sessions.sumOf { it.score.toInt() }
        val avgScore = if (totalQ > 0) ((totalCorrect * 100f) / totalQ).toInt().coerceIn(0, 100) else null

        return LearningPathInsights(
            activeGoals = goalViews,
            categoryPerformance = catPerf,
            skillMistakeAreas = skillAreas,
            frequentMistakePhrases = missedPhrases,
            recommendations = recommendations,
            totalLessonsCompleted = totalLessons,
            totalMastered = totalMastered,
            averageQuizScorePercent = avgScore,
            totalQuizzesCompleted = sessions.size
        )
    }

    private fun computeGoalCurrentCount(
        goal: LearningGoalDoc,
        progressMap: Map<String, PhraseProgressDoc>,
        sessions: List<QuizSessionDoc>,
        todayDay: String,
        currentWeek: String
    ): Int {
        val catFilter = goal.categoryEnum
        val isDaily = goal.cadenceEnum == GoalCadence.DAILY
        return when (goal.metricEnum) {
            GoalMetricType.LEARN_NEW -> progressMap.values.count { p ->
                p.lessonCompleted &&
                    catFilter.matches(p.categoryEnum) &&
                    if (isDaily) p.lessonCompletedDay == todayDay else p.lessonCompletedWeek == currentWeek
            }
            GoalMetricType.MASTER_PHRASES -> progressMap.values.count { p ->
                p.masteryLevel >= 3L &&
                    catFilter.matches(p.categoryEnum) &&
                    if (isDaily) p.masteredDay == todayDay else p.masteredWeek == currentWeek
            }
            GoalMetricType.COMPLETE_QUIZZES -> sessions.count { s ->
                val sCat = CategoryFilter.fromRaw(s.category)
                val matchesCat = catFilter == CategoryFilter.ALL || sCat == catFilter
                val matchesPeriod = if (isDaily) s.periodDay == todayDay else s.periodWeek == currentWeek
                matchesCat && matchesPeriod
            }
        }
    }
}
