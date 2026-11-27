package com.example.data.catalog

import com.example.data.model.DialogueTurn
import com.example.data.model.PhraseCategory
import java.util.Locale
import kotlin.math.abs

/**
 * Synthesizes authentic, grammatically natural spoken American English conversations
 * across all 5 vocabulary categories (Slang, Idioms, Phrasal Verbs, Workplace, GRE).
 *
 * Strictly avoids meta-linguistic filler (never quotes the target expression as "a phrase",
 * never says "sounds super natural", "I'd call it X", "native speakers say", or "in context").
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
        COUNTABLE_NOUN,
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

    private val objectRequiringTailPreps = setOf(
        "to", "for", "with", "from", "of", "at", "after", "against", "about", "into", "upon",
        "onto", "by", "over", "through", "on"
    )

    private val definiteNounStarters = setOf(
        "short", "long", "best", "worst", "upper", "lion's", "critical", "bottom", "front",
        "wrong", "right", "flip", "other", "last", "first", "final", "whole", "elephant",
        "tip", "rest", "bulk", "heart", "root", "core", "top", "end", "benefit", "thick"
    )

    fun cleanTerm(term: String): String =
        term.replace(Regex("\\([^)]*\\)"), "").trim()

    fun articleFor(word: String): String {
        val lower = word.trim().lowercase(Locale.US)
        if (lower.startsWith("uni") || lower.startsWith("use") || lower.startsWith("eu") ||
            lower.startsWith("one") || lower.startsWith("honest").not() && lower.startsWith("ho")
        ) {
            // Handle common exceptions ("unique", "useful", "one-off")
            if (lower.startsWith("uni") || lower.startsWith("use") || lower.startsWith("eu") || lower.startsWith("one")) {
                return "a"
            }
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
            defLower.startsWith("exclamation") ||
            defLower.startsWith("interjection") ||
            defLower.startsWith("a quick reply") ||
            defLower.startsWith("said when") ||
            defLower.startsWith("used to express agreement")
        ) {
            return ExpressionGrammar.STANDALONE_REACTION
        }

        // Past-tense completed slang/idioms ("ate that", "nailed it", "crushed it")
        if (firstWord in setOf("ate", "nailed", "crushed", "killed", "slayed", "bombed", "cooked", "dodged", "dropped") &&
            words.size >= 2
        ) {
            return ExpressionGrammar.PAST_VERB_PHRASE
        }

        // Determiner-led nouns ("a piece of the pie", "the extra mile")
        if (firstWord in setOf("a", "an", "the")) {
            return ExpressionGrammar.DETERMINER_NOUN
        }

        // Category-tuned rules
        if (category == PhraseCategory.PHRASAL_VERB) {
            val isIntransitive = defLower.startsWith("to become") ||
                defLower.startsWith("to move") ||
                defLower.startsWith("to walk") ||
                defLower.startsWith("to stay") ||
                defLower.startsWith("to subside") ||
                defLower.startsWith("to behave") ||
                defLower.startsWith("to malfunction") ||
                defLower.startsWith("to wait") ||
                defLower.startsWith("to leave") ||
                defLower.startsWith("to arrive") ||
                defLower.startsWith("to happen") ||
                defLower.startsWith("to appear") ||
                defLower.startsWith("to vanish") ||
                defLower.startsWith("to fail") ||
                defLower.startsWith("to stop") ||
                defLower.startsWith("to collapse") ||
                defLower.startsWith("to sleep") ||
                defLower.startsWith("to relax") ||
                defLower.startsWith("to gather") ||
                defLower.startsWith("to agree") ||
                defLower.startsWith("to argue") ||
                defLower.startsWith("to speak") ||
                defLower.startsWith("to laugh") ||
                defLower.startsWith("to cry") ||
                defLower.startsWith("to wake") ||
                defLower.startsWith("to hurry") ||
                defLower.startsWith("to slow") ||
                defLower.startsWith("to fall") ||
                defLower.startsWith("to rise") ||
                defLower.startsWith("to run") ||
                defLower.startsWith("to sit") ||
                defLower.startsWith("to stand") ||
                defLower.startsWith("to hang") ||
                defLower.startsWith("to die") ||
                defLower.startsWith("to live") ||
                defLower.startsWith("to work") ||
                defLower.startsWith("to grow") ||
                defLower.startsWith("to calm") ||
                defLower.startsWith("to settle") ||
                defLower.startsWith("to fade") ||
                defLower.startsWith("to break down") ||
                defLower.startsWith("to show up") ||
                lastWord in setOf("together", "apart", "ahead", "along", "away", "around")

            val targetsPerson = defLower.contains("someone") ||
                defLower.contains("a friend") ||
                defLower.contains("a person") ||
                defLower.contains("people") ||
                defLower.contains("colleague") ||
                defLower.contains("partner")

            return when {
                isIntransitive && lastWord !in setOf("to", "for", "with", "from", "of", "at", "after", "into") ->
                    ExpressionGrammar.INTRANSITIVE_VERB
                targetsPerson -> ExpressionGrammar.TRANSITIVE_PERSON_VERB
                else -> ExpressionGrammar.TRANSITIVE_THING_VERB
            }
        }

        if (category == PhraseCategory.GRE_VOCAB) {
            return classifyGre(lower, defLower)
        }

        // Prepositional / negated predicate states ("above and beyond", "out of pocket", "up to your neck", "not in Kansas anymore")
        if (firstWord in prepStarters || firstWord == "not" || firstWord == "no" ||
            lower.contains(" and ") && (firstWord in setOf("sick", "tired", "fed", "safe", "high", "down", "far", "fast", "loud", "dead", "alive", "free", "fair", "short", "sweet", "rough", "ready"))
        ) {
            return ExpressionGrammar.PREPOSITIONAL_STATE
        }

        // Verb phrases in Idioms / Workplace / Slang
        if (firstWord in commonVerbStarters) {
            val alreadyHasObject = words.size >= 2 && (
                lastWord in setOf("it", "them", "him", "her", "us", "yourself", "myself", "hand", "sword", "ice", "bait", "bullet", "chase", "base", "curve", "line", "ball", "hat", "water", "fire", "air", "wind", "ship", "deck", "table", "floor", "wall", "book", "card", "cards", "hatchet", "towel", "beans", "cat", "bag", "boat", "bridge", "bridges", "bucket", "cake", "candle", "ends", "corner", "corners", "cord", "day", "deal", "dirt", "dust", "edge", "eye", "face", "feet", "foot", "gap", "gas", "gear", "gold", "ground", "gun", "hair", "head", "heat", "hell", "hook", "horn", "horse", "house", "jaw", "knee", "knot", "leg", "lid", "light", "loop", "mark", "mind", "money", "moon", "mouth", "music", "nail", "neck", "nerve", "news", "night", "nose", "nut", "ocean", "oil", "pace", "page", "pain", "pan", "paper", "part", "party", "path", "peace", "penny", "pie", "pill", "pin", "pipe", "pitch", "place", "plan", "plate", "plug", "point", "pot", "plug", "pulse", "punch", "question", "rain", "record", "red", "reins", "ring", "road", "rock", "roof", "room", "rope", "round", "row", "rug", "rule", "salt", "sand", "scene", "score", "scratch", "screw", "sea", "seat", "seed", "sense", "shape", "sheet", "shell", "shirt", "shoe", "shoes", "shot", "shoulder", "show", "side", "sight", "sign", "skin", "sky", "slack", "slate", "sleeve", "smoke", "snow", "sock", "socks", "song", "soul", "soup", "spade", "spark", "speed", "spell", "spin", "spirit", "spot", "stage", "stake", "stakes", "stand", "star", "stars", "steam", "steel", "step", "stick", "stone", "storm", "story", "straw", "streak", "street", "stride", "strike", "string", "strings", "strip", "stuff", "suit", "sun", "surface", "sweat", "switch", "tab", "tail", "tape", "target", "taste", "tea", "team", "teeth", "tempo", "test", "thick", "thin", "thing", "things", "thread", "throat", "thumb", "thunder", "ticket", "tide", "time", "tin", "tip", "title", "toe", "toes", "toll", "tone", "tongue", "tooth", "top", "torch", "touch", "town", "track", "tracks", "trade", "trail", "train", "trap", "trash", "tree", "trend", "trick", "trigger", "trip", "trouble", "truck", "trump", "truth", "tube", "tune", "turf", "turn", "twist", "two", "umbrella", "up", "value", "veil", "vein", "view", "voice", "void", "vote", "wage", "wagon", "waist", "wake", "walk", "war", "wash", "watch", "wave", "way", "weather", "web", "wedge", "weed", "weeds", "week", "weight", "well", "west", "wheel", "wheels", "whim", "whip", "whirl", "whistle", "white", "whole", "wick", "wife", "wild", "will", "win", "wing", "wings", "wink", "wire", "wish", "wit", "wits", "wolf", "wood", "woods", "wool", "word", "words", "work", "world", "worm", "worms", "worth", "wound", "wrap", "wraps", "wreck", "wrench", "wrist", "wrong", "yard", "year", "years", "yellow", "yes", "yield", "yoke", "zero", "zone") ||
                    words.any { it in setOf("a", "an", "the", "your", "my", "our", "his", "her", "their", "one's") }
                )
            return when {
                alreadyHasObject -> ExpressionGrammar.COMPLETE_VERB_PHRASE
                lastWord in objectRequiringTailPreps && (defLower.contains("someone") || defLower.contains("person") || defLower.contains("them")) ->
                    ExpressionGrammar.TRANSITIVE_PERSON_VERB
                lastWord in objectRequiringTailPreps || defLower.contains("something") || defLower.contains("a ") || defLower.contains("the ") ->
                    ExpressionGrammar.TRANSITIVE_THING_VERB
                else -> ExpressionGrammar.INTRANSITIVE_VERB
            }
        }

        // Nouns vs Adjectives based on definition
        if (defLower.startsWith("a ") || defLower.startsWith("an ") || defLower.startsWith("someone ") || defLower.startsWith("one who ")) {
            return if (firstWord in definiteNounStarters) {
                ExpressionGrammar.DEFINITE_NOUN
            } else {
                ExpressionGrammar.COUNTABLE_NOUN
            }
        }
        if (defLower.startsWith("the ") || defLower.startsWith("having ") || defLower.startsWith("earning ") ||
            defLower.startsWith("treating ") || defLower.startsWith("discussing ") || defLower.startsWith("holding ") ||
            defLower.startsWith("how a ") || defLower.startsWith("rapid, ") || defLower.startsWith("formal ") ||
            defLower.startsWith("coffee, ")
        ) {
            return when {
                firstWord in definiteNounStarters -> ExpressionGrammar.DEFINITE_NOUN
                defLower.startsWith("having ") || defLower.startsWith("feeling ") || defLower.startsWith("acting ") ||
                    defLower.startsWith("utterly ") || defLower.startsWith("deeply ") || defLower.startsWith("available ") ||
                    defLower.startsWith("suddenly ") || defLower.startsWith("pumped ") || defLower.startsWith("hostile") ||
                    defLower.startsWith("conducted ") || defLower.startsWith("repeating ") -> ExpressionGrammar.ADJECTIVE
                else -> ExpressionGrammar.UNCOUNTABLE_NOUN
            }
        }

        // Adjective cues
        if (defLower.startsWith("extremely ") || defLower.startsWith("very ") || defLower.startsWith("overly ") ||
            defLower.startsWith("unusually ") || defLower.startsWith("highly ") || defLower.startsWith("noticeably ") ||
            defLower.startsWith("fiercely ") || defLower.startsWith("wildly ") || defLower.startsWith("utterly ") ||
            defLower.startsWith("openly ") || defLower.startsWith("emotionally ") || defLower.startsWith("physically ") ||
            defLower.startsWith("socially ") || defLower.startsWith("financially ") ||
            lower.endsWith("ed") || lower.endsWith("ous") || lower.endsWith("ive") ||
            lower.endsWith("ful") || lower.endsWith("less") || lower.endsWith("able") ||
            lower.endsWith("ible") || lower.endsWith("ish") || lower.endsWith("ic") ||
            lower.endsWith("al") || lower.endsWith("y") && words.size == 1
        ) {
            return ExpressionGrammar.ADJECTIVE
        }

        // Verb cues from definition
        if (defLower.startsWith("to ") || defLower.startsWith("perform ") || defLower.startsWith("make ") ||
            defLower.startsWith("cause ") || defLower.startsWith("deciding ") || defLower.startsWith("cast ") ||
            defLower.startsWith("push ") || defLower.startsWith("take ") || defLower.startsWith("exhaust ") ||
            defLower.startsWith("dream ") || defLower.startsWith("reveal ") || defLower.startsWith("abruptly ") ||
            defLower.startsWith("underestimate ") || defLower.startsWith("subject ")
        ) {
            return if (defLower.contains("someone") || defLower.contains("person")) {
                ExpressionGrammar.TRANSITIVE_PERSON_VERB
            } else {
                ExpressionGrammar.COMPLETE_VERB_PHRASE
            }
        }

        return if (words.size >= 2) {
            ExpressionGrammar.UNCOUNTABLE_NOUN
        } else {
            ExpressionGrammar.ADJECTIVE
        }
    }

    private fun classifyGre(lowerTerm: String, lowerDef: String): ExpressionGrammar {
        val nounPrefixes = listOf(
            "a ", "an ", "the ", "one who ", "someone who ", "something ",
            "lack of", "state of", "quality of", "act of", "growth or",
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
            "subside", "fade", "vanish", "hesitate", "fluctuate", "persist"
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
            "overlook ", "avoid ", "escape ", "abandon ", "surrender ", "seize ",
            "take ", "give ", "offer ", "grant ", "deny ", "refuse ", "require ",
            "demand ", "force ", "compel ", "allow ", "permit ", "forbid ", "ban "
        )

        if (lowerDef.startsWith("a ") || lowerDef.startsWith("an ") || lowerDef.startsWith("one who ")) {
            return ExpressionGrammar.COUNTABLE_NOUN
        }
        if (intransitiveStarts.any { lowerDef.startsWith(it) }) {
            return ExpressionGrammar.INTRANSITIVE_VERB
        }
        if (transitiveStarts.any { lowerDef.startsWith(it) }) {
            return ExpressionGrammar.TRANSITIVE_THING_VERB
        }
        if (lowerDef.startsWith("the ") || nounPrefixes.any { lowerDef.startsWith(it) }) {
            return ExpressionGrammar.UNCOUNTABLE_NOUN
        }
        return when {
            lowerTerm.endsWith("tion") || lowerTerm.endsWith("sion") ||
                lowerTerm.endsWith("ment") || lowerTerm.endsWith("ity") ||
                lowerTerm.endsWith("ness") || lowerTerm.endsWith("ism") ||
                lowerTerm.endsWith("ance") || lowerTerm.endsWith("ence") ||
                lowerTerm.endsWith("acy") || lowerTerm.endsWith("itude") -> ExpressionGrammar.UNCOUNTABLE_NOUN
            lowerTerm.endsWith("ist") || (lowerTerm.endsWith("ant") && lowerDef.contains("person")) ->
                ExpressionGrammar.COUNTABLE_NOUN
            lowerTerm.endsWith("ate") && lowerDef.startsWith("make") -> ExpressionGrammar.TRANSITIVE_THING_VERB
            else -> ExpressionGrammar.ADJECTIVE
        }
    }

    /**
     * Wraps an existing hand-written bespoke example sentence in a natural 3-turn exchange
     * with ZERO meta-linguistic commentary about wording, framing, or textbooks.
     */
    fun wrapBespokeExampleInDialogue(
        category: PhraseCategory,
        exampleLine: String,
        hash: Int
    ): List<DialogueTurn> {
        val speakers = if (category == PhraseCategory.WORKPLACE) workplaceSpeakers else everydaySpeakers
        val (speakerA, speakerB) = speakers[hash % speakers.size]

        val openers = when (category) {
            PhraseCategory.WORKPLACE -> listOf(
                "How did the check-in with leadership go this morning?",
                "Wait—any update after you talked to the client team?",
                "Before we wrap up standup, where did we land on that?",
                "How are we looking for Thursday's launch review?",
                "Did finance get back to you on the Q4 numbers yet?"
            )
            PhraseCategory.SLANG -> listOf(
                "Wait, tell me everything—how did last night go?",
                "Be honest, what actually happened after I left?",
                "Hold on, did you end up going or staying in?",
                "Okay, spill—what's the update?",
                "No way—how did that turn out?"
            )
            PhraseCategory.IDIOM -> listOf(
                "How are things shaking out with the new schedule?",
                "Wait, what happened after you talked it over with them?",
                "Sounds like a wild week—how's it looking now?",
                "So what's your plan before Monday?",
                "Did everything finally settle down over there?"
            )
            PhraseCategory.PHRASAL_VERB -> listOf(
                "Hey, did you get a chance to deal with that yet?",
                "How did the rest of your afternoon end up going?",
                "Wait—what happened after you called them back?",
                "Are you still heading over there after work?",
                "Any luck sorting that out before the weekend?"
            )
            PhraseCategory.GRE_VOCAB -> listOf(
                "Wait, what did you mean earlier when you brought that up over lunch?",
                "Seriously? Fill me in on how that actually went down.",
                "How did everyone react once that came out? I missed the whole thing.",
                "You sounded pretty fired up about that earlier—what happened?",
                "Did that really happen? Give me the short version before our next meeting."
            )
        }

        val reactions = when (category) {
            PhraseCategory.WORKPLACE -> listOf(
                "Makes sense. Let's circle back after lunch once everyone's seen the doc.",
                "Good call—I'll send a quick heads-up to design so they aren't surprised.",
                "Got it. That definitely saves us a headache before Friday.",
                "Fair enough. Ping me if you need another pair of eyes on it.",
                "Totally with you—let's lock that in before noon."
            )
            PhraseCategory.SLANG -> listOf(
                "Haha, no way—I would've reacted the exact same way.",
                "Honestly, good for you. You needed a win this week.",
                "Wait, seriously? Okay, tell me the rest over coffee.",
                "Oof, I felt that. At least it's over now.",
                "Fair enough—count me in next time."
            )
            PhraseCategory.IDIOM -> listOf(
                "Honestly, I can't blame you at all.",
                "Yeah, that makes a ton of sense now that I hear the whole story.",
                "Good call—definitely let me know how it plays out next week.",
                "Whoa, I didn't realize it had gotten that intense.",
                "Right? Sometimes you just have to trust your gut on those."
            )
            PhraseCategory.PHRASAL_VERB -> listOf(
                "Sounds like a solid plan—text me once you're done.",
                "Whew, glad that didn't drag into the weekend.",
                "Yeah, good call handling it now instead of waiting.",
                "Let me know if you want a hand with that later.",
                "Nice—glad it all worked out in the end."
            )
            PhraseCategory.GRE_VOCAB -> listOf(
                "Honestly, that explains so much—I would've had the exact same reaction.",
                "Whoa, no wonder people were still talking about it this morning.",
                "Haha, yeah, I can totally picture that happening.",
                "Good point. Definitely glad you gave me the heads-up.",
                "Wait, really? Keep me posted on what they decide."
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
     * The target word/phrase is spoken directly inside natural American grammar—never quoted or lectured about.
     */
    fun buildNaturalLessonDialogue(
        category: PhraseCategory,
        term: String,
        definition: String,
        hash: Int
    ): List<DialogueTurn> {
        val clean = cleanTerm(term)
        val grammar = classifyGrammar(category, clean, definition)
        val speakers = if (category == PhraseCategory.WORKPLACE) workplaceSpeakers else everydaySpeakers
        val (speakerA, speakerB) = speakers[hash % speakers.size]
        val usesSecondPerson = clean.lowercase(Locale.US).let { it.contains("your") || it.contains("yourself") }

        val vignettes: List<Triple<String, String, String>> = when (grammar) {
            ExpressionGrammar.STANDALONE_REACTION -> {
                val cap = clean.replaceFirstChar { it.uppercase(Locale.US) }
                listOf(
                    Triple(
                        "Want to grab tacos by the park once we wrap up at five?",
                        "$cap—just text me when you're heading out the door.",
                        "Awesome, see you at the corner in a bit."
                    ),
                    Triple(
                        "I'm telling you, that new ramen place on 4th is worth the twenty-minute wait.",
                        "$cap? Alright, let's check it out Friday night.",
                        "Deal—I'll put our names down early so we don't starve."
                    ),
                    Triple(
                        "Wait, did you actually finish the whole deck before lunch?",
                        "$cap—had to skip my morning walk, though.",
                        "Respect. Go grab yourself a real break."
                    )
                )
            }

            ExpressionGrammar.PAST_VERB_PHRASE -> listOf(
                Triple(
                    "Did you catch Maya's set at the open mic last night?",
                    "Yeah, we sat right up front—she honestly $clean and had the whole room cheering.",
                    "Ugh, so mad I missed it! Tell me someone got a video."
                ),
                Triple(
                    "How did your presentation go with the regional directors?",
                    "Way better than I expected—Jordan stepped in on the Q&A and totally $clean.",
                    "Nice! You guys earned an early Friday after that."
                ),
                Triple(
                    "Wait, how was your sister's audition this morning?",
                    "She just texted me from the train—she $clean and already got a callback.",
                    "No way! Tell her congrats for me."
                )
            )

            ExpressionGrammar.TRANSITIVE_PERSON_VERB -> listOf(
                Triple(
                    "Have you heard from Alex since they switched teams last month?",
                    "Yeah, I ran into Sam at coffee and made sure to $clean him before the meeting.",
                    "Good call—tell him I said hi next time you catch up."
                ),
                Triple(
                    "Think our new neighbor is settling in okay?",
                    "Seems like it! It took a couple tries to $clean them, but we chatted for twenty minutes on the porch.",
                    "Love that—always nice actually knowing the people on your block."
                ),
                Triple(
                    "How did you handle that awkward mix-up with the vendor?",
                    "I jumped on a quick call to $clean her directly instead of letting emails pile up.",
                    "Smart move. A five-minute call beats ten emails every time."
                )
            )

            ExpressionGrammar.TRANSITIVE_THING_VERB -> if (category == PhraseCategory.WORKPLACE) {
                listOf(
                    Triple(
                        "Are we ready to share the roadmap with engineering tomorrow?",
                        "Almost—I still need to $clean the final timeline before we send out the invite.",
                        "Got it. Ping me when it's ready and I'll give it a quick read."
                    ),
                    Triple(
                        "Did legal raise any flags on the partner agreement?",
                        "Just one small section—we're meeting at two to $clean the wording before sign-off.",
                        "Sounds clean. Glad we caught it ahead of the deadline."
                    ),
                    Triple(
                        "How are we handling the extra requests from the pilot accounts?",
                        "Let's $clean the backlog first so nobody gets stretched too thin this sprint.",
                        "Totally agree—better to ship three things cleanly than rush six."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "Any chance we can wrap this up before dinner?",
                        "Yeah, give me twenty minutes to $clean the last few things and I'm ready to head out.",
                        "Take your time—I'll grab our coats and call the ride."
                    ),
                    Triple(
                        "Why did everyone get so quiet when Alex brought up the old policy?",
                        "Because nobody expected leadership to $clean it right in the middle of the Q&A.",
                        "Oof, yeah, that definitely shifted the energy in the room."
                    ),
                    Triple(
                        "Did the new documentary actually change your mind on the case?",
                        "Kind of! The third episode digs up interviews that really $clean the official story.",
                        "Okay, now I have to watch it tonight—don't spoil the ending."
                    ),
                    Triple(
                        "How did your conversation with the landlord go after the repair delay?",
                        "Honestly, they tried to $clean the issue at first, so I sent over the photos and dates.",
                        "Good for you for keeping receipts—that usually gets things moving fast."
                    )
                )
            }

            ExpressionGrammar.COMPLETE_VERB_PHRASE -> if (usesSecondPerson) {
                listOf(
                    Triple(
                        "I feel awful about the typo in yesterday's client email—should I offer to step off the account?",
                        "Whoa, slow down—you really don't need to $clean over one tiny mistake.",
                        "Fair point. I'll just send a quick correction and move on."
                    ),
                    Triple(
                        "Should we tell them our exact budget ceiling right at the start of the call?",
                        "Wait a bit—you don't want to $clean before hearing what they're actually offering.",
                        "Good call. Let's listen first and see where they land."
                    ),
                    Triple(
                        "I've been staying up till 1 a.m. and waking up at 6 to finish this remodel.",
                        "You can't keep going like that—if you $clean all month, you're going to wipe out.",
                        "Yeah, you're right. I'm taking Sunday completely off."
                    )
                )
            } else if (category == PhraseCategory.WORKPLACE) {
                listOf(
                    Triple(
                        "This side question about next year's pricing is eating up our whole standup.",
                        "Good catch—let's $clean for now and pick it back up in Thursday's sync.",
                        "Sounds good. That keeps us on track for the demo."
                    ),
                    Triple(
                        "We've spent three sprints on that experiment and adoption still hasn't budged.",
                        "Honestly, it might be time to $clean and put those engineers back on onboarding.",
                        "Tough call, but the numbers make it pretty clear."
                    ),
                    Triple(
                        "How are we making sure leadership doesn't get blindsided in Friday's review?",
                        "I set up quick fifteen-minute chats tomorrow so we can $clean ahead of time.",
                        "Smart—way better than debating everything live in front of twenty people."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "We've got ten minutes before the concert starts and traffic just cleared up.",
                        "Alright, buckle up—I'm gonna $clean so we don't miss the opening song.",
                        "Haha, just don't get us pulled over!"
                    ),
                    Triple(
                        "Think we should wait until Monday to bring up the schedule conflict?",
                        "Nah, better to $clean right now before everyone books their flights.",
                        "True—ripping off the bandage early saves everyone a headache."
                    ),
                    Triple(
                        "How'd you handle that awkward silence at the start of the workshop?",
                        "I cracked a quick joke about the broken coffee machine to $clean, and everyone relaxed.",
                        "Classic move—nothing bonds a room faster than bad office coffee."
                    )
                )
            }

            ExpressionGrammar.INTRANSITIVE_VERB -> listOf(
                Triple(
                    "Is the rain still coming down sideways? I left my jacket at home.",
                    "Yeah, it's wild out there, though the forecast says it should $clean in about twenty minutes.",
                    "Perfect—gives me an excuse to grab one more coffee before walking back."
                ),
                Triple(
                    "Wait, did that whole online argument already blow over?",
                    "Pretty much—these things spike for two days and then $clean as soon as the next headline drops.",
                    "So true. Everybody moves on in 48 hours."
                ),
                Triple(
                    "Think the team will push back on moving the launch to Tuesday?",
                    "They grumbled a bit at standup, but everyone will probably $clean once they realize it saves their weekend.",
                    "Totally—nobody wants to push code on a Friday evening anyway."
                ),
                Triple(
                    "How's your brother doing since moving out to Chicago?",
                    "The first month was a whirlwind, though things are starting to $clean now that he found his routine.",
                    "Love hearing that—winter's gonna be the real test, though!"
                )
            )

            ExpressionGrammar.PREPOSITIONAL_STATE -> if (usesSecondPerson) {
                listOf(
                    Triple(
                        "Sorry I missed your text—between taxes and the apartment move, this week's been chaos.",
                        "No worries at all, sounds like you're $clean right now—let's catch up next week.",
                        "Appreciate it! Coffee's on me once the boxes are unpacked."
                    ),
                    Triple(
                        "Do you think I should volunteer to organize the neighborhood block party too?",
                        "Honestly, you're already $clean with the new job—let somebody else run that one.",
                        "Yeah, you're probably right. I need to stop saying yes to everything."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "How did the catering team handle the last-minute guest count jump?",
                        "They honestly went $clean—brought extra trays and didn't even charge a rush fee.",
                        "Whoa, save their number. We're definitely hiring them again."
                    ),
                    Triple(
                        "Wait, why'd you leave the group chat so suddenly last night?",
                        "Because by midnight I was $clean with everyone arguing over a restaurant pick.",
                        "Haha, fair—thirty messages just to end up at the same pizza spot."
                    ),
                    Triple(
                        "Are you leaning toward taking the Austin offer or staying put?",
                        "Still $clean, honestly—both have big pros and I need the weekend to sleep on it.",
                        "Take your time. Happy to talk through the math over lunch tomorrow."
                    ),
                    Triple(
                        "How's the new apartment hunting going in that neighborhood?",
                        "Pretty wild—everything good goes fast, so we have to stay $clean just to book a tour.",
                        "Ugh, moving season is brutal. Let me know if you spot anything near the train."
                    )
                )
            }

            ExpressionGrammar.ADJECTIVE -> if (category == PhraseCategory.WORKPLACE) {
                listOf(
                    Triple(
                        "Can your team squeeze in the analytics refresh before Friday?",
                        "We're a little $clean this sprint, so the earliest we can pick it up is next Wednesday.",
                        "Totally fair—I'll update the milestone tracker so everyone's aligned."
                    ),
                    Triple(
                        "Did procurement sign off on the new agency contract?",
                        "Yep, everything in their audit came back $clean, so we're good to kick off Monday.",
                        "Awesome—I'll send over the onboarding brief this afternoon."
                    ),
                    Triple(
                        "Got five minutes to walk through the slide deck before 2 p.m.?",
                        "I'm $clean until four, but let's grab ten minutes right after my client call.",
                        "Works for me—I'll drop a quick hold on your calendar."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "Did you end up listening to that podcast episode I sent over?",
                        "Yeah, on my commute—the host's take got surprisingly $clean in the second half once they dug into the backstory.",
                        "Right? I wasn't expecting them to go there, but I couldn't stop listening."
                    ),
                    Triple(
                        "How's this month's book club pick? Worth starting before Thursday?",
                        "Honestly, I'm hooked, though the middle chapters get a little $clean if you're reading past midnight.",
                        "Good call—I'll save those chapters for Saturday morning coffee instead."
                    ),
                    Triple(
                        "How did the client check-in go? You looked way more relaxed walking out.",
                        "Way better than I thought. Their VP seemed $clean at the start, so I braced myself, then we clicked on the timeline.",
                        "Nice! Nothing beats turning around a tough room like that."
                    ),
                    Triple(
                        "Did you catch the clips from last night's neighborhood town hall?",
                        "Yeah, over breakfast—the whole Q&A got weirdly $clean the second the zoning map went up.",
                        "Classic local politics. At least they finally agreed on the bike lane."
                    ),
                    Triple(
                        "Wait, how was your flight back from Denver?",
                        "Totally fine until the gate change—then the whole boarding line went $clean for twenty minutes.",
                        "Ugh, airport chaos is the worst. Glad you made it home okay."
                    )
                )
            }

            ExpressionGrammar.DETERMINER_NOUN -> listOf(
                Triple(
                    "Why did so many extra teams suddenly ask to join our pilot launch?",
                    "Because once something gets traction, everybody wants $clean.",
                    "Haha, true—nobody volunteered when it was just a messy spreadsheet."
                ),
                Triple(
                    "Are you turning down the startup equity offer to keep your current role?",
                    "Honestly, yeah—I'm sticking with $clean while the market's this unpredictable.",
                    "Makes total sense. Stability's underrated right now."
                ),
                Triple(
                    "Wait, how did a simple lunch order turn into an hour-long debate?",
                    "No idea, man—one comment about delivery fees and suddenly it was $clean.",
                    "Classic office lunch. Next time we're just walking to the deli."
                )
            )

            ExpressionGrammar.DEFINITE_NOUN -> listOf(
                Triple(
                    "How did the shift swap end up working out for you?",
                    "Not great—I somehow got the $clean and ended up closing both Friday and Saturday.",
                    "Oof, they definitely owe you next weekend off."
                ),
                Triple(
                    "What's holding up the apartment lease renewal?",
                    "Parking spots have been the $clean since day one—nobody wants to budge.",
                    "Hope they meet you halfway so you don't have to move."
                ),
                Triple(
                    "Are we still on schedule for the beta release?",
                    "Only if design signs off today—that checkout screen is right on the $clean.",
                    "Got it. I'll check in with Priya right after lunch."
                )
            )

            ExpressionGrammar.COUNTABLE_NOUN -> {
                val art = articleFor(clean)
                if (category == PhraseCategory.WORKPLACE) {
                    listOf(
                        Triple(
                            "What came out of yesterday's customer interviews?",
                            "We pulled $art solid $clean from the checkout recordings—way clearer than the survey data.",
                            "Nice! Drop that into Monday's deck before leadership sees it."
                        ),
                        Triple(
                            "Why did engineering push back on copying the old signup flow?",
                            "They flagged it as $art classic $clean that'll slow down every release down the road.",
                            "Fair enough—glad they spoke up before we built it."
                        ),
                        Triple(
                            "What's the next step before we pick a new analytics vendor?",
                            "Procurement's sending out $art formal $clean next week so all three vendors quote the same scope.",
                            "Smart—makes comparing pricing way easier."
                        )
                    )
                } else {
                    listOf(
                        Triple(
                            "Did you see how everyone reacted to Maya's talk at the meetup last night?",
                            "Oh yeah—the whole Q&A turned into $art full-blown $clean, and people were still buzzing in the lobby.",
                            "Wish I hadn't missed it! Let me know if they post the recording."
                        ),
                        Triple(
                            "How did your sister handle the five-hour flight delay with two toddlers?",
                            "She was $art total $clean—kept them laughing the whole time and didn't lose her cool once.",
                            "Honestly, superhuman. Buy her a giant coffee next time you see her."
                        ),
                        Triple(
                            "Still up for hanging out for another half hour before heading home?",
                            "Sure—let's grab $art quick $clean at that corner place and call it a night.",
                            "Sounds perfect. I'll grab us a booth by the window."
                        ),
                        Triple(
                            "Have you read that Sunday profile on the retired neighborhood architect?",
                            "Yeah, twice—her neighbors describe her as $art $clean who somehow got the whole block behind the park redesign.",
                            "Send me the link! That's the exact kind of story I love reading with coffee."
                        )
                    )
                }
            }

            ExpressionGrammar.UNCOUNTABLE_NOUN -> if (category == PhraseCategory.WORKPLACE) {
                listOf(
                    Triple(
                        "What are we focusing on in tomorrow's leadership offsite?",
                        "Mostly sharpening our $clean before Q4 budgets get locked.",
                        "Good call—that's the piece everyone had questions about last quarter."
                    ),
                    Triple(
                        "Did anyone type up a summary from the client workshop?",
                        "Yep, I just shared the $clean in the project channel a minute ago.",
                        "Awesome, thanks for turning that around so fast."
                    ),
                    Triple(
                        "How did the new team boost signups without raising ad spend?",
                        "They leaned hard into $clean instead of buying another round of billboards.",
                        "Impressive—let's ask them to walk us through what worked."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "How did the joint planning meeting feel from your side of the table?",
                        "Honestly, you could feel the $clean in the room the second both teams sat down.",
                        "Oof—hopefully once the details settle, everyone loosens up a bit."
                    ),
                    Triple(
                        "What made you trust their founder so quickly during the interview?",
                        "It was her $clean—she didn't dodge a single tough question about the rough quarters.",
                        "That's rare. Usually people just spin things instead of leveling with you."
                    ),
                    Triple(
                        "I'm surprised how smoothly your parents handled the flight cancellation.",
                        "Same! They took the whole mess with surprising $clean and ended up finding an awesome ramen spot near the hotel.",
                        "Honestly, goals. I would've been stressing at the gate."
                    ),
                    Triple(
                        "Wait, how are you still wide awake after driving six hours straight?",
                        "Pure iced coffee and $clean—plus a really good playlist.",
                        "Haha, fair enough. Get some real sleep tonight, though."
                    )
                )
            }
        }

        val (turn1, turn2, turn3) = vignettes[hash % vignettes.size]
        return listOf(
            DialogueTurn(speakerA, turn1),
            DialogueTurn(speakerB, turn2),
            DialogueTurn(speakerA, turn3)
        )
    }

    /**
     * Generates a BRAND-NEW unseen spoken conversation for True Transfer Learning (distinct from lesson dialogue).
     * Never uses meta-linguistic templates like "I'd call it ___" or "captures that best".
     */
    fun buildTransferScene(
        category: PhraseCategory,
        term: String,
        definition: String,
        seed: Int
    ): TransferScene {
        val clean = cleanTerm(term)
        val grammar = classifyGrammar(category, clean, definition)
        val (speakerA, speakerB) = transferSpeakers[seed % transferSpeakers.size]
        val usesSecondPerson = clean.lowercase(Locale.US).let { it.contains("your") || it.contains("yourself") }

        val blank = "________"

        // Each triple is (domainLabel, leadLine, replyTemplateWithPlaceholder "%s")
        val templates: List<Triple<String, String, String>> = when (grammar) {
            ExpressionGrammar.STANDALONE_REACTION -> listOf(
                Triple(
                    "Group Chat",
                    "Are we still meeting at the coffee shop on Main at nine?",
                    "%s—I'm grabbing my keys right now."
                ),
                Triple(
                    "Post-Game Walk",
                    "Tell me that wasn't the wildest overtime finish all season.",
                    "%s—I still can't believe that last shot went in."
                )
            )

            ExpressionGrammar.PAST_VERB_PHRASE -> listOf(
                Triple(
                    "Brunch Catchup",
                    "How did Devon's keynote go at the conference?",
                    "Seriously, he walked up without notes and completely %s."
                ),
                Triple(
                    "Coffee Run",
                    "Did your cousin's band survive the outdoor festival rain?",
                    "Better than survived—they kept playing and honestly %s."
                )
            )

            ExpressionGrammar.TRANSITIVE_PERSON_VERB -> listOf(
                Triple(
                    "Commute Call",
                    "Did you ever hear back from your old mentor in Boston?",
                    "Yeah, I called to %s her on my drive home and we talked for half an hour."
                ),
                Triple(
                    "Office Kitchen",
                    "How's the new analyst settling in after their first week?",
                    "Pretty well—I stopped by to %s him before lunch and he's already rolling."
                )
            )

            ExpressionGrammar.TRANSITIVE_THING_VERB -> listOf(
                Triple(
                    "Client Call",
                    "Should we wait until Monday to fix the onboarding hiccup?",
                    "Better to %s it this afternoon while the whole team's still online."
                ),
                Triple(
                    "Editorial Desk",
                    "This opening section still feels cluttered before the chart.",
                    "Agreed—let's %s the first two paragraphs before we send it to layout."
                ),
                Triple(
                    "Road Trip",
                    "Wait, did we check the cabin booking before leaving town?",
                    "Yep, I called the host at noon to %s everything so we don't hit surprises."
                )
            )

            ExpressionGrammar.COMPLETE_VERB_PHRASE -> if (usesSecondPerson) {
                listOf(
                    Triple(
                        "Coffee Catchup",
                        "I'm tempted to take all the blame for the shipping delay even though the vendor missed the date.",
                        "Come on, don't %s when it wasn't even your fault."
                    ),
                    Triple(
                        "Evening Walk",
                        "Should I tell the recruiter my exact competing number on the first screen?",
                        "Hold off—you don't want to %s before they even confirm the level."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "Team Standup",
                        "This side thread about next quarter's swag budget is eating our whole sync.",
                        "Let's %s for now and finish the release checklist first."
                    ),
                    Triple(
                        "Airport Gate",
                        "Boarding starts in ten minutes and we're still stuck behind slow walkers.",
                        "Grab your bag—we might actually have to %s to make Group 2."
                    ),
                    Triple(
                        "Client Debrief",
                        "We've poured six weeks into that ad channel with zero lift.",
                        "Honestly, we should just %s and move the budget to referrals."
                    )
                )
            }

            ExpressionGrammar.INTRANSITIVE_VERB -> listOf(
                Triple(
                    "Commute Call",
                    "Are you still stuck in that downtown concert traffic?",
                    "Yeah, bumper-to-bumper—I'm grabbing a taco until the rush starts to %s."
                ),
                Triple(
                    "Weekend Hike",
                    "Think we should wait out these gusts before heading up the ridge?",
                    "Good idea—the trail report said the wind should %s right around three."
                ),
                Triple(
                    "Slack DM",
                    "Are people still stressing over the office seating shuffle?",
                    "A couple folks vented this morning, but it'll %s by Thursday once everyone settles in."
                )
            )

            ExpressionGrammar.PREPOSITIONAL_STATE -> if (usesSecondPerson) {
                listOf(
                    Triple(
                        "Weekend Text",
                        "Sorry for the late reply—packing for the move plus client deadlines wiped me out.",
                        "No stress at all, sounds like you're %s this week—let's grab dinner once you settle."
                    )
                )
            } else {
                listOf(
                    Triple(
                        "Dinner Table",
                        "Did the mechanic charge extra for staying past closing to finish your brakes?",
                        "Not a dime—they went %s so I could still make my road trip."
                    ),
                    Triple(
                        "Podcast Chat",
                        "Are you switching to the new neighborhood gym or keeping your old membership?",
                        "Still %s—the new spot's closer, though the pool at my old place is hard to beat."
                    ),
                    Triple(
                        "Team Retro",
                        "How did support handle that surprise traffic spike on Sunday night?",
                        "Honestly, everyone stayed %s and cleared the queue before Monday morning."
                    )
                )
            }

            ExpressionGrammar.ADJECTIVE -> listOf(
                Triple(
                    "Book Club",
                    "What did you think of the memoir's final three chapters?",
                    "I loved the opening, though the author's tone turned weirdly %s near the end."
                ),
                Triple(
                    "Client Sync",
                    "Can design take on the extra landing page before Thursday's launch?",
                    "We're a bit %s right now, so let's slate that for next Tuesday instead."
                ),
                Triple(
                    "Coffee Shop",
                    "How did your neighbor react when the HOA changed the parking rules?",
                    "Way more %s than anyone expected—they didn't hold back at the meeting."
                )
            )

            ExpressionGrammar.DETERMINER_NOUN -> listOf(
                Triple(
                    "Lunch Break",
                    "Why is every department suddenly asking for a seat on our pilot?",
                    "Because once a project starts paying off, everybody wants %s."
                ),
                Triple(
                    "Phone Call",
                    "So you're keeping the steady consulting retainer instead of chasing that vague startup pitch?",
                    "Yep—I'm going with %s this year."
                )
            )

            ExpressionGrammar.DEFINITE_NOUN -> listOf(
                Triple(
                    "Team Retro",
                    "Why did the mobile build slip by two days?",
                    "That payment screen was sitting right on the %s and blocked QA."
                ),
                Triple(
                    "Dinner Banter",
                    "How did splitting the cabin rooms end up going?",
                    "We arrived last, so we definitely got the %s right next to the loud fridge."
                )
            )

            ExpressionGrammar.COUNTABLE_NOUN -> {
                val art = articleFor(clean)
                listOf(
                    Triple(
                        "Newsroom Edit",
                        "Why did you cut that entire paragraph from the feature draft?",
                        "Our fact-checker flagged it as $art classic %s that distracted from the real story."
                    ),
                    Triple(
                        "Weekend Brunch",
                        "Wait, your dad still drove everyone to the trailhead after twisting his ankle?",
                        "Seriously, he's $art total %s—refused to let us cancel the trip."
                    ),
                    Triple(
                        "Product Review",
                        "Did anything jump out from last week's user testing sessions?",
                        "Yeah, we uncovered $art huge %s right on the first onboarding screen."
                    )
                )
            }

            ExpressionGrammar.UNCOUNTABLE_NOUN -> listOf(
                Triple(
                    "Executive Sync",
                    "What's the main focus before next quarter's partner pitch?",
                    "Dialing in our %s so buyers immediately see what sets us apart."
                ),
                Triple(
                    "Post-Meeting Walk",
                    "How did the budget conversation go with the new VP?",
                    "Honestly, I appreciated her %s—she laid out the real constraints without sugarcoating."
                ),
                Triple(
                    "Coffee Run",
                    "How are you surviving a double shift after barely sleeping?",
                    "Cold brew, good music, and pure %s."
                )
            )
        }

        val (domain, lead, replyTpl) = templates[abs(seed) % templates.size]
        val formattedTerm = if (replyTpl.startsWith("%s")) {
            clean.replaceFirstChar { it.uppercase(Locale.US) }
        } else {
            clean
        }
        return TransferScene(
            domainLabel = domain,
            speakerA = speakerA,
            speakerB = speakerB,
            leadLine = lead,
            replyWithTarget = replyTpl.replace("%s", formattedTerm),
            replyWithBlank = replyTpl.replace("%s", blank)
        )
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
