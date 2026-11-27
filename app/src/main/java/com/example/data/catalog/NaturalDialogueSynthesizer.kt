package com.example.data.catalog

import com.example.data.catalog.SemanticIntentClassifier.Polarity
import com.example.data.catalog.SemanticIntentClassifier.SemanticIntent
import com.example.data.model.DialogueTurn
import com.example.data.model.PhraseCategory
import java.util.Locale
import kotlin.math.abs

/**
 * Synthesizes authentic, meaning-aligned spoken American English conversations
 * across all 5 vocabulary categories (Slang, Idioms, Phrasal Verbs, Workplace, GRE).
 *
 * Every generated lesson dialogue and Transfer Learning scene is routed by BOTH
 * grammatical slot (ExpressionGrammar) AND semantic meaning/polarity (SemanticIntent),
 * preventing unrelated or wrong-sense stories from being paired with a target expression.
 * Strictly avoids meta-linguistic filler.
 */
object NaturalDialogueSynthesizer {

    enum class ExpressionGrammar {
        STANDALONE_REACTION,
        PAST_VERB_PHRASE,
        TRANSITIVE_PERSON_VERB,
        TRANSITIVE_THING_VERB,
        COMPLETE_VERB_PHRASE,
        INTRANSITIVE_VERB,
        PREPOSITIONAL_STATE,
        ADJECTIVE,
        DETERMINER_NOUN,
        DEFINITE_NOUN,
        PERSON_NOUN,
        THING_NOUN,
        UNCOUNTABLE_NOUN
    }

    data class TransferScene(
        val domainLabel: String,
        val speakerA: String,
        val speakerB: String,
        val leadLine: String,
        val replyWithTarget: String,
        val replyWithBlank: String
    )

    private val everydaySpeakers = listOf(
        "Maya" to "Jordan",
        "Tessa" to "Liam",
        "Sam" to "Priya",
        "Chloe" to "Marcus",
        "Nora" to "Owen",
        "Hannah" to "Devon",
        "Ava" to "Ethan",
        "Claire" to "Caleb",
        "Zoe" to "Noah",
        "Rachel" to "Ben"
    )

    private val workplaceSpeakers = listOf(
        "Elena" to "Marcus",
        "Priya" to "Devon",
        "Nadia" to "Jordan",
        "Rachel" to "Carlos",
        "Hannah" to "Sam",
        "Claire" to "Alex"
    )

    private val transferSpeakers = listOf(
        "Tessa" to "Devon",
        "Priya" to "Marcus",
        "Chloe" to "Sam",
        "Noah" to "Elena",
        "Nora" to "Liam",
        "Ava" to "Owen",
        "Rachel" to "Caleb",
        "Hannah" to "Jordan"
    )

    private val commonVerbStarters = setOf(
        "act", "add", "ask", "back", "bail", "bang", "bark", "bear", "beat", "bend", "bet", "bite",
        "black", "blank", "blast", "blend", "blow", "boil", "bone", "book", "boost", "bottle",
        "bounce", "bow", "box", "branch", "break", "breathe", "bring", "brush", "buckle", "build",
        "bump", "burn", "burst", "butt", "buy", "call", "calm", "camp", "carry", "carve", "cash",
        "cast", "catch", "cave", "chalk", "check", "cheer", "chew", "chill", "chime", "chip",
        "choke", "chop", "circle", "clamp", "clean", "clear", "click", "climb", "clock", "close",
        "cloud", "clown", "coast", "come", "confide", "cool", "cop", "count", "cover", "crack",
        "crank", "crash", "creep", "crop", "cross", "cry", "cut", "dash", "deal", "dial", "die",
        "dig", "dip", "dish", "dive", "do", "dodge", "double", "drag", "drain", "draw", "dream",
        "dress", "drift", "drill", "drink", "drive", "drop", "drown", "drum", "dry", "duck",
        "dumb", "dump", "ease", "eat", "edge", "egg", "eke", "end", "even", "eye", "face", "fade",
        "faff", "fall", "fan", "fathom", "feed", "feel", "fend", "fess", "fight", "figure", "file",
        "fill", "filter", "find", "finish", "fire", "firm", "fish", "fit", "fix", "fizzle", "flag",
        "flake", "flame", "flare", "flash", "flatten", "flex", "flick", "flip", "float", "flog",
        "floor", "fluff", "flush", "fly", "fob", "focus", "fold", "follow", "fool", "fork", "foul",
        "freak", "free", "freeze", "freshen", "front", "frown", "fudge", "fuel", "gad", "gang",
        "gas", "gear", "geek", "get", "ghost", "give", "gloss", "go", "gobble", "goof", "grind",
        "gross", "grow", "haggle", "hammer", "hand", "hang", "harp", "hash", "haul", "have",
        "haze", "head", "hear", "heat", "hedge", "help", "hem", "hide", "hinge", "hit", "hold",
        "hole", "home", "hone", "hook", "hop", "horse", "hose", "hound", "hunker", "hunt", "hurry",
        "hush", "hype", "ice", "iron", "itch", "jab", "jack", "jam", "jazz", "jerk", "jibe",
        "join", "joke", "jot", "juice", "jump", "keel", "keep", "key", "kick", "kill", "kiss",
        "knuckle", "knock", "know", "lace", "land", "lap", "lash", "latch", "laugh", "launch",
        "lay", "lead", "leak", "lean", "leap", "learn", "leave", "let", "level", "lick", "lie",
        "lift", "light", "lighten", "limber", "line", "link", "listen", "live", "load", "lock",
        "log", "look", "loom", "loop", "loosen", "lose", "louse", "luck", "lump", "make", "man",
        "map", "march", "mark", "mash", "max", "measure", "meet", "melt", "mess", "mete", "milk",
        "mill", "mind", "miss", "mix", "mock", "mollify", "monkey", "mop", "mount", "mouth",
        "move", "muck", "muddle", "mull", "muscle", "muster", "nail", "name", "narrow", "nerd",
        "nibble", "nick", "nip", "nod", "nose", "notch", "nudge", "nurse", "nut", "offer", "open",
        "opt", "order", "own", "pack", "pad", "pair", "pal", "palm", "pan", "paper", "pare",
        "park", "part", "pass", "patch", "pay", "peel", "peg", "pencil", "pep", "perk", "peter",
        "phase", "phone", "pick", "piece", "pig", "pile", "pin", "pipe", "pitch", "pivot", "plan",
        "play", "plod", "plough", "plow", "pluck", "plug", "plump", "plunge", "point", "poke",
        "polish", "pony", "poop", "pop", "pore", "pour", "power", "pre-wire", "press", "price",
        "prick", "print", "prod", "prop", "psych", "puff", "pull", "pump", "punch", "push", "put",
        "quiet", "quit", "rack", "rail", "rain", "raise", "rake", "rally", "ram", "ramp", "rat",
        "ratchet", "rattle", "rave", "reach", "read", "reap", "reason", "reckon", "reel", "rein",
        "rely", "rent", "rest", "rev", "ride", "riff", "rig", "ring", "rinse", "rip", "rise",
        "risk", "roast", "rock", "roll", "root", "rope", "rough", "round", "rout", "rub", "rule",
        "run", "rush", "rustle", "saddle", "sail", "salt", "sand", "save", "say", "scale", "scare",
        "scoop", "scoot", "score", "scout", "scrape", "scratch", "scream", "screen", "screw",
        "scrub", "seal", "search", "see", "seek", "seize", "sell", "send", "serve", "set",
        "settle", "sew", "shack", "shade", "shake", "shape", "share", "shave", "shed", "shell",
        "shift", "shine", "ship", "shirk", "shoot", "shop", "shore", "short", "shout", "shove",
        "show", "shrug", "shut", "shy", "side", "sift", "sigh", "sign", "simmer", "sing", "single",
        "sink", "sit", "size", "skate", "sketch", "skimp", "skip", "skirt", "slack", "slam",
        "slap", "slash", "sleep", "slice", "slide", "slim", "sling", "slip", "slog", "slop",
        "slouch", "slow", "slug", "slump", "smack", "smash", "smell", "smile", "smoke", "smooth",
        "snap", "snatch", "sneak", "sniff", "snoop", "snuff", "soak", "sober", "sock", "soften",
        "soldier", "sort", "sound", "soup", "sow", "space", "spark", "speak", "speed", "spell",
        "spend", "spill", "spin", "spit", "splash", "split", "spoil", "sponge", "spoon", "spot",
        "spread", "spring", "spruce", "spur", "square", "squash", "squeeze", "squint", "stab",
        "stack", "staff", "stage", "stake", "stall", "stamp", "stand", "stare", "start", "stash",
        "stave", "stay", "steal", "steam", "steer", "stem", "step", "stew", "stick", "stiff",
        "stifle", "stir", "stitch", "stock", "stomp", "stoop", "stop", "store", "storm", "stow",
        "straighten", "strap", "stray", "streamline", "stretch", "strike", "string", "strip",
        "stroll", "stub", "stuff", "stumble", "stump", "subject", "suck", "suit", "sum", "summon",
        "swallow", "swap", "swarm", "swear", "sweat", "sweep", "swell", "swim", "swing", "swipe",
        "switch", "sync", "table", "tack", "tackle", "tag", "tail", "take", "talk", "tally",
        "tamp", "tangle", "tank", "tap", "taper", "tar", "taste", "tear", "tease", "tee", "tell",
        "test", "thaw", "think", "thrash", "thread", "throw", "thrust", "thumb", "tick", "tide",
        "tidy", "tie", "tighten", "tilt", "time", "tinker", "tip", "tire", "toast", "toe", "tone",
        "toot", "top", "torch", "toss", "tote", "touch", "tough", "tow", "toy", "trace", "track",
        "trade", "trail", "trample", "tread", "treat", "trick", "trigger", "trim", "trip", "trot",
        "tuck", "tug", "tumble", "tune", "turn", "tweak", "twist", "type", "unpack", "unravel",
        "unwind", "up", "use", "usher", "vamp", "veg", "vent", "vet", "vibe", "vote", "vouch",
        "wade", "waffle", "wait", "wake", "walk", "waltz", "wander", "ward", "warm", "warn",
        "wash", "waste", "watch", "water", "wave", "waver", "wax", "wear", "weasel", "weather",
        "weave", "weed", "weigh", "well", "whack", "wheel", "whip", "whisk", "whistle", "whittle",
        "widen", "wiggle", "win", "wind", "wing", "wink", "wipe", "wire", "wise", "wish", "wither",
        "wolf", "work", "worm", "worry", "wrap", "wreak", "wrench", "wrest", "wrestle", "wriggle",
        "wring", "write", "yak", "yank", "yap", "yell", "yield", "zero", "zip", "zone", "zoom"
    )

    private val prepStarters = setOf(
        "above", "across", "after", "against", "ahead", "along", "amid", "among", "around", "at",
        "back", "before", "behind", "below", "beneath", "beside", "between", "beyond", "by",
        "down", "for", "from", "in", "inside", "into", "near", "off", "on", "onto", "out",
        "outside", "over", "past", "through", "to", "toward", "under", "underneath", "up", "upon",
        "with", "within", "without"
    )

    private val standaloneReactionTerms = setOf(
        "bet", "no cap", "say less", "my bad", "fair enough", "facts", "same here", "ditto",
        "for real", "fr", "word", "real talk", "big mood", "mood", "yikes", "oof", "touché",
        "touche", "period", "periodt", "slay", "true that", "deadass", "on god", "valid",
        "you do you", "it is what it is", "that tracks", "hard pass", "not me", "say no more",
        "good call", "spot on", "hands down", "no brainer", "no-brainer", "my lips are sealed",
        "long story short", "bottom line", "to each their own", "different strokes",
        "speak of the devil", "break a leg", "fingers crossed", "knock on wood", "time will tell",
        "better late than never", "actions speak louder than words", "easier said than done"
    )

    private val negativeReactionTerms = setOf("my bad", "yikes", "oof", "hard pass", "not me")
    private val neutralReactionTerms = setOf(
        "fair enough", "touché", "touche", "you do you", "it is what it is",
        "to each their own", "different strokes", "time will tell",
        "better late than never", "easier said than done", "my lips are sealed",
        "break a leg", "fingers crossed", "knock on wood", "speak of the devil"
    )

    private val objectRequiringTailPreps = setOf(
        "to", "for", "with", "from", "of", "at", "after", "against", "about", "into", "upon",
        "onto", "by", "over", "through", "on"
    )

    private val definiteNounStarters = setOf(
        "short", "long", "best", "worst", "upper", "lion's", "critical", "bottom", "front",
        "wrong", "right", "flip", "other", "last", "first", "final", "whole", "elephant",
        "tip", "rest", "bulk", "heart", "root", "core", "top", "end", "benefit", "thick"
    )

    private val adjectiveDefinitionStarters = listOf(
        "extremely ", "very ", "overly ", "unusually ", "highly ", "noticeably ",
        "fiercely ", "wildly ", "utterly ", "openly ", "emotionally ", "physically ",
        "socially ", "financially ", "exceptionally ", "enthusiastic modern slang meaning extraordinarily",
        "over-the-top", "mediocre", "predictably ", "feeling ", "uncomfortably ",
        "ready and willing", "completely ", "brand-new", "unfiltered", "trim, efficient",
        "stylishly ", "painfully ", "super close", "having every hour", "classic surf",
        "modern slang for disappointingly", "sharp, ", "bitter, ", "lacking ", "full of ",
        "characterized by", "marked by", "showing ", "inclined to", "prone to", "capable of",
        "lasting ", "impossible to", "difficult to", "easy to", "willing ", "reluctant ",
        "deeply ", "acting ", "available ", "suddenly ", "pumped ", "hostile", "conducted ",
        "repeating ", "caught ", "playfully caught", "legitimate, ", "honest and ", "fresh, ",
        "mainstream, ", "short-lived", "clear and ", "vague", "secret", "hidden", "harsh",
        "gentle", "calm", "bold", "timid", "lazy", "eager", "stubborn", "flexible"
    )

    fun cleanTerm(term: String): String =
        term.replace(Regex("\\([^)]*\\)"), "").trim()

    fun articleFor(word: String): String {
        val lower = word.trim().lowercase(Locale.US)
        if (lower.startsWith("uni") || lower.startsWith("use") || lower.startsWith("eu") || lower.startsWith("one")) {
            return "a"
        }
        if (lower.startsWith("honest") || lower.startsWith("hour") || lower.startsWith("heir")) {
            return "an"
        }
        val first = lower.firstOrNull() ?: return "a"
        return if (first in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
    }

    fun classifyGrammar(
        category: PhraseCategory,
        term: String,
        definition: String
    ): ExpressionGrammar {
        val clean = cleanTerm(term)
        val lower = clean.lowercase(Locale.US)
        val words = lower.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val firstWord = words.firstOrNull().orEmpty()
        val lastWord = words.lastOrNull().orEmpty()
        val defLower = definition.trim().lowercase(Locale.US)

        if (lower in standaloneReactionTerms ||
            defLower.startsWith("quick casual way to say") ||
            defLower.startsWith("casual way to say") ||
            defLower.startsWith("short affirmative reply") ||
            defLower.startsWith("a crisp modern slang confirmation") ||
            defLower.startsWith("exclamation") ||
            defLower.startsWith("interjection") ||
            defLower.startsWith("a quick reply") ||
            defLower.startsWith("said when") ||
            defLower.startsWith("used to express agreement")
        ) {
            return ExpressionGrammar.STANDALONE_REACTION
        }

        if (firstWord in setOf("ate", "nailed", "crushed", "killed", "slayed", "bombed", "dodged", "dropped") &&
            words.size >= 2
        ) {
            return ExpressionGrammar.PAST_VERB_PHRASE
        }

        if (firstWord in setOf("a", "an", "the")) {
            return ExpressionGrammar.DETERMINER_NOUN
        }

        if (category == PhraseCategory.PHRASAL_VERB) {
            return classifyPhrasalVerb(lastWord, defLower)
        }

        if (category == PhraseCategory.GRE_VOCAB) {
            return classifyGre(clean, lower, defLower)
        }

        // Prepositional / negated predicate states ("out of pocket", "above board", "up to your neck", "sick and tired")
        if (firstWord in prepStarters || firstWord == "not" || firstWord == "no" ||
            (lower.contains(" and ") && firstWord in setOf(
                "sick", "tired", "fed", "safe", "high", "down", "far", "fast", "loud",
                "dead", "alive", "free", "fair", "short", "sweet", "rough", "ready"
            ))
        ) {
            return ExpressionGrammar.PREPOSITIONAL_STATE
        }

        // Check definition-driven NOUNS BEFORE commonVerbStarters so polysemous nouns
        // like "cap", "tea", "shade", "pitch", "base", "margin", "runway", "cliff", "sponsor"
        // are NEVER misclassified as verbs!
        if (defLower.startsWith("a ") || defLower.startsWith("an ") ||
            defLower.startsWith("someone ") || defLower.startsWith("one who ") ||
            defLower.startsWith("modern slang meaning a ") || defLower.startsWith("slang for a ")
        ) {
            return when {
                firstWord in definiteNounStarters -> ExpressionGrammar.DEFINITE_NOUN
                SemanticIntentClassifier.isPersonNoun(clean, definition) -> ExpressionGrammar.PERSON_NOUN
                else -> ExpressionGrammar.THING_NOUN
            }
        }
        if (defLower.startsWith("juicy,") || defLower.startsWith("subtle,") ||
            defLower.startsWith("home turf") || defLower.startsWith("ownership shares") ||
            defLower.startsWith("available time") || defLower.startsWith("the quality of") ||
            defLower.startsWith("the state of") || defLower.startsWith("the percentage") ||
            defLower.startsWith("the initial") || defLower.startsWith("formal ") ||
            defLower.startsWith("rapid, ") || defLower.startsWith("how a ") ||
            (defLower.startsWith("the ") && !defLower.startsWith("the act of"))
        ) {
            return if (firstWord in definiteNounStarters) {
                ExpressionGrammar.DEFINITE_NOUN
            } else {
                ExpressionGrammar.UNCOUNTABLE_NOUN
            }
        }

        // Check definition-driven ADJECTIVES BEFORE commonVerbStarters so polysemous adjectives
        // like "fire", "sick", "steep", "tight", "lean", "sharp", "booked solid", "cooked", "mid", "extra"
        // are NEVER misclassified as verbs!
        if (adjectiveDefinitionStarters.any { defLower.startsWith(it) }) {
            return ExpressionGrammar.ADJECTIVE
        }

        // Verb phrases (definition starts with verb or term begins with common verb starter)
        val defLooksLikeVerb = defLower.startsWith("to ") ||
            defLower.startsWith("perform ") || defLower.startsWith("make ") ||
            defLower.startsWith("cause ") || defLower.startsWith("cast ") ||
            defLower.startsWith("push ") || defLower.startsWith("take ") ||
            defLower.startsWith("give ") || defLower.startsWith("put ") ||
            defLower.startsWith("show off") || defLower.startsWith("cover ") ||
            defLower.startsWith("pay ") || defLower.startsWith("present ") ||
            defLower.startsWith("call out") || defLower.startsWith("hype ") ||
            defLower.startsWith("abruptly ") || defLower.startsWith("simmer ") ||
            defLower.startsWith("sit simmering") || defLower.startsWith("walk ") ||
            defLower.startsWith("reveal ") || defLower.startsWith("exhaust ") ||
            defLower.startsWith("underestimate ") || defLower.startsWith("subject ")

        if (defLooksLikeVerb || firstWord in commonVerbStarters) {
            val alreadyHasObject = words.size >= 2 && (
                lastWord in setOf(
                    "it", "them", "him", "her", "us", "yourself", "myself", "oneself",
                    "hand", "sword", "ice", "bait", "bullet", "chase", "base", "curve", "line",
                    "ball", "hat", "water", "fire", "air", "wind", "ship", "deck", "table",
                    "floor", "wall", "book", "card", "cards", "hatchet", "towel", "beans",
                    "cat", "bag", "boat", "bridge", "bridges", "bucket", "cake", "candle",
                    "ends", "corner", "corners", "cord", "day", "deal", "dirt", "dust", "edge",
                    "eye", "face", "feet", "foot", "gap", "gas", "gear", "gold", "ground",
                    "gun", "hair", "head", "heat", "hell", "hook", "horn", "horse", "house",
                    "jaw", "knee", "knot", "leg", "lid", "light", "loop", "mark", "mind",
                    "money", "moon", "mouth", "music", "nail", "neck", "nerve", "news",
                    "night", "nose", "nut", "ocean", "oil", "pace", "page", "pain", "pan",
                    "paper", "part", "party", "path", "peace", "penny", "pie", "pill", "pin",
                    "pipe", "pitch", "place", "plan", "plate", "plug", "point", "pot", "pulse",
                    "punch", "question", "rain", "record", "red", "reins", "ring", "road",
                    "rock", "roof", "room", "rope", "round", "row", "rug", "rule", "salt",
                    "sand", "scene", "score", "scratch", "screw", "sea", "seat", "seed",
                    "sense", "shape", "sheet", "shell", "shirt", "shoe", "shoes", "shot",
                    "shoulder", "show", "side", "sight", "sign", "skin", "sky", "slack",
                    "slate", "sleeve", "smoke", "snow", "sock", "socks", "song", "soul",
                    "soup", "spade", "spark", "speed", "spell", "spin", "spirit", "spot",
                    "stage", "stake", "stakes", "stand", "star", "stars", "steam", "steel",
                    "step", "stick", "stone", "storm", "story", "straw", "streak", "street",
                    "stride", "strike", "string", "strings", "strip", "stuff", "suit", "sun",
                    "surface", "sweat", "switch", "tab", "tail", "tape", "target", "taste",
                    "tea", "team", "teeth", "tempo", "test", "thick", "thin", "thing",
                    "things", "thread", "throat", "thumb", "thunder", "ticket", "tide", "time",
                    "tin", "tip", "title", "toe", "toes", "toll", "tone", "tongue", "tooth",
                    "top", "torch", "touch", "town", "track", "tracks", "trade", "trail",
                    "train", "trap", "trash", "tree", "trend", "trick", "trigger", "trip",
                    "trouble", "truck", "trump", "truth", "tube", "tune", "turf", "turn",
                    "twist", "two", "umbrella", "value", "veil", "vein", "view", "voice",
                    "void", "vote", "wage", "wagon", "waist", "wake", "walk", "war", "wash",
                    "watch", "wave", "way", "weather", "web", "wedge", "weed", "weeds", "week",
                    "weight", "well", "west", "wheel", "wheels", "whim", "whip", "whirl",
                    "whistle", "white", "whole", "wick", "wife", "wild", "will", "win",
                    "wing", "wings", "wink", "wire", "wish", "wit", "wits", "wolf", "wood",
                    "woods", "wool", "word", "words", "work", "world", "worm", "worms",
                    "worth", "wound", "wrap", "wraps", "wreck", "wrench", "wrist", "wrong",
                    "yard", "year", "years", "yellow", "yes", "yield", "yoke", "zero", "zone"
                ) || words.any {
                    it in setOf("a", "an", "the", "your", "my", "our", "his", "her", "their", "one's", "someone's", "someone")
                }
            )
            return when {
                alreadyHasObject -> ExpressionGrammar.COMPLETE_VERB_PHRASE
                (lastWord in objectRequiringTailPreps || words.size == 1) &&
                    (defLower.contains("someone") || defLower.contains("a friend") || defLower.contains("a buddy") || defLower.contains("person")) ->
                    ExpressionGrammar.TRANSITIVE_PERSON_VERB
                lastWord in objectRequiringTailPreps || defLower.contains("something") ||
                    defLower.startsWith("call out") || defLower.startsWith("present ") ||
                    defLower.startsWith("pay ") || defLower.startsWith("cover ") ->
                    ExpressionGrammar.TRANSITIVE_THING_VERB
                else -> ExpressionGrammar.INTRANSITIVE_VERB
            }
        }

        // Morphological adjective cues
        if (lower.endsWith("ed") || lower.endsWith("ous") || lower.endsWith("ive") ||
            lower.endsWith("ful") || lower.endsWith("less") || lower.endsWith("able") ||
            lower.endsWith("ible") || lower.endsWith("ish") || lower.endsWith("ic") ||
            lower.endsWith("al") || (lower.endsWith("y") && words.size == 1)
        ) {
            return ExpressionGrammar.ADJECTIVE
        }

        return if (words.size >= 2) {
            ExpressionGrammar.UNCOUNTABLE_NOUN
        } else {
            ExpressionGrammar.ADJECTIVE
        }
    }

    private fun classifyPhrasalVerb(lastWord: String, defLower: String): ExpressionGrammar {
        val isIntransitive = defLower.startsWith("to become") ||
            defLower.startsWith("to move") || defLower.startsWith("to walk") ||
            defLower.startsWith("to stay") || defLower.startsWith("to subside") ||
            defLower.startsWith("to behave") || defLower.startsWith("to malfunction") ||
            defLower.startsWith("to wait") || defLower.startsWith("to leave") ||
            defLower.startsWith("to arrive") || defLower.startsWith("to happen") ||
            defLower.startsWith("to appear") || defLower.startsWith("to vanish") ||
            defLower.startsWith("to fail") || defLower.startsWith("to stop") ||
            defLower.startsWith("to collapse") || defLower.startsWith("to sleep") ||
            defLower.startsWith("to relax") || defLower.startsWith("to gather") ||
            defLower.startsWith("to agree") || defLower.startsWith("to argue") ||
            defLower.startsWith("to speak") || defLower.startsWith("to laugh") ||
            defLower.startsWith("to cry") || defLower.startsWith("to wake") ||
            defLower.startsWith("to hurry") || defLower.startsWith("to slow") ||
            defLower.startsWith("to fall") || defLower.startsWith("to rise") ||
            defLower.startsWith("to run") || defLower.startsWith("to sit") ||
            defLower.startsWith("to stand") || defLower.startsWith("to hang") ||
            defLower.startsWith("to die") || defLower.startsWith("to live") ||
            defLower.startsWith("to work") || defLower.startsWith("to grow") ||
            defLower.startsWith("to calm") || defLower.startsWith("to settle") ||
            defLower.startsWith("to fade") || defLower.startsWith("to break down") ||
            defLower.startsWith("to show up") ||
            lastWord in setOf("together", "apart", "ahead", "along", "away", "around")

        val targetsPerson = defLower.contains("someone") ||
            defLower.contains("a friend") || defLower.contains("a person") ||
            defLower.contains("people") || defLower.contains("colleague") ||
            defLower.contains("partner")

        return when {
            isIntransitive && lastWord !in setOf("to", "for", "with", "from", "of", "at", "after", "into") ->
                ExpressionGrammar.INTRANSITIVE_VERB
            targetsPerson -> ExpressionGrammar.TRANSITIVE_PERSON_VERB
            else -> ExpressionGrammar.TRANSITIVE_THING_VERB
        }
    }

    private fun classifyGre(clean: String, lowerTerm: String, lowerDef: String): ExpressionGrammar {
        val nounPrefixes = listOf(
            "the ", "lack of", "state of", "quality of", "act of", "growth or",
            "bitterness", "humble ", "conformity", "brisk,", "excessive ",
            "deep ", "intense ", "strong ", "public ", "official ", "formal "
        )
        val intransitiveStarts = listOf(
            "become ", "to become ", "leave ", "to leave ", "agree ", "to agree ",
            "accept ", "to accept ", "comply ", "to comply ", "yield ", "to yield ",
            "rise ", "to rise ", "fall ", "to fall ", "grow ", "to grow ",
            "shrink ", "to shrink ", "speak ", "to speak ", "talk ", "to talk ",
            "wait ", "to wait ", "live ", "to live ", "move ", "to move ",
            "act ", "to act ", "wander ", "to wander ", "waver ", "to waver ",
            "walk ", "to walk ", "subside", "fade", "vanish", "hesitate", "fluctuate",
            "persist", "flee", "escape", "proliferate", "burgeon", "languish", "dwindle"
        )
        val transitiveStarts = listOf(
            "to ", "make ", "cause ", "renounce ", "regard ", "solemnly ", "detest ",
            "scrape ", "shorten ", "repeal ", "set or ", "cite ", "confirm ",
            "remove ", "improve ", "draw out", "decorate ", "match or", "approach ",
            "praise ", "criticize ", "attack ", "weaken ", "strengthen ", "soothe ",
            "calm ", "reduce ", "increase ", "explain ", "clarify ", "confuse ",
            "deceive ", "mislead ", "delay ", "hinder ", "block ", "prevent ",
            "support ", "endorse ", "reject ", "condemn ", "forgive ", "excuse ",
            "persuade ", "convince ", "encourage ", "provoke ", "arouse ", "suppress ",
            "hide ", "reveal ", "expose ", "copy ", "imitate ", "replace ", "change ",
            "alter ", "combine ", "separate ", "divide ", "examine ", "inspect ",
            "verify ", "prove ", "disprove ", "challenge ", "question ", "ignore ",
            "overlook ", "avoid ", "abandon ", "surrender ", "seize ", "soak ",
            "take ", "give ", "offer ", "grant ", "deny ", "refuse ", "require ",
            "demand ", "force ", "compel ", "allow ", "permit ", "forbid ", "ban "
        )

        if (lowerDef.startsWith("a ") || lowerDef.startsWith("an ") || lowerDef.startsWith("one who ") || lowerDef.startsWith("someone who ")) {
            return if (SemanticIntentClassifier.isPersonNoun(clean, lowerDef)) {
                ExpressionGrammar.PERSON_NOUN
            } else {
                ExpressionGrammar.THING_NOUN
            }
        }
        if (intransitiveStarts.any { lowerDef.startsWith(it) }) {
            return ExpressionGrammar.INTRANSITIVE_VERB
        }
        if (transitiveStarts.any { lowerDef.startsWith(it) }) {
            return if (lowerDef.contains("someone") || lowerDef.contains("a person")) {
                ExpressionGrammar.TRANSITIVE_PERSON_VERB
            } else {
                ExpressionGrammar.TRANSITIVE_THING_VERB
            }
        }
        if (nounPrefixes.any { lowerDef.startsWith(it) }) {
            return ExpressionGrammar.UNCOUNTABLE_NOUN
        }
        return when {
            lowerTerm.endsWith("tion") || lowerTerm.endsWith("sion") ||
                lowerTerm.endsWith("ment") || lowerTerm.endsWith("ity") ||
                lowerTerm.endsWith("ness") || lowerTerm.endsWith("ism") ||
                lowerTerm.endsWith("ance") || lowerTerm.endsWith("ence") ||
                lowerTerm.endsWith("acy") || lowerTerm.endsWith("itude") -> ExpressionGrammar.UNCOUNTABLE_NOUN
            lowerTerm.endsWith("ist") || (lowerTerm.endsWith("ant") && lowerDef.contains("person")) ->
                ExpressionGrammar.PERSON_NOUN
            lowerTerm.endsWith("ate") && lowerDef.startsWith("make") -> ExpressionGrammar.TRANSITIVE_THING_VERB
            else -> ExpressionGrammar.ADJECTIVE
        }
    }

    /**
     * Wraps a hand-written bespoke example sentence in a natural 3-turn exchange whose
     * opener and closing reaction match the example's polarity/domain without clashing.
     */
    fun wrapBespokeExampleInDialogue(
        category: PhraseCategory,
        exampleLine: String,
        hash: Int,
        definition: String = ""
    ): List<DialogueTurn> {
        val speakers = if (category == PhraseCategory.WORKPLACE) workplaceSpeakers else everydaySpeakers
        val (speakerA, speakerB) = speakers[hash % speakers.size]
        val intent = SemanticIntentClassifier.classifySemanticIntent(category, "", "$definition $exampleLine")
        val polarity = SemanticIntentClassifier.classifyPolarity(intent, "$definition $exampleLine")

        val openers = when (category) {
            PhraseCategory.WORKPLACE -> listOf(
                "Quick check—where did things land on that?",
                "Wait—what's the latest update before our afternoon sync?",
                "Before we wrap up, how are we looking on that?",
                "Did you get a chance to check in with the rest of the team?"
            )
            PhraseCategory.SLANG -> listOf(
                "Wait, what's the story with that?",
                "Be honest—what ended up happening?",
                "Hold on, fill me in—what's the update?",
                "Okay, spill—what's going on?"
            )
            PhraseCategory.IDIOM -> listOf(
                "How are things shaking out over there?",
                "Wait, what happened after you talked it through?",
                "Sounds like a lot—how's it looking now?",
                "So where do things stand?"
            )
            PhraseCategory.PHRASAL_VERB -> listOf(
                "Hey, how did that end up shaking out?",
                "Wait—what happened after you looked into it?",
                "How did the rest of that go?",
                "Any update on how that worked out?"
            )
            PhraseCategory.GRE_VOCAB -> listOf(
                "Wait, what happened earlier? I missed the first part.",
                "Seriously? Give me the short version.",
                "How did that actually go down?",
                "You mentioned that earlier—what's the full story?"
            )
        }

        val reactions = when (polarity) {
            Polarity.POSITIVE -> listOf(
                "Love to hear that—definitely sounds like it paid off.",
                "Nice! That worked out way better than expected.",
                "Honestly, that's awesome. Glad it came together.",
                "Good call—sounds like the right move all around."
            )
            Polarity.NEGATIVE -> listOf(
                "Ugh, yeah—that's frustrating to deal with.",
                "Oof, I don't blame you for feeling that way.",
                "Yikes, good thing you spotted that early.",
                "Yeah, that's rough. Hopefully it gets sorted out soon."
            )
            Polarity.NEUTRAL -> listOf(
                "Makes total sense now that I hear the whole context.",
                "Got it—thanks for filling me in.",
                "Fair enough. Definitely keep me posted on how it goes.",
                "Yeah, that tracks. Let me know what happens next."
            )
        }

        return listOf(
            DialogueTurn(speakerA, openers[hash % openers.size]),
            DialogueTurn(speakerB, exampleLine),
            DialogueTurn(speakerA, reactions[(hash / 5) % reactions.size])
        )
    }

    /**
     * Builds a realistic 3-turn lesson dialogue for compact 5-column entries across ALL 5 categories.
     * Routes by BOTH grammatical slot AND semantic intent/polarity so the conversation
     * always reinforces the word's true meaning and never evokes a different word.
     */
    fun buildNaturalLessonDialogue(
        category: PhraseCategory,
        term: String,
        definition: String,
        hash: Int,
        synonyms: List<String> = emptyList()
    ): List<DialogueTurn> {
        val clean = cleanTerm(term)
        val grammar = classifyGrammar(category, clean, definition)
        val intent = SemanticIntentClassifier.classifySemanticIntent(category, clean, definition, synonyms)
        val polarity = SemanticIntentClassifier.classifyPolarity(intent, definition)
        val spoken = SemanticIntentClassifier.inflectForSpeech(clean, secondPerson = true)
        val spokenThird = SemanticIntentClassifier.inflectForSpeech(clean, secondPerson = false)
        val speakers = if (category == PhraseCategory.WORKPLACE) workplaceSpeakers else everydaySpeakers
        val (speakerA, speakerB) = speakers[hash % speakers.size]
        val usesSecondPerson = spoken.lowercase(Locale.US).let { it.contains("your") || it.contains("yourself") }

        val vignettes = pickLessonVignettes(
            category = category,
            grammar = grammar,
            intent = intent,
            polarity = polarity,
            clean = clean,
            spoken = spoken,
            spokenThird = spokenThird,
            usesSecondPerson = usesSecondPerson
        )

        val (turn1, turn2, turn3) = vignettes[abs(hash) % vignettes.size]
        return listOf(
            DialogueTurn(speakerA, turn1),
            DialogueTurn(speakerB, turn2),
            DialogueTurn(speakerA, turn3)
        )
    }

    /**
     * Generates a BRAND-NEW unseen spoken conversation for True Transfer Learning
     * (strictly distinct from lesson dialogues, matched to exact grammar AND semantic intent).
     */
    fun buildTransferScene(
        category: PhraseCategory,
        term: String,
        definition: String,
        seed: Int,
        synonyms: List<String> = emptyList()
    ): TransferScene {
        val clean = cleanTerm(term)
        val grammar = classifyGrammar(category, clean, definition)
        val intent = SemanticIntentClassifier.classifySemanticIntent(category, clean, definition, synonyms)
        val polarity = SemanticIntentClassifier.classifyPolarity(intent, definition)
        val spoken = SemanticIntentClassifier.inflectForSpeech(clean, secondPerson = true)
        val spokenThird = SemanticIntentClassifier.inflectForSpeech(clean, secondPerson = false)
        val (speakerA, speakerB) = transferSpeakers[abs(seed) % transferSpeakers.size]
        val usesSecondPerson = spoken.lowercase(Locale.US).let { it.contains("your") || it.contains("yourself") }

        val templates = pickTransferTemplates(
            category = category,
            grammar = grammar,
            intent = intent,
            polarity = polarity,
            clean = clean,
            spoken = spoken,
            spokenThird = spokenThird,
            usesSecondPerson = usesSecondPerson
        )

        val (domain, lead, replyPair) = templates[abs(seed) % templates.size]
        val (replyTpl, surfaceForm) = replyPair
        val formattedTerm = if (replyTpl.startsWith("%s")) {
            surfaceForm.replaceFirstChar { it.uppercase(Locale.US) }
        } else {
            surfaceForm
        }
        return TransferScene(
            domainLabel = domain,
            speakerA = speakerA,
            speakerB = speakerB,
            leadLine = lead,
            replyWithTarget = replyTpl.replace("%s", formattedTerm),
            replyWithBlank = replyTpl.replace("%s", "________")
        )
    }

    private fun pickLessonVignettes(
        category: PhraseCategory,
        grammar: ExpressionGrammar,
        intent: SemanticIntent,
        polarity: Polarity,
        clean: String,
        spoken: String,
        spokenThird: String,
        usesSecondPerson: Boolean
    ): List<Triple<String, String, String>> {
        val lower = clean.lowercase(Locale.US)
        val cap = spoken.replaceFirstChar { it.uppercase(Locale.US) }
        val art = articleFor(spoken)
        val artThird = articleFor(spokenThird)

        return when (grammar) {
            ExpressionGrammar.STANDALONE_REACTION -> when {
                lower in negativeReactionTerms || polarity == Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Did you see that someone accidentally replied-all to the entire company thread?",
                        "$cap—I would've closed my laptop and walked straight outside.",
                        "Seriously, my stomach dropped just reading it."
                    ),
                    Triple(
                        "They're asking us to drive three hours each way for a twenty-minute check-in.",
                        "$cap—let's ask if we can just jump on a quick video call instead.",
                        "Right? No way is that worth six hours in traffic."
                    )
                )
                lower in neutralReactionTerms -> listOf(
                    Triple(
                        "I know it sounds ambitious, but I'm going to see how the first month shakes out.",
                        "$cap—you'll have a much clearer read once you're actually in it.",
                        "True. One week at a time."
                    ),
                    Triple(
                        "Honestly, I'd rather take the slower scenic train than deal with airport security.",
                        "$cap—everyone's got their own travel rhythm.",
                        "Exactly. Give me a window seat and coffee any day."
                    )
                )
                else -> listOf(
                    Triple(
                        "Want to grab tacos by the park once we wrap up at five?",
                        "$cap—just text me when you're heading out the door.",
                        "Awesome, see you at the corner in a bit."
                    ),
                    Triple(
                        "That new spot on 4th is way better than the old one downtown.",
                        "$cap—we went last Friday and ordered way too much food.",
                        "Haha, I'm already sold. Let's go next week."
                    )
                )
            }

            ExpressionGrammar.PAST_VERB_PHRASE -> if (polarity == Polarity.NEGATIVE) {
                listOf(
                    Triple(
                        "How did the live demo go after the Wi-Fi cut out?",
                        "Rough—we totally $spokenThird on the second half, so we're rescheduling a clean walkthrough.",
                        "Ugh, tech glitches are the worst. Next run will go way smoother."
                    ),
                    Triple(
                        "Did everything stay on track with the vendor handoff?",
                        "Not really—they $spokenThird right before the deadline, so we had to scramble.",
                        "That's frustrating. Good thing you caught it before Monday."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "Did you catch Maya's set at the open mic last night?",
                        "Yeah, we sat right up front—she honestly $spokenThird and had the whole room cheering.",
                        "Ugh, so mad I missed it! Tell me someone got a video."
                    ),
                    Triple(
                        "How did your presentation go with the regional directors?",
                        "Way better than I expected—Jordan stepped in on Q&A and totally $spokenThird.",
                        "Nice! You guys earned an early Friday after that."
                    )
                )
            }

            ExpressionGrammar.TRANSITIVE_PERSON_VERB -> when (intent) {
                SemanticIntent.OPPOSE_BLOCK, SemanticIntent.CRITICISM_NEGATIVE -> listOf(
                    Triple(
                        "Why did Sam walk out of that group chat so abruptly?",
                        "Because everyone kept piling on and trying to $spoken him instead of hearing him out.",
                        "Oof, no wonder he muted the thread."
                    ),
                    Triple(
                        "Are they still returning your messages after the disagreement?",
                        "Nope—they decided to $spoken me right after I asked for a straight answer.",
                        "That's rough. Some people really hate awkward conversations."
                    )
                )
                SemanticIntent.SUPPORT_COLLABORATE, SemanticIntent.PRAISE_POSITIVE -> listOf(
                    Triple(
                        "How's the new teammate holding up before her first big presentation?",
                        "She was a little nervous, so I grabbed coffee to $spoken her before the meeting.",
                        "That's huge—having someone in your corner makes all the difference."
                    ),
                    Triple(
                        "Did you get a chance to catch up with Devon after the promotion?",
                        "Yeah, we met up at lunch so I could $spoken him properly.",
                        "Love that—he's been working toward that for months."
                    )
                )
                SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "Did you talk to Alex after the announcement went out?",
                        "Yeah, I pulled him aside for five minutes to $spoken him before rumors started.",
                        "Good move—hearing it directly is always better."
                    )
                )
                else -> listOf(
                    Triple(
                        "How did you handle that situation with the vendor lead?",
                        "I hopped on a quick call to $spoken her directly instead of letting emails drag on.",
                        "Smart call—a quick conversation usually clears things up faster."
                    ),
                    Triple(
                        "Have you caught up with Jordan since the team shuffle?",
                        "Yeah, I stopped by his desk before lunch to $spoken him.",
                        "Nice—glad you two got a minute to connect."
                    )
                )
            }

            ExpressionGrammar.TRANSITIVE_THING_VERB -> when (intent) {
                SemanticIntent.REDUCE_LESSEN -> listOf(
                    Triple(
                        "Is there any way to calm down the backlash before tomorrow's meeting?",
                        "Sending a clear, honest update tonight should $spoken the tension right away.",
                        "Agreed—better to address it head-on now."
                    ),
                    Triple(
                        "This onboarding flow still feels way too heavy for first-time users.",
                        "Let's $spoken the extra steps so people can finish in under a minute.",
                        "Smart—simpler is almost always better."
                    )
                )
                SemanticIntent.INCREASE_GROW -> listOf(
                    Triple(
                        "Should we post a hasty reply to those angry comments right now?",
                        "Definitely not—rushing out a defensive answer will only $spoken the problem.",
                        "Good point. Let's take a breath and get the facts straight first."
                    ),
                    Triple(
                        "What happened when they layered two more approvals onto the process?",
                        "It managed to $spoken the delays across every single department.",
                        "Oof—classic unintended consequence."
                    )
                )
                SemanticIntent.OPPOSE_BLOCK, SemanticIntent.CRITICISM_NEGATIVE -> listOf(
                    Triple(
                        "Did leadership end up approving that rushed policy change?",
                        "No—the review board voted to $spoken the proposal as soon as they saw the numbers.",
                        "Honestly, probably for the best."
                    ),
                    Triple(
                        "How did the editorial panel react to the draft report?",
                        "They didn't hold back—they moved to $spoken the whole premise in the first ten minutes.",
                        "Whoa, sounds like it's going back for a major rewrite."
                    )
                )
                SemanticIntent.DISCOVER_SOLVE -> listOf(
                    Triple(
                        "Did anyone figure out why the monthly totals weren't matching up?",
                        "We're sitting down at two to $spoken the discrepancy before finance closes the books.",
                        "Nice—let me know what you end up finding."
                    ),
                    Triple(
                        "That second chapter raises a ton of unanswered questions.",
                        "Keep reading—the author starts to $spoken the whole pattern by chapter four.",
                        "Okay, good—I'm staying with it tonight."
                    )
                )
                SemanticIntent.SUPPORT_COLLABORATE, SemanticIntent.PRAISE_POSITIVE -> listOf(
                    Triple(
                        "Do we have enough data to back up the recommendation in Friday's deck?",
                        "Yeah, the customer interviews really $spoken our case.",
                        "Awesome—that'll make the sign-off way easier."
                    )
                )
                SemanticIntent.SECRET_CONFUSING -> listOf(
                    Triple(
                        "Why did that press release leave everyone with more questions than answers?",
                        "Honestly, it felt like they wrote it to $spoken the real reason behind the delay.",
                        "Yeah, nobody buys vague corporate spin anymore."
                    )
                )
                SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "How are we handling the upfront venue deposit for the group trip?",
                        "I can $spoken the cost today and everyone can pay me back on Friday.",
                        "You're a lifesaver—I'll send my share tonight."
                    )
                )
                SemanticIntent.WORK_STRATEGY -> listOf(
                    Triple(
                        "Are we ready to share the plan with the broader group tomorrow?",
                        "Almost—I want to $spoken a few loose ends before the invite goes out.",
                        "Sounds good—ping me if you want another pair of eyes on it."
                    )
                )
                else -> listOf(
                    Triple(
                        "What's the plan for the rest of the afternoon?",
                        "I need twenty minutes to $spoken a couple open items, then I'm free.",
                        "Take your time—no rush at all."
                    ),
                    Triple(
                        "Wait, how did they handle that twist halfway through the project?",
                        "They stepped back to $spoken the whole approach before committing any further.",
                        "Makes sense—better to adjust early."
                    )
                )
            }

            ExpressionGrammar.COMPLETE_VERB_PHRASE -> when {
                usesSecondPerson && polarity == Polarity.NEGATIVE -> listOf(
                    Triple(
                        "I'm frustrated enough to send a pretty blunt reply to the whole thread.",
                        "Take five minutes first—you really don't want to $spoken over one tense afternoon.",
                        "Fair point. I'll draft it, let it sit, and cool off."
                    ),
                    Triple(
                        "Think I should skip the final check so I can leave early?",
                        "I wouldn't—if you $spoken now, it'll just come back to bite you next week.",
                        "Yeah, you're right. Better to finish cleanly."
                    )
                )
                usesSecondPerson -> listOf(
                    Triple(
                        "Should I wait for their first offer before showing all my numbers?",
                        "Definitely listen first—you don't want to $spoken too early.",
                        "Good advice. I'll hear where they land first."
                    ),
                    Triple(
                        "I've got a packed month ahead—any advice on pacing myself?",
                        "Just make sure you don't $spoken trying to please everybody at once.",
                        "Needed to hear that. I'm blocking off Sunday."
                    )
                )
                intent == SemanticIntent.CRITICISM_NEGATIVE || intent == SemanticIntent.OPPOSE_BLOCK -> listOf(
                    Triple(
                        "Why did that contractor's remodel fall apart after six months?",
                        "Because they tried to $spokenThird instead of doing the prep work properly.",
                        "Ugh, cheap shortcuts always cost twice as much later."
                    ),
                    Triple(
                        "Did anyone warn them that rushing the rollout was risky?",
                        "Plenty of people did, but they still chose to $spokenThird anyway.",
                        "No surprise it blew up on launch day."
                    )
                )
                intent == SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "We're running low on time before the next meeting starts.",
                        "Good catch—let's $spokenThird for now and pick it back up tomorrow afternoon.",
                        "Works for me. That keeps us on schedule."
                    ),
                    Triple(
                        "Are we really going to keep debating this past six o'clock?",
                        "Honestly, let's $spokenThird and look at it with fresh eyes in the morning.",
                        "Deal—my brain is fried anyway."
                    )
                )
                intent == SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Are we still going forward with that pricey vendor add-on?",
                        "Once we saw the renewal quote, we decided to $spokenThird before spending another dime.",
                        "Smart—no reason to sink more money into it."
                    )
                )
                intent == SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "How did the conversation go once everybody sat down?",
                        "Nobody beat around the bush—they decided to $spokenThird right away.",
                        "Honestly, I respect directness way more than awkward hinting."
                    )
                )
                intent == SemanticIntent.PRAISE_POSITIVE || intent == SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "How did your team pull off that turnaround in a single week?",
                        "Everyone jumped in ready to $spokenThird, and nobody waited to be asked.",
                        "That's the kind of crew you never want to leave."
                    )
                )
                else -> listOf(
                    Triple(
                        "Think we should wait until Monday to deal with that?",
                        "Nah, better to $spokenThird right now while everything's fresh.",
                        "True—handling it sooner saves everyone a headache."
                    ),
                    Triple(
                        "What did you end up deciding after talking it over?",
                        "We weighed the options and figured it made the most sense to $spokenThird.",
                        "Fair enough—let me know how it shakes out."
                    )
                )
            }

            ExpressionGrammar.INTRANSITIVE_VERB -> when (intent) {
                SemanticIntent.REDUCE_LESSEN -> listOf(
                    Triple(
                        "Is the rain still coming down sideways? I left my umbrella at home.",
                        "Yeah, it's wild out there, though the forecast says it should $spoken in about twenty minutes.",
                        "Perfect—gives me an excuse to grab another coffee first."
                    ),
                    Triple(
                        "Are people still worked up over that online argument?",
                        "Not really—those flare-ups usually $spoken as soon as the weekend hits.",
                        "So true. Everybody moves on pretty fast."
                    )
                )
                SemanticIntent.INCREASE_GROW -> listOf(
                    Triple(
                        "Did adding another layer of rules actually calm things down?",
                        "Not at all—complaints started to $spoken within twenty-four hours.",
                        "Oof, that backfired fast."
                    ),
                    Triple(
                        "How fast are those neighborhood pop-ups catching on?",
                        "Wildly fast—new spots seem to $spoken every single month.",
                        "No kidding—I can barely keep track of all of them."
                    )
                )
                SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "How was the panel discussion last night?",
                        "Interesting, though one speaker started to $spoken for twenty minutes straight without taking questions.",
                        "Haha, every conference has at least one person like that."
                    )
                )
                SemanticIntent.OPPOSE_BLOCK, SemanticIntent.CRITICISM_NEGATIVE -> listOf(
                    Triple(
                        "Wait, did Jordan actually stick around for the tough part of the project?",
                        "Nope—the second things got messy, he decided to $spoken.",
                        "Ugh, that leaves everyone else scrambling."
                    )
                )
                SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "Are we still waiting on an answer before booking the tickets?",
                        "Yeah, I hate to $spoken, but prices jump at midnight if we don't lock it in.",
                        "Good call—let's decide after lunch."
                    )
                )
                else -> listOf(
                    Triple(
                        "How are things settling in after last week's big shift?",
                        "The first couple days were chaotic, though things are starting to $spoken now.",
                        "Glad to hear it's smoothing out."
                    ),
                    Triple(
                        "Think everyone's going to stick with the original plan?",
                        "Hard to tell yet—let's see how things $spoken over the next couple days.",
                        "Fair enough. Keep me posted."
                    )
                )
            }

            ExpressionGrammar.PREPOSITIONAL_STATE -> when {
                usesSecondPerson -> listOf(
                    Triple(
                        "Sorry I missed your text—between deadlines and packing, this week's been nonstop.",
                        "No worries at all, sounds like you're $spoken right now—let's catch up next week.",
                        "Appreciate it! Coffee's on me once things calm down."
                    )
                )
                intent == SemanticIntent.CRITICISM_NEGATIVE || intent == SemanticIntent.OPPOSE_BLOCK -> listOf(
                    Triple(
                        "Wait, what happened at dinner last night? You looked stunned.",
                        "His comment to the server was completely $spokenThird—the whole table went quiet.",
                        "Yikes, I'm sorry you had to sit through that."
                    ),
                    Triple(
                        "How did people react to that surprise email from management?",
                        "Pretty frustrated—honestly, the whole thing felt $spokenThird.",
                        "Yeah, nobody appreciated getting blindsided like that."
                    )
                )
                intent == SemanticIntent.PRAISE_POSITIVE || intent == SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "How did the neighborhood crew handle helping with the move?",
                        "They were honestly $spokenThird—stayed late and didn't even let me buy pizza.",
                        "You've got the best friends."
                    ),
                    Triple(
                        "Did the new vendor deliver what they promised?",
                        "Even better—their support team went $spokenThird all week.",
                        "Love when a team actually comes through like that."
                    )
                )
                intent == SemanticIntent.CLEAR_DIRECT -> listOf(
                    Triple(
                        "Did procurement finish checking the new partner contract?",
                        "Yep, everything came back $spokenThird, so we're good to kick off Monday.",
                        "Awesome—glad there weren't any surprises."
                    )
                )
                intent == SemanticIntent.SECRET_CONFUSING -> listOf(
                    Triple(
                        "Did anyone actually tell your team about the reorganization ahead of time?",
                        "Not a word—we were completely $spokenThird until the all-hands.",
                        "Ugh, finding out like that is the worst."
                    )
                )
                intent == SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "Are we able to get a quick answer from legal before noon?",
                        "Probably not—half their team is $spokenThird until Thursday afternoon.",
                        "Got it, thanks for the heads-up. I'll adjust the timeline."
                    )
                )
                intent == SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Did the airline end up covering your hotel after the missed connection?",
                        "Not yet—I had to pay $spokenThird last night and submit the receipt.",
                        "Make sure you screenshot every receipt before filing."
                    )
                )
                else -> listOf(
                    Triple(
                        "Have you decided whether you're taking the new apartment or renewing?",
                        "Still $spokenThird—both options have real tradeoffs.",
                        "Take the weekend to think it through; no need to rush."
                    ),
                    Triple(
                        "How are things looking with the weekend plans?",
                        "Honestly, everything's still $spokenThird until we hear back from Sam.",
                        "Makes sense—just text me once it's locked in."
                    )
                )
            }

            ExpressionGrammar.ADJECTIVE -> when (intent) {
                SemanticIntent.PRAISE_POSITIVE, SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "So, was that new documentary actually worth staying up for?",
                        "Honestly, yes—the cinematography and pacing were genuinely $spoken.",
                        "Okay, adding it to my weekend watchlist right now."
                    ),
                    Triple(
                        "How did your meeting with the new design lead go?",
                        "Great—her feedback was super $spoken, and we left with a much clearer plan.",
                        "Love working with people who elevate the whole room."
                    )
                )
                SemanticIntent.CRITICISM_NEGATIVE, SemanticIntent.OPPOSE_BLOCK -> listOf(
                    Triple(
                        "Did you read the theater critic's column on the new play?",
                        "Yeah, over breakfast—their review was surprisingly $spoken, didn't spare a single scene.",
                        "Oof, tough crowd. Still curious to see it myself, though."
                    ),
                    Triple(
                        "How did that debate at the neighborhood meeting go?",
                        "Pretty tense—a couple folks got weirdly $spoken as soon as parking came up.",
                        "Yikes, local meetings always escalate way too fast."
                    )
                )
                SemanticIntent.BORING_OVERUSED -> listOf(
                    Triple(
                        "Worth watching the sequel this weekend, or should I skip it?",
                        "Skip it—the plot felt pretty $spoken, like we'd seen every twist a dozen times.",
                        "Thanks for saving me two hours and twenty bucks."
                    ),
                    Triple(
                        "How was the keynote at the industry summit?",
                        "Honestly a little $spoken—same recycled slides everyone's seen for three years.",
                        "Ugh, hate sitting through talks like that."
                    )
                )
                SemanticIntent.BRIEF_TEMPORARY -> listOf(
                    Triple(
                        "Think that viral food trend is still going to be packed next month?",
                        "Doubt it—those hype waves are usually pretty $spoken around here.",
                        "True, give it three weeks and there won't even be a line."
                    ),
                    Triple(
                        "Did they give a long explanation for the policy shift?",
                        "Nope—the memo was super $spoken, just two short lines and nothing else.",
                        "Whoa, definitely leaves a lot to unpack."
                    )
                )
                SemanticIntent.EXCESSIVE_INTENSE -> listOf(
                    Triple(
                        "How was your cousin's wedding reception?",
                        "Fun, though the five-piece fireworks show during dessert felt a little $spoken.",
                        "Haha, okay, that definitely sounds unforgettable."
                    ),
                    Triple(
                        "How's the inbox looking after taking three days off?",
                        "Honestly, pretty $spoken—I've got two hundred unread threads waiting.",
                        "Oof, grab a big coffee before diving in."
                    )
                )
                SemanticIntent.REDUCE_LESSEN -> listOf(
                    Triple(
                        "Did the crowd thin out after the first band wrapped up?",
                        "Yeah, the energy got much more $spoken by ten o'clock.",
                        "Nice—that's honestly my favorite vibe."
                    )
                )
                SemanticIntent.SECRET_CONFUSING -> listOf(
                    Triple(
                        "Did the legal disclaimer actually answer your question?",
                        "Not even close—the wording was so $spoken that nobody could tell what it meant.",
                        "Classic fine print. Ask them for a plain-English summary."
                    )
                )
                SemanticIntent.CLEAR_DIRECT -> listOf(
                    Triple(
                        "Was it obvious what the director wanted changed in the edit?",
                        "Completely—her notes were super $spoken, so we fixed it in ten minutes.",
                        "Nothing beats crisp, clear feedback."
                    )
                )
                SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Did you end up booking that boutique hotel near the conference?",
                        "No way—the nightly rate felt way too $spoken for a two-night work trip.",
                        "Smart call. There are way better options two blocks over."
                    )
                )
                SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "Got ten minutes to review the draft before three?",
                        "I'm pretty $spoken this afternoon, so let's grab time first thing tomorrow.",
                        "Works for me—I'll drop a hold on your morning calendar."
                    )
                )
                else -> listOf(
                    Triple(
                        "Did you finish that podcast episode I sent over?",
                        "Yeah, on my commute—the host's take turned surprisingly $spoken in the second half.",
                        "Right? I wasn't expecting it to go in that direction."
                    ),
                    Triple(
                        "How's this month's book club pick so far?",
                        "Pretty gripping, though the middle chapters feel a little $spoken.",
                        "Good to know—I'll dive into it Saturday morning."
                    )
                )
            }

            ExpressionGrammar.DETERMINER_NOUN -> when (polarity) {
                Polarity.NEGATIVE -> listOf(
                    Triple(
                        "How did a ten-minute logistics sync turn into an hour of arguing?",
                        "No idea—one comment about seating and suddenly we were dealing with $spoken.",
                        "Oof, glad I missed that meeting."
                    )
                )
                Polarity.POSITIVE -> listOf(
                    Triple(
                        "Why is every team suddenly excited to join the pilot?",
                        "Because once something starts paying off, everybody wants $spoken.",
                        "Haha, true—nobody volunteered when it was still rough."
                    )
                )
                Polarity.NEUTRAL -> listOf(
                    Triple(
                        "How are you feeling about the decision now that the dust settled?",
                        "Honestly, pretty good—it feels like $spoken for where we're at right now.",
                        "Makes total sense. Glad it's feeling solid."
                    )
                )
            }

            ExpressionGrammar.DEFINITE_NOUN -> when (polarity) {
                Polarity.NEGATIVE -> listOf(
                    Triple(
                        "How did splitting up the weekend shifts end up working out?",
                        "Not great—I somehow got the $spoken and ended up closing both nights.",
                        "Oof, they definitely owe you next weekend off."
                    )
                )
                Polarity.POSITIVE -> listOf(
                    Triple(
                        "How did your team pull ahead in the second half of the pitch?",
                        "Once we showed the live numbers, we clearly had the $spoken.",
                        "Nice! Nothing beats walking in prepared."
                    )
                )
                Polarity.NEUTRAL -> listOf(
                    Triple(
                        "What's the main thing holding up the final decision?",
                        "That detail has been the $spoken since day one—everything else is already agreed.",
                        "Got it. Let's focus there so we can close it out."
                    )
                )
            }

            ExpressionGrammar.PERSON_NOUN -> when (polarity) {
                Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Why did everyone roll their eyes when Marcus chimed in during the all-hands?",
                        "Because he always acts like $art $spoken whenever leadership is in the room.",
                        "Ugh, yeah—everybody sees right through that."
                    ),
                    Triple(
                        "Are you inviting your old coworker to the dinner Friday?",
                        "Probably not—he can be a bit of $art $spoken and turns simple hangs into drama.",
                        "Fair enough. Let's keep the group relaxed."
                    )
                )
                Polarity.POSITIVE -> listOf(
                    Triple(
                        "Have you worked with Nadia on the community project yet?",
                        "Yeah, she's $art real $spoken—everybody trusts her judgment when things get tough.",
                        "Love hearing that. Can't wait to meet her next week."
                    ),
                    Triple(
                        "How did your sister handle organizing the whole reunion by herself?",
                        "She's $art total $spoken—kept everyone calm and somehow made it look effortless.",
                        "Honestly, buy her dinner next time you see her."
                    )
                )
                Polarity.NEUTRAL -> listOf(
                    Triple(
                        "Have you read that Sunday profile on the neighborhood architect?",
                        "Yeah, twice—people who know her describe her as $art $spoken with a pretty distinct way of working.",
                        "Send me the link! Sounds like a great weekend read."
                    )
                )
            }

            ExpressionGrammar.THING_NOUN -> when (intent) {
                SemanticIntent.CRITICISM_NEGATIVE, SemanticIntent.OPPOSE_BLOCK, SemanticIntent.BORING_OVERUSED -> listOf(
                    Triple(
                        "Why did engineering push back on copying the old signup flow?",
                        "They flagged it as $art classic $spoken that'll create headaches down the road.",
                        "Fair enough—glad they spoke up before we built it."
                    ),
                    Triple(
                        "Did the committee's big rebrand actually fix the underlying issue?",
                        "Not really—most folks walked away calling it $art $spoken.",
                        "Oof, what a waste of three months."
                    )
                )
                SemanticIntent.PRAISE_POSITIVE, SemanticIntent.DISCOVER_SOLVE, SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "What came out of yesterday's customer interviews?",
                        "We came away with $art real $spoken that clarified our whole roadmap.",
                        "Nice! Drop that into Monday's deck before leadership sees it."
                    ),
                    Triple(
                        "Did the new neighborhood market live up to the hype?",
                        "Totally—finding fresh sourdough right around the corner is $art real $spoken.",
                        "Okay, I'm definitely walking over there Saturday."
                    )
                )
                SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "Did you catch the ending of that interview clip?",
                        "Yeah—his final line was $art total $spoken, and nobody knew what to say next.",
                        "Haha, send me the clip—I have to see that."
                    )
                )
                SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "How did the budget review go with the new vendor?",
                        "They threw in $art surprise $spoken right at the end of the quote.",
                        "Good thing you read the whole page before signing."
                    )
                )
                else -> listOf(
                    Triple(
                        "What's the main thing you took away from yesterday's session?",
                        "Honestly, that moment turned into $art real $spoken for the whole group.",
                        "Makes sense—definitely worth keeping in mind."
                    ),
                    Triple(
                        "Wait, how did that side conversation shift the whole plan?",
                        "It introduced $art unexpected $spoken nobody had factored in yet.",
                        "Good catch—better to spot it now than next week."
                    )
                )
            }

            ExpressionGrammar.UNCOUNTABLE_NOUN -> when (intent) {
                SemanticIntent.CRITICISM_NEGATIVE, SemanticIntent.OPPOSE_BLOCK, SemanticIntent.BORING_OVERUSED -> listOf(
                    Triple(
                        "How did the joint planning meeting feel from your side of the table?",
                        "Pretty rough—you could feel the $spoken in the room before anyone even opened their slides.",
                        "Oof, hopefully everyone cools off before the next check-in."
                    ),
                    Triple(
                        "Why did people stop showing up to those Friday town halls?",
                        "Honestly, the $spoken just wore everybody down over time.",
                        "Yeah, that's hard to bounce back from."
                    )
                )
                SemanticIntent.PRAISE_POSITIVE, SemanticIntent.CLEAR_DIRECT, SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "What made you trust their founder so quickly during the interview?",
                        "It was her $spoken—she answered every tough question without dodging a single detail.",
                        "That's rare. Usually people just spin things."
                    ),
                    Triple(
                        "How did your team pull together after such a rocky quarter?",
                        "Honestly, a lot of $spoken—everybody had each other's backs.",
                        "Love hearing that. That kind of culture is hard to build."
                    )
                )
                SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "Wait, why is everyone whispering by the coffee machine?",
                        "Because Sam just walked in with fresh $spoken from last night's company mixer.",
                        "Haha, okay, now I have to hear what happened."
                    )
                )
                SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Are you happy with the compensation package they sent over?",
                        "Mostly—I just want to double-check the $spoken before I sign anything.",
                        "Smart move. Always read the fine print on those."
                    )
                )
                SemanticIntent.WORK_STRATEGY -> listOf(
                    Triple(
                        "What are we focusing on in tomorrow's planning offsite?",
                        "Mostly tightening up our $spoken before next quarter kicks off.",
                        "Good call—that's the piece everyone had questions about."
                    )
                )
                else -> listOf(
                    Triple(
                        "What stood out most to you after talking with the whole team?",
                        "Honestly, the sheer level of $spoken—you don't see that every day.",
                        "Makes sense. Sounds like it left a real impression."
                    )
                )
            }
        }
    }

    /**
     * Each tuple is: (domainLabel, leadLine, Pair(replyTemplateWith%s, surfaceFormToInsert)).
     * Strictly distinct in setting & wording from lesson dialogues!
     */
    private fun pickTransferTemplates(
        category: PhraseCategory,
        grammar: ExpressionGrammar,
        intent: SemanticIntent,
        polarity: Polarity,
        clean: String,
        spoken: String,
        spokenThird: String,
        usesSecondPerson: Boolean
    ): List<Triple<String, String, Pair<String, String>>> {
        val lower = clean.lowercase(Locale.US)
        val art = articleFor(spoken)
        val artThird = articleFor(spokenThird)

        return when (grammar) {
            ExpressionGrammar.STANDALONE_REACTION -> when {
                lower in negativeReactionTerms || polarity == Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Group Chat",
                        "Wait—did they really cancel the train five minutes before departure?",
                        "%s—guess we're splitting a cab to the airport." to spoken
                    ),
                    Triple(
                        "Lunch Break",
                        "They want to schedule a mandatory two-hour brainstorm at 5 p.m. Friday.",
                        "%s—I'm already packed for the weekend." to spoken
                    )
                )
                lower in neutralReactionTerms -> listOf(
                    Triple(
                        "Coffee Run",
                        "I'm going to try the new commute route for a week before judging it.",
                        "%s—you'll know by Friday if it saves time." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Group Chat",
                        "Are we still meeting at the coffee shop on Main at nine?",
                        "%s—I'm grabbing my keys right now." to spoken
                    ),
                    Triple(
                        "Post-Game Walk",
                        "Tell me that wasn't the wildest overtime finish all season.",
                        "%s—I still can't believe that last shot went in." to spoken
                    )
                )
            }

            ExpressionGrammar.PAST_VERB_PHRASE -> if (polarity == Polarity.NEGATIVE) {
                listOf(
                    Triple(
                        "Post-Meeting Walk",
                        "How did the surprise pop quiz go in your seminar?",
                        "Honestly, I completely %s on the last two essays." to spokenThird
                    )
                )
            } else {
                listOf(
                    Triple(
                        "Brunch Catchup",
                        "How did Devon's keynote go at the conference?",
                        "Seriously, he walked up without notes and completely %s." to spokenThird
                    ),
                    Triple(
                        "Coffee Run",
                        "Did your cousin's band survive the outdoor festival rain?",
                        "Better than survived—they kept playing and honestly %s." to spokenThird
                    )
                )
            }

            ExpressionGrammar.TRANSITIVE_PERSON_VERB -> when (intent) {
                SemanticIntent.OPPOSE_BLOCK, SemanticIntent.CRITICISM_NEGATIVE -> listOf(
                    Triple(
                        "Weekend Brunch",
                        "Did the recruiter ever follow up after your final round?",
                        "Not a single email—they really decided to %s me after three interviews." to spoken
                    ),
                    Triple(
                        "Evening Walk",
                        "Why did Carlos leave the trivia team mid-season?",
                        "A couple folks kept trying to %s him every time he missed a question." to spoken
                    )
                )
                SemanticIntent.SUPPORT_COLLABORATE, SemanticIntent.PRAISE_POSITIVE -> listOf(
                    Triple(
                        "Office Kitchen",
                        "How's the new analyst settling in before her first client readout?",
                        "Great—I grabbed five minutes before lunch to %s her so she feels ready." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Commute Call",
                        "Did you ever get a chance to talk with your old mentor in Boston?",
                        "Yeah, I called to %s her on my drive home and we caught up for twenty minutes." to spoken
                    )
                )
            }

            ExpressionGrammar.TRANSITIVE_THING_VERB -> when (intent) {
                SemanticIntent.REDUCE_LESSEN -> listOf(
                    Triple(
                        "Client Call",
                        "People are still nervous about the billing migration next month.",
                        "Walking them through a test account first should %s their worries." to spoken
                    )
                )
                SemanticIntent.INCREASE_GROW -> listOf(
                    Triple(
                        "Team Retro",
                        "Should we announce half-baked pricing changes before the FAQ is written?",
                        "No way—that'll only %s the confusion in support." to spoken
                    )
                )
                SemanticIntent.OPPOSE_BLOCK, SemanticIntent.CRITICISM_NEGATIVE -> listOf(
                    Triple(
                        "Editorial Desk",
                        "Did the town council approve that late-night construction permit?",
                        "No—residents showed up and pushed the board to %s the plan." to spoken
                    )
                )
                SemanticIntent.DISCOVER_SOLVE -> listOf(
                    Triple(
                        "Product Review",
                        "Any clue why checkout conversion dipped over the weekend?",
                        "We're pulling the logs after lunch to %s the root cause." to spoken
                    )
                )
                SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Road Trip",
                        "Who's covering the cabin booking until everyone sends their split?",
                        "I can %s the deposit today and you guys can reimburse me next week." to spoken
                    )
                )
                SemanticIntent.SECRET_CONFUSING -> listOf(
                    Triple(
                        "Newsroom Edit",
                        "Why was that spokesperson's statement so hard to follow?",
                        "It honestly felt designed to %s what actually happened." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Client Sync",
                        "Should we wait until Monday to deal with that open item?",
                        "Better to %s it this afternoon while everyone's still online." to spoken
                    ),
                    Triple(
                        "Editorial Desk",
                        "This opening section still feels slightly off before the chart.",
                        "Agreed—let's %s the first paragraph before sending it over." to spoken
                    )
                )
            }

            ExpressionGrammar.COMPLETE_VERB_PHRASE -> when {
                usesSecondPerson && polarity == Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Coffee Catchup",
                        "I'm tempted to rush the handoff without testing the backup.",
                        "Don't %s just to save half an hour—it'll cost us tomorrow." to spoken
                    )
                )
                usesSecondPerson -> listOf(
                    Triple(
                        "Evening Walk",
                        "Should I share my exact counteroffer on the very first screening call?",
                        "Hold off—you don't want to %s before they confirm the role level." to spoken
                    )
                )
                intent == SemanticIntent.CRITICISM_NEGATIVE || intent == SemanticIntent.OPPOSE_BLOCK -> listOf(
                    Triple(
                        "Client Debrief",
                        "Why did that third-party agency lose the renewal so quickly?",
                        "They started to %s once they thought nobody was checking the details." to spokenThird
                    )
                )
                intent == SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "Team Standup",
                        "This side question about next quarter's swag budget is eating our whole sync.",
                        "Let's %s for now and finish the launch checklist first." to spokenThird
                    )
                )
                intent == SemanticIntent.PRAISE_POSITIVE || intent == SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "Volunteer Shift",
                        "How did you guys get all two hundred meal boxes packed before noon?",
                        "Everybody showed up ready to %s without waiting for instructions." to spokenThird
                    )
                )
                else -> listOf(
                    Triple(
                        "Client Debrief",
                        "We've been circling around this decision for two weeks now.",
                        "Honestly, it's time to %s so the team can move forward." to spokenThird
                    )
                )
            }

            ExpressionGrammar.INTRANSITIVE_VERB -> when (intent) {
                SemanticIntent.REDUCE_LESSEN -> listOf(
                    Triple(
                        "Commute Call",
                        "Are you still stuck in that downtown concert traffic?",
                        "Yeah, bumper-to-bumper—I'm grabbing a taco until the rush starts to %s." to spoken
                    ),
                    Triple(
                        "Weekend Hike",
                        "Think we should wait out these gusts before heading up the ridge?",
                        "Good idea—the trail report said the wind should %s right around three." to spoken
                    )
                )
                SemanticIntent.INCREASE_GROW -> listOf(
                    Triple(
                        "Neighborhood Walk",
                        "Did those new coffee carts really take over the whole plaza?",
                        "Yeah—new ones seemed to %s as soon as spring hit." to spoken
                    )
                )
                SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "Book Club",
                        "Did everybody get a turn to share their thoughts on the ending?",
                        "Almost—once Greg started to %s, we lost fifteen minutes." to spoken
                    )
                )
                SemanticIntent.OPPOSE_BLOCK, SemanticIntent.CRITICISM_NEGATIVE -> listOf(
                    Triple(
                        "Slack DM",
                        "Did their lead volunteer actually stay to help clean up?",
                        "Nope—he decided to %s the second the event wrapped." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Slack DM",
                        "Are people still stressing over the schedule shift?",
                        "A couple folks vented earlier, though things should %s by Thursday." to spoken
                    )
                )
            }

            ExpressionGrammar.PREPOSITIONAL_STATE -> when {
                usesSecondPerson -> listOf(
                    Triple(
                        "Weekend Text",
                        "Sorry for the late reply—packing for the move plus client deadlines wiped me out.",
                        "No stress at all, sounds like you're %s this week—let's grab dinner once you settle." to spoken
                    )
                )
                intent == SemanticIntent.CRITICISM_NEGATIVE || intent == SemanticIntent.OPPOSE_BLOCK -> listOf(
                    Triple(
                        "Coffee Shop",
                        "Did you hear what he said to the intern in front of the whole team?",
                        "Yeah, that remark was completely %s—somebody really needed to step in." to spokenThird
                    )
                )
                intent == SemanticIntent.PRAISE_POSITIVE || intent == SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "Dinner Table",
                        "Did the local repair shop charge extra for staying past closing to finish your brakes?",
                        "Not a dime—they went %s so I could still make my weekend trip." to spokenThird
                    )
                )
                intent == SemanticIntent.CLEAR_DIRECT -> listOf(
                    Triple(
                        "Client Review",
                        "Did legal find any red flags in the new vendor contract?",
                        "None at all—everything looked completely %s." to spokenThird
                    )
                )
                intent == SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "Slack DM",
                        "Can we get Priya's sign-off on the design spec before lunch?",
                        "She's %s until tomorrow morning, so let's leave comments in the doc." to spokenThird
                    )
                )
                intent == SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Airport Gate",
                        "Did work give you a travel card for the last-minute flight change?",
                        "No, I covered it %s and I'm submitting the expense report tonight." to spokenThird
                    )
                )
                else -> listOf(
                    Triple(
                        "Podcast Chat",
                        "Are you switching to the new neighborhood gym or keeping your old membership?",
                        "Still %s—both places have real pros and cons." to spokenThird
                    )
                )
            }

            ExpressionGrammar.ADJECTIVE -> when (intent) {
                SemanticIntent.PRAISE_POSITIVE, SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "Weekend Brunch",
                        "Was that new acoustic set worth the drive across town?",
                        "One hundred percent—the arrangement and vocals were genuinely %s." to spoken
                    )
                )
                SemanticIntent.CRITICISM_NEGATIVE, SemanticIntent.OPPOSE_BLOCK -> listOf(
                    Triple(
                        "Book Club",
                        "What did you think of the columnist's response to the city plan?",
                        "Honestly, his tone turned unexpectedly %s in the second half." to spoken
                    )
                )
                SemanticIntent.BORING_OVERUSED -> listOf(
                    Triple(
                        "Movie Night",
                        "Should we stream that thriller everyone was hyping last month?",
                        "Honestly, it felt pretty %s—nothing we haven't seen a dozen times." to spoken
                    )
                )
                SemanticIntent.BRIEF_TEMPORARY -> listOf(
                    Triple(
                        "Coffee Shop",
                        "Think that sudden drop in wait times will last through the holiday rush?",
                        "Probably not—calm stretches like this are usually pretty %s." to spoken
                    )
                )
                SemanticIntent.TIME_SCHEDULE -> listOf(
                    Triple(
                        "Client Sync",
                        "Can design squeeze in an extra landing page before Thursday's launch?",
                        "We're pretty %s right now, so let's slate that for next Tuesday." to spoken
                    )
                )
                SemanticIntent.MONEY_VALUE -> listOf(
                    Triple(
                        "Dinner Table",
                        "Did you end up signing up for that boutique fitness studio?",
                        "No—the monthly fee felt way too %s for how often I'd actually go." to spoken
                    )
                )
                SemanticIntent.SECRET_CONFUSING -> listOf(
                    Triple(
                        "Study Group",
                        "Did the new policy FAQ clarify who qualifies for the rebate?",
                        "Not really—the phrasing is still weirdly %s." to spoken
                    )
                )
                SemanticIntent.CLEAR_DIRECT -> listOf(
                    Triple(
                        "Team Retro",
                        "Were the client's expectations obvious after yesterday's call?",
                        "Totally—their feedback was super %s, zero guesswork." to spoken
                    )
                )
                SemanticIntent.EXCESSIVE_INTENSE -> listOf(
                    Triple(
                        "Post-Event Walk",
                        "How was the launch party downtown?",
                        "Fun, though the giant ice sculpture and confetti cannons felt a little %s." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Book Club",
                        "What did you think of the memoir's final three chapters?",
                        "I loved the opening, though the pacing felt surprisingly %s near the end." to spoken
                    )
                )
            }

            ExpressionGrammar.DETERMINER_NOUN -> when (polarity) {
                Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Dinner Banter",
                        "How did a simple flight rebooking take four hours?",
                        "Between the weather delay and the gate swap, it turned into %s." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Lunch Break",
                        "Why is every department suddenly asking for a seat on our pilot?",
                        "Because once a project starts paying off, everybody wants %s." to spoken
                    )
                )
            }

            ExpressionGrammar.DEFINITE_NOUN -> when (polarity) {
                Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Dinner Banter",
                        "How did splitting the cabin rooms end up going?",
                        "We arrived last, so we definitely got the %s right next to the noisy stairs." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Team Retro",
                        "What's the single biggest factor in the rollout timeline?",
                        "That checkout approval is sitting right on the %s." to spoken
                    )
                )
            }

            ExpressionGrammar.PERSON_NOUN -> when (polarity) {
                Polarity.NEGATIVE -> listOf(
                    Triple(
                        "Coffee Run",
                        "Why did nobody want to partner with Derek on the group presentation?",
                        "Because he acts like $art total %s whenever managers are watching." to spoken
                    )
                )
                Polarity.POSITIVE -> listOf(
                    Triple(
                        "Weekend Brunch",
                        "Wait, your sister still drove everyone to the trailhead after working late?",
                        "Seriously, she's $art total %s—never lets the group down." to spoken
                    )
                )
                Polarity.NEUTRAL -> listOf(
                    Triple(
                        "Book Club",
                        "How did the biography describe the museum's founder?",
                        "Mostly as $art %s who preferred working outside the usual crowd." to spoken
                    )
                )
            }

            ExpressionGrammar.THING_NOUN -> when (intent) {
                SemanticIntent.CRITICISM_NEGATIVE, SemanticIntent.OPPOSE_BLOCK, SemanticIntent.BORING_OVERUSED -> listOf(
                    Triple(
                        "Newsroom Edit",
                        "Why did you cut that entire paragraph from the feature draft?",
                        "Our editor flagged it as $art classic %s that weakened the piece." to spoken
                    )
                )
                SemanticIntent.PRAISE_POSITIVE, SemanticIntent.DISCOVER_SOLVE, SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "Product Review",
                        "Did anything jump out from last week's user testing sessions?",
                        "Yeah, we uncovered $art huge %s right on the onboarding flow." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Coffee Catchup",
                        "What changed your mind halfway through the negotiation?",
                        "They brought up $art unexpected %s that shifted the whole conversation." to spoken
                    )
                )
            }

            ExpressionGrammar.UNCOUNTABLE_NOUN -> when (intent) {
                SemanticIntent.CRITICISM_NEGATIVE, SemanticIntent.OPPOSE_BLOCK, SemanticIntent.BORING_OVERUSED -> listOf(
                    Triple(
                        "Post-Meeting Walk",
                        "Why did that Q&A wrap up fifteen minutes early?",
                        "Honestly, the %s in the room made it impossible to have a real discussion." to spoken
                    )
                )
                SemanticIntent.PRAISE_POSITIVE, SemanticIntent.CLEAR_DIRECT, SemanticIntent.SUPPORT_COLLABORATE -> listOf(
                    Triple(
                        "Post-Meeting Walk",
                        "How did the budget conversation go with the new VP?",
                        "Honestly, I appreciated her %s—she laid out the real constraints without sugarcoating." to spoken
                    )
                )
                SemanticIntent.COMMUNICATION_SPEECH -> listOf(
                    Triple(
                        "Lunch Break",
                        "Why did everyone crowd around Maya's desk after the town hall?",
                        "She came back with fresh %s about next month's leadership shuffle." to spoken
                    )
                )
                else -> listOf(
                    Triple(
                        "Executive Sync",
                        "What's the main focus before next quarter's partner pitch?",
                        "Dialing in our %s so buyers immediately see what sets us apart." to spoken
                    )
                )
            }
        }
    }

    private val metaLanguagePatterns = listOf(
        "to sum up that feeling",
        "to sum up",
        "i'd call it",
        "i would call it",
        "would call it",
        "calling that",
        "natural expression",
        "sounds super natural",
        "sounds natural",
        "native speakers",
        "stiff textbook",
        "captures that",
        "captures the",
        "sums up",
        "nails that",
        "nails the",
        "conveying ",
        "in one word",
        "in one phrase",
        "in a casual tone",
        "in a formal tone",
        "in a professional tone",
        "in a conversational tone",
        "use ________",
        "drop in ________",
        "swap that clunky"
    )

    fun containsMetaLanguage(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase(Locale.US)
        return metaLanguagePatterns.any { lower.contains(it) }
    }
}
