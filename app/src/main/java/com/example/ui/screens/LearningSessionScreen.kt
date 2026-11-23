package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.ui.components.AudioListenButton
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EditorialTag
import com.example.ui.theme.PhoneticMonoFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LearningSessionScreen(
    trackLabel: String,
    categoryFilter: CategoryFilter,
    phrases: List<PhraseEntry>,
    progressMap: Map<String, PhraseProgressDoc>,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleBookmark: (PhraseEntry) -> Unit,
    onCompletePhrase: (PhraseEntry) -> Unit,
    onFinishSession: (List<PhraseEntry>) -> Unit,
    onLaunchTransferQuiz: (CategoryFilter, List<String>) -> Unit,
    onLaunchReviewQuiz: (List<String>) -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isCompleted by rememberSaveable { mutableStateOf(false) }

    if (phrases.isEmpty()) {
        onExit()
        return
    }

    val safeIndex = currentIndex.coerceIn(0, phrases.lastIndex)
    val currentPhrase = phrases[safeIndex]
    val isBookmarked = progressMap[currentPhrase.id]?.bookmarked == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = trackLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!isCompleted) {
                            Text(
                                text = "CARD ${safeIndex + 1} OF ${phrases.size}",
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.testTag("learning_session_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit learning session"
                        )
                    }
                },
                actions = {
                    if (!isCompleted) {
                        IconButton(
                            onClick = { onToggleBookmark(currentPhrase) },
                            modifier = Modifier.testTag("learning_session_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Save phrase",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!isCompleted) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (safeIndex > 0) {
                                OutlinedButton(
                                    onClick = { currentIndex = safeIndex - 1 },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .heightIn(min = 50.dp)
                                        .testTag("learning_session_prev_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous card",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onCompletePhrase(currentPhrase)
                                    if (safeIndex < phrases.lastIndex) {
                                        currentIndex = safeIndex + 1
                                    } else {
                                        isCompleted = true
                                        onFinishSession(phrases)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("learning_session_next_button")
                            ) {
                                Text(
                                    text = if (safeIndex < phrases.lastIndex) {
                                        "Got It • Next Phrase"
                                    } else {
                                        "Complete Session"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (safeIndex < phrases.lastIndex) {
                                        Icons.AutoMirrored.Filled.ArrowForward
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isCompleted) {
                SessionCompleteView(
                    trackLabel = trackLabel,
                    categoryFilter = categoryFilter,
                    phrases = phrases,
                    onLaunchTransferQuiz = onLaunchTransferQuiz,
                    onLaunchReviewQuiz = onLaunchReviewQuiz,
                    onDone = onExit
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "progress") {
                        EditorialProgressBar(
                            fraction = (safeIndex + 1).toFloat() / phrases.size.toFloat(),
                            fillColor = MaterialTheme.colorScheme.primary,
                            description = "Card ${safeIndex + 1} of ${phrases.size}"
                        )
                    }

                    // Main Headword + Audio Card
                    item(key = "headword_${currentPhrase.id}") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EditorialTag(
                                    text = currentPhrase.category.label,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                EditorialTag(
                                    text = currentPhrase.cefrLevel.label,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                EditorialTag(text = currentPhrase.register.label)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = currentPhrase.term,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentPhrase.ipa.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentPhrase.ipa,
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            AudioListenButton(
                                isSpeaking = speakingItemId == "session_${currentPhrase.id}",
                                isSlow = isSlowAudio,
                                label = "Pronounce (US)",
                                onSpeakClick = { onSpeak("session_${currentPhrase.id}", currentPhrase.term) },
                                onToggleSpeed = onToggleSpeed
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = currentPhrase.definition,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nuance: ${currentPhrase.nuanceSummary}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Real-World Dialogue Card
                    item(key = "dialogue_${currentPhrase.id}") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "IN REAL CONVERSATION",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val dialogueScript = currentPhrase.dialogue.joinToString(". ") { it.line }
                                AudioListenButton(
                                    isSpeaking = speakingItemId == "session_dlg_${currentPhrase.id}",
                                    isSlow = isSlowAudio,
                                    label = "Play",
                                    onSpeakClick = { onSpeak("session_dlg_${currentPhrase.id}", dialogueScript) }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                currentPhrase.dialogue.forEachIndexed { idx, turn ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (idx % 2 == 1) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = turn.speaker,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "\"${turn.line}\"",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Origin, Synonyms & Usage Notes Card
                    item(key = "depth_${currentPhrase.id}") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ORIGIN & NUANCE DETAILS",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentPhrase.etymology,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (currentPhrase.synonyms.isNotEmpty() || currentPhrase.antonyms.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                if (currentPhrase.synonyms.isNotEmpty()) {
                                    Text(
                                        text = "Close Synonyms",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        currentPhrase.synonyms.forEach { syn ->
                                            EditorialTag(
                                                text = syn.term,
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                                if (currentPhrase.antonyms.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Contrasts / Antonyms",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        currentPhrase.antonyms.forEach { ant ->
                                            EditorialTag(text = ant)
                                        }
                                    }
                                }
                            }

                            if (currentPhrase.usageNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                currentPhrase.usageNotes.forEach { note ->
                                    Text(
                                        text = "• $note",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteView(
    trackLabel: String,
    categoryFilter: CategoryFilter,
    phrases: List<PhraseEntry>,
    onLaunchTransferQuiz: (CategoryFilter, List<String>) -> Unit,
    onLaunchReviewQuiz: (List<String>) -> Unit,
    onDone: () -> Unit
) {
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
                    .testTag("learning_session_complete_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "SESSION COMPLETE",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${phrases.size} New Expressions Studied!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Great momentum on $trackLabel. These expressions are now enrolled in your Ebbinghaus review schedule. Quiz them now or test them inside brand-new conversations with Transfer Learning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        item {
            EditorialCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PHRASES COVERED",
                    fontFamily = PhoneticMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                phrases.forEachIndexed { idx, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.term,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = entry.definition,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Studied",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (idx < phrases.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onLaunchReviewQuiz(phrases.map { it.id }) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("session_complete_review_quiz_button")
                ) {
                    Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quiz These ${phrases.size} Phrases")
                }

                Button(
                    onClick = { onLaunchTransferQuiz(categoryFilter, phrases.map { it.id }) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("session_complete_transfer_quiz_button")
                ) {
                    Icon(imageVector = Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transfer Quiz (New Contexts)")
                }

                OutlinedButton(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("session_complete_done_button")
                ) {
                    Text("Back to Learn Hub")
                }
            }
        }
    }
}
