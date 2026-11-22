package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CategoryFilter
import com.example.data.model.CustomPhraseDoc
import com.example.data.model.GoalCadence
import com.example.data.model.GoalMetricType
import com.example.data.model.LearningGoalDoc
import com.example.data.model.PeriodKeys
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizAnswerRecord
import com.example.data.model.QuizMode
import com.example.data.model.QuizQuestionType
import com.example.data.model.QuizSessionDoc
import com.example.data.model.UserProfileDoc
import com.example.data.model.sanitizePhraseId
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LexiconFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun handleFlowError(error: Throwable, operation: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operation, path)
        }
    }

    fun observeUserProfile(): Flow<UserProfileDoc?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeLearningGoals(): Flow<List<LearningGoalDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/learningGoals"
        emitAll(
            db.collection("users").document(uid).collection("learningGoals")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LearningGoalDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePhraseProgress(): Flow<Map<String, PhraseProgressDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/phraseProgress"
        emitAll(
            db.collection("users").document(uid).collection("phraseProgress")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PhraseProgressDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .associateBy { it.phraseId }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCustomPhrases(): Flow<List<PhraseEntry>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/customPhrases"
        emitAll(
            db.collection("users").document(uid).collection("customPhrases")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomPhraseDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .map { it.toPhraseEntry() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeQuizSessions(): Flow<List<QuizSessionDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/quizSessions"
        emitAll(
            db.collection("users").document(uid).collection("quizSessions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuizSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.completedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(rawDisplayName: String?): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = rawDisplayName?.trim()?.take(80).takeUnless { it.isNullOrBlank() } ?: "Learner"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "currentStreak" to 0L,
                    "longestStreak" to 0L,
                    "lastPracticeDate" to "",
                    "totalLessonsCompleted" to 0L,
                    "totalQuizzesCompleted" to 0L,
                    "cumulativeCorrectAnswers" to 0L,
                    "cumulativeQuizQuestions" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createLearningGoal(
        title: String,
        cadence: GoalCadence,
        metricType: GoalMetricType,
        categoryFilter: CategoryFilter,
        targetCount: Int
    ): Result<Unit> {
        var path = "users/unknown/learningGoals"
        return try {
            val uid = requireUserId()
            val trimmedTitle = title.trim().take(120)
            require(trimmedTitle.isNotEmpty()) { "Goal title cannot be blank." }
            val boundedTarget = targetCount.coerceIn(1, 50)
            val ref = db.collection("users").document(uid).collection("learningGoals").document()
            path = ref.path
            val payload = mapOf(
                "title" to trimmedTitle,
                "cadence" to cadence.name,
                "metricType" to metricType.name,
                "categoryFilter" to categoryFilter.name,
                "targetCount" to boundedTarget.toLong(),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateLearningGoal(
        goalId: String,
        title: String,
        cadence: GoalCadence,
        metricType: GoalMetricType,
        categoryFilter: CategoryFilter,
        targetCount: Int,
        active: Boolean
    ): Result<Unit> {
        var path = "users/unknown/learningGoals/$goalId"
        return try {
            val uid = requireUserId()
            require(goalId.isNotBlank()) { "Goal ID is required." }
            val trimmedTitle = title.trim().take(120)
            require(trimmedTitle.isNotEmpty()) { "Goal title cannot be blank." }
            val boundedTarget = targetCount.coerceIn(1, 50)
            val ref = db.collection("users").document(uid).collection("learningGoals").document(goalId)
            path = ref.path
            val payload = mapOf(
                "title" to trimmedTitle,
                "cadence" to cadence.name,
                "metricType" to metricType.name,
                "categoryFilter" to categoryFilter.name,
                "targetCount" to boundedTarget.toLong(),
                "active" to active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLearningGoal(goalId: String): Result<Unit> {
        var path = "users/unknown/learningGoals/$goalId"
        return try {
            val uid = requireUserId()
            require(goalId.isNotBlank()) { "Goal ID is required." }
            val ref = db.collection("users").document(uid).collection("learningGoals").document(goalId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(phrase: PhraseEntry, currentlyBookmarked: Boolean): Result<Unit> {
        var path = "users/unknown/phraseProgress/${phrase.id}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("phraseProgress").document(phrase.id)
            path = ref.path
            val snap = ref.get().await()
            val newBookmarked = !currentlyBookmarked
            if (!snap.exists()) {
                ref.set(
                    buildInitialProgressPayload(
                        phraseId = phrase.id,
                        category = phrase.category,
                        bookmarked = newBookmarked
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "bookmarked" to newBookmarked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markLessonCompleted(phrase: PhraseEntry): Result<Unit> {
        var path = "users/unknown/phraseProgress/${phrase.id}"
        return try {
            val uid = requireUserId()
            val today = PeriodKeys.todayDayKey()
            val week = PeriodKeys.currentWeekKey()
            val progressRef = db.collection("users").document(uid).collection("phraseProgress").document(phrase.id)
            path = progressRef.path

            val snap = progressRef.get().await()
            val wasAlreadyCompleted = if (!snap.exists()) {
                progressRef.set(
                    buildInitialProgressPayload(
                        phraseId = phrase.id,
                        category = phrase.category,
                        lessonCompleted = true,
                        lessonCompletedDay = today,
                        lessonCompletedWeek = week,
                        masteryLevel = 1L,
                        lastReviewedDay = today
                    )
                ).await()
                false
            } else {
                val existing = snap.toObject(PhraseProgressDoc::class.java)
                val alreadyDone = existing?.lessonCompleted == true
                val newMastery = maxOf(existing?.masteryLevel ?: 0L, 1L).coerceIn(0L, 3L)
                progressRef.update(
                    mapOf(
                        "lessonCompleted" to true,
                        "lessonCompletedDay" to (existing?.lessonCompletedDay?.takeIf { it.isNotBlank() } ?: today),
                        "lessonCompletedWeek" to (existing?.lessonCompletedWeek?.takeIf { it.isNotBlank() } ?: week),
                        "masteryLevel" to newMastery,
                        "lastReviewedDay" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                alreadyDone
            }

            updateUserProfilePractice(
                uid = uid,
                addedLessons = if (wasAlreadyCompleted) 0L else 1L,
                addedQuizzes = 0L,
                addedCorrect = 0L,
                addedQuestions = 0L
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCustomPhrase(phrase: PhraseEntry): Result<PhraseEntry> {
        val docId = sanitizePhraseId(phrase.term)
        var path = "users/unknown/customPhrases/$docId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("customPhrases").document(docId)
            path = ref.path
            val snap = ref.get().await()

            val dialogueMaps = phrase.dialogue.take(10).map {
                mapOf("speaker" to it.speaker.take(60), "line" to it.line.take(400))
            }
            val synonymMaps = phrase.synonyms.take(10).map {
                mapOf("term" to it.term.take(80), "nuanceDifference" to it.nuanceDifference.take(300))
            }
            val antonymsList = phrase.antonyms.take(10).map { it.take(80) }
            val notesList = phrase.usageNotes.take(10).map { it.take(300) }

            val baseFields = mapOf(
                "term" to phrase.term.trim().take(120).ifBlank { "Expression" },
                "ipa" to phrase.ipa.trim().take(120),
                "category" to phrase.category.name,
                "register" to phrase.register.name,
                "cefrLevel" to phrase.cefrLevel.name,
                "definition" to phrase.definition.trim().take(800).ifBlank { "American English expression." },
                "nuanceSummary" to phrase.nuanceSummary.trim().take(800),
                "dialogue" to dialogueMaps,
                "etymology" to phrase.etymology.trim().take(1600),
                "synonyms" to synonymMaps,
                "antonyms" to antonymsList,
                "usageNotes" to notesList,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!snap.exists()) {
                ref.set(baseFields + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                ref.update(baseFields).await()
            }

            // Also bookmark it in phraseProgress so it appears cleanly in Saved Lexicon
            val progressRef = db.collection("users").document(uid).collection("phraseProgress").document(docId)
            val progSnap = progressRef.get().await()
            if (!progSnap.exists()) {
                progressRef.set(
                    buildInitialProgressPayload(
                        phraseId = docId,
                        category = phrase.category,
                        bookmarked = true
                    )
                ).await()
            } else {
                progressRef.update(
                    mapOf("bookmarked" to true, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }

            Result.success(phrase.copy(id = docId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomPhrase(phraseId: String): Result<Unit> {
        var path = "users/unknown/customPhrases/$phraseId"
        return try {
            val uid = requireUserId()
            require(phraseId.isNotBlank()) { "Phrase ID is required." }
            val ref = db.collection("users").document(uid).collection("customPhrases").document(phraseId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizCompletion(
        mode: QuizMode,
        category: CategoryFilter,
        records: List<QuizAnswerRecord>
    ): Result<Unit> {
        if (records.isEmpty()) return Result.success(Unit)
        var path = "users/unknown/quizSessions"
        return try {
            val uid = requireUserId()
            val today = PeriodKeys.todayDayKey()
            val week = PeriodKeys.currentWeekKey()

            val totalQuestions = records.size.coerceIn(1, 50)
            val score = records.count { it.isCorrect }.coerceIn(0, totalQuestions)
            val missedPhraseIds = records.filterNot { it.isCorrect }
                .map { it.question.phraseId }
                .distinct()
                .take(50)
            val missedCloze = records.count { !it.isCorrect && it.question.questionType == QuizQuestionType.CLOZE }
            val missedRegister = records.count { !it.isCorrect && it.question.questionType == QuizQuestionType.REGISTER }
            val missedMeaning = records.count { !it.isCorrect && it.question.questionType == QuizQuestionType.MEANING }

            // 1. Write append-only quiz session document
            val sessionRef = db.collection("users").document(uid).collection("quizSessions").document()
            path = sessionRef.path
            sessionRef.set(
                mapOf(
                    "mode" to mode.name,
                    "category" to category.name,
                    "score" to score.toLong(),
                    "totalQuestions" to totalQuestions.toLong(),
                    "missedPhraseIds" to missedPhraseIds,
                    "missedCloze" to missedCloze.toLong(),
                    "missedRegister" to missedRegister.toLong(),
                    "missedMeaning" to missedMeaning.toLong(),
                    "periodDay" to today,
                    "periodWeek" to week,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 2. Update per-phrase mastery & mistake telemetry for every tested phrase
            val grouped = records.groupBy { it.question.phraseId }
            for ((phraseId, phraseRecords) in grouped) {
                if (!phraseId.matches(Regex("^[a-zA-Z0-9_\\-]{1,100}$"))) continue
                val sample = phraseRecords.first().question
                val pRef = db.collection("users").document(uid).collection("phraseProgress").document(phraseId)
                path = pRef.path
                val snap = pRef.get().await()
                val existing = snap.toObject(PhraseProgressDoc::class.java)

                val addedCorrect = phraseRecords.count { it.isCorrect }.toLong()
                val addedAttempts = phraseRecords.size.toLong()
                val addedMisses = phraseRecords.count { !it.isCorrect }.toLong()
                val addedClozeMiss = phraseRecords.count { !it.isCorrect && it.question.questionType == QuizQuestionType.CLOZE }.toLong()
                val addedRegisterMiss = phraseRecords.count { !it.isCorrect && it.question.questionType == QuizQuestionType.REGISTER }.toLong()
                val addedMeaningMiss = phraseRecords.count { !it.isCorrect && it.question.questionType == QuizQuestionType.MEANING }.toLong()

                val prevConsecutive = existing?.consecutiveCorrect ?: 0L
                val newConsecutive = if (addedMisses > 0L) 0L else prevConsecutive + addedCorrect
                val prevMastery = existing?.masteryLevel ?: 0L
                val newMastery = when {
                    addedMisses > 0L -> maxOf(1L, prevMastery - 1L)
                    newConsecutive >= 2L -> 3L
                    else -> maxOf(prevMastery, 2L)
                }.coerceIn(0L, 3L)

                val reachedMasteryNow = newMastery == 3L && prevMastery < 3L
                val masteredDay = when {
                    reachedMasteryNow -> today
                    newMastery == 3L -> existing?.masteredDay.orEmpty()
                    else -> ""
                }
                val masteredWeek = when {
                    reachedMasteryNow -> week
                    newMastery == 3L -> existing?.masteredWeek.orEmpty()
                    else -> ""
                }

                if (!snap.exists()) {
                    pRef.set(
                        mapOf(
                            "phraseId" to phraseId,
                            "category" to sample.category.name,
                            "bookmarked" to false,
                            "lessonCompleted" to false,
                            "lessonCompletedDay" to "",
                            "lessonCompletedWeek" to "",
                            "masteryLevel" to newMastery,
                            "masteredDay" to masteredDay,
                            "masteredWeek" to masteredWeek,
                            "correctCount" to addedCorrect,
                            "attemptCount" to addedAttempts,
                            "missCount" to addedMisses,
                            "consecutiveCorrect" to newConsecutive,
                            "missedClozeCount" to addedClozeMiss,
                            "missedRegisterCount" to addedRegisterMiss,
                            "missedMeaningCount" to addedMeaningMiss,
                            "lastReviewedDay" to today,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    pRef.update(
                        mapOf(
                            "masteryLevel" to newMastery,
                            "masteredDay" to masteredDay,
                            "masteredWeek" to masteredWeek,
                            "correctCount" to ((existing?.correctCount ?: 0L) + addedCorrect),
                            "attemptCount" to ((existing?.attemptCount ?: 0L) + addedAttempts),
                            "missCount" to ((existing?.missCount ?: 0L) + addedMisses),
                            "consecutiveCorrect" to newConsecutive,
                            "missedClozeCount" to ((existing?.missedClozeCount ?: 0L) + addedClozeMiss),
                            "missedRegisterCount" to ((existing?.missedRegisterCount ?: 0L) + addedRegisterMiss),
                            "missedMeaningCount" to ((existing?.missedMeaningCount ?: 0L) + addedMeaningMiss),
                            "lastReviewedDay" to today,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // 3. Update UserProfile streak & quiz metrics
            updateUserProfilePractice(
                uid = uid,
                addedLessons = 0L,
                addedQuizzes = 1L,
                addedCorrect = score.toLong(),
                addedQuestions = totalQuestions.toLong()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun updateUserProfilePractice(
        uid: String,
        addedLessons: Long,
        addedQuizzes: Long,
        addedCorrect: Long,
        addedQuestions: Long
    ) {
        val today = PeriodKeys.todayDayKey()
        val yesterday = PeriodKeys.yesterdayDayKey()
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        val displayName = auth.currentUser?.displayName?.trim()?.take(80)
            .takeUnless { it.isNullOrBlank() } ?: "Learner"

        if (!snap.exists()) {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "currentStreak" to 1L,
                    "longestStreak" to 1L,
                    "lastPracticeDate" to today,
                    "totalLessonsCompleted" to addedLessons,
                    "totalQuizzesCompleted" to addedQuizzes,
                    "cumulativeCorrectAnswers" to addedCorrect,
                    "cumulativeQuizQuestions" to addedQuestions,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val existing = snap.toObject(UserProfileDoc::class.java)
            val prevDate = existing?.lastPracticeDate.orEmpty()
            val prevStreak = existing?.currentStreak ?: 0L
            val newStreak = when (prevDate) {
                today -> maxOf(1L, prevStreak)
                yesterday -> prevStreak + 1L
                else -> 1L
            }
            val newLongest = maxOf(existing?.longestStreak ?: 0L, newStreak)
            val newLessons = (existing?.totalLessonsCompleted ?: 0L) + addedLessons
            val newQuizzes = (existing?.totalQuizzesCompleted ?: 0L) + addedQuizzes
            val newCorrect = (existing?.cumulativeCorrectAnswers ?: 0L) + addedCorrect
            val newQuestions = (existing?.cumulativeQuizQuestions ?: 0L) + addedQuestions

            ref.update(
                mapOf(
                    "currentStreak" to newStreak,
                    "longestStreak" to newLongest,
                    "lastPracticeDate" to today,
                    "totalLessonsCompleted" to newLessons,
                    "totalQuizzesCompleted" to newQuizzes,
                    "cumulativeCorrectAnswers" to minOf(newCorrect, newQuestions),
                    "cumulativeQuizQuestions" to newQuestions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private fun buildInitialProgressPayload(
        phraseId: String,
        category: PhraseCategory,
        bookmarked: Boolean = false,
        lessonCompleted: Boolean = false,
        lessonCompletedDay: String = "",
        lessonCompletedWeek: String = "",
        masteryLevel: Long = 0L,
        lastReviewedDay: String = ""
    ): Map<String, Any> = mapOf(
        "phraseId" to phraseId,
        "category" to category.name,
        "bookmarked" to bookmarked,
        "lessonCompleted" to lessonCompleted,
        "lessonCompletedDay" to lessonCompletedDay,
        "lessonCompletedWeek" to lessonCompletedWeek,
        "masteryLevel" to masteryLevel,
        "masteredDay" to "",
        "masteredWeek" to "",
        "correctCount" to 0L,
        "attemptCount" to 0L,
        "missCount" to 0L,
        "consecutiveCorrect" to 0L,
        "missedClozeCount" to 0L,
        "missedRegisterCount" to 0L,
        "missedMeaningCount" to 0L,
        "lastReviewedDay" to lastReviewedDay,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
