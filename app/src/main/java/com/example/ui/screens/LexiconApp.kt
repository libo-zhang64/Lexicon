package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.TrackChanges
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
    val tag: String
) {
    TODAY("Today", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_today"),
    EXPLORE("Explore", Icons.Default.Explore, Icons.Outlined.Explore, "nav_explore"),
    PRACTICE("Practice", Icons.Default.Quiz, Icons.Outlined.Quiz, "nav_practice"),
    PATH("Path", Icons.Default.TrackChanges, Icons.Outlined.TrackChanges, "nav_path")
}

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
        factory = LexiconViewModel.factory(firestoreRepo, aiRepo)
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
    val quizState by quizViewModel.uiState.collectAsStateWithLifecycle()
    val speakingItemId by pronunciationController.speakingItemId.collectAsStateWithLifecycle()
    val isSlowAudio by pronunciationController.isSlowSpeed.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(LexiconTab.TODAY) }
    var activeDetailPhrase by remember { mutableStateOf<PhraseEntry?>(null) }

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
                selectedTab = LexiconTab.PRACTICE
                quizViewModel.startSinglePhraseMiniQuiz(detail, lexiconState.allPhrases)
            }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        LexiconTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
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
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    LexiconTab.TODAY -> TodayScreen(
                        uiState = lexiconState,
                        speakingItemId = speakingItemId,
                        isSlowAudio = isSlowAudio,
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onToggleSpeed = { pronunciationController.toggleSpeed() },
                        onSelectCategory = { lexiconViewModel.selectCategory(it) },
                        onOpenPhrase = { activeDetailPhrase = it },
                        onToggleBookmark = { lexiconViewModel.toggleBookmark(it) },
                        onQuickDailyQuiz = {
                            selectedTab = LexiconTab.PRACTICE
                            quizViewModel.startCuratedQuiz(
                                mode = QuizMode.SMART_REVIEW,
                                category = CategoryFilter.ALL,
                                allPhrases = lexiconState.allPhrases,
                                progressMap = lexiconState.progressMap,
                                focusPhraseIds = lexiconState.insights.frequentMistakePhrases.map { it.phrase.id }
                            )
                        },
                        onRecommendationClick = handleRecommendation,
                        onOpenGoalsTab = { selectedTab = LexiconTab.PATH },
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
                        onSpeak = { itemId, text -> pronunciationController.speak(itemId, text) },
                        onToggleSpeed = { pronunciationController.toggleSpeed() },
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
                        onStartAiQuiz = { topic, category ->
                            val weakTerms = lexiconState.insights.frequentMistakePhrases.map { it.phrase.term }
                            quizViewModel.startAiCustomQuiz(topic, weakTerms, category)
                        },
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
