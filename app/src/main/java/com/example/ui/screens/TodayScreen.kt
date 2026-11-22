package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryFilter
import com.example.data.model.PeriodKeys
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.domain.GoalProgressView
import com.example.domain.ReviewRecommendation
import com.example.ui.components.AudioListenButton
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.components.MasteryBadge
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.LexiconUiState

@Composable
fun TodayScreen(
    uiState: LexiconUiState,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    onSelectCategory: (CategoryFilter) -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onToggleBookmark: (PhraseEntry) -> Unit,
    onQuickDailyQuiz: () -> Unit,
    onRecommendationClick: (ReviewRecommendation) -> Unit,
    onOpenGoalsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayLabel = remember { PeriodKeys.readableTodayDate() }
    val featured = uiState.featuredPhrase
    val featuredProgress = uiState.progressMap[featured.id]
    val topRec = uiState.insights.recommendations.firstOrNull()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Editorial Masthead
            item(key = "masthead") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = todayLabel.uppercase(),
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Lexicon",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.testTag("streak_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Current daily streak",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${uiState.profile.currentStreak}d",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            // Sync error notice if present
            if (!uiState.syncError.isNullOrBlank()) {
                item(key = "sync_error") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sync notice: ${uiState.syncError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Active Learning Goals Strip
            item(key = "goals_strip") {
                ActiveGoalsStrip(
                    goals = uiState.insights.activeGoals,
                    onManageGoals = onOpenGoalsTab
                )
            }

            // Adaptive Review Recommendation Banner
            if (topRec != null) {
                item(key = "top_recommendation") {
                    EditorialCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        borderColor = MaterialTheme.colorScheme.outline,
                        onClick = { onRecommendationClick(topRec) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_recommendation_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorialTag(
                                text = topRec.badge,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = topRec.actionLabel,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = topRec.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = topRec.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = topRec.actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Featured Phrase of the Day Hero Card
            item(key = "featured_hero") {
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    onClick = { onOpenPhrase(featured) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("featured_phrase_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHRASE OF THE DAY • ${featured.cefrLevel.name}",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        MasteryBadge(
                            masteryLevel = featuredProgress?.masteryLevel?.toInt() ?: 0,
                            lessonCompleted = featuredProgress?.lessonCompleted == true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = featured.term,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (featured.ipa.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${featured.ipa}  •  ${featured.register.label}",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = featured.definition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioListenButton(
                            isSpeaking = speakingItemId == "featured_${featured.id}",
                            isSlow = isSlowAudio,
                            label = "Listen",
                            onSpeakClick = { onSpeak("featured_${featured.id}", featured.term) },
                            onToggleSpeed = onToggleSpeed,
                            modifier = Modifier.testTag("featured_listen_button")
                        )
                        OutlinedButton(
                            onClick = { onOpenPhrase(featured) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("featured_read_button")
                        ) {
                            Text("Read lesson")
                        }
                    }
                }
            }

            // Quick 5-Minute Daily Quiz Bar
            item(key = "daily_quiz_cta") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Retention Check",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "5 adaptive questions from dialogues & register nuances",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onQuickDailyQuiz,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("today_start_quiz_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quiz")
                        }
                    }
                }
            }

            // Category Filter Header + Chips
            item(key = "catalog_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Curated Collection",
                        title = "American English Tracks"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryFilter.entries.forEach { filter ->
                            val selected = uiState.selectedCategory == filter
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategory(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("category_chip_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Curated + Custom Phrase Cards
            items(uiState.filteredTodayPhrases, key = { it.id }) { phrase ->
                PhraseListRowCard(
                    phrase = phrase,
                    progress = uiState.progressMap[phrase.id],
                    isSpeaking = speakingItemId == "list_${phrase.id}",
                    onSpeak = { onSpeak("list_${phrase.id}", phrase.term) },
                    onToggleBookmark = { onToggleBookmark(phrase) },
                    onClick = { onOpenPhrase(phrase) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ActiveGoalsStrip(
    goals: List<GoalProgressView>,
    onManageGoals: () -> Unit
) {
    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_goals_strip")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LEARNING GOALS",
                fontFamily = PhoneticMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                onClick = onManageGoals,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("today_manage_goals_button")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (goals.isEmpty()) "+ Set a goal" else "Manage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (goals.isEmpty()) {
            Text(
                text = "Set a daily or weekly target like 'Learn 5 new idioms' or 'Master 3 phrasal verbs' to personalize your daily path.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                goals.take(2).forEach { g ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = g.goal.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${g.currentCount}/${g.targetCount}",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (g.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                        EditorialProgressBar(
                            fraction = g.fraction,
                            fillColor = if (g.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            description = "${g.goal.title}: ${g.currentCount} of ${g.targetCount}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhraseListRowCard(
    phrase: PhraseEntry,
    progress: PhraseProgressDoc?,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarked = progress?.bookmarked == true
    EditorialCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("phrase_card_${phrase.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                EditorialTag(text = phrase.category.shortLabel)
                EditorialTag(
                    text = phrase.register.label,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            }
            MasteryBadge(
                masteryLevel = progress?.masteryLevel?.toInt() ?: 0,
                lessonCompleted = progress?.lessonCompleted == true
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phrase.term,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (phrase.ipa.isNotBlank()) {
                    Text(
                        text = phrase.ipa,
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.testTag("speak_button_${phrase.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce ${phrase.term}",
                        tint = if (isSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_button_${phrase.id}")
                ) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (bookmarked) "Remove bookmark" else "Save to lexicon",
                        tint = if (bookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = phrase.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if ((progress?.missCount ?: 0L) > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Missed ${progress?.missCount} time(s) in quizzes • Review recommended",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
