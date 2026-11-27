package com.example.data.catalog

import com.example.data.model.PhraseCategory
import java.util.Locale

/**
 * Classifies the semantic intent, polarity, and referent type (person vs. thing)
 * of a vocabulary entry using its definition, synonyms, term, and category so that
 * synthesized lesson dialogues and Transfer Learning scenes match the EXACT sense
 * of the word being studied rather than an unrelated or contradictory context.
 */
object SemanticIntentClassifier {

    enum class SemanticIntent {
        PRAISE_POSITIVE,
        CRITICISM_NEGATIVE,
        BORING_OVERUSED,
        BRIEF_TEMPORARY,
        EXCESSIVE_INTENSE,
        REDUCE_LESSEN,
        INCREASE_GROW,
        SECRET_CONFUSING,
        CLEAR_DIRECT,
        MONEY_VALUE,
        TIME_SCHEDULE,
        SUPPORT_COLLABORATE,
        OPPOSE_BLOCK,
        DISCOVER_SOLVE,
        COMMUNICATION_SPEECH,
        SOCIAL_RELATIONSHIP,
        WORK_STRATEGY,
        NEUTRAL_GENERAL
    }

    enum class Polarity {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    private val personRoleCues = listOf(
        "a person", "someone who", "one who", "an individual", "a worker", "a colleague",
        "a leader", "a manager", "a mentor", "a friend", "a buddy", "a partner", "a rival",
        "a critic", "a follower", "a flatterer", "a speaker", "a storyteller", "a beginner",
        "a novice", "an expert", "a specialist", "a fan", "a supporter", "a member",
        "a newcomer", "a loner", "a hermit", "a fraud", "an impostor", "a liar",
        "a rebel", "a maverick", "a zealot", "a fanatic", "a tyrant", "a bully",
        "a sycophant", "a yes-man", "a pedant", "a skeptic", "a cynic", "a miser",
        "a spendthrift", "a glutton", "a coward", "a hero", "a champion", "an ally",
        "an adversary", "an enemy", "an opponent", "a competitor", "a customer",
        "a client", "a host", "a guest", "a boss", "a teammate", "a roommate",
        "a neighbor", "a stranger", "a witness", "a victim", "a culprit", "a suspect",
        "people who", "person who", "character who", "type of person", "tough, resilient person",
        "reliable person", "close friend", "loyal friend", "trusted friend"
    )

    private val boringOverusedCues = listOf(
        "boring", "dull", "tedious", "monotonous", "unoriginal", "cliché", "cliche",
        "overused", "hackneyed", "trite", "banal", "vapid", "insipid", "mundane",
        "mediocre", "unremarkable", "forgettable", "predictably mainstream", "predictable",
        "lacking original", "average", "overrated", "stale", "humdrum", "prosaic",
        "repetitive", "wearisome", "tiresome", "bland", "lackluster", "pedestrian",
        "uninspired", "flat and dull"
    )

    private val briefTemporaryCues = listOf(
        "short-lived", "short time", "fleeting", "temporary", "momentary", "ephemeral",
        "transient", "passing", "brief", "concise", "terse", "succinct", "laconic",
        "pithy", "short and", "abrupt", "sudden", "quick", "instant", "in a flash",
        "short-term", "impermanent", "evanescent", "fugacious"
    )

    private val reduceLessenCues = listOf(
        "less intense", "become less", "make less", "reduce", "lessen", "subside",
        "ease up", "calm down", "soothe", "soften", "mitigate", "alleviate", "allay",
        "assuage", "mollify", "pacify", "placate", "appease", "diminish", "dwindle",
        "shrink", "fade", "taper", "decline", "weaken", "wane", "ebbing", "ebb",
        "scarce", "sparse", "meager", "scanty", "paucity", "dearth", "lack of",
        "restrain", "curb", "trim", "pare down", "cut back", "scale back", "wind down",
        "phase out", "cool off", "blow over", "die down", "let up", "lose energy",
        "become tired", "quiet down", "settle down", "simmer down", "tone down",
        "de-escalate", "relief", "moderate", "temperate", "frugal", "sparing"
    )

    private val increaseGrowCues = listOf(
        "worse", "worsen", "exacerbate", "aggravate", "intensify", "escalate", "amplify",
        "multiply", "proliferate", "spread rapidly", "surge", "skyrocket", "balloon",
        "snowball", "accelerate", "ramp up", "scale up", "boost", "expand", "swell",
        "grow rapidly", "pile up", "mount", "ratchet up", "heighten", "compound",
        "augment", "burgeon", "flourish", "thrive", "boom", "spike"
    )

    private val secretConfusingCues = listOf(
        "secret", "hidden", "covert", "clandestine", "furtive", "stealthy", "sneaky",
        "mysterious", "cryptic", "enigmatic", "obscure", "arcane", "esoteric", "abstruse",
        "recondite", "vague", "ambiguous", "equivocal", "nebulous", "murky", "cloudy",
        "confusing", "baffling", "perplexing", "bewildering", "labyrinthine", "convoluted",
        "complicated", "tangled", "subtle", "indirect", "coded", "unspoken", "private",
        "confidential", "under wraps", "behind the scenes", "in the dark", "disguised",
        "obfuscate", "muddle", "opaque", "hard to understand", "difficult to grasp"
    )

    private val clearDirectCues = listOf(
        "clear", "obvious", "unmistakable", "transparent", "legitimate", "honest and open",
        "above board", "candid", "frank", "blunt", "forthright", "direct", "straightforward",
        "explicit", "lucid", "pellucid", "perspicuous", "crystal", "unambiguous",
        "plainspoken", "upfront", "open and", "exact", "precise", "accurate", "dead-on",
        "spot on", "undeniable", "unquestionable", "self-evident", "manifest", "apparent",
        "no-nonsense", "tell it like it is"
    )

    private val moneyValueCues = listOf(
        "money", "cash", "cost", "expensive", "pricey", "steep", "cheap", "budget",
        "pay", "paid", "payment", "reimburs", "debt", "profit", "revenue", "price",
        "salary", "wage", "equity", "stock", "share", "financial", "finance", "fee",
        "invest", "spend", "afford", "wealth", "rich", "poor", "broke", "bankrupt",
        "stingy", "frugal", "generous", "lavish", "lucrative", "bargain", "discount",
        "bill", "tab", "tip", "loan", "fund", "grant", "margin", "runway", "dollar",
        "buck", "penny", "dime", "worth", "valuable", "mercenary", "venal", "cupidity",
        "avarice", "pecuniary", "indigent", "penurious", "affluent", "opulent"
    )

    private val timeScheduleCues = listOf(
        "calendar", "schedule", "booked", "busy", "swamped", "deadline", "overdue",
        "delay", "postpone", "defer", "put off", "procrastinat", "pause", "wait",
        "hurry", "rush", "speed up", "faster", "late", "early", "on time", "punctual",
        "last minute", "last-minute", "hour", "minute", "weekend", "overnight",
        "unavailable", "away from desk", "out of office", "bandwidth", "capacity",
        "time-consuming", "long-term", "ahead of schedule", "behind schedule",
        "drag on", "wrap up", "call it a day", "clock out", "all-nighter", "midnight"
    )

    private val supportCollaborateCues = listOf(
        "help", "assist", "support", "back up", "endorse", "champion", "sponsor",
        "mentor", "encourage", "hype", "rally", "cooperate", "collaborate", "team up",
        "align", "agree", "consensus", "buy-in", "reconcile", "forgive", "trust",
        "defend", "protect", "cover for", "vouch for", "stand by", "stick up for",
        "pitch in", "chip in", "lend a hand", "accommodate", "welcome", "embrace",
        "praise", "compliment", "applaud", "laud", "extol", "bolster", "buttress",
        "corroborate", "validate", "affirm", "nurture", "foster"
    )

    private val opposeBlockCues = listOf(
        "oppose", "reject", "refuse", "decline", "turn down", "push back", "resist",
        "object", "block", "hinder", "impede", "obstruct", "prevent", "stop", "halt",
        "cancel", "scrap", "abandon", "quit", "give up", "surrender", "walk away",
        "avoid", "dodge", "evade", "shirk", "skip", "ignore", "dismiss", "brush off",
        "ghost", "cut off", "exclude", "leave out", "criticize", "condemn", "denounce",
        "attack", "belittle", "undermine", "sabotage", "thwart", "veto", "ban",
        "forbid", "repeal", "abrogate", "repudiate", "spurn", "rebuff", "reprove",
        "berate", "chastise", "censure", "castigate", "excoriate", "disparage",
        "deride", "mock", "taunt", "heckle", "argue", "clash", "feud", "bicker"
    )

    private val discoverSolveCues = listOf(
        "discover", "reveal", "uncover", "expose", "unearth", "find out", "figure out",
        "solve", "resolve", "fix", "iron out", "hash out", "thrash out", "sort out",
        "work out", "analyze", "examine", "inspect", "investigate", "dig into",
        "unpack", "clarify", "explain", "decode", "infer", "realize", "notice",
        "spot", "identify", "pinpoint", "zero in", "verify", "confirm", "prove",
        "disprove", "debunk", "detect", "diagnose", "fathom", "decipher", "elucidate",
        "ascertain", "discern", "comprehend", "grasp", "learn"
    )

    private val communicationSpeechCues = listOf(
        "gossip", "rumor", "tea", "drama", "talk", "speak", "chat", "converse",
        "brag", "boast", "flex", "lie", "exaggerat", "fib", "bluff", "joke",
        "tease", "roast", "complain", "vent", "rant", "lecture", "preach", "pontificate",
        "whisper", "announce", "declare", "summarize", "recap", "brief", "pitch",
        "warn", "heads-up", "flatter", "sweet-talk", "blather", "ramble", "babble",
        "chatter", "garrulous", "loquacious", "verbose", "wordy", "articulate",
        "eloquent", "rhetoric", "speech", "remark", "comment", "dig", "insult",
        "shade", "tone", "wit", "sarcas", "irony", "satire", "diatribe", "tirade",
        "harangue", "eulogy", "panegyric", "platitude", "apolog", "confess", "confide",
        "fess up", "spill"
    )

    private val excessiveIntenseCues = listOf(
        "over-the-top", "overly dramatic", "excessive", "extreme", "overwhelming",
        "abundant", "lavish", "extravagant", "flashy", "ostentatious", "flamboyant",
        "pompous", "grandiose", "chaotic", "frantic", "hectic", "wild", "intense",
        "nonstop", "relentless", "fierce", "severe", "huge", "massive", "enormous",
        "colossal", "prodigious", "copious", "profuse", "plethora", "myriad",
        "surfeit", "glut", "deluge", "uproar", "commotion", "tumult", "clamor",
        "cacophony", "loud", "noisy", "boisterous", "rambunctious", "vehement",
        "fervent", "zealous", "obsessive", "fanatical", "overwhelm", "swamp",
        "buried in", "up to your neck", "snowed under"
    )

    private val praisePositiveCues = listOf(
        "impressive", "awesome", "amazing", "extraordinarily cool", "exceptionally good",
        "stylish", "exciting", "flawless", "spotless", "brand-new", "mint", "brilliant",
        "clever", "smart", "sharp", "witty", "skilled", "skillful", "adept", "adroit",
        "deft", "masterful", "proficient", "capable", "talented", "gifted", "resourceful",
        "generous", "kind", "warm", "friendly", "charming", "affable", "amiable",
        "genial", "cordial", "gregarious", "loyal", "faithful", "steadfast", "resilient",
        "brave", "courageous", "bold", "intrepid", "audacious", "calm", "composed",
        "serene", "tranquil", "poised", "equanimity", "diligent", "thorough", "careful",
        "meticulous", "conscientious", "assiduous", "industrious", "energetic",
        "enthusiastic", "vibrant", "lively", "joyful", "happy", "ecstatic", "elated",
        "thrilled", "bliss", "optimistic", "hopeful", "confident", "proud", "admirable",
        "noble", "ethical", "honest", "fair", "impartial", "just", "sincere", "genuine",
        "authentic", "efficient", "smooth", "clean", "sleek", "crisp", "refreshing",
        "helpful", "beneficial", "advantageous", "successful", "winning", "triumph",
        "breakthrough", "excellent", "outstanding", "top-notch", "first-rate", "stellar",
        "superb", "radiant", "delightful", "pleasing", "satisfying", "further than expected",
        "go the extra mile", "above and beyond", "worth", "classic win", "great"
    )

    private val criticismNegativeCues = listOf(
        "rude", "inappropriate", "disrespect", "arrogant", "conceited", "haughty",
        "condescending", "patronizing", "smug", "bitter", "resentful", "salty", "petty",
        "spiteful", "vindictive", "cruel", "callous", "harsh", "biting", "acerbic",
        "caustic", "scathing", "mordant", "vitriolic", "hostile", "antagonistic",
        "belligerent", "pugnacious", "quarrelsome", "irritable", "cranky", "grumpy",
        "testy", "peevish", "petulant", "deceptive", "dishonest", "fake", "phony",
        "fraud", "insincere", "hypocritical", "two-faced", "duplicitous", "mendacious",
        "treacherous", "sneaky", "corrupt", "unethical", "shady", "sketchy", "lazy",
        "sluggish", "lethargic", "indolent", "apathetic", "indifferent", "careless",
        "sloppy", "negligent", "reckless", "rash", "impulsive", "imprudent", "foolish",
        "naive", "gullible", "dense", "slow to catch", "stubborn", "obstinate",
        "intractable", "obdurate", "inflexible", "rigid", "selfish", "greedy", "vain",
        "narcissistic", "obsequious", "servile", "fawning", "sycophant", "cowardly",
        "timid", "meek", "weak", "fragile", "flimsy", "rickety", "shoddy", "broken",
        "doomed", "exhausted", "burned out", "miserable", "gloomy", "sad", "melancholy",
        "blue", "down", "anxious", "worried", "stressed", "angry", "furious", "annoyed",
        "frustrated", "exasperated", "fed up", "sick and tired", "embarrass", "awkward",
        "cringe", "unfair", "biased", "prejudiced", "harmful", "toxic", "pernicious",
        "deleterious", "detrimental", "destructive", "disastrous", "catastrophic",
        "flaw", "fault", "mistake", "blunder", "mess", "disaster", "fiasco", "debacle",
        "problem", "trouble", "headache", "burden", "annoying", "irritating", "unpleasant",
        "disappointing", "bad", "poor", "fail", "collapse", "crash", "ruin",
        "waste", "pointless", "futile", "useless", "skipping important steps", "cheat",
        "steal", "betray", "hurt", "offend", "insult", "humiliate", "exploit", "manipulate"
    )

    private val socialRelationshipCues = listOf(
        "friend", "dating", "romantic", "crush", "flirt", "party", "hangout", "hang out",
        "social", "crowd", "guest", "host", "invite", "invitation", "neighbor", "family",
        "couple", "marriage", "breakup", "bond", "clique", "popular", "lonely", "isolated",
        "recluse", "introvert", "extrovert", "awkward", "vibe", "mood", "chemistry",
        "affection", "jealous", "loyal", "close-knit", "tight-knit", "relationship",
        "acquaintance", "stranger", "community", "gathering", "dinner", "brunch", "drink"
    )

    private val workStrategyCues = listOf(
        "workplace", "office", "meeting", "standup", "sprint", "project", "roadmap",
        "stakeholder", "leadership", "executive", "client", "vendor", "contract",
        "team", "department", "company", "startup", "corporate", "business", "strategy",
        "metric", "kpi", "conversion", "retention", "churn", "onboarding", "launch",
        "release", "backlog", "scope", "milestone", "deliverable", "workflow", "process",
        "audit", "compliance", "policy", "promotion", "career", "hiring", "recruiter",
        "interview", "resume", "portfolio", "quarterly", "q1", "q2", "q3", "q4",
        "presentation", "deck", "memo", "agenda", "retrospective", "sign-off"
    )

    fun isPersonNoun(term: String, definition: String): Boolean {
        val defLower = definition.trim().lowercase(Locale.US)
        if (personRoleCues.any { defLower.contains(it) }) return true
        val clean = term.replace(Regex("\\([^)]*\\)"), "").trim().lowercase(Locale.US)
        if (!clean.contains(" ") && (clean.endsWith("ist") || clean.endsWith("eer") || clean.endsWith("phile"))) {
            if (defLower.contains("who ") || defLower.startsWith("a ") || defLower.startsWith("an ")) {
                return !defLower.contains("a thing") && !defLower.contains("an object") && !defLower.contains("a device")
            }
        }
        return false
    }

    fun classifySemanticIntent(
        category: PhraseCategory,
        term: String,
        definition: String,
        synonyms: List<String> = emptyList()
    ): SemanticIntent {
        val defLower = definition.trim().lowercase(Locale.US)
        val combined = buildString {
            append(defLower)
            if (synonyms.isNotEmpty()) {
                append(' ')
                append(synonyms.joinToString(" ").lowercase(Locale.US))
            }
        }

        // 1. Specific high-precision domains & directional changes first
        val scores = mutableMapOf<SemanticIntent, Int>()
        fun addScore(intent: SemanticIntent, cues: List<String>, weight: Int = 2) {
            var hits = 0
            for (cue in cues) {
                if (defLower.contains(cue)) hits += weight
                else if (combined.contains(cue)) hits += 1
            }
            if (hits > 0) scores[intent] = (scores[intent] ?: 0) + hits
        }

        addScore(SemanticIntent.BORING_OVERUSED, boringOverusedCues, weight = 4)
        addScore(SemanticIntent.BRIEF_TEMPORARY, briefTemporaryCues, weight = 4)
        addScore(SemanticIntent.REDUCE_LESSEN, reduceLessenCues, weight = 4)
        addScore(SemanticIntent.INCREASE_GROW, increaseGrowCues, weight = 4)
        addScore(SemanticIntent.SECRET_CONFUSING, secretConfusingCues, weight = 3)
        addScore(SemanticIntent.CLEAR_DIRECT, clearDirectCues, weight = 3)
        addScore(SemanticIntent.MONEY_VALUE, moneyValueCues, weight = 3)
        addScore(SemanticIntent.TIME_SCHEDULE, timeScheduleCues, weight = 3)
        addScore(SemanticIntent.SUPPORT_COLLABORATE, supportCollaborateCues, weight = 3)
        addScore(SemanticIntent.OPPOSE_BLOCK, opposeBlockCues, weight = 3)
        addScore(SemanticIntent.DISCOVER_SOLVE, discoverSolveCues, weight = 3)
        addScore(SemanticIntent.COMMUNICATION_SPEECH, communicationSpeechCues, weight = 3)
        addScore(SemanticIntent.EXCESSIVE_INTENSE, excessiveIntenseCues, weight = 3)
        addScore(SemanticIntent.CRITICISM_NEGATIVE, criticismNegativeCues, weight = 3)
        addScore(SemanticIntent.PRAISE_POSITIVE, praisePositiveCues, weight = 3)
        addScore(SemanticIntent.SOCIAL_RELATIONSHIP, socialRelationshipCues, weight = 2)
        addScore(SemanticIntent.WORK_STRATEGY, workStrategyCues, weight = 2)

        // Directional change verbs ("make ... worse", "become less ...") should take precedence
        // over generic sentiment words inside the object noun ("problem", "bad situation").
        if (defLower.contains("worse") || defLower.contains("worsen") || defLower.contains("intensif") ||
            defLower.contains("escalat") || defLower.contains("proliferat") || defLower.contains("aggravat")
        ) {
            scores[SemanticIntent.INCREASE_GROW] = (scores[SemanticIntent.INCREASE_GROW] ?: 0) + 10
        }

        // Small boost for workplace category only if no strong non-work semantic intent won
        if (category == PhraseCategory.WORKPLACE && scores.isEmpty()) {
            return SemanticIntent.WORK_STRATEGY
        }

        // Guard against polysemous definitions where the first clause states the primary sense
        // (e.g., "Acting wildly inappropriate and rude, or temporarily unavailable/paying personally")
        val primaryClause = defLower.substringBefore(";").substringBefore(", or ").substringBefore(" or ")
        if (primaryClause.isNotBlank() && primaryClause.length >= 10) {
            if (criticismNegativeCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.CRITICISM_NEGATIVE] = (scores[SemanticIntent.CRITICISM_NEGATIVE] ?: 0) + 5
            }
            if (praisePositiveCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.PRAISE_POSITIVE] = (scores[SemanticIntent.PRAISE_POSITIVE] ?: 0) + 5
            }
            if (boringOverusedCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.BORING_OVERUSED] = (scores[SemanticIntent.BORING_OVERUSED] ?: 0) + 5
            }
            if (briefTemporaryCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.BRIEF_TEMPORARY] = (scores[SemanticIntent.BRIEF_TEMPORARY] ?: 0) + 5
            }
            if (reduceLessenCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.REDUCE_LESSEN] = (scores[SemanticIntent.REDUCE_LESSEN] ?: 0) + 5
            }
            if (increaseGrowCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.INCREASE_GROW] = (scores[SemanticIntent.INCREASE_GROW] ?: 0) + 5
            }
            if (timeScheduleCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.TIME_SCHEDULE] = (scores[SemanticIntent.TIME_SCHEDULE] ?: 0) + 4
            }
            if (moneyValueCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.MONEY_VALUE] = (scores[SemanticIntent.MONEY_VALUE] ?: 0) + 4
            }
            if (communicationSpeechCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.COMMUNICATION_SPEECH] = (scores[SemanticIntent.COMMUNICATION_SPEECH] ?: 0) + 4
            }
            if (secretConfusingCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.SECRET_CONFUSING] = (scores[SemanticIntent.SECRET_CONFUSING] ?: 0) + 4
            }
            if (clearDirectCues.any { primaryClause.contains(it) }) {
                scores[SemanticIntent.CLEAR_DIRECT] = (scores[SemanticIntent.CLEAR_DIRECT] ?: 0) + 4
            }
        }

        val best = scores.maxByOrNull { it.value } ?: return SemanticIntent.NEUTRAL_GENERAL
        return if (best.value >= 2) best.key else SemanticIntent.NEUTRAL_GENERAL
    }

    fun classifyPolarity(
        intent: SemanticIntent,
        definition: String
    ): Polarity {
        val defLower = definition.trim().lowercase(Locale.US)
        return when (intent) {
            SemanticIntent.PRAISE_POSITIVE,
            SemanticIntent.SUPPORT_COLLABORATE,
            SemanticIntent.CLEAR_DIRECT,
            SemanticIntent.DISCOVER_SOLVE -> Polarity.POSITIVE

            SemanticIntent.CRITICISM_NEGATIVE,
            SemanticIntent.BORING_OVERUSED,
            SemanticIntent.OPPOSE_BLOCK -> Polarity.NEGATIVE

            else -> {
                val neg = criticismNegativeCues.count { defLower.contains(it) } +
                    boringOverusedCues.count { defLower.contains(it) }
                val pos = praisePositiveCues.count { defLower.contains(it) } +
                    supportCollaborateCues.count { defLower.contains(it) }
                when {
                    neg > pos -> Polarity.NEGATIVE
                    pos > neg -> Polarity.POSITIVE
                    else -> Polarity.NEUTRAL
                }
            }
        }
    }

    /**
     * Replaces dictionary placeholder pronouns ("one's", "someone's", "someone", "oneself")
     * with natural spoken English pronouns so dialogues never read like a dictionary headword.
     */
    fun inflectForSpeech(cleanTerm: String, secondPerson: Boolean = true): String {
        var result = cleanTerm
        val poss = if (secondPerson) "your" else "their"
        val refl = if (secondPerson) "yourself" else "themselves"
        result = result.replace(Regex("\\bone's\\b", RegexOption.IGNORE_CASE), poss)
        result = result.replace(Regex("\\bsomeone's\\b", RegexOption.IGNORE_CASE), "their")
        result = result.replace(Regex("\\boneself\\b", RegexOption.IGNORE_CASE), refl)
        result = result.replace(Regex("\\bsomeone\\b", RegexOption.IGNORE_CASE), "them")
        return result
    }
}
