package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryFilter
import com.example.data.model.QuizMode
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.LexiconUiState
import com.example.ui.viewmodel.QuizUiState

@Composable
fun PracticeScreen(
    lexiconState: LexiconUiState,
    quizState: QuizUiState,
    speakingItemId: String?,
    onSpeak: (itemId: String, text: String) -> Unit,
    onStartCuratedQuiz: (mode: QuizMode, category: CategoryFilter, focusIds: List<String>) -> Unit,
    onStartAiQuiz: (topic: String, category: CategoryFilter) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNextOrFinish: () -> Unit,
    onResetQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (quizState is QuizUiState.Active || quizState is QuizUiState.Summary || quizState is QuizUiState.Error) {
        BackHandler(onBack = onResetQuiz)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (quizState) {
            is QuizUiState.Setup -> QuizSetupView(
                lexiconState = lexiconState,
                onStartCuratedQuiz = onStartCuratedQuiz,
                onStartAiQuiz = onStartAiQuiz
            )

            is QuizUiState.LoadingAi -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = quizState.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Building contextual dialogue, tone, and synonym challenges...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is QuizUiState.Active -> ActiveQuizView(
                state = quizState,
                speakingItemId = speakingItemId,
                onSpeak = onSpeak,
                onSelectAnswer = onSelectAnswer,
                onNextOrFinish = onNextOrFinish,
                onExitQuiz = onResetQuiz
            )

            is QuizUiState.Summary -> QuizSummaryView(
                state = quizState,
                onDone = onResetQuiz,
                onPracticeMissed = { missedIds ->
                    onStartCuratedQuiz(QuizMode.SMART_REVIEW, CategoryFilter.ALL, missedIds)
                }
            )

            is QuizUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EditorialCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Quiz Unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = quizState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onResetQuiz,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Back to Practice")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizSetupView(
    lexiconState: LexiconUiState,
    onStartCuratedQuiz: (mode: QuizMode, category: CategoryFilter, focusIds: List<String>) -> Unit,
    onStartAiQuiz: (topic: String, category: CategoryFilter) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf(CategoryFilter.ALL) }
    var aiTopic by rememberSaveable { mutableStateOf("") }

    val weakPhraseIds = lexiconState.insights.frequentMistakePhrases.map { it.phrase.id }
    val activeGoal = lexiconState.insights.activeGoals.firstOrNull { !it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            EditorialSectionHeader(
                eyebrow = "Active Recall",
                title = "Interactive Practice"
            )
        }

        // Smart Weak-Spot Review Card
        item {
            EditorialCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ADAPTIVE RETENTION",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (weakPhraseIds.isNotEmpty()) "Smart Weak-Spot Review" else "Daily Mixed Retention Quiz",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (weakPhraseIds.isNotEmpty()) {
                        "Prioritizes the ${weakPhraseIds.size} expression(s) you've missed recently alongside unmastered dialogues."
                    } else {
                        "5 curated questions blending Context Cloze, Register & Nuance, and Synonym Precision."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onStartCuratedQuiz(QuizMode.SMART_REVIEW, CategoryFilter.ALL, weakPhraseIds)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice_smart_review_button")
                ) {
                    Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start 5-Question Session")
                }
            }
        }

        // Active Goal Drill Card
        if (activeGoal != null) {
            item {
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "GOAL-ALIGNED DRILL",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeGoal.goal.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${activeGoal.currentCount}/${activeGoal.targetCount} complete (${activeGoal.periodSubtitle})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            onStartCuratedQuiz(QuizMode.GOAL_DRILL, activeGoal.goal.categoryEnum, weakPhraseIds)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("practice_goal_drill_button")
                    ) {
                        Text("Practice for This Goal")
                    }
                }
            }
        }

        // Category Focus Drill
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TRACK DRILL",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Drill by Category",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryFilter.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        onStartCuratedQuiz(QuizMode.CATEGORY_DRILL, selectedCategory, emptyList())
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice_category_drill_button")
                ) {
                    Text("Start ${selectedCategory.label} Drill")
                }
            }
        }

        // AI Custom Scenario Quiz
        item {
            EditorialCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI CUSTOM SCENARIO",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Generate Tailored Quiz with AI",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optional scenario (e.g., 'salary negotiation', 'coffee with coworkers', 'product launch review'). AI also incorporates your recent weak spots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = aiTopic,
                    onValueChange = { if (it.length <= 80) aiTopic = it },
                    placeholder = { Text("Optional context or workplace scenario...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("practice_ai_topic_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onStartAiQuiz(aiTopic.trim(), selectedCategory) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("practice_ai_generate_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate AI Quiz")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ActiveQuizView(
    state: QuizUiState.Active,
    speakingItemId: String?,
    onSpeak: (itemId: String, text: String) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNextOrFinish: () -> Unit,
    onExitQuiz: () -> Unit
) {
    val q = state.currentQuestion
    val progressFraction = (state.currentIndex + 1).toFloat() / state.questions.size.toFloat()

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
                Text(
                    text = "QUESTION ${state.currentIndex + 1} OF ${state.questions.size}",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedButton(
                    onClick = onExitQuiz,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text("Exit")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            EditorialProgressBar(
                fraction = progressFraction,
                description = "Question ${state.currentIndex + 1} of ${state.questions.size}"
            )
        }

        item {
            EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialTag(
                        text = q.questionType.label,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    EditorialTag(text = q.category.shortLabel)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = q.prompt,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!q.contextSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = q.contextSnippet,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                q.options.forEachIndexed { idx, optionText ->
                    val isSelected = state.selectedOptionIndex == idx
                    val isCorrect = idx == q.correctIndex
                    val showFeedback = state.isAnswered

                    val bgColor = when {
                        !showFeedback -> MaterialTheme.colorScheme.surfaceContainerLowest
                        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                        isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val borderColor = when {
                        !showFeedback -> MaterialTheme.colorScheme.outlineVariant
                        isCorrect -> MaterialTheme.colorScheme.tertiary
                        isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }

                    Surface(
                        onClick = { onSelectAnswer(idx) },
                        enabled = !showFeedback,
                        shape = RoundedCornerShape(14.dp),
                        color = bgColor,
                        border = BorderStroke(if (showFeedback && (isCorrect || isSelected)) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("quiz_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (showFeedback && isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Correct answer",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            } else if (showFeedback && isSelected && !isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Incorrect answer",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(visible = state.isAnswered) {
                val answeredCorrectly = state.selectedOptionIndex == q.correctIndex
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    borderColor = if (answeredCorrectly) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (answeredCorrectly) "Exact Match" else "Nuance Note",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (answeredCorrectly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                        IconButton(
                            onClick = { onSpeak("quiz_${q.id}", q.phraseTerm) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronounce ${q.phraseTerm}",
                                tint = if (speakingItemId == "quiz_${q.id}") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = q.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onNextOrFinish,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("quiz_next_button")
                    ) {
                        Text(if (state.isLastQuestion) "Finish & Save Progress" else "Next Question")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuizSummaryView(
    state: QuizUiState.Summary,
    onDone: () -> Unit,
    onPracticeMissed: (List<String>) -> Unit
) {
    val missed = state.records.filterNot { it.isCorrect }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            EditorialCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_summary_card")
            ) {
                Text(
                    text = "SESSION COMPLETE • ${state.mode.label.uppercase()}",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${state.score} / ${state.total} Correct (${state.accuracyPercent}%)",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when {
                            state.syncSaved -> "Synced to your Learning Path, streak & goals."
                            !state.syncError.isNullOrBlank() -> state.syncError
                            else -> "Saving session to cloud..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (missed.isNotEmpty()) {
            item {
                EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text(
                        text = "EXPRESSIONS TO REVIEW",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        missed.forEachIndexed { idx, record ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${record.question.phraseTerm} (${record.question.questionType.label})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = record.question.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (idx < missed.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (missed.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onPracticeMissed(missed.map { it.question.phraseId }.distinct()) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("quiz_retry_missed_button")
                    ) {
                        Text("Retry Missed Expressions")
                    }
                }
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("quiz_summary_done_button")
                ) {
                    Text("Done")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
