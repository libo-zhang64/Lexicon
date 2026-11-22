package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.CuratedCatalog
import com.example.data.firebase.AiLexiconRepository
import com.example.data.firebase.LexiconFirestoreRepository
import com.example.data.model.CategoryFilter
import com.example.data.model.GoalCadence
import com.example.data.model.GoalMetricType
import com.example.data.model.LearningGoalDoc
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizSessionDoc
import com.example.data.model.UserProfileDoc
import com.example.domain.AdaptivePathEngine
import com.example.domain.LearningPathInsights
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiLookupState {
    data object Idle : AiLookupState
    data class Loading(val query: String) : AiLookupState
    data class Success(val entry: PhraseEntry, val savedToLexicon: Boolean) : AiLookupState
    data class Error(val message: String, val lastQuery: String) : AiLookupState
}

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
    val featuredPhrase: PhraseEntry = CuratedCatalog.phraseOfTheDay(),
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

private fun <T> Flow<T>.wrapSyncErrors(fallback: T, label: String): Flow<SyncEnvelope<T>> =
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
    private val firestoreRepo: LexiconFirestoreRepository,
    private val aiRepo: AiLexiconRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _aiLookupState = MutableStateFlow<AiLookupState>(AiLookupState.Idle)
    private val _bannerMessage = MutableStateFlow<String?>(null)

    private val profileFlow = firestoreRepo.observeUserProfile()
        .wrapSyncErrors(null, "profile")
    private val goalsFlow = firestoreRepo.observeLearningGoals()
        .wrapSyncErrors(emptyList(), "learning goals")
    private val progressFlow = firestoreRepo.observePhraseProgress()
        .wrapSyncErrors(emptyMap(), "phrase progress")
    private val customPhrasesFlow = firestoreRepo.observeCustomPhrases()
        .wrapSyncErrors(emptyList(), "saved AI phrases")
    private val sessionsFlow = firestoreRepo.observeQuizSessions()
        .wrapSyncErrors(emptyList(), "quiz sessions")

    private val syncedDataFlow = combine(
        profileFlow,
        goalsFlow,
        progressFlow,
        customPhrasesFlow,
        sessionsFlow
    ) { profEnv, goalsEnv, progEnv, customEnv, sessEnv ->
        val firstError = listOf(profEnv, goalsEnv, progEnv, customEnv, sessEnv)
            .filterIsInstance<SyncEnvelope.Error>()
            .firstOrNull()?.message

        val profile = (profEnv as? SyncEnvelope.Data)?.value ?: UserProfileDoc(
            displayName = Firebase.auth.currentUser?.displayName ?: "Learner"
        )
        val goals = (goalsEnv as? SyncEnvelope.Data)?.value ?: emptyList()
        val progress = (progEnv as? SyncEnvelope.Data)?.value ?: emptyMap()
        val custom = (customEnv as? SyncEnvelope.Data)?.value ?: emptyList()
        val sessions = (sessEnv as? SyncEnvelope.Data)?.value ?: emptyList()

        val customIds = custom.map { it.id }.toSet()
        val mergedPhrases = custom + CuratedCatalog.entries.filterNot { it.id in customIds }
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

    val uiState: StateFlow<LexiconUiState> = combine(
        syncedDataFlow,
        _selectedCategory,
        _searchQuery,
        _aiLookupState,
        _bannerMessage
    ) { synced, category, query, aiState, banner ->
        LexiconUiState(
            isLoading = false,
            syncError = synced.syncError,
            profile = synced.profile,
            allPhrases = synced.allPhrases,
            customPhrases = synced.customPhrases,
            featuredPhrase = CuratedCatalog.phraseOfTheDay(CuratedCatalog.entries),
            progressMap = synced.progressMap,
            goals = synced.goals,
            quizSessions = synced.sessions,
            insights = synced.insights,
            selectedCategory = category,
            searchQuery = query,
            aiLookupState = aiState,
            bannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LexiconUiState(isLoading = true)
    )

    init {
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

    fun dismissBanner() {
        _bannerMessage.value = null
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

    fun markLessonCompleted(phrase: PhraseEntry) {
        viewModelScope.launch {
            firestoreRepo.markLessonCompleted(phrase)
                .onSuccess {
                    _bannerMessage.value = "Lesson completed for '${phrase.term}'."
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Could not save lesson progress."
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
            firestoreRepo: LexiconFirestoreRepository,
            aiRepo: AiLexiconRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LexiconViewModel(firestoreRepo, aiRepo) as T
            }
        }
    }
}
