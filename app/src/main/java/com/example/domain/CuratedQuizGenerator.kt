package com.example.domain

import com.example.data.catalog.NaturalDialogueSynthesizer
import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType
import java.util.Locale
import kotlin.math.abs

object CuratedQuizGenerator {

    fun buildQuiz(
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        categoryFilter: CategoryFilter = CategoryFilter.ALL,
        focusPhraseIds: List<String> = emptyList(),
        questionCount: Int = 5,
        includeTransfer: Boolean = false
    ): List<QuizQuestion> {
        if (allPhrases.isEmpty()) return emptyList()

        // True Transfer Learning: only interleave fresh-context transfer questions from ALREADY-LEARNED words
        val transferQuestions = if (includeTransfer && questionCount >= 3) {
            TransferScenarioEngine.buildTransferQuestionsForLearned(
                allPhrases = allPhrases,
                progressMap = progressMap,
                categoryFilter = categoryFilter,
                focusPhraseIds = focusPhraseIds,
                questionCount = 2
            )
        } else {
            emptyList()
        }
        val transferPhraseIds = transferQuestions.map { it.phraseId }.toSet()
        val libraryCount = (questionCount - transferQuestions.size).coerceAtLeast(1)

        val filteredPool = allPhrases.filter { categoryFilter.matches(it.category) }
            .ifEmpty { allPhrases }

        val focusSet = focusPhraseIds.toSet()
        val prioritized = filteredPool.sortedWith(
            compareByDescending<PhraseEntry> { it.id in focusSet }
                .thenByDescending { progressMap[it.id]?.missCount ?: 0L }
                .thenBy { progressMap[it.id]?.masteryLevel ?: 0L }
                .thenByDescending { if (progressMap[it.id]?.bookmarked == true) 1 else 0 }
        )

        val topCandidates = prioritized.take((libraryCount * 3).coerceAtMost(filteredPool.size))
        val selected = (prioritized.filter { it.id in focusSet || (progressMap[it.id]?.missCount ?: 0L) > 0L } +
            topCandidates.shuffled())
            .filterNot { it.id in transferPhraseIds && filteredPool.size > questionCount }
            .distinctBy { it.id }
            .take(libraryCount.coerceAtMost(filteredPool.size))

        val typesCycle = listOf(
            QuizQuestionType.CLOZE,
            QuizQuestionType.REGISTER,
            QuizQuestionType.MEANING
        )

        val curatedQuestions = selected.mapIndexed { index, target ->
            val qType = typesCycle[index % typesCycle.size]
            buildQuestionForPhrase(target, allPhrases, qType, index)
        }

        if (transferQuestions.isEmpty()) return curatedQuestions
        val merged = mutableListOf<QuizQuestion>()
        var cIdx = 0
        var tIdx = 0
        while (cIdx < curatedQuestions.size || tIdx < transferQuestions.size) {
            if (cIdx < curatedQuestions.size) merged.add(curatedQuestions[cIdx++])
            if (cIdx < curatedQuestions.size) merged.add(curatedQuestions[cIdx++])
            if (tIdx < transferQuestions.size) merged.add(transferQuestions[tIdx++])
        }
        return merged.take(questionCount)
    }

    /**
     * Builds a True Transfer Learning quiz using the user's already-learned phrases
     * inside brand-new unseen scenarios (distinct from their lesson dialogues).
     */
    fun buildTransferQuiz(
        allPhrases: List<PhraseEntry>,
        progressMap: Map<String, PhraseProgressDoc>,
        categoryFilter: CategoryFilter = CategoryFilter.ALL,
        focusPhraseIds: List<String> = emptyList(),
        questionCount: Int = 5,
        allowStarterPreviewIfEmpty: Boolean = false
    ): List<QuizQuestion> {
        val learnedQuestions = TransferScenarioEngine.buildTransferQuestionsForLearned(
            allPhrases = allPhrases,
            progressMap = progressMap,
            categoryFilter = categoryFilter,
            focusPhraseIds = focusPhraseIds,
            questionCount = questionCount
        )
        if (learnedQuestions.isNotEmpty() || !allowStarterPreviewIfEmpty) {
            return learnedQuestions
        }
        return TransferScenarioEngine.buildStarterPreview(
            allPhrases = allPhrases,
            categoryFilter = categoryFilter,
            questionCount = questionCount
        )
    }

    /**
     * Builds an Ebbinghaus Active Recall quiz for due (or early-review) learned phrases,
     * mixing fresh-context cloze recall, register recall, and meaning recall.
     */
    fun buildEbbinghausReviewQuiz(
        reviewPhrases: List<PhraseEntry>,
        allPhrases: List<PhraseEntry>,
        questionCount: Int = 8
    ): List<QuizQuestion> {
        if (reviewPhrases.isEmpty() || allPhrases.isEmpty()) return emptyList()
        val selected = reviewPhrases.take(questionCount)
        val typesCycle = listOf(
            QuizQuestionType.CLOZE,
            QuizQuestionType.MEANING,
            QuizQuestionType.REGISTER
        )
        return selected.mapIndexed { index, target ->
            val qType = typesCycle[index % typesCycle.size]
            buildQuestionForPhrase(target, allPhrases, qType, index)
        }
    }

    fun buildSinglePhraseMiniQuiz(
        target: PhraseEntry,
        allPhrases: List<PhraseEntry>
    ): List<QuizQuestion> {
        return listOf(
            buildQuestionForPhrase(target, allPhrases, QuizQuestionType.CLOZE, 0),
            buildQuestionForPhrase(target, allPhrases, QuizQuestionType.REGISTER, 1),
            buildQuestionForPhrase(target, allPhrases, QuizQuestionType.MEANING, 2)
        )
    }

    internal fun selectSafeDistractors(
        target: PhraseEntry,
        allPhrases: List<PhraseEntry>,
        count: Int = 3
    ): List<PhraseEntry> {
        val targetNorm = normalizeTerm(target.term)
        val blockedTerms = buildSet {
            add(targetNorm)
            target.synonyms.forEach { add(normalizeTerm(it.term)) }
        }
        fun isSafe(candidate: PhraseEntry): Boolean {
            if (candidate.id == target.id) return false
            val candNorm = normalizeTerm(candidate.term)
            if (candNorm in blockedTerms) return false
            if (candidate.synonyms.any { normalizeTerm(it.term) == targetNorm }) return false
            if (candidate.definition.trim().equals(target.definition.trim(), ignoreCase = true)) return false
            return true
        }
        val sameCategory = allPhrases.filter { it.category == target.category && isSafe(it) }
        val pool = if (sameCategory.size >= count) sameCategory else allPhrases.filter(::isSafe)
        return pool.shuffled().take(count)
    }

    private fun normalizeTerm(raw: String): String =
        raw.lowercase(Locale.US)
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun buildQuestionForPhrase(
        target: PhraseEntry,
        allPhrases: List<PhraseEntry>,
        type: QuizQuestionType,
        idx: Int
    ): QuizQuestion {
        val distractors = selectSafeDistractors(target, allPhrases, count = 3)

        return when (type) {
            QuizQuestionType.CLOZE -> {
                val clozeSnippet = buildClozeDialogue(target)
                val optionEntries = (distractors + target).shuffled()
                val correctIndex = optionEntries.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "q_cloze_${target.id}_$idx",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.CLOZE,
                    prompt = "Which expression naturally completes this conversation?",
                    contextSnippet = clozeSnippet,
                    options = optionEntries.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "'${target.term}' means: ${target.definition} ${target.nuanceSummary}",
                    isTransferQuestion = false
                )
            }

            QuizQuestionType.REGISTER -> {
                val scenarioHint = target.nuanceSummary.ifBlank { target.definition }
                val optionEntries = (distractors + target).shuffled()
                val correctIndex = optionEntries.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
                QuizQuestion(
                    id = "q_reg_${target.id}_$idx",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.REGISTER,
                    prompt = "Select the expression that best matches this ${target.register.label.lowercase(Locale.US)} nuance:",
                    contextSnippet = "\"$scenarioHint\"",
                    options = optionEntries.map { it.term },
                    correctIndex = correctIndex,
                    explanation = "'${target.term}' (${target.register.label} • ${target.cefrLevel.name}): ${target.usageNotes.firstOrNull() ?: target.definition}",
                    isTransferQuestion = false
                )
            }

            QuizQuestionType.MEANING -> {
                val optionDefs = (distractors.map { it.definition } + target.definition).shuffled()
                val correctIndex = optionDefs.indexOf(target.definition).coerceAtLeast(0)
                val synHint = target.synonyms.firstOrNull()?.let { "Closest synonym: '${it.term}'." }.orEmpty()
                QuizQuestion(
                    id = "q_mean_${target.id}_$idx",
                    phraseId = target.id,
                    phraseTerm = target.term,
                    category = target.category,
                    questionType = QuizQuestionType.MEANING,
                    prompt = "What is the most accurate meaning of \"${target.term}\" in American English?",
                    contextSnippet = "Register: ${target.register.label} • ${target.category.label}",
                    options = optionDefs,
                    correctIndex = correctIndex,
                    explanation = "${target.definition} $synHint".trim(),
                    isTransferQuestion = false
                )
            }
        }
    }

    private val irregularForms: Map<String, List<String>> = mapOf(
        "be" to listOf("be", "am", "is", "are", "was", "were", "been", "being"),
        "bear" to listOf("bear", "bears", "bearing", "bore", "borne"),
        "beat" to listOf("beat", "beats", "beating", "beaten"),
        "become" to listOf("become", "becomes", "becoming", "became"),
        "begin" to listOf("begin", "begins", "beginning", "began", "begun"),
        "bend" to listOf("bend", "bends", "bending", "bent"),
        "bet" to listOf("bet", "bets", "betting"),
        "bite" to listOf("bite", "bites", "biting", "bit", "bitten"),
        "blow" to listOf("blow", "blows", "blowing", "blew", "blown"),
        "break" to listOf("break", "breaks", "breaking", "broke", "broken"),
        "bring" to listOf("bring", "brings", "bringing", "brought"),
        "build" to listOf("build", "builds", "building", "built"),
        "burn" to listOf("burn", "burns", "burning", "burned", "burnt"),
        "buy" to listOf("buy", "buys", "buying", "bought"),
        "can" to listOf("can", "could"),
        "catch" to listOf("catch", "catches", "catching", "caught"),
        "choose" to listOf("choose", "chooses", "choosing", "chose", "chosen"),
        "come" to listOf("come", "comes", "coming", "came"),
        "cut" to listOf("cut", "cuts", "cutting"),
        "deal" to listOf("deal", "deals", "dealing", "dealt"),
        "dig" to listOf("dig", "digs", "digging", "dug"),
        "do" to listOf("do", "does", "doing", "did", "done"),
        "draw" to listOf("draw", "draws", "drawing", "drew", "drawn"),
        "drink" to listOf("drink", "drinks", "drinking", "drank", "drunk"),
        "drive" to listOf("drive", "drives", "driving", "drove", "driven"),
        "eat" to listOf("eat", "eats", "eating", "ate", "eaten"),
        "fall" to listOf("fall", "falls", "falling", "fell", "fallen"),
        "feed" to listOf("feed", "feeds", "feeding", "fed"),
        "feel" to listOf("feel", "feels", "feeling", "felt"),
        "fight" to listOf("fight", "fights", "fighting", "fought"),
        "find" to listOf("find", "finds", "finding", "found"),
        "flee" to listOf("flee", "flees", "fleeing", "fled"),
        "fly" to listOf("fly", "flies", "flying", "flew", "flown"),
        "forget" to listOf("forget", "forgets", "forgetting", "forgot", "forgotten"),
        "freeze" to listOf("freeze", "freezes", "freezing", "froze", "frozen"),
        "get" to listOf("get", "gets", "getting", "got", "gotten"),
        "give" to listOf("give", "gives", "giving", "gave", "given"),
        "go" to listOf("go", "goes", "going", "went", "gone"),
        "greenlight" to listOf("greenlight", "greenlights", "greenlighting", "greenlit", "greenlighted"),
        "grow" to listOf("grow", "grows", "growing", "grew", "grown"),
        "hang" to listOf("hang", "hangs", "hanging", "hung", "hanged"),
        "have" to listOf("have", "has", "having", "had"),
        "hear" to listOf("hear", "hears", "hearing", "heard"),
        "hide" to listOf("hide", "hides", "hiding", "hid", "hidden"),
        "hit" to listOf("hit", "hits", "hitting"),
        "hold" to listOf("hold", "holds", "holding", "held"),
        "keep" to listOf("keep", "keeps", "keeping", "kept"),
        "know" to listOf("know", "knows", "knowing", "knew", "known"),
        "lay" to listOf("lay", "lays", "laying", "laid"),
        "lead" to listOf("lead", "leads", "leading", "led"),
        "leave" to listOf("leave", "leaves", "leaving", "left"),
        "lend" to listOf("lend", "lends", "lending", "lent"),
        "let" to listOf("let", "lets", "letting"),
        "lie" to listOf("lie", "lies", "lying", "lay", "lain"),
        "light" to listOf("light", "lights", "lighting", "lit", "lighted"),
        "lose" to listOf("lose", "loses", "losing", "lost"),
        "make" to listOf("make", "makes", "making", "made"),
        "mean" to listOf("mean", "means", "meaning", "meant"),
        "meet" to listOf("meet", "meets", "meeting", "met"),
        "pay" to listOf("pay", "pays", "paying", "paid"),
        "put" to listOf("put", "puts", "putting"),
        "read" to listOf("read", "reads", "reading"),
        "retrospective" to listOf("retrospective", "retrospectives", "retro", "retros"),
        "ride" to listOf("ride", "rides", "riding", "rode", "ridden"),
        "ring" to listOf("ring", "rings", "ringing", "rang", "rung"),
        "rise" to listOf("rise", "rises", "rising", "rose", "risen"),
        "run" to listOf("run", "runs", "running", "ran"),
        "say" to listOf("say", "says", "saying", "said"),
        "see" to listOf("see", "sees", "seeing", "saw", "seen"),
        "seek" to listOf("seek", "seeks", "seeking", "sought"),
        "sell" to listOf("sell", "sells", "selling", "sold"),
        "send" to listOf("send", "sends", "sending", "sent"),
        "set" to listOf("set", "sets", "setting"),
        "shake" to listOf("shake", "shakes", "shaking", "shook", "shaken"),
        "shine" to listOf("shine", "shines", "shining", "shone", "shined"),
        "shoot" to listOf("shoot", "shoots", "shooting", "shot"),
        "show" to listOf("show", "shows", "showing", "showed", "shown"),
        "shrink" to listOf("shrink", "shrinks", "shrinking", "shrank", "shrunk"),
        "shut" to listOf("shut", "shuts", "shutting"),
        "sing" to listOf("sing", "sings", "singing", "sang", "sung"),
        "sink" to listOf("sink", "sinks", "sinking", "sank", "sunk"),
        "sit" to listOf("sit", "sits", "sitting", "sat"),
        "sleep" to listOf("sleep", "sleeps", "sleeping", "slept"),
        "slide" to listOf("slide", "slides", "sliding", "slid"),
        "speak" to listOf("speak", "speaks", "speaking", "spoke", "spoken"),
        "spend" to listOf("spend", "spends", "spending", "spent"),
        "spin" to listOf("spin", "spins", "spinning", "spun"),
        "split" to listOf("split", "splits", "splitting"),
        "spread" to listOf("spread", "spreads", "spreading"),
        "spring" to listOf("spring", "springs", "springing", "sprang", "sprung"),
        "stand" to listOf("stand", "stands", "standing", "stood"),
        "steal" to listOf("steal", "steals", "stealing", "stole", "stolen"),
        "stick" to listOf("stick", "sticks", "sticking", "stuck"),
        "stink" to listOf("stink", "stinks", "stinking", "stank", "stunk"),
        "strike" to listOf("strike", "strikes", "striking", "struck"),
        "swear" to listOf("swear", "swears", "swearing", "swore", "sworn"),
        "sweep" to listOf("sweep", "sweeps", "sweeping", "swept"),
        "swim" to listOf("swim", "swims", "swimming", "swam", "swum"),
        "swing" to listOf("swing", "swings", "swinging", "swung"),
        "take" to listOf("take", "takes", "taking", "took", "taken"),
        "teach" to listOf("teach", "teaches", "teaching", "taught"),
        "tear" to listOf("tear", "tears", "tearing", "tore", "torn"),
        "tell" to listOf("tell", "tells", "telling", "told"),
        "think" to listOf("think", "thinks", "thinking", "thought"),
        "throw" to listOf("throw", "throws", "throwing", "threw", "thrown"),
        "tread" to listOf("tread", "treads", "treading", "trod", "trodden"),
        "understand" to listOf("understand", "understands", "understanding", "understood"),
        "wake" to listOf("wake", "wakes", "waking", "woke", "woken"),
        "wear" to listOf("wear", "wears", "wearing", "wore", "worn"),
        "weep" to listOf("weep", "weeps", "weeping", "wept"),
        "win" to listOf("win", "wins", "winning", "won"),
        "wind" to listOf("wind", "winds", "winding", "wound"),
        "wring" to listOf("wring", "wrings", "wringing", "wrung"),
        "write" to listOf("write", "writes", "writing", "wrote", "written")
    )

    internal fun maskTermInLine(term: String, line: String): String {
        val baseTerm = term
            .replace(Regex("\\([^)]*\\)"), "")
            .trim()
        if (baseTerm.isBlank()) return line

        // 1. Direct case-insensitive substring match (handles exact phrases cleanly)
        val exactRegex = Regex("\\b${Regex.escape(baseTerm)}\\b", RegexOption.IGNORE_CASE)
        if (exactRegex.containsMatchIn(line)) {
            return exactRegex.replaceFirst(line, "________")
        }
        if (line.contains(baseTerm, ignoreCase = true)) {
            return line.replaceFirst(Regex(Regex.escape(baseTerm), RegexOption.IGNORE_CASE), "________")
        }

        // 2. Token-aware inflection & pronoun-placeholder matching
        val tokens = baseTerm.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return line

        fun tokenPattern(token: String): String {
            val lower = token.lowercase(Locale.US)
            return when (lower) {
                "one's", "someone's", "your" -> "(?:my|your|his|her|its|our|their|one's|someone's|[a-z]+['’]s)"
                "someone" -> "(?:me|you|him|her|us|them|someone|[a-z]+)"
                "oneself", "yourself" -> "(?:myself|yourself|himself|herself|itself|ourselves|themselves|oneself)"
                "one" -> "(?:i|you|he|she|we|they|one)"
                "it" -> "(?:it|that|this|them|things)"
                else -> {
                    val forms = irregularForms[lower]
                    if (forms != null) {
                        "(?:${forms.joinToString("|") { Regex.escape(it) }})"
                    } else if (lower.length >= 4) {
                        val stem = when {
                            lower.endsWith("e") -> lower.dropLast(1)
                            lower.endsWith("y") -> lower.dropLast(1)
                            else -> lower.take(maxOf(3, lower.length - 1))
                        }
                        "${Regex.escape(stem)}[a-zA-Z'’-]{0,6}"
                    } else {
                        "${Regex.escape(lower)}[a-zA-Z'’-]{0,4}"
                    }
                }
            }
        }

        val fullPattern = tokens.joinToString(".{0,24}") { tokenPattern(it) }
        val fullRegex = Regex("\\b$fullPattern\\b", RegexOption.IGNORE_CASE)
        fullRegex.find(line)?.let { match ->
            return line.replaceRange(match.range, "________")
        }

        // 3. Single-word or head-token fallback without strict trailing word boundary (for punctuation/hyphens)
        val relaxedRegex = Regex(fullPattern, RegexOption.IGNORE_CASE)
        relaxedRegex.find(line)?.let { match ->
            return line.replaceRange(match.range, "________")
        }

        return line
    }

    internal fun buildClozeDialogue(entry: PhraseEntry): String {
        val lines = entry.dialogue.map { turn ->
            val masked = maskTermInLine(entry.term, turn.line)
            "${turn.speaker}: \"$masked\""
        }

        val combined = lines.joinToString("\n")
        return if (combined.contains("________")) {
            combined
        } else {
            val scene = NaturalDialogueSynthesizer.buildTransferScene(
                category = entry.category,
                term = entry.term,
                definition = entry.definition,
                seed = abs(entry.id.hashCode()),
                synonyms = entry.synonyms.map { it.term }
            )
            "${scene.speakerA}: \"${scene.leadLine}\"\n${scene.speakerB}: \"${scene.replyWithBlank}\""
        }
    }
}
