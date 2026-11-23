package com.example.domain

import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.RecallRating
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class EbbinghausScheduleUpdate(
    val stage: Long,
    val intervalDays: Long,
    val lastReviewAtMillis: Long,
    val nextReviewAtMillis: Long,
    val reviewCount: Long,
    val lapseCount: Long,
    val nextIntervalLabel: String
)

data class DueReviewItem(
    val phrase: PhraseEntry,
    val progress: PhraseProgressDoc,
    val stage: Int,
    val intervalDays: Int,
    val retentionPercent: Int,
    val isDueNow: Boolean,
    val overdueHours: Long,
    val nextReviewAtMillis: Long,
    val dueStatusLabel: String
)

data class EbbinghausDashboard(
    val dueItems: List<DueReviewItem>,
    val upcomingItems: List<DueReviewItem>,
    val totalTrackedCount: Int,
    val dueNowCount: Int,
    val dueTomorrowCount: Int,
    val dueNext7DaysCount: Int,
    val averageRetentionPercent: Int,
    val dueOrLapsedCount: Int,
    val shortTermCount: Int,
    val consolidatingCount: Int,
    val longTermCount: Int,
    val nextDueLabel: String
)

object EbbinghausScheduler {
    const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val HOUR_MILLIS = 60L * 60L * 1000L

    // Classic Ebbinghaus spaced-repetition interval ladder in days:
    // Stage 0 = New / Lapsed (recheck soon), Stage 1 = 1d, 2 = 3d, 3 = 7d, 4 = 14d, 5 = 30d, 6 = 60d
    val STAGE_INTERVAL_DAYS = intArrayOf(0, 1, 3, 7, 14, 30, 60)

    // Stability S (in days) for R(t) = e^(-t / S) at each stage
    private val STAGE_STABILITY_DAYS = doubleArrayOf(0.55, 1.45, 4.3, 10.0, 20.5, 43.0, 88.0)

    fun initialEnrollment(nowMillis: Long = System.currentTimeMillis()): EbbinghausScheduleUpdate {
        val nextAt = nowMillis + STAGE_INTERVAL_DAYS[1] * DAY_MILLIS
        return EbbinghausScheduleUpdate(
            stage = 1L,
            intervalDays = 1L,
            lastReviewAtMillis = nowMillis,
            nextReviewAtMillis = nextAt,
            reviewCount = 0L,
            lapseCount = 0L,
            nextIntervalLabel = "1 day"
        )
    }

    fun computeNextSchedule(
        existing: PhraseProgressDoc?,
        rating: RecallRating,
        nowMillis: Long = System.currentTimeMillis()
    ): EbbinghausScheduleUpdate {
        val currentStage = inferEffectiveStage(existing).coerceIn(0, STAGE_INTERVAL_DAYS.lastIndex)
        val prevReviewCount = existing?.reviewCount ?: 0L
        val prevLapseCount = existing?.lapseCount ?: 0L

        return when (rating) {
            RecallRating.FORGOT -> {
                // Reset back to stage 0 so the forgetting curve gets reinforced within hours
                val recheckAt = nowMillis + 6L * HOUR_MILLIS
                EbbinghausScheduleUpdate(
                    stage = 0L,
                    intervalDays = 1L,
                    lastReviewAtMillis = nowMillis,
                    nextReviewAtMillis = recheckAt,
                    reviewCount = prevReviewCount + 1L,
                    lapseCount = prevLapseCount + 1L,
                    nextIntervalLabel = "6 hours"
                )
            }

            RecallRating.HARD -> {
                val nextStage = max(1, currentStage)
                val baseDays = STAGE_INTERVAL_DAYS[nextStage]
                val days = max(1, (baseDays * 0.7).roundToInt())
                EbbinghausScheduleUpdate(
                    stage = nextStage.toLong(),
                    intervalDays = days.toLong(),
                    lastReviewAtMillis = nowMillis,
                    nextReviewAtMillis = nowMillis + days * DAY_MILLIS,
                    reviewCount = prevReviewCount + 1L,
                    lapseCount = prevLapseCount,
                    nextIntervalLabel = formatIntervalDays(days)
                )
            }

            RecallRating.GOOD -> {
                val nextStage = min(STAGE_INTERVAL_DAYS.lastIndex, currentStage + 1)
                val days = max(1, STAGE_INTERVAL_DAYS[nextStage])
                EbbinghausScheduleUpdate(
                    stage = nextStage.toLong(),
                    intervalDays = days.toLong(),
                    lastReviewAtMillis = nowMillis,
                    nextReviewAtMillis = nowMillis + days * DAY_MILLIS,
                    reviewCount = prevReviewCount + 1L,
                    lapseCount = prevLapseCount,
                    nextIntervalLabel = formatIntervalDays(days)
                )
            }

            RecallRating.EASY -> {
                val nextStage = min(STAGE_INTERVAL_DAYS.lastIndex, currentStage + 2)
                val days = max(2, STAGE_INTERVAL_DAYS[nextStage])
                EbbinghausScheduleUpdate(
                    stage = nextStage.toLong(),
                    intervalDays = days.toLong(),
                    lastReviewAtMillis = nowMillis,
                    nextReviewAtMillis = nowMillis + days * DAY_MILLIS,
                    reviewCount = prevReviewCount + 1L,
                    lapseCount = prevLapseCount,
                    nextIntervalLabel = formatIntervalDays(days)
                )
            }
        }
    }

    fun computeDashboard(
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        nowMillis: Long = System.currentTimeMillis()
    ): EbbinghausDashboard {
        if (allPhrases.isEmpty() || progressMap.isEmpty()) {
            return EbbinghausDashboard(
                dueItems = emptyList(),
                upcomingItems = emptyList(),
                totalTrackedCount = 0,
                dueNowCount = 0,
                dueTomorrowCount = 0,
                dueNext7DaysCount = 0,
                averageRetentionPercent = 100,
                dueOrLapsedCount = 0,
                shortTermCount = 0,
                consolidatingCount = 0,
                longTermCount = 0,
                nextDueLabel = "Learn a lesson to start"
            )
        }

        val phraseById = allPhrases.associateBy { it.id }
        val trackedItems = mutableListOf<DueReviewItem>()

        for ((phraseId, prog) in progressMap) {
            // Enroll any phrase that has been studied in a lesson or tested in a quiz
            val isLearned = prog.lessonCompleted || prog.attemptCount > 0L || prog.reviewCount > 0L
            if (!isLearned) continue
            val phrase = phraseById[phraseId] ?: continue

            val stage = inferEffectiveStage(prog)
            val intervalDays = inferEffectiveIntervalDays(prog, stage)
            val lastTouchMs = inferLastTouchMillis(prog, nowMillis)
            val nextReviewMs = if (prog.nextReviewAtMillis > 0L) {
                prog.nextReviewAtMillis
            } else {
                lastTouchMs + max(1, intervalDays) * DAY_MILLIS
            }

            val elapsedDays = ((nowMillis - lastTouchMs).coerceAtLeast(0L)).toDouble() / DAY_MILLIS.toDouble()
            val stability = STAGE_STABILITY_DAYS[stage.coerceIn(0, STAGE_STABILITY_DAYS.lastIndex)]
            val retention = (exp(-elapsedDays / stability) * 100.0).roundToInt().coerceIn(5, 100)
            val isDueNow = nextReviewMs <= nowMillis
            val overdueHours = if (isDueNow) ((nowMillis - nextReviewMs) / HOUR_MILLIS).coerceAtLeast(0L) else 0L

            trackedItems.add(
                DueReviewItem(
                    phrase = phrase,
                    progress = prog,
                    stage = stage,
                    intervalDays = intervalDays,
                    retentionPercent = retention,
                    isDueNow = isDueNow,
                    overdueHours = overdueHours,
                    nextReviewAtMillis = nextReviewMs,
                    dueStatusLabel = formatDueStatus(nextReviewMs, nowMillis)
                )
            )
        }

        val dueItems = trackedItems
            .filter { it.isDueNow }
            .sortedWith(
                compareBy<DueReviewItem> { it.retentionPercent }
                    .thenByDescending { it.overdueHours }
                    .thenBy { it.stage }
            )

        val upcomingItems = trackedItems
            .filterNot { it.isDueNow }
            .sortedBy { it.nextReviewAtMillis }

        val endOfTomorrow = nowMillis + 2L * DAY_MILLIS
        val endOfWeek = nowMillis + 7L * DAY_MILLIS

        val dueTomorrow = upcomingItems.count { it.nextReviewAtMillis <= endOfTomorrow }
        val dueNext7Days = upcomingItems.count { it.nextReviewAtMillis <= endOfWeek }
        val avgRetention = if (trackedItems.isNotEmpty()) {
            trackedItems.map { it.retentionPercent }.average().roundToInt()
        } else {
            100
        }

        val dueOrLapsed = trackedItems.count { it.isDueNow || it.stage == 0 }
        val shortTerm = trackedItems.count { !it.isDueNow && it.stage in 1..2 }
        val consolidating = trackedItems.count { !it.isDueNow && it.stage in 3..4 }
        val longTerm = trackedItems.count { !it.isDueNow && it.stage >= 5 }

        val nextLabel = when {
            dueItems.isNotEmpty() -> "${dueItems.size} due now"
            upcomingItems.isNotEmpty() -> formatDueStatus(upcomingItems.first().nextReviewAtMillis, nowMillis)
            else -> "Complete a lesson to schedule"
        }

        return EbbinghausDashboard(
            dueItems = dueItems,
            upcomingItems = upcomingItems,
            totalTrackedCount = trackedItems.size,
            dueNowCount = dueItems.size,
            dueTomorrowCount = dueTomorrow,
            dueNext7DaysCount = dueNext7Days,
            averageRetentionPercent = avgRetention,
            dueOrLapsedCount = dueOrLapsed,
            shortTermCount = shortTerm,
            consolidatingCount = consolidating,
            longTermCount = longTerm,
            nextDueLabel = nextLabel
        )
    }

    fun retentionCurvePoints(stage: Int, maxDays: Int = 30): List<Pair<Float, Float>> {
        val stability = STAGE_STABILITY_DAYS[stage.coerceIn(0, STAGE_STABILITY_DAYS.lastIndex)]
        return (0..maxDays).map { day ->
            val r = exp(-day.toDouble() / stability).toFloat().coerceIn(0.05f, 1f)
            day.toFloat() / maxDays.toFloat() to r
        }
    }

    fun stageLabel(stage: Int): String = when (stage.coerceIn(0, 6)) {
        0 -> "Relearning"
        1 -> "Stage 1 • 1d"
        2 -> "Stage 2 • 3d"
        3 -> "Stage 3 • 7d"
        4 -> "Stage 4 • 14d"
        5 -> "Stage 5 • 30d"
        else -> "Stage 6 • 60d"
    }

    fun formatIntervalDays(days: Int): String = when {
        days <= 0 -> "Today"
        days == 1 -> "1 day"
        else -> "$days days"
    }

    private fun inferEffectiveStage(prog: PhraseProgressDoc?): Int {
        if (prog == null) return 0
        if (prog.ebbinghausStage > 0L || prog.reviewCount > 0L || prog.nextReviewAtMillis > 0L) {
            return prog.ebbinghausStage.toInt().coerceIn(0, STAGE_INTERVAL_DAYS.lastIndex)
        }
        return when (prog.masteryLevel.toInt()) {
            3 -> 3
            2 -> 2
            1 -> 1
            else -> if (prog.lessonCompleted) 1 else 0
        }
    }

    private fun inferEffectiveIntervalDays(prog: PhraseProgressDoc, stage: Int): Int {
        if (prog.intervalDays > 0L) return prog.intervalDays.toInt()
        return max(1, STAGE_INTERVAL_DAYS[stage.coerceIn(0, STAGE_INTERVAL_DAYS.lastIndex)])
    }

    private fun inferLastTouchMillis(prog: PhraseProgressDoc, nowMillis: Long): Long {
        if (prog.lastReviewAtMillis > 0L) return prog.lastReviewAtMillis
        prog.updatedAt?.toDate()?.time?.let { if (it > 0L) return it }
        val dayKey = prog.lastReviewedDay.ifBlank { prog.lessonCompletedDay }
        if (dayKey.isNotBlank()) {
            runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                fmt.parse(dayKey)?.time
            }.getOrNull()?.let { return it }
        }
        return nowMillis
    }

    private fun formatDueStatus(nextReviewAtMillis: Long, nowMillis: Long): String {
        val diff = nextReviewAtMillis - nowMillis
        if (diff <= 0L) {
            val overdueDays = (-diff / DAY_MILLIS).toInt()
            return when {
                overdueDays >= 2 -> "Overdue ${overdueDays}d"
                overdueDays == 1 -> "Overdue 1d"
                else -> "Due now"
            }
        }
        val hours = (diff / HOUR_MILLIS).toInt()
        val days = ((diff + DAY_MILLIS / 2) / DAY_MILLIS).toInt()
        return when {
            hours < 1 -> "In <1 hr"
            hours < 24 -> "In ${hours}h"
            days == 1 -> "Tomorrow"
            else -> "In ${days}d"
        }
    }
}
