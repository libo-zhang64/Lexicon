package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.PhoneticMonoFamily

data class GoalPreset(
    val title: String,
    val cadence: GoalCadence,
    val metric: GoalMetricType,
    val category: CategoryFilter,
    val target: Int
)

val starterGoalPresets = listOf(
    GoalPreset("Learn 5 new idioms", GoalCadence.WEEKLY, GoalMetricType.LEARN_NEW, CategoryFilter.IDIOM, 5),
    GoalPreset("Master 3 phrasal verbs", GoalCadence.WEEKLY, GoalMetricType.MASTER_PHRASES, CategoryFilter.PHRASAL_VERB, 3),
    GoalPreset("Study 2 daily slang phrases", GoalCadence.DAILY, GoalMetricType.LEARN_NEW, CategoryFilter.SLANG, 2),
    GoalPreset("Complete 2 workplace quizzes", GoalCadence.WEEKLY, GoalMetricType.COMPLETE_QUIZZES, CategoryFilter.WORKPLACE, 2)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalBuilderSheet(
    editingGoal: LearningGoalDoc?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        cadence: GoalCadence,
        metric: GoalMetricType,
        category: CategoryFilter,
        targetCount: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var cadence by remember(editingGoal) { mutableStateOf(editingGoal?.cadenceEnum ?: GoalCadence.WEEKLY) }
    var metric by remember(editingGoal) { mutableStateOf(editingGoal?.metricEnum ?: GoalMetricType.LEARN_NEW) }
    var category by remember(editingGoal) { mutableStateOf(editingGoal?.categoryEnum ?: CategoryFilter.IDIOM) }
    var targetCount by remember(editingGoal) { mutableIntStateOf(editingGoal?.targetCount?.toInt() ?: 5) }
    var customTitle by remember(editingGoal) { mutableStateOf(editingGoal?.title ?: "") }

    val generatedTitle = remember(metric, category, targetCount) {
        val unit = when (metric) {
            GoalMetricType.COMPLETE_QUIZZES -> if (targetCount == 1) "quiz" else "quizzes"
            else -> when (category) {
                CategoryFilter.ALL -> if (targetCount == 1) "phrase" else "phrases"
                CategoryFilter.SLANG -> "slang expressions"
                CategoryFilter.IDIOM -> if (targetCount == 1) "idiom" else "idioms"
                CategoryFilter.PHRASAL_VERB -> if (targetCount == 1) "phrasal verb" else "phrasal verbs"
                CategoryFilter.WORKPLACE -> "workplace phrases"
            }
        }
        val prefix = when (metric) {
            GoalMetricType.LEARN_NEW -> "Learn $targetCount new"
            GoalMetricType.MASTER_PHRASES -> "Master $targetCount"
            GoalMetricType.COMPLETE_QUIZZES -> "Complete $targetCount"
        }
        if (metric == GoalMetricType.COMPLETE_QUIZZES && category != CategoryFilter.ALL) {
            "$prefix ${category.label.lowercase()} $unit"
        } else {
            "$prefix $unit"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (editingGoal == null) "Create Learning Goal" else "Edit Learning Goal",
                style = MaterialTheme.typography.headlineSmall
            )

            if (editingGoal == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUICK PRESETS",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        starterGoalPresets.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    cadence = preset.cadence
                                    metric = preset.metric
                                    category = preset.category
                                    targetCount = preset.target
                                    customTitle = preset.title
                                },
                                label = { Text(preset.title) },
                                modifier = Modifier.heightIn(min = 40.dp)
                            )
                        }
                    }
                }
            }

            // Cadence Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CADENCE",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalCadence.entries.forEach { c ->
                        FilterChip(
                            selected = cadence == c,
                            onClick = { cadence = c },
                            label = { Text(c.label) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("goal_cadence_${c.name.lowercase()}")
                        )
                    }
                }
            }

            // Metric Type
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GOAL TYPE",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GoalMetricType.entries.forEach { m ->
                        FilterChip(
                            selected = metric == m,
                            onClick = { metric = m },
                            label = { Text(m.label) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("goal_metric_${m.name.lowercase()}")
                        )
                    }
                }
            }

            // Category Scope
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "FOCUS TRACK",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryFilter.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("goal_category_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            // Target Count Stepper
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target per ${cadence.label.lowercase()}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Resets automatically ${cadence.periodLabel.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { if (targetCount > 1) targetCount-- },
                            modifier = Modifier.testTag("goal_target_decrease")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease target",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = targetCount.toString(),
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { if (targetCount < 25) targetCount++ },
                            modifier = Modifier.testTag("goal_target_increase")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase target",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Optional custom goal label
            OutlinedTextField(
                value = customTitle,
                onValueChange = { if (it.length <= 100) customTitle = it },
                label = { Text("Goal label (optional)") },
                placeholder = { Text(generatedTitle) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_title_input")
            )

            Button(
                onClick = {
                    val finalTitle = customTitle.trim().ifBlank { generatedTitle }
                    onSave(finalTitle, cadence, metric, category, targetCount)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("goal_save_button")
            ) {
                Text(if (editingGoal == null) "Save Learning Goal" else "Update Goal")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
