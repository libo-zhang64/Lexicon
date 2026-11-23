package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PhraseEntry
import com.example.data.model.RecallRating
import com.example.domain.DueReviewItem
import com.example.domain.EbbinghausScheduler
import com.example.ui.components.AudioListenButton
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.LexiconUiState

private data class CompletedFlashcardOutcome(
    val phraseTerm: String,
    val rating: RecallRating,
    val nextInterval: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    uiState: LexiconUiState,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    onStartActiveRecallQuiz: (List<PhraseEntry>) -> Unit,
    onRateFlashcard: (phrase: PhraseEntry, rating: RecallRating, onScheduled: (String) -> Unit) -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onStartLessonFromEmpty: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboard = uiState.ebbinghaus
    var activeDeck by remember { mutableStateOf<List<DueReviewItem>?>(null) }
    var currentCardIdx by rememberSaveable { mutableIntStateOf(0) }
    var isCardFlipped by rememberSaveable { mutableStateOf(false) }
    var flashcardOutcomes by remember { mutableStateOf<List<CompletedFlashcardOutcome>>(emptyList()) }
    var showFlashcardSummary by rememberSaveable { mutableStateOf(false) }

    val exitFlashcards = {
        activeDeck = null
        currentCardIdx = 0
        isCardFlipped = false
        showFlashcardSummary = false
        flashcardOutcomes = emptyList()
    }

    if (activeDeck != null || showFlashcardSummary) {
        BackHandler(onBack = exitFlashcards)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            showFlashcardSummary -> {
                FlashcardSummaryView(
                    outcomes = flashcardOutcomes,
                    onDone = exitFlashcards
                )
            }

            activeDeck != null -> {
                val deck = activeDeck!!
                val safeIdx = currentCardIdx.coerceIn(0, deck.lastIndex)
                val currentItem = deck[safeIdx]
                FlashcardSessionView(
                    item = currentItem,
                    index = safeIdx,
                    total = deck.size,
                    isFlipped = isCardFlipped,
                    speakingItemId = speakingItemId,
                    isSlowAudio = isSlowAudio,
                    onFlip = { isCardFlipped = !isCardFlipped },
                    onSpeak = onSpeak,
                    onToggleSpeed = onToggleSpeed,
                    onRate = { rating ->
                        val previewSchedule = EbbinghausScheduler.computeNextSchedule(
                            existing = currentItem.progress,
                            rating = rating
                        )
                        onRateFlashcard(currentItem.phrase, rating) { _ -> }
                        flashcardOutcomes = flashcardOutcomes + CompletedFlashcardOutcome(
                            phraseTerm = currentItem.phrase.term,
                            rating = rating,
                            nextInterval = previewSchedule.nextIntervalLabel
                        )
                        isCardFlipped = false
                        if (safeIdx < deck.lastIndex) {
                            currentCardIdx = safeIdx + 1
                        } else {
                            activeDeck = null
                            showFlashcardSummary = true
                        }
                    },
                    onExit = exitFlashcards
                )
            }

            else -> {
                ReviewDashboardView(
                    uiState = uiState,
                    speakingItemId = speakingItemId,
                    onSpeak = onSpeak,
                    onStartFlashcards = { items ->
                        if (items.isNotEmpty()) {
                            activeDeck = items.take(10)
                            currentCardIdx = 0
                            isCardFlipped = false
                            flashcardOutcomes = emptyList()
                            showFlashcardSummary = false
                        }
                    },
                    onStartActiveRecallQuiz = onStartActiveRecallQuiz,
                    onOpenPhrase = onOpenPhrase,
                    onStartLessonFromEmpty = onStartLessonFromEmpty
                )
            }
        }
    }
}

@Composable
private fun ReviewDashboardView(
    uiState: LexiconUiState,
    speakingItemId: String?,
    onSpeak: (itemId: String, text: String) -> Unit,
    onStartFlashcards: (List<DueReviewItem>) -> Unit,
    onStartActiveRecallQuiz: (List<PhraseEntry>) -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onStartLessonFromEmpty: () -> Unit
) {
    val dash = uiState.ebbinghaus
    val reviewPool = if (dash.dueItems.isNotEmpty()) dash.dueItems else dash.upcomingItems

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "review_header") {
            EditorialSectionHeader(
                eyebrow = "Ebbinghaus Forgetting Curve",
                title = "Spaced Review"
            )
        }

        // Empty state when user has not studied any words yet
        if (dash.totalTrackedCount == 0) {
            item(key = "review_empty_state") {
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_empty_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoGraph,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR MEMORY CURVE STARTS HERE",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Complete your first lesson",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Every word or phrase you study is automatically scheduled along the Ebbinghaus forgetting curve (1d → 3d → 7d → 14d → 30d → 60d) so you review right before memory fades.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onStartLessonFromEmpty,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("review_start_first_lesson_button")
                    ) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start a 5-Phrase Lesson")
                    }
                }
            }
            item(key = "curve_preview_empty") {
                EbbinghausCurveCard(
                    averageRetention = 100,
                    dueNow = 0,
                    dueTomorrow = 0,
                    dueNext7Days = 0,
                    dueOrLapsed = 0,
                    shortTerm = 0,
                    consolidating = 0,
                    longTerm = 0
                )
            }
            return@LazyColumn
        }

        // Primary Due Review Action Card
        item(key = "review_hero_card") {
            val hasDue = dash.dueNowCount > 0
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (hasDue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (hasDue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
                shadowElevation = if (hasDue) 2.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = (if (hasDue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                                .copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (hasDue) "DUE ON MEMORY CURVE" else "CAUGHT UP FOR NOW",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Text(
                            text = "${dash.averageRetentionPercent}% avg recall",
                            fontFamily = PhoneticMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Text(
                        text = if (hasDue) {
                            "${dash.dueNowCount} expression${if (dash.dueNowCount == 1) "" else "s"} ready to reinforce"
                        } else {
                            "All ${dash.totalTrackedCount} learned expressions are fresh"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = if (hasDue) {
                            "Test yourself with an active-recall quiz or flip flashcards to push each word to its next Ebbinghaus interval."
                        } else {
                            "Next scheduled review: ${dash.nextDueLabel}. Want extra reps? Run an early check anytime."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = (if (hasDue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer)
                            .copy(alpha = 0.88f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartActiveRecallQuiz(reviewPool.map { it.phrase }) },
                            colors = if (hasDue) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .testTag("review_start_recall_quiz_button")
                        ) {
                            Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (hasDue) "Recall Quiz" else "Early Quiz", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onStartFlashcards(reviewPool) },
                            border = BorderStroke(
                                1.2.dp,
                                (if (hasDue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer)
                                    .copy(alpha = 0.65f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (hasDue) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .testTag("review_start_flashcards_button")
                        ) {
                            Icon(imageVector = Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (hasDue) "Flashcards" else "Early Cards", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Ebbinghaus Forgetting Curve Visualization + Memory Strength Buckets
        item(key = "ebbinghaus_curve_card") {
            EbbinghausCurveCard(
                averageRetention = dash.averageRetentionPercent,
                dueNow = dash.dueNowCount,
                dueTomorrow = dash.dueTomorrowCount,
                dueNext7Days = dash.dueNext7DaysCount,
                dueOrLapsed = dash.dueOrLapsedCount,
                shortTerm = dash.shortTermCount,
                consolidating = dash.consolidatingCount,
                longTerm = dash.longTermCount
            )
        }

        // Due Now Queue
        if (dash.dueItems.isNotEmpty()) {
            item(key = "due_queue_header") {
                EditorialSectionHeader(
                    eyebrow = "Sorted by Lowest Retention First",
                    title = "Due Now (${dash.dueItems.size})"
                )
            }

            items(dash.dueItems.take(15), key = { "due_${it.phrase.id}" }) { item ->
                ReviewQueueRowCard(
                    item = item,
                    isSpeaking = speakingItemId == "rev_${item.phrase.id}",
                    onSpeak = { onSpeak("rev_${item.phrase.id}", item.phrase.term) },
                    onReviewSingleCard = { onStartFlashcards(listOf(item)) },
                    onOpenDetail = { onOpenPhrase(item.phrase) }
                )
            }
        }

        // Upcoming Schedule Preview
        if (dash.upcomingItems.isNotEmpty()) {
            item(key = "upcoming_header") {
                EditorialSectionHeader(
                    eyebrow = "Scheduled on Your Curve",
                    title = "Upcoming Reviews (${dash.upcomingItems.size})"
                )
            }

            items(dash.upcomingItems.take(10), key = { "up_${it.phrase.id}" }) { item ->
                ReviewQueueRowCard(
                    item = item,
                    isSpeaking = speakingItemId == "rev_${item.phrase.id}",
                    onSpeak = { onSpeak("rev_${item.phrase.id}", item.phrase.term) },
                    onReviewSingleCard = { onStartFlashcards(listOf(item)) },
                    onOpenDetail = { onOpenPhrase(item.phrase) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun EbbinghausCurveCard(
    averageRetention: Int,
    dueNow: Int,
    dueTomorrow: Int,
    dueNext7Days: Int,
    dueOrLapsed: Int,
    shortTerm: Int,
    consolidating: Int,
    longTerm: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ebbinghaus_curve_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MEMORY RETENTION MODEL",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Forgetting Curve & Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            EditorialTag(
                text = "1d→3d→7d→14d→30d→60d",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas plotting Ebbinghaus decay (unreviewed vs spaced-review reinforcement)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            val leftPad = 14.dp.toPx()
            val rightPad = 12.dp.toPx()
            val topPad = 10.dp.toPx()
            val bottomPad = 18.dp.toPx()
            val w = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val h = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            // Horizontal retention gridlines (100%, 70%, 40%)
            listOf(0f, 0.3f, 0.6f, 1f).forEach { frac ->
                val y = topPad + h * frac
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (frac > 0f && frac < 1f) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
                )
            }

            // Unreviewed decay curve (Stage 1 steep drop) in dashed warm amber
            val rawCurve = EbbinghausScheduler.retentionCurvePoints(stage = 1, maxDays = 30)
            val rawPath = Path().apply {
                rawCurve.forEachIndexed { i, (xF, yF) ->
                    val px = leftPad + xF * w
                    val py = topPad + (1f - yF) * h
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
            }
            drawPath(
                path = rawPath,
                color = secondaryColor.copy(alpha = 0.7f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            )

            // Spaced-repetition sawtooth reinforcement curve (1d, 3d, 7d, 14d, 30d reviews) in vivid cobalt/emerald
            val reviewCheckpoints = listOf(0, 1, 3, 7, 14, 30)
            val spacedPath = Path()
            var started = false
            for (seg in 0 until reviewCheckpoints.lastIndex) {
                val startDay = reviewCheckpoints[seg]
                val endDay = reviewCheckpoints[seg + 1]
                val stageStability = listOf(1.5, 4.3, 10.0, 20.5, 43.0)[seg]
                val steps = 12
                for (s in 0..steps) {
                    val day = startDay + (endDay - startDay) * (s.toFloat() / steps.toFloat())
                    val elapsed = day - startDay
                    val r = kotlin.math.exp(-elapsed / stageStability).toFloat().coerceIn(0.1f, 1f)
                    val px = leftPad + (day / 30f) * w
                    val py = topPad + (1f - r) * h
                    if (!started) {
                        spacedPath.moveTo(px, py)
                        started = true
                    } else {
                        spacedPath.lineTo(px, py)
                    }
                }
                // Vertical recall boost back to ~98% at checkpoint
                if (endDay < 30) {
                    val boostX = leftPad + (endDay / 30f) * w
                    val boostY = topPad + (1f - 0.98f) * h
                    spacedPath.lineTo(boostX, boostY)
                    drawCircle(
                        color = tertiaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(boostX, boostY)
                    )
                }
            }
            drawPath(
                path = spacedPath,
                color = primaryColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurveLegendDot(color = primaryColor, label = "With Ebbinghaus Review")
            CurveLegendDot(color = secondaryColor, label = "Without Review")
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        // Forecast metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ForecastPill(
                label = "Due Now",
                value = "$dueNow",
                tint = if (dueNow > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                accent = if (dueNow > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            ForecastPill(
                label = "Tomorrow",
                value = "$dueTomorrow",
                tint = MaterialTheme.colorScheme.surfaceContainerLow,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ForecastPill(
                label = "Next 7d",
                value = "$dueNext7Days",
                tint = MaterialTheme.colorScheme.surfaceContainerLow,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            ForecastPill(
                label = "Recall",
                value = "$averageRetention%",
                tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        // Memory stage breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Due/Lapsed: $dueOrLapsed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "1–3d: $shortTerm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "7–14d: $consolidating",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "30–60d: $longTerm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun CurveLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ForecastPill(
    label: String,
    value: String,
    tint: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = PhoneticMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReviewQueueRowCard(
    item: DueReviewItem,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onReviewSingleCard: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val retentionColor = when {
        item.retentionPercent < 55 -> MaterialTheme.colorScheme.error
        item.retentionPercent < 80 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        borderColor = if (item.isDueNow) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant,
        onClick = onOpenDetail,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_item_${item.phrase.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditorialTag(
                    text = item.dueStatusLabel,
                    containerColor = if (item.isDueNow) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = if (item.isDueNow) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                EditorialTag(
                    text = EbbinghausScheduler.stageLabel(item.stage),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = "${item.retentionPercent}%",
                fontFamily = PhoneticMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                color = retentionColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.phrase.term,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.phrase.definition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce ${item.phrase.term}",
                        tint = if (isSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onReviewSingleCard,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Review ${item.phrase.term} flashcard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardSessionView(
    item: DueReviewItem,
    index: Int,
    total: Int,
    isFlipped: Boolean,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onFlip: () -> Unit,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    onRate: (RecallRating) -> Unit,
    onExit: () -> Unit
) {
    val phrase = item.phrase
    val progress by animateFloatAsState(
        targetValue = (index + 1).toFloat() / total.toFloat(),
        animationSpec = tween(250),
        label = "flashcard_progress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EBBINGHAUS FLASHCARD CHECK",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Card ${index + 1} of $total  •  ${EbbinghausScheduler.stageLabel(item.stage)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.testTag("flashcard_exit_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Exit flashcard review")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            EditorialProgressBar(
                fraction = progress,
                fillColor = MaterialTheme.colorScheme.primary,
                description = "Flashcard ${index + 1} of $total"
            )
        }

        // Tactile Flip Card
        item {
            EditorialCard(
                containerColor = if (isFlipped) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                borderColor = if (isFlipped) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                onClick = onFlip,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 290.dp)
                    .testTag("flashcard_surface")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditorialTag(text = phrase.category.shortLabel)
                        EditorialTag(text = phrase.cefrLevel.name)
                    }
                    EditorialTag(
                        text = "Est. recall ${item.retentionPercent}%",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = isFlipped,
                    label = "flashcard_flip_content"
                ) { revealed ->
                    if (!revealed) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = phrase.term,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (phrase.ipa.isNotBlank()) {
                                Text(
                                    text = phrase.ipa,
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AudioListenButton(
                                isSpeaking = speakingItemId == "fc_${phrase.id}",
                                isSlow = isSlowAudio,
                                label = "Pronounce",
                                onSpeakClick = { onSpeak("fc_${phrase.id}", phrase.term) },
                                onToggleSpeed = onToggleSpeed
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Recall meaning & usage, then tap to flip",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = phrase.term,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = phrase.definition,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (phrase.nuanceSummary.isNotBlank()) {
                                Text(
                                    text = phrase.nuanceSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val sampleTurn = phrase.dialogue.firstOrNull {
                                it.line.contains(phrase.term.substringBefore("(").trim(), ignoreCase = true)
                            } ?: phrase.dialogue.getOrNull(1) ?: phrase.dialogue.firstOrNull()
                            if (sampleTurn != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${sampleTurn.speaker} in conversation:",
                                            fontFamily = PhoneticMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\"${sampleTurn.line}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            AudioListenButton(
                                isSpeaking = speakingItemId == "fc_rev_${phrase.id}",
                                isSlow = isSlowAudio,
                                label = "Hear Example",
                                onSpeakClick = {
                                    onSpeak(
                                        "fc_rev_${phrase.id}",
                                        "${phrase.term}. ${sampleTurn?.line.orEmpty()}"
                                    )
                                },
                                onToggleSpeed = onToggleSpeed
                            )
                        }
                    }
                }
            }
        }

        // Flip or Self-Rate Controls
        item {
            if (!isFlipped) {
                Button(
                    onClick = onFlip,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("flashcard_reveal_button")
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reveal Answer & Self-Check", fontWeight = FontWeight.Bold)
                }
            } else {
                AnimatedVisibility(visible = isFlipped) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "HOW WELL DID YOU RECALL THIS?",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecallRatingButton(
                                rating = RecallRating.FORGOT,
                                nextLabel = EbbinghausScheduler.computeNextSchedule(item.progress, RecallRating.FORGOT).nextIntervalLabel,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                onClick = { onRate(RecallRating.FORGOT) },
                                modifier = Modifier.weight(1f)
                            )
                            RecallRatingButton(
                                rating = RecallRating.HARD,
                                nextLabel = EbbinghausScheduler.computeNextSchedule(item.progress, RecallRating.HARD).nextIntervalLabel,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = { onRate(RecallRating.HARD) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecallRatingButton(
                                rating = RecallRating.GOOD,
                                nextLabel = EbbinghausScheduler.computeNextSchedule(item.progress, RecallRating.GOOD).nextIntervalLabel,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = { onRate(RecallRating.GOOD) },
                                modifier = Modifier.weight(1f)
                            )
                            RecallRatingButton(
                                rating = RecallRating.EASY,
                                nextLabel = EbbinghausScheduler.computeNextSchedule(item.progress, RecallRating.EASY).nextIntervalLabel,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = { onRate(RecallRating.EASY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecallRatingButton(
    rating: RecallRating,
    nextLabel: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .heightIn(min = 60.dp)
            .testTag("flashcard_rate_${rating.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = rating.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Next: $nextLabel",
                fontFamily = PhoneticMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun FlashcardSummaryView(
    outcomes: List<CompletedFlashcardOutcome>,
    onDone: () -> Unit
) {
    val remembered = outcomes.count { it.rating != RecallRating.FORGOT }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            EditorialCard(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_summary_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "EBBINGHAUS REVIEW SAVED",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$remembered / ${outcomes.size} Recalled",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Every expression's next review interval has been updated along your forgetting curve and synced to Firestore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        item {
            EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Text(
                    text = "UPDATED INTERVALS",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                outcomes.forEachIndexed { idx, out ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = out.phraseTerm,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Rated: ${out.rating.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = out.nextInterval,
                                fontFamily = PhoneticMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (idx < outcomes.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("flashcard_summary_done_button")
            ) {
                Text("Back to Review Hub")
            }
        }
    }
}
