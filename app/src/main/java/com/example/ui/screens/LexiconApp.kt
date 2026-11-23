package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.audio.PronunciationController
import com.example.data.firebase.AiLexiconRepository
import com.example.data.firebase.LexiconFirestoreRepository
import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseEntry
import com.example.data.model.QuizMode
import com.example.domain.ReviewAction
import com.example.domain.ReviewRecommendation
import com.example.ui.viewmodel.LexiconViewModel
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.signOutGoogle
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private enum class LexiconTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
    val showInBottomBar: Boolean = true
) {
    TODAY("Learn", Icons.Default.School, Icons.Outlined.School, "nav_today"),
    REVIEW("Review", Icons.Default.AutoGraph, Icons.Outlined.AutoGraph, "nav_review"),
    EXPLORE("Explore", Icons.Default.Explore, Icons.Outlined.Explore, "nav_explore"),
    PRACTICE("Practice", Icons.Default.Quiz, Icons.Outlined.Quiz, "nav_practice"),
    PATH("Path", Icons.Default.TrackChanges, Icons.Outlined.TrackChanges, "nav_path"),
    EXTRACT("Extract", Icons.Default.UploadFile, Icons.Outlined.UploadFile, "nav_extract", showInBottomBar = false)
}

private data class ActiveLearningSession(
    val trackLabel: String,
    val category: CategoryFilter,
    val phrases: List<PhraseEntry>
)

@Composable
fun LexiconAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            AuthenticatedLexiconShell(
                currentUserId = uid,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedLexiconShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val firestoreRepo = remember(currentUserId) { LexiconFirestoreRepository(context) }
    val aiRepo = remember(currentUserId) { AiLexiconRepository() }

    val lexiconViewModel: LexiconViewModel = viewModel(
        key = "lexicon_$currentUserId",
        factory = LexiconViewModel.factory(context, firestoreRepo, aiRepo)
    )
    val quizViewModel: QuizViewModel = viewModel(
        key = "quiz_$currentUserId",
        factory = QuizViewModel.factory(firestoreRepo, aiRepo)
    )

    val pronunciationController = remember(context) { PronunciationController(context) }
    DisposableEffect(pronunciationController) {
        onDispose { pronunciationController.shutdown() }
    }

    val lexiconState by lexiconViewModel.uiState.collectAsStateWithLifecycle()
    val extractorState by lexiconViewModel.textExtractorState.collectAsStateWithLifecycle()
    val quizState by quizViewModel.uiState.collectAsStateWithLifecycle()
    val speakingItemId by pronunciationController.speakingItemId.collectAsStateWithLifecycle()
    val isSlowAudio by pronunciationController.isSlowSpeed.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(LexiconTab.TODAY) }
    var showExtractorOverlay by rememberSaveable { mutableStateOf(false) }
    var activeDetailPhrase by remember { mutableStateOf<PhraseEntry?>(null) }
    var activeLearningSession by remember { mutableStateOf<ActiveLearningSession?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(lexiconState.bannerMessage) {
        val msg = lexiconState.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            lexiconViewModel.dismissBanner()
        }
    }

    val handleRecommendation: (ReviewRecommendation) -> Unit = { rec ->
        when (val action = rec.action) {
            is ReviewAction.OpenPhrase -> {
                val target = lexiconState.allPhrases.firstOrNull { it.id == action.phraseId }
                if (target != null) activeDetailPhrase = target
            }
            is ReviewAction.LaunchTargetedQuiz -> {
                selectedTab = LexiconTab.PRACTICE
                quizViewModel.startCuratedQuiz(
                    mode = action.mode,
                    category = action.category,
                    allPhrases = lexiconState.allPhrases,
                    progressMap = lexiconState.progressMap,
                    focusPhraseIds = action.focusPhraseIds
                )
            }
        }
    }

    val startGuidedLesson: (CategoryFilter) -> Unit = { category ->
        val batch = lexiconViewModel.buildLearningBatch(category = category, count = 5)
        if (batch.isNotEmpty()) {
            activeLearningSession = ActiveLearningSession(
                trackLabel = if (category == CategoryFilter.ALL) "Daily Guided Session" else "${category.label} Track",
                category = category,
                phrases = batch
            )
        } else {
            selectedTab = LexiconTab.TODAY
        }
    }

    val session = activeLearningSession
    if (session != null) {
        LearningSessionScreen(
            trackLabel = session.trackLabel,
            categoryFilter = session.category,
            phrases = session.phrases,
            progressMap = lexiconState.progressMap,
            speakingItemId = speakingItemId,
            isSlowAudio = isSlowAudio,
            onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
            onToggleSpeed = { pronunciationController.toggleSpeed() },
            onToggleBookmark = { lexiconViewModel.toggleBookmark(it) },
            onCompletePhrase = { lexiconViewModel.markLessonCompleted(it, showBanner = false) },
            onFinishSession = { lexiconViewModel.markSessionLessonsCompleted(it) },
            onLaunchTransferQuiz = { category, studiedIds ->
                pronunciationController.stop()
                activeLearningSession = null
                showExtractorOverlay = false
                selectedTab = LexiconTab.PRACTICE
                quizViewModel.startTransferQuiz(
                    allPhrases = lexiconState.allPhrases,
                    progressMap = lexiconState.progressMap,
                    category = category,
                    focusPhraseIds = studiedIds,
                    allowStarterPreviewIfEmpty = true
                )
            },
            onLaunchReviewQuiz = { studiedIds ->
                pronunciationController.stop()
                activeLearningSession = null
                showExtractorOverlay = false
                selectedTab = LexiconTab.PRACTICE
                quizViewModel.startCuratedQuiz(
                    mode = QuizMode.SMART_REVIEW,
                    category = session.category,
                    allPhrases = lexiconState.allPhrases,
                    progressMap = lexiconState.progressMap,
                    focusPhraseIds = studiedIds
                )
            },
            onExit = {
                pronunciationController.stop()
                activeLearningSession = null
            }
        )
        return
    }

    val detail = activeDetailPhrase
    if (detail != null) {
        PhraseDetailScreen(
            phrase = detail,
            progress = lexiconState.progressMap[detail.id],
            speakingItemId = speakingItemId,
            isSlowAudio = isSlowAudio,
            onBack = {
                pronunciationController.stop()
                activeDetailPhrase = null
            },
            onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
            onToggleSpeed = { pronunciationController.toggleSpeed() },
            onToggleBookmark = { lexiconViewModel.toggleBookmark(detail) },
            onMarkLessonComplete = { lexiconViewModel.markLessonCompleted(detail) },
            onQuizThisPhrase = {
                pronunciationController.stop()
                activeDetailPhrase = null
                showExtractorOverlay = false
                selectedTab = LexiconTab.PRACTICE
                quizViewModel.startSinglePhraseMiniQuiz(detail, lexiconState.allPhrases)
            }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val dueReviewCount = lexiconState.ebbinghaus.dueNowCount
        val bottomTabs = remember { LexiconTab.entries.filter { it.showInBottomBar } }

        val openExtractor: () -> Unit = {
            if (useRail) {
                selectedTab = LexiconTab.EXTRACT
            } else {
                showExtractorOverlay = true
            }
        }

        if (!useRail && showExtractorOverlay) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                TextExtractorScreen(
                    state = extractorState,
                    speakingItemId = speakingItemId,
                    onTextChange = { lexiconViewModel.updateExtractorText(it) },
                    onFileLoaded = { name, content -> lexiconViewModel.onExtractorFileLoaded(name, content) },
                    onFileError = { lexiconViewModel.onExtractorFileError(it) },
                    onSelectFocus = { lexiconViewModel.selectExtractionFocus(it) },
                    onExtract = { lexiconViewModel.extractVocabularyFromCurrentText() },
                    onClear = { lexiconViewModel.clearTextExtractor() },
                    onSavePhrase = { lexiconViewModel.saveExtractedPhrase(it) },
                    onSaveAll = { lexiconViewModel.saveAllExtractedPhrases() },
                    onOpenPhrase = { activeDetailPhrase = it },
                    onStudyExtracted = { extractedEntries ->
                        if (extractedEntries.isNotEmpty()) {
                            showExtractorOverlay = false
                            activeLearningSession = ActiveLearningSession(
                                trackLabel = extractorState.fileName ?: "Extracted Vocab Deck",
                                category = CategoryFilter.ALL,
                                phrases = extractedEntries
                            )
                        }
                    },
                    onQuizExtracted = { extractedEntries ->
                        if (extractedEntries.isNotEmpty()) {
                            showExtractorOverlay = false
                            selectedTab = LexiconTab.PRACTICE
                            quizViewModel.startExtractedQuiz(
                                extractedPhrases = extractedEntries,
                                allPhrases = lexiconState.allPhrases,
                                progressMap = lexiconState.progressMap
                            )
                        }
                    },
                    onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                    onBack = { showExtractorOverlay = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            return@BoxWithConstraints
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        bottomTabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        isSelected = isSelected,
                                        dueReviewCount = dueReviewCount
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        LexiconTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        isSelected = isSelected,
                                        dueReviewCount = dueReviewCount
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                val effectiveTab = if (!useRail && selectedTab == LexiconTab.EXTRACT) {
                    LexiconTab.EXPLORE
                } else {
                    selectedTab
                }

                when (effectiveTab) {
                    LexiconTab.TODAY -> TodayScreen(
                        uiState = lexiconState,
                        speakingItemId = speakingItemId,
                        isSlowAudio = isSlowAudio,
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onToggleSpeed = { pronunciationController.toggleSpeed() },
                        onSelectCategory = { lexiconViewModel.selectCategory(it) },
                        onStartLearningSession = startGuidedLesson,
                        onStartTransferChallenge = {
                            selectedTab = LexiconTab.PRACTICE
                            if (lexiconState.learnedPhraseCount > 0) {
                                quizViewModel.startTransferQuiz(
                                    allPhrases = lexiconState.allPhrases,
                                    progressMap = lexiconState.progressMap,
                                    category = CategoryFilter.ALL
                                )
                            } else {
                                quizViewModel.resetToSetup()
                            }
                        },
                        onCycleSpotlight = { delta -> lexiconViewModel.cycleSpotlight(delta) },
                        onOpenPhrase = { activeDetailPhrase = it },
                        onToggleBookmark = { lexiconViewModel.toggleBookmark(it) },
                        onOpenExplore = { selectedTab = LexiconTab.EXPLORE },
                        onOpenExtractor = openExtractor,
                        onOpenReviewTab = { selectedTab = LexiconTab.REVIEW },
                        onRecommendationClick = handleRecommendation,
                        onOpenGoalsTab = { selectedTab = LexiconTab.PATH },
                        modifier = Modifier.weight(1f)
                    )

                    LexiconTab.REVIEW -> ReviewScreen(
                        uiState = lexiconState,
                        speakingItemId = speakingItemId,
                        isSlowAudio = isSlowAudio,
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onToggleSpeed = { pronunciationController.toggleSpeed() },
                        onStartActiveRecallQuiz = { reviewPhrases ->
                            selectedTab = LexiconTab.PRACTICE
                            quizViewModel.startEbbinghausReviewQuiz(
                                reviewPhrases = reviewPhrases,
                                allPhrases = lexiconState.allPhrases
                            )
                        },
                        onRateFlashcard = { phrase, rating, onScheduled ->
                            lexiconViewModel.recordFlashcardReview(phrase, rating, onScheduled)
                        },
                        onOpenPhrase = { activeDetailPhrase = it },
                        onStartLessonFromEmpty = { startGuidedLesson(CategoryFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )

                    LexiconTab.EXPLORE -> ExploreScreen(
                        uiState = lexiconState,
                        speakingItemId = speakingItemId,
                        isSlowAudio = isSlowAudio,
                        onQueryChange = { lexiconViewModel.updateSearchQuery(it) },
                        onAskAi = { lexiconViewModel.lookupPhraseWithAi(it) },
                        onSaveAiPhrase = { lexiconViewModel.saveAiPhraseToLexicon(it) },
                        onOpenPhrase = { activeDetailPhrase = it },
                        onToggleBookmark = { lexiconViewModel.toggleBookmark(it) },
                        onOpenExtractor = openExtractor,
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onToggleSpeed = { pronunciationController.toggleSpeed() },
                        modifier = Modifier.weight(1f)
                    )

                    LexiconTab.EXTRACT -> TextExtractorScreen(
                        state = extractorState,
                        speakingItemId = speakingItemId,
                        onTextChange = { lexiconViewModel.updateExtractorText(it) },
                        onFileLoaded = { name, content -> lexiconViewModel.onExtractorFileLoaded(name, content) },
                        onFileError = { lexiconViewModel.onExtractorFileError(it) },
                        onSelectFocus = { lexiconViewModel.selectExtractionFocus(it) },
                        onExtract = { lexiconViewModel.extractVocabularyFromCurrentText() },
                        onClear = { lexiconViewModel.clearTextExtractor() },
                        onSavePhrase = { lexiconViewModel.saveExtractedPhrase(it) },
                        onSaveAll = { lexiconViewModel.saveAllExtractedPhrases() },
                        onOpenPhrase = { activeDetailPhrase = it },
                        onStudyExtracted = { extractedEntries ->
                            if (extractedEntries.isNotEmpty()) {
                                activeLearningSession = ActiveLearningSession(
                                    trackLabel = extractorState.fileName ?: "Extracted Vocab Deck",
                                    category = CategoryFilter.ALL,
                                    phrases = extractedEntries
                                )
                            }
                        },
                        onQuizExtracted = { extractedEntries ->
                            if (extractedEntries.isNotEmpty()) {
                                selectedTab = LexiconTab.PRACTICE
                                quizViewModel.startExtractedQuiz(
                                    extractedPhrases = extractedEntries,
                                    allPhrases = lexiconState.allPhrases,
                                    progressMap = lexiconState.progressMap
                                )
                            }
                        },
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        modifier = Modifier.weight(1f)
                    )

                    LexiconTab.PRACTICE -> PracticeScreen(
                        lexiconState = lexiconState,
                        quizState = quizState,
                        speakingItemId = speakingItemId,
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onStartCuratedQuiz = { mode, category, focusIds ->
                            quizViewModel.startCuratedQuiz(
                                mode = mode,
                                category = category,
                                allPhrases = lexiconState.allPhrases,
                                progressMap = lexiconState.progressMap,
                                focusPhraseIds = focusIds
                            )
                        },
                        onStartTransferQuiz = { category, allowPreview ->
                            quizViewModel.startTransferQuiz(
                                allPhrases = lexiconState.allPhrases,
                                progressMap = lexiconState.progressMap,
                                category = category,
                                allowStarterPreviewIfEmpty = allowPreview
                            )
                        },
                        onStartAiTransferBoost = { category ->
                            quizViewModel.startAiTransferBoostQuiz(
                                allPhrases = lexiconState.allPhrases,
                                progressMap = lexiconState.progressMap,
                                category = category
                            )
                        },
                        onStartAiQuiz = { topic, category ->
                            val weakTerms = lexiconState.insights.frequentMistakePhrases.map { it.phrase.term }
                            quizViewModel.startAiCustomQuiz(topic, weakTerms, category)
                        },
                        onGoToLearn = { selectedTab = LexiconTab.TODAY },
                        onSelectAnswer = { quizViewModel.selectAnswer(it) },
                        onNextOrFinish = { quizViewModel.nextOrFinish() },
                        onResetQuiz = { quizViewModel.resetToSetup() },
                        modifier = Modifier.weight(1f)
                    )

                    LexiconTab.PATH -> PathScreen(
                        uiState = lexiconState,
                        speakingItemId = speakingItemId,
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onCreateGoal = { title, cadence, metric, cat, target ->
                            lexiconViewModel.createGoal(title, cadence, metric, cat, target)
                        },
                        onUpdateGoal = { id, title, cadence, metric, cat, target, active ->
                            lexiconViewModel.updateGoal(id, title, cadence, metric, cat, target, active)
                        },
                        onDeleteGoal = { lexiconViewModel.deleteGoal(it) },
                        onRecommendationClick = handleRecommendation,
                        onAiWeakSpotDrill = {
                            selectedTab = LexiconTab.PRACTICE
                            val weakTerms = lexiconState.insights.frequentMistakePhrases.map { it.phrase.term }
                            quizViewModel.startAiCustomQuiz(
                                topicPrompt = "Targeted review of my recent mistakes and challenging workplace/idiom nuances",
                                weakTerms = weakTerms,
                                category = CategoryFilter.ALL
                            )
                        },
                        onOpenPhrase = { activeDetailPhrase = it },
                        onToggleBookmark = { lexiconViewModel.toggleBookmark(it) },
                        onSignOut = {
                            pronunciationController.stop()
                            signOutGoogle(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignedOut
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: LexiconTab,
    isSelected: Boolean,
    dueReviewCount: Int
) {
    val icon = if (isSelected) tab.selectedIcon else tab.unselectedIcon
    if (tab == LexiconTab.REVIEW && dueReviewCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(text = if (dueReviewCount > 99) "99+" else dueReviewCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = "${tab.label}, $dueReviewCount due")
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
