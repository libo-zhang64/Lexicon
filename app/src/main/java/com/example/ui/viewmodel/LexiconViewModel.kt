package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.CuratedCatalog
import com.example.data.catalog.DailySpotlightCatalog
import com.example.data.catalog.DailySpotlightEntry
import com.example.data.firebase.AiLexiconRepository
import com.example.data.firebase.LexiconFirestoreRepository
import com.example.data.model.CategoryFilter
import com.example.data.model.GoalCadence
import com.example.data.model.GoalMetricType
import com.example.data.model.LearningGoalDoc
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizSessionDoc
import com.example.data.model.TextExtractionFocus
import com.example.data.model.TextExtractionResult
import com.example.data.model.UserProfileDoc
import com.example.domain.AdaptivePathEngine
import com.example.domain.LearningPathInsights
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AiLookupState {
    data object Idle : AiLookupState
    data class Loading(val query: String) : AiLookupState
    data class Success(val entry: PhraseEntry, val savedToLexicon: Boolean) : AiLookupState
    data class Error(val message: String, val lastQuery: String) : AiLookupState
}

sealed interface TextExtractionStatus {
    data object Idle : TextExtractionStatus
    data class Loading(val label: String) : TextExtractionStatus
    data class Success(val result: TextExtractionResult) : TextExtractionStatus
    data class Error(val message: String) : TextExtractionStatus
}

data class TextExtractorUiState(
    val sourceText: String = "",
    val fileName: String? = null,
    val focus: TextExtractionFocus = TextExtractionFocus.BALANCED,
    val status: TextExtractionStatus = TextExtractionStatus.Idle,
    val savedPhraseIds: Set<String> = emptySet(),
    val isSavingAll: Boolean = false
)

private sealed interface SyncEnvelope<out T> {
    data class Data<T>(val value: T) : SyncEnvelope<T>
    data class Error(val message: String) : SyncEnvelope<Nothing>
}

data class LexiconUiState(
    val isLoading: Boolean = true,
    val syncError: String? = null,
    val profile: UserProfileDoc = UserProfileDoc(),
    val allPhrases: List<PhraseEntry> = CuratedCatalog.entries,
    val customPhrases: List<PhraseEntry> = emptyList(),
    val dailySpotlight: DailySpotlightEntry = DailySpotlightCatalog.spotlightForOffset(0),
    val spotlightOffset: Int = 0,
    val progressMap: Map<String, PhraseProgressDoc> = emptyMap(),
    val goals: List<LearningGoalDoc> = emptyList(),
    val quizSessions: List<QuizSessionDoc> = emptyList(),
    val insights: LearningPathInsights = AdaptivePathEngine.computeInsights(
        allPhrases = CuratedCatalog.entries,
        goals = emptyList(),
        progressMap = emptyMap(),
        sessions = emptyList()
    ),
    val selectedCategory: CategoryFilter = CategoryFilter.ALL,
    val searchQuery: String = "",
    val aiLookupState: AiLookupState = AiLookupState.Idle,
    val bannerMessage: String? = null
) {
    val filteredTodayPhrases: List<PhraseEntry>
        get() = allPhrases.filter { selectedCategory.matches(it.category) }

    val filteredSearchPhrases: List<PhraseEntry>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return allPhrases
            return allPhrases.filter { entry ->
                entry.term.contains(q, ignoreCase = true) ||
                    entry.definition.contains(q, ignoreCase = true) ||
                    entry.category.label.contains(q, ignoreCase = true) ||
                    entry.synonyms.any { it.term.contains(q, ignoreCase = true) }
            }
        }

    val bookmarkedPhrases: List<PhraseEntry>
        get() = allPhrases.filter { progressMap[it.id]?.bookmarked == true }
}

private fun <T> Flow<T>.wrapSyncErrors(label: String): Flow<SyncEnvelope<T>> =
    this.map<T, SyncEnvelope<T>> { SyncEnvelope.Data(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(SyncEnvelope.Error(root.message ?: "Unable to sync $label"))
        }

class LexiconViewModel(
    private val appContext: Context,
    private val firestoreRepo: LexiconFirestoreRepository,
    private val aiRepo: AiLexiconRepository
) : ViewModel() {

    private val _catalogEntries = MutableStateFlow(CuratedCatalog.entries)
    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _aiLookupState = MutableStateFlow<AiLookupState>(AiLookupState.Idle)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private val _spotlightOffset = MutableStateFlow(0)
    private val _textExtractorState = MutableStateFlow(TextExtractorUiState())
    val textExtractorState: StateFlow<TextExtractorUiState> = _textExtractorState.asStateFlow()

    private val profileFlow = firestoreRepo.observeUserProfile()
        .wrapSyncErrors("profile")
    private val goalsFlow = firestoreRepo.observeLearningGoals()
        .wrapSyncErrors("learning goals")
    private val progressFlow = firestoreRepo.observePhraseProgress()
        .wrapSyncErrors("phrase progress")
    private val customPhrasesFlow = firestoreRepo.observeCustomPhrases()
        .wrapSyncErrors("saved AI phrases")
    private val sessionsFlow = firestoreRepo.observeQuizSessions()
        .wrapSyncErrors("quiz sessions")

    private val cloudRawFlow = combine(
        profileFlow,
        goalsFlow,
        progressFlow,
        customPhrasesFlow,
        sessionsFlow
    ) { profEnv, goalsEnv, progEnv, customEnv, sessEnv ->
        CloudRaw(profEnv, goalsEnv, progEnv, customEnv, sessEnv)
    }

    private val syncedDataFlow = combine(
        cloudRawFlow,
        _catalogEntries
    ) { cloud, curatedCatalog ->
        val firstError = listOf(
            cloud.profEnv,
            cloud.goalsEnv,
            cloud.progEnv,
            cloud.customEnv,
            cloud.sessEnv
        ).filterIsInstance<SyncEnvelope.Error>().firstOrNull()?.message

        val profile = (cloud.profEnv as? SyncEnvelope.Data)?.value ?: UserProfileDoc(
            displayName = Firebase.auth.currentUser?.displayName ?: "Learner"
        )
        val goals = (cloud.goalsEnv as? SyncEnvelope.Data)?.value ?: emptyList()
        val progress = (cloud.progEnv as? SyncEnvelope.Data)?.value ?: emptyMap()
        val custom = (cloud.customEnv as? SyncEnvelope.Data)?.value ?: emptyList()
        val sessions = (cloud.sessEnv as? SyncEnvelope.Data)?.value ?: emptyList()

        val customIds = custom.map { it.id }.toSet()
        val mergedPhrases = custom + curatedCatalog.filterNot { it.id in customIds }
        val insights = AdaptivePathEngine.computeInsights(
            allPhrases = mergedPhrases,
            goals = goals,
            progressMap = progress,
            sessions = sessions
        )
        SyncedSnapshot(
            syncError = firstError,
            profile = profile,
            allPhrases = mergedPhrases,
            customPhrases = custom,
            progressMap = progress,
            goals = goals,
            sessions = sessions,
            insights = insights
        )
    }

    private val uiControlsFlow = combine(
        _selectedCategory,
        _searchQuery,
        _aiLookupState,
        _bannerMessage,
        _spotlightOffset
    ) { category, query, aiState, banner, offset ->
        UiControls(category, query, aiState, banner, offset)
    }

    val uiState: StateFlow<LexiconUiState> = combine(
        syncedDataFlow,
        uiControlsFlow
    ) { synced, controls ->
        LexiconUiState(
            isLoading = false,
            syncError = synced.syncError,
            profile = synced.profile,
            allPhrases = synced.allPhrases,
            customPhrases = synced.customPhrases,
            dailySpotlight = DailySpotlightCatalog.spotlightForOffset(controls.spotlightOffset),
            spotlightOffset = controls.spotlightOffset,
            progressMap = synced.progressMap,
            goals = synced.goals,
            quizSessions = synced.sessions,
            insights = synced.insights,
            selectedCategory = controls.category,
            searchQuery = controls.query,
            aiLookupState = controls.aiState,
            bannerMessage = controls.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LexiconUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                CuratedCatalog.ensureLoaded(appContext)
            }
            _catalogEntries.value = loaded
        }
        viewModelScope.launch {
            firestoreRepo.ensureUserProfile(Firebase.auth.currentUser?.displayName)
        }
    }

    fun selectCategory(category: CategoryFilter) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cycleSpotlight(delta: Int) {
        _spotlightOffset.value += delta
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    fun buildLearningBatch(
        category: CategoryFilter = CategoryFilter.ALL,
        count: Int = 5
    ): List<PhraseEntry> {
        val state = uiState.value
        val pool = state.allPhrases.filter { category.matches(it.category) }
            .ifEmpty { state.allPhrases }

        val unstudied = pool.filter { state.progressMap[it.id]?.lessonCompleted != true }
        val needsReinforcement = pool.filter {
            val p = state.progressMap[it.id]
            p != null && p.lessonCompleted && (p.missCount > 0L || p.masteryLevel < 2L)
        }

        val ordered = if (unstudied.isNotEmpty()) {
            // Mix next unstudied phrases across categories smoothly when ALL is selected
            if (category == CategoryFilter.ALL) {
                val grouped = unstudied.groupBy { it.category }.values.toList()
                val interleaved = mutableListOf<PhraseEntry>()
                var round = 0
                while (interleaved.size < count && round < 10) {
                    for (group in grouped) {
                        if (round < group.size && interleaved.size < count) {
                            interleaved.add(group[round])
                        }
                    }
                    round++
                }
                interleaved
            } else {
                unstudied.take(count)
            }
        } else {
            (needsReinforcement + pool.shuffled()).distinctBy { it.id }.take(count)
        }

        return ordered.take(count)
    }

    fun toggleBookmark(phrase: PhraseEntry) {
        val current = uiState.value.progressMap[phrase.id]?.bookmarked == true
        viewModelScope.launch {
            firestoreRepo.toggleBookmark(phrase, current)
                .onSuccess {
                    _bannerMessage.value = if (!current) {
                        "Saved '${phrase.term}' to your lexicon."
                    } else {
                        "Removed '${phrase.term}' from saved phrases."
                    }
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Could not update bookmark."
                }
        }
    }

    fun markLessonCompleted(phrase: PhraseEntry, showBanner: Boolean = true) {
        viewModelScope.launch {
            firestoreRepo.markLessonCompleted(phrase)
                .onSuccess {
                    if (showBanner) {
                        _bannerMessage.value = "Lesson completed for '${phrase.term}'."
                    }
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Could not save lesson progress."
                }
        }
    }

    fun markSessionLessonsCompleted(entries: List<PhraseEntry>) {
        if (entries.isEmpty()) return
        viewModelScope.launch {
            var completedNow = 0
            for (phrase in entries) {
                val result = firestoreRepo.markLessonCompleted(phrase)
                if (result.isSuccess) completedNow++
            }
            if (completedNow > 0) {
                _bannerMessage.value = "Session complete! Logged $completedNow lessons to your learning path."
            }
        }
    }

    fun lookupPhraseWithAi(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isBlank()) {
            _aiLookupState.value = AiLookupState.Error("Type an American phrase or idiom first.", "")
            return
        }
        viewModelScope.launch {
            _aiLookupState.value = AiLookupState.Loading(query)
            aiRepo.explainPhrase(query)
                .onSuccess { entry ->
                    val alreadySaved = uiState.value.customPhrases.any { it.id == entry.id }
                    _aiLookupState.value = AiLookupState.Success(entry, savedToLexicon = alreadySaved)
                }
                .onFailure { err ->
                    _aiLookupState.value = AiLookupState.Error(
                        message = err.localizedMessage ?: "Could not explain phrase.",
                        lastQuery = query
                    )
                }
        }
    }

    fun clearAiLookup() {
        _aiLookupState.value = AiLookupState.Idle
    }

    fun saveAiPhraseToLexicon(entry: PhraseEntry) {
        viewModelScope.launch {
            firestoreRepo.saveCustomPhrase(entry)
                .onSuccess { saved ->
                    _aiLookupState.value = AiLookupState.Success(saved, savedToLexicon = true)
                    _bannerMessage.value = "Saved '${saved.term}' to your cloud lexicon."
                }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Could not save custom phrase."
                }
        }
    }

    fun deleteCustomPhrase(phraseId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteCustomPhrase(phraseId)
                .onSuccess { _bannerMessage.value = "Removed custom phrase." }
                .onFailure { err -> _bannerMessage.value = err.localizedMessage ?: "Could not remove phrase." }
        }
    }

    // --- Text Upload & Vocabulary Extractor ---

    fun updateExtractorText(text: String) {
        val capped = text.take(18_000)
        _textExtractorState.update { it.copy(sourceText = capped) }
    }

    fun onExtractorFileLoaded(fileName: String, content: String) {
        val capped = content.trim().take(18_000)
        if (capped.isEmpty()) {
            _textExtractorState.update {
                it.copy(status = TextExtractionStatus.Error("The selected file appears to be empty. Please choose a text file with readable words."))
            }
            return
        }
        _textExtractorState.update {
            it.copy(
                fileName = fileName,
                sourceText = capped,
                status = TextExtractionStatus.Idle
            )
        }
        _bannerMessage.value = "Loaded \"$fileName\" (${capped.length} characters)."
    }

    fun onExtractorFileError(message: String) {
        _textExtractorState.update {
            it.copy(status = TextExtractionStatus.Error(message))
        }
    }

    fun selectExtractionFocus(focus: TextExtractionFocus) {
        _textExtractorState.update { it.copy(focus = focus) }
    }

    fun clearTextExtractor() {
        _textExtractorState.value = TextExtractorUiState()
    }

    fun extractVocabularyFromCurrentText() {
        val current = _textExtractorState.value
        val text = current.sourceText.trim()
        if (text.length < 25) {
            _textExtractorState.update {
                it.copy(status = TextExtractionStatus.Error("Upload a text file or paste at least a short paragraph (25+ characters) to extract strong vocabulary."))
            }
            return
        }

        viewModelScope.launch {
            val label = current.fileName?.let { "Analyzing \"$it\"..." } ?: "Analyzing your passage..."
            _textExtractorState.update { it.copy(status = TextExtractionStatus.Loading(label)) }

            aiRepo.extractVocabularyFromText(text, current.focus)
                .onSuccess { result ->
                    val existingSavedIds = uiState.value.customPhrases.map { it.id }.toSet()
                    val matchedSaved = result.items.map { it.entry.id }.filter { it in existingSavedIds }.toSet()
                    _textExtractorState.update {
                        it.copy(
                            status = TextExtractionStatus.Success(result),
                            savedPhraseIds = it.savedPhraseIds + matchedSaved
                        )
                    }
                }
                .onFailure { err ->
                    _textExtractorState.update {
                        it.copy(
                            status = TextExtractionStatus.Error(
                                err.localizedMessage ?: "Could not extract vocabulary from this text. Please try again."
                            )
                        )
                    }
                }
        }
    }

    fun saveExtractedPhrase(entry: PhraseEntry) {
        viewModelScope.launch {
            firestoreRepo.saveCustomPhrase(entry)
                .onSuccess { saved ->
                    _textExtractorState.update { it.copy(savedPhraseIds = it.savedPhraseIds + saved.id) }
                    _bannerMessage.value = "Saved '${saved.term}' to your cloud lexicon."
                }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Could not save '${entry.term}'."
                }
        }
    }

    fun saveAllExtractedPhrases() {
        val current = _textExtractorState.value
        val success = current.status as? TextExtractionStatus.Success ?: return
        val unsaved = success.result.items
            .map { it.entry }
            .filterNot { it.id in current.savedPhraseIds }
        if (unsaved.isEmpty()) {
            _bannerMessage.value = "All extracted vocabulary items are already in your lexicon."
            return
        }

        viewModelScope.launch {
            _textExtractorState.update { it.copy(isSavingAll = true) }
            val newlySaved = mutableSetOf<String>()
            for (entry in unsaved) {
                firestoreRepo.saveCustomPhrase(entry)
                    .onSuccess { saved -> newlySaved.add(saved.id) }
            }
            _textExtractorState.update {
                it.copy(
                    savedPhraseIds = it.savedPhraseIds + newlySaved,
                    isSavingAll = false
                )
            }
            if (newlySaved.isNotEmpty()) {
                _bannerMessage.value = "Saved ${newlySaved.size} extracted words & phrases to your cloud lexicon."
            } else {
                _bannerMessage.value = "Could not save extracted vocabulary right now."
            }
        }
    }

    fun createGoal(
        title: String,
        cadence: GoalCadence,
        metricType: GoalMetricType,
        categoryFilter: CategoryFilter,
        targetCount: Int
    ) {
        viewModelScope.launch {
            firestoreRepo.createLearningGoal(title, cadence, metricType, categoryFilter, targetCount)
                .onSuccess { _bannerMessage.value = "Goal added: $title" }
                .onFailure { err -> _bannerMessage.value = err.localizedMessage ?: "Could not create goal." }
        }
    }

    fun updateGoal(
        goalId: String,
        title: String,
        cadence: GoalCadence,
        metricType: GoalMetricType,
        categoryFilter: CategoryFilter,
        targetCount: Int,
        active: Boolean
    ) {
        viewModelScope.launch {
            firestoreRepo.updateLearningGoal(goalId, title, cadence, metricType, categoryFilter, targetCount, active)
                .onSuccess { _bannerMessage.value = "Updated goal." }
                .onFailure { err -> _bannerMessage.value = err.localizedMessage ?: "Could not update goal." }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteLearningGoal(goalId)
                .onSuccess { _bannerMessage.value = "Goal removed." }
                .onFailure { err -> _bannerMessage.value = err.localizedMessage ?: "Could not delete goal." }
        }
    }

    private data class CloudRaw(
        val profEnv: SyncEnvelope<UserProfileDoc?>,
        val goalsEnv: SyncEnvelope<List<LearningGoalDoc>>,
        val progEnv: SyncEnvelope<Map<String, PhraseProgressDoc>>,
        val customEnv: SyncEnvelope<List<PhraseEntry>>,
        val sessEnv: SyncEnvelope<List<QuizSessionDoc>>
    )

    private data class UiControls(
        val category: CategoryFilter,
        val query: String,
        val aiState: AiLookupState,
        val banner: String?,
        val spotlightOffset: Int
    )

    private data class SyncedSnapshot(
        val syncError: String?,
        val profile: UserProfileDoc,
        val allPhrases: List<PhraseEntry>,
        val customPhrases: List<PhraseEntry>,
        val progressMap: Map<String, PhraseProgressDoc>,
        val goals: List<LearningGoalDoc>,
        val sessions: List<QuizSessionDoc>,
        val insights: LearningPathInsights
    )

    companion object {
        fun factory(
            context: Context,
            firestoreRepo: LexiconFirestoreRepository,
            aiRepo: AiLexiconRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LexiconViewModel(context.applicationContext, firestoreRepo, aiRepo) as T
            }
        }
    }
}
