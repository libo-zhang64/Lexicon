package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PhraseCategory(val label: String, val shortLabel: String) {
    SLANG("Daily Slang", "Slang"),
    IDIOM("Idioms", "Idioms"),
    PHRASAL_VERB("Phrasal Verbs", "Phrasal Verbs"),
    WORKPLACE("Workplace & Nuance", "Workplace"),
    GRE_VOCAB("GRE Vocabulary", "GRE");

    companion object {
        fun fromRaw(raw: String?): PhraseCategory =
            entries.firstOrNull { it.name == raw } ?: SLANG
    }
}

enum class CategoryFilter(val label: String) {
    ALL("All Tracks"),
    SLANG("Daily Slang"),
    IDIOM("Idioms"),
    PHRASAL_VERB("Phrasal Verbs"),
    WORKPLACE("Workplace"),
    GRE_VOCAB("GRE Words");

    fun matches(category: PhraseCategory): Boolean = when (this) {
        ALL -> true
        SLANG -> category == PhraseCategory.SLANG
        IDIOM -> category == PhraseCategory.IDIOM
        PHRASAL_VERB -> category == PhraseCategory.PHRASAL_VERB
        WORKPLACE -> category == PhraseCategory.WORKPLACE
        GRE_VOCAB -> category == PhraseCategory.GRE_VOCAB
    }

    companion object {
        fun fromRaw(raw: String?): CategoryFilter =
            entries.firstOrNull { it.name == raw } ?: ALL

        fun fromCategory(category: PhraseCategory): CategoryFilter = when (category) {
            PhraseCategory.SLANG -> SLANG
            PhraseCategory.IDIOM -> IDIOM
            PhraseCategory.PHRASAL_VERB -> PHRASAL_VERB
            PhraseCategory.WORKPLACE -> WORKPLACE
            PhraseCategory.GRE_VOCAB -> GRE_VOCAB
        }
    }
}

enum class PhraseRegister(val label: String, val toneHint: String) {
    CASUAL("Casual", "Friends, texts, and informal everyday banter"),
    CONVERSATIONAL("Conversational", "Natural everyday American speech across most settings"),
    PROFESSIONAL("Professional", "Meetings, Slack, cross-functional teams, and clients"),
    FORMAL("Formal", "Executive summaries, presentations, and polished prose");

    companion object {
        fun fromRaw(raw: String?): PhraseRegister =
            entries.firstOrNull { it.name == raw } ?: CONVERSATIONAL
    }
}

enum class CefrLevel(val label: String) {
    B2("B2 Upper-Int"),
    C1("C1 Advanced"),
    C2("C2 Mastery");

    companion object {
        fun fromRaw(raw: String?): CefrLevel =
            entries.firstOrNull { it.name == raw } ?: C1
    }
}

enum class PhraseSource {
    CURATED,
    AI_GENERATED
}

enum class GoalCadence(val label: String, val periodLabel: String) {
    DAILY("Daily", "Today"),
    WEEKLY("Weekly", "This week");

    companion object {
        fun fromRaw(raw: String?): GoalCadence =
            entries.firstOrNull { it.name == raw } ?: WEEKLY
    }
}

enum class GoalMetricType(val label: String, val verb: String) {
    LEARN_NEW("Learn New Lessons", "Learn"),
    MASTER_PHRASES("Master Phrases", "Master"),
    COMPLETE_QUIZZES("Complete Quizzes", "Complete");

    companion object {
        fun fromRaw(raw: String?): GoalMetricType =
            entries.firstOrNull { it.name == raw } ?: LEARN_NEW
    }
}

enum class QuizMode(val label: String) {
    SMART_REVIEW("Smart Review"),
    GOAL_DRILL("Goal Drill"),
    CATEGORY_DRILL("Category Drill"),
    TRANSFER_CHALLENGE("Transfer Challenge"),
    AI_CUSTOM("AI Scenario Quiz"),
    EBBINGHAUS_REVIEW("Ebbinghaus Review");

    companion object {
        fun fromRaw(raw: String?): QuizMode =
            entries.firstOrNull { it.name == raw } ?: SMART_REVIEW
    }
}

enum class RecallRating(val label: String, val hint: String) {
    FORGOT("Forgot", "Reset • Again soon"),
    HARD("Hard", "Reinforce • Shorter gap"),
    GOOD("Good", "Advance • Next step"),
    EASY("Easy", "Strong • Jump ahead")
}

enum class QuizQuestionType(val label: String) {
    CLOZE("Context Cloze"),
    REGISTER("Register & Nuance"),
    MEANING("Meaning & Synonyms");

    companion object {
        fun fromRaw(raw: String?): QuizQuestionType =
            entries.firstOrNull { it.name == raw } ?: CLOZE
    }
}

data class DialogueTurn(
    val speaker: String,
    val line: String
)

data class SynonymNuance(
    val term: String,
    val nuanceDifference: String
)

data class PhraseEntry(
    val id: String,
    val term: String,
    val ipa: String,
    val category: PhraseCategory,
    val register: PhraseRegister,
    val cefrLevel: CefrLevel,
    val definition: String,
    val nuanceSummary: String,
    val dialogue: List<DialogueTurn>,
    val etymology: String,
    val synonyms: List<SynonymNuance>,
    val antonyms: List<String>,
    val usageNotes: List<String>,
    val source: PhraseSource = PhraseSource.CURATED
)

data class QuizQuestion(
    val id: String,
    val phraseId: String,
    val phraseTerm: String,
    val category: PhraseCategory,
    val questionType: QuizQuestionType,
    val prompt: String,
    val contextSnippet: String?,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val isTransferQuestion: Boolean = false,
    val transferDomainLabel: String? = null
)

data class QuizAnswerRecord(
    val question: QuizQuestion,
    val selectedIndex: Int,
    val isCorrect: Boolean
)

enum class TextExtractionFocus(val label: String, val promptHint: String) {
    BALANCED("All Strong Vocab", "Extract the highest-value mix of GRE-level words, idioms, phrasal verbs, and nuanced workplace phrases."),
    GRE_ACADEMIC("GRE & Academic", "Prioritize advanced GRE-level academic vocabulary, precise verbs, and formal analytical words."),
    IDIOMS_PHRASAL("Idioms & Phrasal", "Prioritize idiomatic expressions, figurative phrases, slang, and complex phrasal verbs."),
    WORKPLACE("Workplace Nuance", "Prioritize executive communication, persuasive phrasing, and professional workplace vocabulary.")
}

data class ExtractedVocabItem(
    val entry: PhraseEntry,
    val sourceExcerpt: String,
    val upgradeReason: String
)

data class TextExtractionResult(
    val passageLevel: String,
    val passageSummary: String,
    val items: List<ExtractedVocabItem>
)

// --- Firestore Document Models (1:1 parity with firestore.rules) ---

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    val userId: String = "",
    val displayName: String = "Learner",
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val lastPracticeDate: String = "",
    val totalLessonsCompleted: Long = 0L,
    val totalQuizzesCompleted: Long = 0L,
    val cumulativeCorrectAnswers: Long = 0L,
    val cumulativeQuizQuestions: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningGoalDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val cadence: String = GoalCadence.WEEKLY.name,
    val metricType: String = GoalMetricType.LEARN_NEW.name,
    val categoryFilter: String = CategoryFilter.ALL.name,
    val targetCount: Long = 5L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val cadenceEnum: GoalCadence get() = GoalCadence.fromRaw(cadence)
    val metricEnum: GoalMetricType get() = GoalMetricType.fromRaw(metricType)
    val categoryEnum: CategoryFilter get() = CategoryFilter.fromRaw(categoryFilter)
}

@Keep
@IgnoreExtraProperties
data class PhraseProgressDoc(
    val phraseId: String = "",
    val category: String = PhraseCategory.SLANG.name,
    val bookmarked: Boolean = false,
    val lessonCompleted: Boolean = false,
    val lessonCompletedDay: String = "",
    val lessonCompletedWeek: String = "",
    val masteryLevel: Long = 0L,
    val masteredDay: String = "",
    val masteredWeek: String = "",
    val correctCount: Long = 0L,
    val attemptCount: Long = 0L,
    val missCount: Long = 0L,
    val consecutiveCorrect: Long = 0L,
    val missedClozeCount: Long = 0L,
    val missedRegisterCount: Long = 0L,
    val missedMeaningCount: Long = 0L,
    val lastReviewedDay: String = "",
    val ebbinghausStage: Long = 0L,
    val nextReviewAtMillis: Long = 0L,
    val lastReviewAtMillis: Long = 0L,
    val intervalDays: Long = 0L,
    val reviewCount: Long = 0L,
    val lapseCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: PhraseCategory get() = PhraseCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class CustomPhraseDoc(
    @DocumentId val id: String = "",
    val term: String = "",
    val ipa: String = "",
    val category: String = PhraseCategory.SLANG.name,
    val register: String = PhraseRegister.CONVERSATIONAL.name,
    val cefrLevel: String = CefrLevel.C1.name,
    val definition: String = "",
    val nuanceSummary: String = "",
    val dialogue: List<Map<String, String>> = emptyList(),
    val etymology: String = "",
    val synonyms: List<Map<String, String>> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val usageNotes: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toPhraseEntry(): PhraseEntry = PhraseEntry(
        id = id.ifBlank { sanitizePhraseId(term) },
        term = term,
        ipa = ipa,
        category = PhraseCategory.fromRaw(category),
        register = PhraseRegister.fromRaw(register),
        cefrLevel = CefrLevel.fromRaw(cefrLevel),
        definition = definition,
        nuanceSummary = nuanceSummary,
        dialogue = dialogue.mapNotNull { turn ->
            val speaker = turn["speaker"]?.trim().orEmpty()
            val line = turn["line"]?.trim().orEmpty()
            if (speaker.isNotEmpty() && line.isNotEmpty()) DialogueTurn(speaker, line) else null
        },
        etymology = etymology,
        synonyms = synonyms.mapNotNull { syn ->
            val synTerm = syn["term"]?.trim().orEmpty()
            val diff = syn["nuanceDifference"]?.trim().orEmpty()
            if (synTerm.isNotEmpty()) SynonymNuance(synTerm, diff) else null
        },
        antonyms = antonyms.filter { it.isNotBlank() },
        usageNotes = usageNotes.filter { it.isNotBlank() },
        source = PhraseSource.AI_GENERATED
    )
}

@Keep
@IgnoreExtraProperties
data class QuizSessionDoc(
    @DocumentId val id: String = "",
    val mode: String = QuizMode.SMART_REVIEW.name,
    val category: String = CategoryFilter.ALL.name,
    val score: Long = 0L,
    val totalQuestions: Long = 1L,
    val missedPhraseIds: List<String> = emptyList(),
    val missedCloze: Long = 0L,
    val missedRegister: Long = 0L,
    val missedMeaning: Long = 0L,
    val periodDay: String = "",
    val periodWeek: String = "",
    @ServerTimestamp val completedAt: Timestamp? = null
)

fun sanitizePhraseId(raw: String): String {
    val slug = raw.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(64)
    return if (slug.isNotEmpty()) "ai_$slug" else "ai_phrase_${System.currentTimeMillis()}"
}

object PeriodKeys {
    fun todayDayKey(date: Date = Date()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(date)
    }

    fun yesterdayDayKey(date: Date = Date()): String {
        val cal = Calendar.getInstance(Locale.US).apply {
            time = date
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return todayDayKey(cal.time)
    }

    fun currentWeekKey(date: Date = Date()): String {
        val cal = Calendar.getInstance(Locale.US).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            time = date
        }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun daysBetween(olderDayKey: String, newerDayKey: String = todayDayKey()): Int {
        if (olderDayKey.isBlank()) return 999
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d1 = fmt.parse(olderDayKey) ?: return 999
            val d2 = fmt.parse(newerDayKey) ?: return 999
            val diffMs = d2.time - d1.time
            (diffMs / (1000L * 60L * 60L * 24L)).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            999
        }
    }

    fun readableTodayDate(date: Date = Date()): String {
        val fmt = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        return fmt.format(date)
    }
}
