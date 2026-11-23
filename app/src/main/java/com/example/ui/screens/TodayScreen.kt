package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.DailySpotlightEntry
import com.example.data.model.CategoryFilter
import com.example.data.model.PeriodKeys
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.domain.ReviewRecommendation
import com.example.ui.components.AudioListenButton
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.components.MasteryBadge
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.LexiconUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    uiState: LexiconUiState,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    onSelectCategory: (CategoryFilter) -> Unit,
    onStartLearningSession: (CategoryFilter) -> Unit,
    onStartTransferChallenge: () -> Unit,
    onCycleSpotlight: (Int) -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onToggleBookmark: (PhraseEntry) -> Unit,
    onOpenExplore: () -> Unit,
    onOpenExtractor: () -> Unit,
    onOpenReviewTab: () -> Unit,
    onRecommendationClick: (ReviewRecommendation) -> Unit,
    onOpenGoalsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visibleCount by rememberSaveable(uiState.selectedCategory) { mutableIntStateOf(20) }

    val totalEntries = uiState.allPhrases.size
    val totalStudied = uiState.progressMap.values.count { it.lessonCompleted }
    val filteredEntries = uiState.filteredTodayPhrases
    val shownEntries = filteredEntries.take(visibleCount)
    val nextUpPreview = uiState.allPhrases
        .filter { uiState.progressMap[it.id]?.lessonCompleted != true }
        .take(3)

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
            // 1. Bright Greeting Header + Library Counter + Streak Pill
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = PeriodKeys.readableTodayDate().uppercase(),
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Learn American English",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Current streak",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${uiState.profile.currentStreak}d",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Cloud Sync Error Notice (if any)
            if (!uiState.syncError.isNullOrBlank()) {
                item(key = "sync_error") {
                    EditorialCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "Cloud sync note: ${uiState.syncError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 1b. EBBINGHAUS DUE-FOR-REVIEW BANNER (Always prominent when old words are due!)
            val ebbinghaus = uiState.ebbinghaus
            if (ebbinghaus.dueNowCount > 0) {
                item(key = "ebbinghaus_due_banner") {
                    EditorialCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                        onClick = onOpenReviewTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_ebbinghaus_due_banner")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoGraph,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FORGETTING CURVE • ${ebbinghaus.averageRetentionPercent}% AVG RECALL",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${ebbinghaus.dueNowCount} learned word${if (ebbinghaus.dueNowCount == 1) "" else "s"} due for review",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Tap to reinforce via Active Recall Quiz or Flashcards.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Ebbinghaus Review",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 2. UNMISTAKABLE HERO ENTRY POINT FOR LEARNING
            item(key = "hero_learning_launcher") {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_learning_card")
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "DAILY LESSON DECK • 5 PHRASES",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(
                                text = "$totalStudied / $totalEntries studied",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }

                        Text(
                            text = if (totalStudied == 0) {
                                "Start your first guided session"
                            } else {
                                "Continue your daily learning path"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Text(
                            text = if (nextUpPreview.isNotEmpty()) {
                                "Up next: ${nextUpPreview.joinToString(" • ") { it.term }}"
                            } else {
                                "Step through 5 interactive cards with US audio, dialogues, origin stories, and nuances."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onStartLearningSession(CategoryFilter.ALL) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .heightIn(min = 52.dp)
                                    .testTag("start_learning_session_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start Learning",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = onStartTransferChallenge,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("today_transfer_challenge_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transfer Quiz")
                            }
                        }
                    }
                }
            }

            // 3. 5 LEARNING TRACKS (Daily Slang, Idioms, Phrasal Verbs, Workplace, and GRE Vocabulary)
            item(key = "tracks_grid") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "$totalEntries B2–C2 & GRE Expressions",
                        title = "Choose a Learning Track"
                    )

                    val trackConfigs = listOf(
                        TrackMeta(
                            category = PhraseCategory.SLANG,
                            filter = CategoryFilter.SLANG,
                            icon = Icons.Default.ChatBubbleOutline,
                            accent = MaterialTheme.colorScheme.primary,
                            tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        TrackMeta(
                            category = PhraseCategory.IDIOM,
                            filter = CategoryFilter.IDIOM,
                            icon = Icons.Default.FormatQuote,
                            accent = MaterialTheme.colorScheme.secondary,
                            tint = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ),
                        TrackMeta(
                            category = PhraseCategory.PHRASAL_VERB,
                            filter = CategoryFilter.PHRASAL_VERB,
                            icon = Icons.Default.SwapHoriz,
                            accent = MaterialTheme.colorScheme.tertiary,
                            tint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ),
                        TrackMeta(
                            category = PhraseCategory.WORKPLACE,
                            filter = CategoryFilter.WORKPLACE,
                            icon = Icons.Default.BusinessCenter,
                            accent = MaterialTheme.colorScheme.primary,
                            tint = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        trackConfigs.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { track ->
                                    val countInTrack = uiState.allPhrases.count { it.category == track.category }
                                    val studiedInTrack = uiState.allPhrases.count {
                                        it.category == track.category &&
                                            uiState.progressMap[it.id]?.lessonCompleted == true
                                    }
                                    TrackLauncherCard(
                                        meta = track,
                                        studiedCount = studiedInTrack,
                                        totalCount = countInTrack,
                                        onStartTrack = { onStartLearningSession(track.filter) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 5th Full-Width Track: GRE Vocabulary (equal ~250-word bank)
                        val greMeta = TrackMeta(
                            category = PhraseCategory.GRE_VOCAB,
                            filter = CategoryFilter.GRE_VOCAB,
                            icon = Icons.Default.AutoStories,
                            accent = MaterialTheme.colorScheme.tertiary,
                            tint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        )
                        val greTotal = uiState.allPhrases.count { it.category == PhraseCategory.GRE_VOCAB }
                        val greStudied = uiState.allPhrases.count {
                            it.category == PhraseCategory.GRE_VOCAB &&
                                uiState.progressMap[it.id]?.lessonCompleted == true
                        }
                        TrackLauncherCard(
                            meta = greMeta,
                            studiedCount = greStudied,
                            totalCount = greTotal,
                            onStartTrack = { onStartLearningSession(CategoryFilter.GRE_VOCAB) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3b. UPLOAD TEXT & EXTRACT VOCAB QUICK ENTRY
            item(key = "upload_extractor_banner") {
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    onClick = onOpenExtractor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_open_extractor_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PERSONAL VOCAB LAB",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Upload Text & Extract Vocab",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Upload a .txt/.md file or paste an article to pull GRE words, idioms & nuances into a study deck.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Text Extractor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 4. WORD / PHRASE FOR TODAY (BONUS DISCOVERY • NOT IN LEARNING SESSIONS)
            item(key = "daily_spotlight_${uiState.dailySpotlight.id}") {
                DailySpotlightCard(
                    entry = uiState.dailySpotlight,
                    isSpeaking = speakingItemId == "spotlight_${uiState.dailySpotlight.id}",
                    isSlowAudio = isSlowAudio,
                    onSpeak = {
                        onSpeak(
                            "spotlight_${uiState.dailySpotlight.id}",
                            "${uiState.dailySpotlight.term}. ${uiState.dailySpotlight.example}"
                        )
                    },
                    onToggleSpeed = onToggleSpeed,
                    onCycleNext = { onCycleSpotlight(1) }
                )
            }

            // 5. Compact Active Goal Snapshot (if user has goals)
            val activeGoal = uiState.insights.activeGoals.firstOrNull()
            if (activeGoal != null) {
                item(key = "goal_snapshot") {
                    EditorialCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        onClick = onOpenGoalsTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ACTIVE GOAL • ${activeGoal.periodSubtitle.uppercase()}",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${activeGoal.currentCount}/${activeGoal.targetCount}",
                                fontFamily = PhoneticMonoFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeGoal.goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        EditorialProgressBar(
                            fraction = activeGoal.fraction,
                            fillColor = if (activeGoal.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            description = "${activeGoal.goal.title}: ${activeGoal.currentCount} of ${activeGoal.targetCount}"
                        )
                    }
                }
            }

            // 6. Adaptive Review Nudge (when mistakes exist)
            val topRec = uiState.insights.recommendations.firstOrNull()
            if (topRec != null) {
                item(key = "adaptive_nudge") {
                    EditorialCard(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMART REVIEW SUGGESTION",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = topRec.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = topRec.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { onRecommendationClick(topRec) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("today_recommendation_cta")
                            ) {
                                Text("Go")
                            }
                        }
                    }
                }
            }

            // 7. Browse the 1,000-Entry Library
            item(key = "browse_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Full Curriculum Library",
                        title = "Browse Expressions",
                        trailing = {
                            TextButton(
                                onClick = onOpenExplore,
                                modifier = Modifier.testTag("browse_search_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search All")
                            }
                        }
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CategoryFilter.entries) { filter ->
                            val selected = uiState.selectedCategory == filter
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategory(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("category_chip_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            items(shownEntries, key = { it.id }) { phrase ->
                PhraseListRowCard(
                    phrase = phrase,
                    progress = uiState.progressMap[phrase.id],
                    isSpeaking = speakingItemId == "list_${phrase.id}",
                    onSpeak = { onSpeak("list_${phrase.id}", phrase.term) },
                    onToggleBookmark = { onToggleBookmark(phrase) },
                    onClick = { onOpenPhrase(phrase) }
                )
            }

            if (filteredEntries.size > shownEntries.size) {
                item(key = "load_more_entries") {
                    OutlinedButton(
                        onClick = { visibleCount += 30 },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("load_more_phrases_button")
                    ) {
                        Text("Load More (Showing ${shownEntries.size} of ${filteredEntries.size})")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private data class TrackMeta(
    val category: PhraseCategory,
    val filter: CategoryFilter,
    val icon: ImageVector,
    val accent: Color,
    val tint: Color
)

@Composable
private fun TrackLauncherCard(
    meta: TrackMeta,
    studiedCount: Int,
    totalCount: Int,
    onStartTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (totalCount > 0) studiedCount.toFloat() / totalCount.toFloat() else 0f
    EditorialCard(
        containerColor = meta.tint,
        borderColor = meta.accent.copy(alpha = 0.25f),
        onClick = onStartTrack,
        modifier = modifier.testTag("track_card_${meta.filter.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = null,
                        tint = meta.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = "$totalCount",
                fontFamily = PhoneticMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                color = meta.accent
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = meta.category.shortLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$studiedCount studied",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        EditorialProgressBar(
            fraction = fraction,
            fillColor = meta.accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            description = "${meta.category.label}: $studiedCount of $totalCount studied"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Study 5",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = meta.accent
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = meta.accent,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DailySpotlightCard(
    entry: DailySpotlightEntry,
    isSpeaking: Boolean,
    isSlowAudio: Boolean,
    onSpeak: () -> Unit,
    onToggleSpeed: () -> Unit,
    onCycleNext: () -> Unit
) {
    EditorialCard(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_spotlight_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "WORD FOR TODAY • BONUS",
                    fontFamily = PhoneticMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Text(
                    text = "Not in Lessons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.term,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${entry.ipa}  •  ${entry.partOfSpeech}  •  ${entry.tagline}",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = entry.definition,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = entry.example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Why Americans love it: ${entry.funFact}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioListenButton(
                isSpeaking = isSpeaking,
                isSlow = isSlowAudio,
                label = "Hear It",
                onSpeakClick = onSpeak,
                onToggleSpeed = onToggleSpeed
            )
            TextButton(
                onClick = onCycleNext,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("spotlight_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Another Gem")
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
    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("phrase_card_${phrase.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialTag(
                        text = phrase.category.shortLabel,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    EditorialTag(text = phrase.cefrLevel.name)
                    MasteryBadge(
                        masteryLevel = (progress?.masteryLevel ?: 0L).toInt(),
                        lessonCompleted = progress?.lessonCompleted == true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    val saved = progress?.bookmarked == true
                    Icon(
                        imageVector = if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (saved) "Remove bookmark for ${phrase.term}" else "Save ${phrase.term}",
                        tint = if (saved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}
