package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiLexiconRepository
import com.example.data.firebase.LexiconFirestoreRepository
import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizAnswerRecord
import com.example.data.model.QuizMode
import com.example.data.model.QuizQuestion
import com.example.domain.CuratedQuizGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    data object Setup : QuizUiState
    data class LoadingAi(val label: String) : QuizUiState
    data class Active(
        val mode: QuizMode,
        val category: CategoryFilter,
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val selectedOptionIndex: Int?,
        val records: List<QuizAnswerRecord>
    ) : QuizUiState {
        val currentQuestion: QuizQuestion get() = questions[currentIndex]
        val isAnswered: Boolean get() = selectedOptionIndex != null
        val isLastQuestion: Boolean get() = currentIndex >= questions.lastIndex
    }

    data class Summary(
        val mode: QuizMode,
        val category: CategoryFilter,
        val records: List<QuizAnswerRecord>,
        val syncSaved: Boolean,
        val syncError: String?
    ) : QuizUiState {
        val score: Int get() = records.count { it.isCorrect }
        val total: Int get() = records.size
        val accuracyPercent: Int get() = if (total > 0) ((score * 100f) / total).toInt() else 0
    }

    data class Error(val message: String) : QuizUiState
}

class QuizViewModel(
    private val firestoreRepo: LexiconFirestoreRepository,
    private val aiRepo: AiLexiconRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Setup)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun resetToSetup() {
        _uiState.value = QuizUiState.Setup
    }

    fun startCuratedQuiz(
        mode: QuizMode,
        category: CategoryFilter,
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        focusPhraseIds: List<String> = emptyList(),
        questionCount: Int = 5
    ) {
        val questions = CuratedQuizGenerator.buildQuiz(
            allPhrases = allPhrases,
            progressMap = progressMap,
            categoryFilter = category,
            focusPhraseIds = focusPhraseIds,
            questionCount = questionCount
        )
        if (questions.isEmpty()) {
            _uiState.value = QuizUiState.Error("No phrases available for this quiz track.")
            return
        }
        _uiState.value = QuizUiState.Active(
            mode = mode,
            category = category,
            questions = questions,
            currentIndex = 0,
            selectedOptionIndex = null,
            records = emptyList()
        )
    }

    fun startSinglePhraseMiniQuiz(target: PhraseEntry, allPhrases: List<PhraseEntry>) {
        val questions = CuratedQuizGenerator.buildSinglePhraseMiniQuiz(target, allPhrases)
        _uiState.value = QuizUiState.Active(
            mode = QuizMode.SMART_REVIEW,
            category = CategoryFilter.fromCategory(target.category),
            questions = questions,
            currentIndex = 0,
            selectedOptionIndex = null,
            records = emptyList()
        )
    }

    fun startAiCustomQuiz(
        topicPrompt: String,
        weakTerms: List<String>,
        category: CategoryFilter
    ) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.LoadingAi(
                if (topicPrompt.isNotBlank()) "Crafting scenario quiz for \"$topicPrompt\"..."
                else "Generating personalized American English quiz..."
            )
            aiRepo.generateCustomQuiz(topicPrompt, weakTerms, category)
                .onSuccess { questions ->
                    _uiState.value = QuizUiState.Active(
                        mode = QuizMode.AI_CUSTOM,
                        category = category,
                        questions = questions,
                        currentIndex = 0,
                        selectedOptionIndex = null,
                        records = emptyList()
                    )
                }
                .onFailure { err ->
                    _uiState.value = QuizUiState.Error(
                        err.localizedMessage ?: "Could not generate AI quiz. Please try again."
                    )
                }
        }
    }

    fun selectAnswer(optionIndex: Int) {
        val active = _uiState.value as? QuizUiState.Active ?: return
        if (active.isAnswered) return
        val q = active.currentQuestion
        val correct = optionIndex == q.correctIndex
        val record = QuizAnswerRecord(
            question = q,
            selectedIndex = optionIndex,
            isCorrect = correct
        )
        _uiState.value = active.copy(
            selectedOptionIndex = optionIndex,
            records = active.records + record
        )
    }

    fun nextOrFinish() {
        val active = _uiState.value as? QuizUiState.Active ?: return
        if (!active.isAnswered) return
        if (!active.isLastQuestion) {
            _uiState.value = active.copy(
                currentIndex = active.currentIndex + 1,
                selectedOptionIndex = null
            )
        } else {
            val mode = active.mode
            val category = active.category
            val records = active.records
            _uiState.value = QuizUiState.Summary(
                mode = mode,
                category = category,
                records = records,
                syncSaved = false,
                syncError = null
            )
            viewModelScope.launch {
                firestoreRepo.recordQuizCompletion(mode, category, records)
                    .onSuccess {
                        val current = _uiState.value as? QuizUiState.Summary ?: return@onSuccess
                        _uiState.value = current.copy(syncSaved = true, syncError = null)
                    }
                    .onFailure { err ->
                        val current = _uiState.value as? QuizUiState.Summary ?: return@onFailure
                        _uiState.value = current.copy(
                            syncSaved = false,
                            syncError = err.localizedMessage ?: "Score displayed locally; cloud sync retry needed."
                        )
                    }
            }
        }
    }

    companion object {
        fun factory(
            firestoreRepo: LexiconFirestoreRepository,
            aiRepo: AiLexiconRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuizViewModel(firestoreRepo, aiRepo) as T
            }
        }
    }
}
