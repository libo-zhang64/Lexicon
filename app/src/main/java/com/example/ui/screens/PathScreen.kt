package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryFilter
import com.example.data.model.GoalCadence
import com.example.data.model.GoalMetricType
import com.example.data.model.LearningGoalDoc
import com.example.data.model.PhraseEntry
import com.example.domain.GoalProgressView
import com.example.domain.ReviewRecommendation
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.LexiconUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PathScreen(
    uiState: LexiconUiState,
    speakingItemId: String?,
    onSpeak: (itemId: String, text: String) -> Unit,
    onCreateGoal: (String, GoalCadence, GoalMetricType, CategoryFilter, Int) -> Unit,
    onUpdateGoal: (String, String, GoalCadence, GoalMetricType, CategoryFilter, Int, Boolean) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onRecommendationClick: (ReviewRecommendation) -> Unit,
    onAiWeakSpotDrill: () -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onToggleBookmark: (PhraseEntry) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<LearningGoalDoc?>(null) }

    val insights = uiState.insights
    val savedPhrases = remember(uiState.bookmarkedPhrases, uiState.customPhrases) {
        (uiState.customPhrases + uiState.bookmarkedPhrases).distinctBy { it.id }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header + Account Row
            item(key = "path_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PERSONALIZED LEARNING PATH",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = uiState.profile.displayName.ifBlank { "Your Progress" },
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out of Google account",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Key Performance Metrics Grid
            item(key = "stats_grid") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            label = "Lessons Studied",
                            value = "${insights.totalLessonsCompleted}",
                            subtitle = "of ${uiState.allPhrases.size} entries",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Avg Quiz Score",
                            value = insights.averageQuizScorePercent?.let { "$it%" } ?: "—",
                            subtitle = "${insights.totalQuizzesCompleted} quiz(zes)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            label = "Mastered",
                            value = "${insights.totalMastered}",
                            subtitle = "2+ streak in quizzes",
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Daily Streak",
                            value = "${uiState.profile.currentStreak}d",
                            subtitle = "Best: ${uiState.profile.longestStreak}d",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Daily & Weekly Learning Goals
            item(key = "goals_section") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Daily & Weekly Targets",
                        title = "Learning Goals",
                        trailing = {
                            Button(
                                onClick = {
                                    editingGoal = null
                                    showGoalSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("add_goal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Goal")
                            }
                        }
                    )

                    if (insights.activeGoals.isEmpty()) {
                        EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Text(
                                text = "Start with a quick goal",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap a starter preset below or build your own daily or weekly study target.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                starterGoalPresets.forEach { preset ->
                                    AssistChip(
                                        onClick = {
                                            onCreateGoal(
                                                preset.title,
                                                preset.cadence,
                                                preset.metric,
                                                preset.category,
                                                preset.target
                                            )
                                        },
                                        label = { Text("+ ${preset.title}") },
                                        modifier = Modifier.heightIn(min = 42.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            insights.activeGoals.forEach { goalView ->
                                GoalProgressCard(
                                    goalView = goalView,
                                    onEdit = {
                                        editingGoal = goalView.goal
                                        showGoalSheet = true
                                    },
                                    onDelete = { onDeleteGoal(goalView.goal.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Suggested Review Topics (Adaptive Recommendations)
            item(key = "recommendations_section") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Performance-Driven",
                        title = "Suggested Review Topics"
                    )
                    insights.recommendations.forEach { rec ->
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            onClick = { onRecommendationClick(rec) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("path_rec_${rec.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorialTag(
                                    text = rec.badge,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = rec.actionLabel,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rec.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rec.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rec.actionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onAiWeakSpotDrill,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("path_ai_remediation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Drill for My Weak Spots")
                    }
                }
            }

            // 4. Category Mastery & Accuracy Breakdown
            item(key = "category_breakdown") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Track Analytics",
                        title = "Performance by Category"
                    )
                    EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            insights.categoryPerformance.forEachIndexed { idx, cat ->
                                val lessonFrac = if (cat.totalInCatalog > 0) {
                                    cat.lessonsCompleted.toFloat() / cat.totalInCatalog.toFloat()
                                } else 0f
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat.category.label,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = buildString {
                                                append("${cat.lessonsCompleted}/${cat.totalInCatalog} studied")
                                                if (cat.accuracyPercent != null) {
                                                    append("  •  ${cat.accuracyPercent}% quiz")
                                                }
                                            },
                                            fontFamily = PhoneticMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    EditorialProgressBar(
                                        fraction = lessonFrac,
                                        fillColor = if ((cat.accuracyPercent ?: 100) < 70) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        description = "${cat.category.label}: ${cat.lessonsCompleted} of ${cat.totalInCatalog} lessons studied"
                                    )
                                }
                                if (idx < insights.categoryPerformance.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Frequent Mistake Areas & Missed Phrases
            item(key = "mistake_areas") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Mistake Diagnostics",
                        title = "Areas Needing Attention"
                    )
                    if (insights.skillMistakeAreas.isEmpty() && insights.frequentMistakePhrases.isEmpty()) {
                        EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Text(
                                text = "No recurring mistakes logged yet.",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Complete a quiz in the Practice tab—Lexicon will pinpoint which skills (Context Cloze, Register & Nuance, or Meaning) and phrases need extra review.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                insights.skillMistakeAreas.forEach { area ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = area.questionType.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "${area.missCount} miss(es)",
                                                fontFamily = PhoneticMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = area.coachingTip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (insights.frequentMistakePhrases.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = "MOST-MISSED EXPRESSIONS",
                                        fontFamily = PhoneticMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    insights.frequentMistakePhrases.forEach { stat ->
                                        Surface(
                                            onClick = { onOpenPhrase(stat.phrase) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stat.phrase.term,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = stat.phrase.category.label,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                EditorialTag(
                                                    text = "${stat.missCount} misses",
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Saved & Custom AI Lexicon
            item(key = "saved_header") {
                EditorialSectionHeader(
                    eyebrow = "${savedPhrases.size} Saved",
                    title = "Saved & AI Lexicon"
                )
            }

            if (savedPhrases.isEmpty()) {
                item(key = "saved_empty") {
                    EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Text(
                            text = "Your personal lexicon is empty.",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bookmark phrases from Today or save custom lookups from Explore to build your personal American English notebook.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(savedPhrases, key = { "saved_${it.id}" }) { phrase ->
                    PhraseListRowCard(
                        phrase = phrase,
                        progress = uiState.progressMap[phrase.id],
                        isSpeaking = speakingItemId == "saved_${phrase.id}",
                        onSpeak = { onSpeak("saved_${phrase.id}", phrase.term) },
                        onToggleBookmark = { onToggleBookmark(phrase) },
                        onClick = { onOpenPhrase(phrase) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showGoalSheet) {
        GoalBuilderSheet(
            editingGoal = editingGoal,
            onDismiss = {
                showGoalSheet = false
                editingGoal = null
            },
            onSave = { title, cadence, metric, category, target ->
                val current = editingGoal
                if (current == null) {
                    onCreateGoal(title, cadence, metric, category, target)
                } else {
                    onUpdateGoal(current.id, title, cadence, metric, category, target, current.active)
                }
                showGoalSheet = false
                editingGoal = null
            }
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = PhoneticMonoFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalProgressCard(
    goalView: GoalProgressView,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        borderColor = if (goalView.isCompleted) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goalView.goal.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (goalView.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Goal completed",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = goalView.goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = goalView.periodSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_goal_${goalView.goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_goal_${goalView.goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (goalView.isCompleted) "Goal achieved" else "${goalView.targetCount - goalView.currentCount} remaining",
                style = MaterialTheme.typography.labelMedium,
                color = if (goalView.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${goalView.currentCount} / ${goalView.targetCount}",
                fontFamily = PhoneticMonoFamily,
                style = MaterialTheme.typography.labelMedium,
                color = if (goalView.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        EditorialProgressBar(
            fraction = goalView.fraction,
            fillColor = if (goalView.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            description = "${goalView.goal.title}: ${goalView.currentCount} of ${goalView.targetCount}"
        )
    }
}
