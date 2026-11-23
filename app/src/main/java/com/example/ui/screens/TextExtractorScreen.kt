package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExtractedVocabItem
import com.example.data.model.PhraseEntry
import com.example.data.model.TextExtractionFocus
import com.example.ui.components.EditorialCard
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.EditorialTag
import com.example.ui.theme.PhoneticMonoFamily
import com.example.ui.viewmodel.TextExtractionStatus
import com.example.ui.viewmodel.TextExtractorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextExtractorScreen(
    state: TextExtractorUiState,
    speakingItemId: String?,
    onTextChange: (String) -> Unit,
    onFileLoaded: (fileName: String, content: String) -> Unit,
    onFileError: (String) -> Unit,
    onSelectFocus: (TextExtractionFocus) -> Unit,
    onExtract: () -> Unit,
    onClear: () -> Unit,
    onSavePhrase: (PhraseEntry) -> Unit,
    onSaveAll: () -> Unit,
    onOpenPhrase: (PhraseEntry) -> Unit,
    onStudyExtracted: (List<PhraseEntry>) -> Unit,
    onQuizExtracted: (List<PhraseEntry>) -> Unit,
    onSpeak: (itemId: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                readTextFromUri(context, uri)
            }
            loaded.onSuccess { (name, content) ->
                onFileLoaded(name, content)
            }.onFailure { err ->
                onFileError(
                    err.localizedMessage ?: "Could not read that file. Please pick a plain text (.txt or .md) document."
                )
            }
        }
    }

    val wordCount = state.sourceText
        .trim()
        .split(Regex("\\s+"))
        .count { it.isNotEmpty() }

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
            item(key = "extractor_header") {
                EditorialSectionHeader(
                    eyebrow = "Vocabulary Lab • Text Analyzer",
                    title = "Upload & Extract Vocab"
                )
            }

            // Upload + Paste Card
            item(key = "extractor_input_card") {
                EditorialCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Turn any article, essay, email, or reading note into a personalized American English & GRE study deck.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Upload document trigger (zero-permission SAF OpenDocument)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                documentPicker.launch(
                                    arrayOf("text/plain", "text/markdown", "text/csv", "text/*")
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("extract_upload_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.fileName != null) "Upload Another File" else "Upload Text File (.txt, .md)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (state.sourceText.isNotEmpty() || state.status !is TextExtractionStatus.Idle) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("extract_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear text and results"
                                )
                            }
                        }
                    }

                    if (!state.fileName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.fileName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$wordCount words",
                                    fontFamily = PhoneticMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.sourceText,
                        onValueChange = onTextChange,
                        label = { Text("Uploaded or Pasted Passage") },
                        placeholder = {
                            Text("Upload a .txt/.md file above, or paste an article, essay, memo, or book excerpt here...")
                        },
                        minLines = 5,
                        maxLines = 9,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extract_text_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (wordCount > 0) "$wordCount words • ${state.sourceText.length} chars" else "Supports up to 18,000 characters",
                            fontFamily = PhoneticMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "EXTRACTION FOCUS",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextExtractionFocus.entries.forEach { focus ->
                            FilterChip(
                                selected = state.focus == focus,
                                onClick = { onSelectFocus(focus) },
                                label = { Text(focus.label) },
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("extract_focus_${focus.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onExtract,
                        enabled = state.sourceText.trim().length >= 25 && state.status !is TextExtractionStatus.Loading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("extract_analyze_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extract Strong Words & Phrases",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Results / Loading / Error States
            when (val status = state.status) {
                is TextExtractionStatus.Idle -> Unit

                is TextExtractionStatus.Loading -> {
                    item(key = "extract_loading") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 3.dp
                                )
                                Column {
                                    Text(
                                        text = status.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Identifying GRE-level words, idioms, phrasal verbs, and nuanced phrases with exact quotes from your text...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                is TextExtractionStatus.Error -> {
                    item(key = "extract_error") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Could Not Analyze Text",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onExtract,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                is TextExtractionStatus.Success -> {
                    val result = status.result
                    val extractedEntries = result.items.map { it.entry }
                    val allSaved = extractedEntries.all { it.id in state.savedPhraseIds }

                    item(key = "extract_summary") {
                        EditorialCard(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extract_results_summary")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorialTag(
                                    text = "${result.items.size} HIGH-VALUE PICKS",
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                                EditorialTag(text = result.passageLevel)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = result.passageSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onStudyExtracted(extractedEntries) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("extract_study_all_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Study Deck")
                                }

                                OutlinedButton(
                                    onClick = { onQuizExtracted(extractedEntries) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("extract_quiz_all_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Quiz,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Quiz These")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onSaveAll,
                                enabled = !allSaved && !state.isSavingAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("extract_save_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdded,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    when {
                                        state.isSavingAll -> "Saving to Your Cloud Lexicon..."
                                        allSaved -> "All Saved to Your Lexicon"
                                        else -> "Save All ${result.items.size} to My Lexicon"
                                    }
                                )
                            }
                        }
                    }

                    items(result.items, key = { "extracted_${it.entry.id}" }) { item ->
                        ExtractedVocabCard(
                            item = item,
                            isSaved = item.entry.id in state.savedPhraseIds,
                            isSpeaking = speakingItemId == "extract_${item.entry.id}",
                            onSpeak = { onSpeak("extract_${item.entry.id}", item.entry.term) },
                            onSave = { onSavePhrase(item.entry) },
                            onOpenDetail = { onOpenPhrase(item.entry) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ExtractedVocabCard(
    item: ExtractedVocabItem,
    isSaved: Boolean,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onSave: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val entry = item.entry
    EditorialCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onClick = onOpenDetail,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extracted_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditorialTag(
                    text = entry.category.shortLabel,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                EditorialTag(text = entry.cefrLevel.name)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce ${entry.term}",
                        tint = if (isSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSave,
                    enabled = !isSaved,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("extract_save_${entry.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved to lexicon" else "Save ${entry.term} to lexicon",
                        tint = if (isSaved) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = entry.term,
            style = MaterialTheme.typography.headlineSmall,
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

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (item.sourceExcerpt.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "FROM YOUR TEXT",
                        fontFamily = PhoneticMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${item.sourceExcerpt.trim('"', '“', '”')}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (item.upgradeReason.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vocab boost: ${item.upgradeReason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onOpenDetail,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Open Full Lesson & Dialogue")
            }
        }
    }
}

private fun readTextFromUri(context: Context, uri: Uri): Result<Pair<String, String>> = runCatching {
    var displayName = "uploaded_text.txt"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) {
                val resolved = cursor.getString(idx)
                if (!resolved.isNullOrBlank()) displayName = resolved
            }
        }
    }
    val stream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Unable to open the selected document.")
    val text = stream.bufferedReader(Charsets.UTF_8).use { reader ->
        val sb = StringBuilder()
        val buf = CharArray(4096)
        var read = reader.read(buf)
        while (read != -1 && sb.length < 20_000) {
            sb.append(buf, 0, read)
            read = reader.read(buf)
        }
        sb.toString()
    }
    displayName to text
}
