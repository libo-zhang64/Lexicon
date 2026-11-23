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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseEntry
import com.example.ui.components.AudioListenButton
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.AiLookupState
import com.example.ui.viewmodel.LexiconUiState

private val suggestedAiLookups = listOf(
    "throw under the bus",
    "quiet quitting",
    "touch base",
    "silver bullet",
    "dogfooding",
    "cut to the chase"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    uiState: LexiconUiState,
    speakingItemId: String?,
    isSlowAudio: Boolean,
    onQueryChange: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onSaveAiPhrase: (PhraseEntry) -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onToggleBookmark: (PhraseEntry) -> Unit,
    onOpenExtractor: () -> Unit,
    onSpeak: (itemId: String, text: String) -> Unit,
    onToggleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(CategoryFilter.ALL) }
    var visibleCount by rememberSaveable(uiState.searchQuery, selectedFilter) { mutableIntStateOf(30) }

    val matchingEntries = uiState.filteredSearchPhrases.filter { selectedFilter.matches(it.category) }
    val displayedEntries = matchingEntries.take(visibleCount)

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
            item(key = "explore_header") {
                EditorialSectionHeader(
                    eyebrow = "Search ${uiState.allPhrases.size} Entries & AI Lexicographer",
                    title = "Explore American English"
                )
            }

            item(key = "explore_extractor_shortcut") {
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    onClick = onOpenExtractor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_open_extractor_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upload Text & Extract Vocab",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Paste an article or upload .txt/.md to pull GRE words & idioms.",
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

            // Search + AI Lookup Field
            item(key = "search_bar") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search library or enter any US phrase...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.testTag("explore_clear_search")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    onAskAi(uiState.searchQuery)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onAskAi(uiState.searchQuery) },
                            enabled = uiState.searchQuery.isNotBlank() && uiState.aiLookupState !is AiLookupState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("explore_ai_lookup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.searchQuery.isBlank()) {
                                    "Type a phrase to explain with AI"
                                } else {
                                    "Explain \"${uiState.searchQuery.trim()}\" with AI"
                                }
                            )
                        }
                    }

                    if (uiState.searchQuery.isBlank() && uiState.aiLookupState is AiLookupState.Idle) {
                        Text(
                            text = "TRY AN AI DEEP-DIVE",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            suggestedAiLookups.forEach { sample ->
                                AssistChip(
                                    onClick = {
                                        onQueryChange(sample)
                                        onAskAi(sample)
                                    },
                                    label = { Text(sample) },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                )
                            }
                        }
                    }
                }
            }

            // AI Lookup Result / Loading / Error Card
            when (val aiState = uiState.aiLookupState) {
                is AiLookupState.Idle -> Unit
                is AiLookupState.Loading -> {
                    item(key = "ai_loading") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = aiState.statusText,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (aiState.receivedChars > 0) {
                                            "Streaming live (${aiState.receivedChars} chars)..."
                                        } else {
                                            "Drafting real-world US dialogue, etymology, synonyms, and usage notes."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                is AiLookupState.Error -> {
                    item(key = "ai_error") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AI Lookup Issue",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (aiState.lastQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { onAskAi(aiState.lastQuery) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Retry lookup")
                                }
                            }
                        }
                    }
                }

                is AiLookupState.Success -> {
                    item(key = "ai_result_${aiState.entry.id}") {
                        val entry = aiState.entry
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_result_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorialTag(
                                    text = "AI Lexicon Entry • ${entry.cefrLevel.name}",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                EditorialTag(text = entry.category.shortLabel)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = entry.term,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (entry.ipa.isNotBlank()) {
                                Text(
                                    text = "${entry.ipa}  •  ${entry.register.label}",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = entry.definition,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (entry.etymology.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Origin: ${entry.etymology}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            AudioListenButton(
                                isSpeaking = speakingItemId == "ai_${entry.id}",
                                isSlow = isSlowAudio,
                                label = "Listen",
                                onSpeakClick = { onSpeak("ai_${entry.id}", entry.term) },
                                onToggleSpeed = onToggleSpeed
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenPhrase(entry) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("ai_open_full_lesson_button")
                                ) {
                                    Text("Full lesson")
                                }
                                Button(
                                    onClick = { onSaveAiPhrase(entry) },
                                    enabled = !aiState.savedToLexicon,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .heightIn(min = 48.dp)
                                        .testTag("ai_save_lexicon_button")
                                ) {
                                    if (aiState.savedToLexicon) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkAdded,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Saved")
                                    } else {
                                        Text("Save to Lexicon")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Matching Library Entries + Category Filter Chips
            item(key = "results_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "Showing ${displayedEntries.size} of ${matchingEntries.size} matching entries",
                        title = if (uiState.searchQuery.isBlank()) "Complete B2–C2 Library" else "Library Matches"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CategoryFilter.entries) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("explore_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (matchingEntries.isEmpty()) {
                item(key = "empty_search") {
                    EditorialCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Text(
                            text = "No matching entry in this filter yet.",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Switch to All Categories or tap the AI button above for a complete breakdown with dialogue, etymology, and usage notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedEntries, key = { "search_${it.id}" }) { phrase ->
                    PhraseListRowCard(
                        phrase = phrase,
                        progress = uiState.progressMap[phrase.id],
                        isSpeaking = speakingItemId == "explore_${phrase.id}",
                        onSpeak = { onSpeak("explore_${phrase.id}", phrase.term) },
                        onToggleBookmark = { onToggleBookmark(phrase) },
                        onClick = { onOpenPhrase(phrase) }
                    )
                }

                if (displayedEntries.size < matchingEntries.size) {
                    item(key = "explore_load_more") {
                        val remaining = matchingEntries.size - displayedEntries.size
                        OutlinedButton(
                            onClick = { visibleCount += 30 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("explore_load_more_button")
                        ) {
                            Text("Load More Expressions ($remaining remaining)")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
