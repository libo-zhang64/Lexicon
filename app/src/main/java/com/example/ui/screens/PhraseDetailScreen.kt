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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.ui.components.AudioListenButton
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.components.MasteryBadge
import com.example.ui.theme.PhoneticMonoFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhraseDetailScreen(
    phrase: PhraseEntry,
    progress: PhraseProgressDoc?,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onBack: () -> Unit,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkLessonComplete: () -> Unit,
    onQuizThisPhrase: () -> Unit
) {
    BackHandler(onBack = onBack)

    val bookmarked = progress?.bookmarked == true
    val lessonCompleted = progress?.lessonCompleted == true
    val masteryLevel = progress?.masteryLevel?.toInt() ?: 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = phrase.category.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to library"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("detail_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (bookmarked) "Remove bookmark" else "Save phrase",
                            tint = if (bookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onQuizThisPhrase,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("detail_quiz_phrase_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mini-Quiz")
                    }
                    Button(
                        onClick = onMarkLessonComplete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lessonCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("detail_complete_lesson_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lessonCompleted) "Lesson Studied" else "Mark Studied")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Block: Headword + IPA + Audio
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorialTag(
                                text = phrase.cefrLevel.label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            EditorialTag(text = phrase.register.label)
                            MasteryBadge(masteryLevel = masteryLevel, lessonCompleted = lessonCompleted)
                        }
                        Text(
                            text = phrase.term,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (phrase.ipa.isNotBlank()) {
                            Text(
                                text = phrase.ipa,
                                fontFamily = PhoneticMonoFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AudioListenButton(
                            isSpeaking = speakingItemId == "detail_head_${phrase.id}",
                            isSlow = isSlowAudio,
                            label = "Hear US Pronunciation",
                            onSpeakClick = { onSpeak("detail_head_${phrase.id}", phrase.term) },
                            onToggleSpeed = onToggleSpeed,
                            modifier = Modifier.testTag("detail_listen_button")
                        )
                    }
                }

                // Definition & Register Snapshot
                item {
                    EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        Text(
                            text = "MEANING & REGISTER",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = phrase.definition,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (phrase.nuanceSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = phrase.nuanceSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tone: ${phrase.register.toneHint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Real-World Dialogue Context
                if (phrase.dialogue.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialSectionHeader(
                                eyebrow = "In Context",
                                title = "Real-World American Dialogue"
                            )
                            EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    phrase.dialogue.forEachIndexed { idx, turn ->
                                        val turnKey = "dialogue_${phrase.id}_$idx"
                                        val isLineSpeaking = speakingItemId == turnKey
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = turn.speaker.uppercase(),
                                                    fontFamily = PhoneticMonoFamily,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = highlightTermInLine(
                                                        line = turn.line,
                                                        term = phrase.term,
                                                        highlightColor = MaterialTheme.colorScheme.primary,
                                                        highlightBg = MaterialTheme.colorScheme.primaryContainer
                                                    ),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(
                                                onClick = { onSpeak(turnKey, turn.line) },
                                                modifier = Modifier.testTag("dialogue_speak_$idx")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Listen to ${turn.speaker}'s line",
                                                    tint = if (isLineSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (idx < phrase.dialogue.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Etymology & Origin Story
                if (phrase.etymology.isNotBlank()) {
                    item {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ETYMOLOGY & ORIGIN STORY",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = phrase.etymology,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Synonyms & Nuance Contrasts
                if (phrase.synonyms.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialSectionHeader(
                                eyebrow = "Precision",
                                title = "Synonyms & Nuance Contrast"
                            )
                            EditorialCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    phrase.synonyms.forEachIndexed { idx, syn ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = syn.term,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (syn.nuanceDifference.isNotBlank()) {
                                                Text(
                                                    text = syn.nuanceDifference,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (idx < phrase.synonyms.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Antonyms
                if (phrase.antonyms.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialSectionHeader(
                                eyebrow = "Contrast",
                                title = "Antonyms & Opposites"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                phrase.antonyms.forEach { antonym ->
                                    EditorialTag(
                                        text = antonym,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Advanced Usage Notes
                if (phrase.usageNotes.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialSectionHeader(
                                eyebrow = "For Advanced Learners",
                                title = "Usage Notes & Pitfalls"
                            )
                            EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    phrase.usageNotes.forEach { note ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

private fun highlightTermInLine(
    line: String,
    term: String,
    highlightColor: androidx.compose.ui.graphics.Color,
    highlightBg: androidx.compose.ui.graphics.Color
) = buildAnnotatedString {
    val cleanBase = term.replace(Regex("\\([^)]*\\)"), "").trim()
    val startIdx = if (cleanBase.isNotEmpty()) line.indexOf(cleanBase, ignoreCase = true) else -1
    if (startIdx >= 0) {
        append(line.substring(0, startIdx))
        withStyle(
            SpanStyle(
                color = highlightColor,
                background = highlightBg,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(line.substring(startIdx, startIdx + cleanBase.length))
        }
        append(line.substring(startIdx + cleanBase.length))
    } else {
        append(line)
    }
}
