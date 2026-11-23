package com.example.data.catalog

import android.content.Context
import com.example.data.model.CefrLevel
import com.example.data.model.DialogueTurn
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseRegister
import com.example.data.model.PhraseSource
import com.example.data.model.SynonymNuance
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

object CuratedCatalog {
    private val flagshipEntries: List<PhraseEntry> by lazy {
        curatedSlangAndIdioms + curatedPhrasalAndWorkplace
    }

    @Volatile
    private var loadedEntries: List<PhraseEntry>? = null

    @Volatile
    private var loadedById: Map<String, PhraseEntry>? = null

    val entries: List<PhraseEntry>
        get() = loadedEntries ?: flagshipEntries

    val isExpandedLoaded: Boolean
        get() = loadedEntries != null

    fun ensureLoaded(context: Context): List<PhraseEntry> {
        loadedEntries?.let { return it }
        synchronized(this) {
            loadedEntries?.let { return it }
            val combined = buildExpandedCatalog(context.applicationContext)
            loadedEntries = combined
            loadedById = combined.associateBy { it.id }
            return combined
        }
    }

    fun findById(id: String): PhraseEntry? {
        loadedById?.get(id)?.let { return it }
        return flagshipEntries.firstOrNull { it.id == id }
    }

    fun phraseOfTheDay(allEntries: List<PhraseEntry> = entries): PhraseEntry {
        val pool = allEntries.ifEmpty { entries }
        val dayOfYear = Calendar.getInstance(Locale.US).get(Calendar.DAY_OF_YEAR)
        return pool[dayOfYear % pool.size]
    }

    private fun buildExpandedCatalog(context: Context): List<PhraseEntry> {
        val result = ArrayList<PhraseEntry>(12200)
        val seenIds = HashSet<String>(13000)
        val seenTerms = HashSet<String>(13000)

        // Exclude Daily Spotlight headwords so Word-for-Today remains an exclusive bonus discovery!
        val excludedTerms = HashSet<String>().apply {
            DailySpotlightCatalog.entries.forEach { add(normalizeKey(it.term)) }
        }

        // 1. Always preserve the 40 richly hand-crafted flagship entries first
        for (entry in flagshipEntries) {
            result.add(entry)
            seenIds.add(entry.id)
            seenTerms.add(normalizeKey(entry.term))
        }

        // 2. Load the 5 category TSV asset files
        val assetMap = listOf(
            "catalog/slang.tsv" to PhraseCategory.SLANG,
            "catalog/idioms.tsv" to PhraseCategory.IDIOM,
            "catalog/phrasal.tsv" to PhraseCategory.PHRASAL_VERB,
            "catalog/workplace.tsv" to PhraseCategory.WORKPLACE,
            "catalog/gre.tsv" to PhraseCategory.GRE_VOCAB
        )

        for ((assetPath, category) in assetMap) {
            val lines = runCatching {
                context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readLines() }
            }.getOrDefault(emptyList())

            for ((idx, rawLine) in lines.withIndex()) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val parts = line.split("|")
                if (parts.size < 5) continue

                val term = parts[0].trim()
                val normalized = normalizeKey(term)
                if (term.isEmpty() || normalized in excludedTerms || normalized in seenTerms) continue

                val ipa: String
                val register: PhraseRegister
                val cefr: CefrLevel
                val definition: String
                val nuance: String
                val exampleLine: String
                val synAntRaw: String
                val hasCustomExample: Boolean

                if (parts.size >= 8) {
                    ipa = parts[1].trim()
                    register = parseRegister(parts[2].trim())
                    cefr = CefrLevel.fromRaw(parts[3].trim())
                    definition = parts[4].trim()
                    nuance = parts[5].trim()
                    exampleLine = parts[6].trim()
                    synAntRaw = parts[7].trim()
                    hasCustomExample = exampleLine.isNotBlank()
                } else {
                    ipa = ""
                    register = parseRegister(parts[1].trim())
                    cefr = CefrLevel.fromRaw(parts[2].trim())
                    definition = parts[3].trim()
                    synAntRaw = parts[4].trim()
                    nuance = buildCompactNuance(category, register, definition)
                    val explicitExample = if (parts.size >= 6) parts[5].trim() else ""
                    hasCustomExample = explicitExample.isNotBlank()
                    exampleLine = if (hasCustomExample) {
                        explicitExample
                    } else {
                        buildCompactExampleLine(category, term, definition, abs(term.hashCode()))
                    }
                }

                val id = makeStableId(category, term, idx, seenIds)
                seenIds.add(id)
                seenTerms.add(normalized)

                val entry = synthesizePhraseEntry(
                    id = id,
                    term = term,
                    ipa = ipa,
                    category = category,
                    register = register,
                    cefr = cefr,
                    definition = definition,
                    nuance = nuance,
                    exampleLine = exampleLine,
                    hasCustomExample = hasCustomExample,
                    synAntRaw = synAntRaw
                )
                result.add(entry)
            }
        }

        return result
    }

    private fun parseRegister(raw: String): PhraseRegister = when (raw.uppercase(Locale.US)) {
        "C", "CASUAL" -> PhraseRegister.CASUAL
        "N", "CONVERSATIONAL" -> PhraseRegister.CONVERSATIONAL
        "P", "PROFESSIONAL" -> PhraseRegister.PROFESSIONAL
        "F", "FORMAL" -> PhraseRegister.FORMAL
        else -> PhraseRegister.fromRaw(raw)
    }

    private fun buildCompactNuance(
        category: PhraseCategory,
        register: PhraseRegister,
        definition: String
    ): String {
        val defLower = definition.trim().trimEnd('.').replaceFirstChar { it.lowercase(Locale.US) }
        return when (category) {
            PhraseCategory.SLANG -> "Used in ${register.label.lowercase(Locale.US)} American banter to express: $defLower."
            PhraseCategory.IDIOM -> "Classic American figurative expression (${register.label.lowercase(Locale.US)}) meaning to $defLower."
            PhraseCategory.PHRASAL_VERB -> "Natural multi-word verb in ${register.label.lowercase(Locale.US)} American speech for: $defLower."
            PhraseCategory.WORKPLACE -> "High-precision US workplace phrasing (${register.label.lowercase(Locale.US)}) signaling: $defLower."
            PhraseCategory.GRE_VOCAB -> "Precise GRE vocabulary (${register.label.lowercase(Locale.US)}) expressing: $defLower."
        }
    }

    private enum class GrePartOfSpeech {
        ADJECTIVE,
        TRANSITIVE_VERB,
        INTRANSITIVE_VERB,
        COUNTABLE_NOUN,
        UNCOUNTABLE_NOUN
    }

    private fun inferGrePartOfSpeech(term: String, definition: String): GrePartOfSpeech {
        val lowerTerm = term.trim().lowercase(Locale.US)
        val trimmedDef = definition.trim()
        val lowerDef = trimmedDef.lowercase(Locale.US)

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
            return GrePartOfSpeech.COUNTABLE_NOUN
        }
        if (intransitiveStarts.any { lowerDef.startsWith(it) }) {
            return GrePartOfSpeech.INTRANSITIVE_VERB
        }
        if (transitiveStarts.any { lowerDef.startsWith(it) }) {
            return GrePartOfSpeech.TRANSITIVE_VERB
        }
        if (lowerDef.startsWith("the ") || nounPrefixes.any { lowerDef.startsWith(it) }) {
            return GrePartOfSpeech.UNCOUNTABLE_NOUN
        }

        // Morphological fallback when definition starts with a participle or descriptor
        return when {
            lowerTerm.endsWith("tion") || lowerTerm.endsWith("sion") ||
                lowerTerm.endsWith("ment") || lowerTerm.endsWith("ity") ||
                lowerTerm.endsWith("ness") || lowerTerm.endsWith("ism") ||
                lowerTerm.endsWith("ance") || lowerTerm.endsWith("ence") ||
                lowerTerm.endsWith("acy") || lowerTerm.endsWith("itude") -> GrePartOfSpeech.UNCOUNTABLE_NOUN
            lowerTerm.endsWith("ist") || lowerTerm.endsWith("ant") && lowerDef.contains("person") -> GrePartOfSpeech.COUNTABLE_NOUN
            lowerTerm.endsWith("ate") && lowerDef.startsWith("make") -> GrePartOfSpeech.TRANSITIVE_VERB
            else -> GrePartOfSpeech.ADJECTIVE
        }
    }

    private fun articleFor(word: String): String {
        val first = word.trim().lowercase(Locale.US).firstOrNull() ?: return "a"
        return if (first in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
    }

    private fun buildNaturalGreDialogue(
        term: String,
        definition: String,
        hasCustomExample: Boolean,
        customExample: String,
        hash: Int
    ): List<DialogueTurn> {
        val speakers = listOf(
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
        val (speakerA, speakerB) = speakers[hash % speakers.size]

        // 1. If the GRE entry already has a bespoke natural sentence (lines 1..257 in gre.tsv),
        // wrap it in a genuine, human everyday American exchange instead of a robotic seminar prompt.
        if (hasCustomExample && customExample.isNotBlank()) {
            val naturalOpeners = listOf(
                "Wait, what did you mean earlier when you brought that up over lunch?",
                "Seriously? You've got to fill me in on how that actually went down.",
                "How did everyone react once that came out? I missed the whole thing.",
                "You sounded pretty fired up about that earlier—what happened?",
                "I was thinking about what you mentioned on our walk yesterday.",
                "Did that really happen? Tell me the short version before our next meeting."
            )
            val naturalReactions = listOf(
                "Honestly, that explains so much—I would've had the exact same reaction.",
                "Whoa, no wonder people were still talking about it this morning.",
                "Right? Once you see it play out in real life, it clicks immediately.",
                "Fair enough—that's actually a super clear way to put it.",
                "Haha, yeah, I can totally picture that happening.",
                "Good point. Definitely glad you gave me the heads-up."
            )
            return listOf(
                DialogueTurn(speakerA, naturalOpeners[hash % naturalOpeners.size]),
                DialogueTurn(speakerB, customExample),
                DialogueTurn(speakerA, naturalReactions[(hash / 5) % naturalReactions.size])
            )
        }

        // 2. For compact GRE entries, craft an authentic, POS-accurate 3-turn American conversation
        // where the word is spoken naturally (never quoted as metalanguage!).
        val cleanTerm = term.replace(Regex("\\([^)]*\\)"), "").trim()
        val pos = inferGrePartOfSpeech(cleanTerm, definition)

        val vignettes: List<Triple<String, String, String>> = when (pos) {
            GrePartOfSpeech.ADJECTIVE -> listOf(
                Triple(
                    "Did you end up listening to that podcast episode I sent over?",
                    "Yeah, on my commute—the host's take turned surprisingly $cleanTerm during the second half, especially when they dug into the backstory.",
                    "Right? I wasn't expecting them to go there, but I couldn't stop listening."
                ),
                Triple(
                    "How's this month's book club pick? Worth starting before Thursday?",
                    "Honestly, I'm hooked, though the middle chapters get a little $cleanTerm if you're trying to read past midnight.",
                    "Good call—I'll save those chapters for Saturday morning coffee instead."
                ),
                Triple(
                    "How did the client check-in go? You looked way more relaxed walking out.",
                    "Way better than I thought. Their VP seemed $cleanTerm at the start, so I braced myself, and then we clicked on the timeline.",
                    "Nice! Nothing beats turning around a tough room like that."
                ),
                Triple(
                    "Are you still up for checking out that indie movie tonight?",
                    "I'm down, though my brother warned me the pacing gets pretty $cleanTerm in the final act.",
                    "Fair warning—let's grab dinner first so we're actually awake for it."
                ),
                Triple(
                    "Did you catch the clips from last night's neighborhood town hall?",
                    "Yeah, I watched them over breakfast—the whole Q&A felt weirdly $cleanTerm once the zoning map went up.",
                    "Classic local politics. At least they finally agreed on the bike lane."
                ),
                Triple(
                    "What's your honest read on the updated project plan from leadership?",
                    "Parts of it are solid, though a couple of the assumptions feel $cleanTerm once you look at real customer numbers.",
                    "Totally agree—let's flag that gently before engineering locks the sprint."
                )
            )

            GrePartOfSpeech.TRANSITIVE_VERB -> listOf(
                Triple(
                    "Are you worried the committee is going to push back on our draft?",
                    "A little—someone on the thread already hinted they might $cleanTerm the whole proposal before Friday's vote.",
                    "Let's walk them through the numbers tomorrow morning so there aren't any surprises."
                ),
                Triple(
                    "Wait, why did everyone get so quiet when Alex brought up the old policy?",
                    "Because nobody expected leadership to $cleanTerm it right in the middle of the Q&A.",
                    "Oof, yeah, that definitely shifted the energy in the room."
                ),
                Triple(
                    "Did the new documentary actually change your mind on the case?",
                    "Kind of! The third episode digs up interviews that really $cleanTerm the official timeline.",
                    "Okay, now I have to binge it tonight—don't spoil the ending."
                ),
                Triple(
                    "Any chance we can simplify this before sending it out to subscribers?",
                    "Yeah, I asked Sara to $cleanTerm the intro section so people actually read past paragraph two.",
                    "Smart move—shorter and punchier always lands better."
                ),
                Triple(
                    "How did your conversation with the landlord go after the repair delay?",
                    "Honestly, they tried to $cleanTerm the issue at first, so I sent over the photos and dates.",
                    "Good for you for keeping receipts—that usually gets things moving fast."
                )
            )

            GrePartOfSpeech.INTRANSITIVE_VERB -> listOf(
                Triple(
                    "Is the rain still coming down sideways? I left my jacket at home.",
                    "Yeah, it's wild out there, though the forecast says it should $cleanTerm in about twenty minutes.",
                    "Perfect—gives me an excuse to grab one more coffee before walking back."
                ),
                Triple(
                    "How's your cousin doing since moving out to Chicago for the new job?",
                    "The first month was a whirlwind, but she's starting to $cleanTerm now that she found a great neighborhood spot.",
                    "Love hearing that—winter will be the real test, though!"
                ),
                Triple(
                    "Think the team will push back on switching the launch date?",
                    "They grumbled a bit at standup, but most folks will probably $cleanTerm once they see how much stress it saves.",
                    "Totally—nobody wants to ship on a Friday evening anyway."
                ),
                Triple(
                    "Wait, did that whole online controversy already blow over?",
                    "Pretty much—these things always spike for 48 hours and then $cleanTerm as soon as the next headline drops.",
                    "So true. Everyone's attention span is like three days max."
                )
            )

            GrePartOfSpeech.COUNTABLE_NOUN -> {
                val art = articleFor(cleanTerm)
                listOf(
                    Triple(
                        "Did you see how people reacted to Maya's talk at the meetup last night?",
                        "Oh yeah—calling that moment $art $cleanTerm isn't even an exaggeration; everyone was talking about it afterward.",
                        "Wish I hadn't missed it! Let me know if they post the recording."
                    ),
                    Triple(
                        "What did you think of the restaurant we tried near the waterfront?",
                        "The food was awesome, though the tiny hand-printed menu felt like $art $cleanTerm in the best possible way.",
                        "Haha, I know exactly what you mean—super memorable spot."
                    ),
                    Triple(
                        "How did the panel discussion wrap up after I had to duck out early?",
                        "Right near the end, one audience member launched into $art $cleanTerm that caught the moderator totally off guard.",
                        "Whoa, really? Sounds like I left right before the fireworks."
                    ),
                    Triple(
                        "Have you read the profile piece on that retired architect yet?",
                        "Yeah, twice—her neighbors describe her as $art $cleanTerm who somehow got the whole block behind the park redesign.",
                        "Send me the link! That's the exact kind of story I love reading on Sundays."
                    )
                )
            }

            GrePartOfSpeech.UNCOUNTABLE_NOUN -> listOf(
                Triple(
                    "How did the joint planning meeting feel from your side of the table?",
                    "Honestly, you could feel the $cleanTerm in the room the second both teams sat down.",
                    "Oof—hopefully once the details settle, everyone loosens up a bit."
                ),
                Triple(
                    "What made you trust their founder so quickly during the interview?",
                    "It was her $cleanTerm—she didn't dodge a single tough question about the rough quarters.",
                    "That's rare. Usually people just spin things instead of leveling with you."
                ),
                Triple(
                    "I'm surprised how smoothly your parents handled the flight cancellation.",
                    "Same! They handled the whole mess with surprising $cleanTerm and ended up finding an awesome ramen place near the hotel.",
                    "Honestly, goals. I would've been stressing at the gate."
                ),
                Triple(
                    "Why do you think that old neighborhood theater finally had to close?",
                    "Mostly sheer $cleanTerm after streaming took over—people loved the idea of it, just didn't buy tickets often enough.",
                    "That's such a bummer. Still have great memories from going there in college."
                )
            )
        }

        val (turn1, turn2, turn3) = vignettes[hash % vignettes.size]
        return listOf(
            DialogueTurn(speakerA, turn1),
            DialogueTurn(speakerB, turn2),
            DialogueTurn(speakerA, turn3)
        )
    }

    private fun buildCompactExampleLine(
        category: PhraseCategory,
        term: String,
        definition: String,
        hash: Int
    ): String {
        val cleanTerm = term.replace(Regex("\\([^)]*\\)"), "").trim()
        val defSnippet = definition.trim().trimEnd('.').replaceFirstChar { it.lowercase(Locale.US) }
        val templates = when (category) {
            PhraseCategory.SLANG -> listOf(
                "Honestly, everyone in our group chat keeps saying \"$cleanTerm\" whenever they mean $defSnippet.",
                "In casual American conversation, you'll hear people drop \"$cleanTerm\" right in the middle of a story.",
                "My roommate used \"$cleanTerm\" at dinner last night—it really captures that exact vibe."
            )
            PhraseCategory.IDIOM -> listOf(
                "You could really use \"$cleanTerm\" here—it sums up $defSnippet in just a few words.",
                "That whole situation reminded me of the classic American idiom \"$cleanTerm\".",
                "In everyday conversation, saying \"$cleanTerm\" lands way more vividly than a literal summary."
            )
            PhraseCategory.PHRASAL_VERB -> listOf(
                "Native speakers naturally say \"$cleanTerm\" in that spot when they mean $defSnippet.",
                "We should definitely \"$cleanTerm\" before Friday so the rest of the plan stays on track.",
                "I heard someone use \"$cleanTerm\" on a podcast today and immediately noticed how smooth it sounded."
            )
            PhraseCategory.WORKPLACE -> listOf(
                "During the executive review, our lead brought up \"$cleanTerm\" to address $defSnippet clearly.",
                "Framing it around \"$cleanTerm\" keeps our client memo sharp, collaborative, and grounded.",
                "You'll hear \"$cleanTerm\" constantly in US cross-functional syncs and quarterly planning."
            )
            PhraseCategory.GRE_VOCAB -> listOf(
                "You could definitely notice how $cleanTerm the whole situation felt once everyone stepped back."
            )
        }
        return templates[hash % templates.size]
    }

    private fun synthesizePhraseEntry(
        id: String,
        term: String,
        ipa: String,
        category: PhraseCategory,
        register: PhraseRegister,
        cefr: CefrLevel,
        definition: String,
        nuance: String,
        exampleLine: String,
        hasCustomExample: Boolean,
        synAntRaw: String
    ): PhraseEntry {
        val hash = abs(term.hashCode())
        val dialogue = if (category == PhraseCategory.GRE_VOCAB) {
            buildNaturalGreDialogue(
                term = term,
                definition = definition,
                hasCustomExample = hasCustomExample,
                customExample = exampleLine,
                hash = hash
            )
        } else {
            val speakerPairs = when (category) {
                PhraseCategory.WORKPLACE -> listOf(
                    "Elena" to "Marcus",
                    "Priya" to "Devon",
                    "Nadia" to "Jordan",
                    "Rachel" to "Carlos",
                    "Hannah" to "Sam"
                )
                PhraseCategory.SLANG -> listOf(
                    "Maya" to "Leo",
                    "Chloe" to "Devon",
                    "Zoe" to "Noah",
                    "Tessa" to "Sam",
                    "Ava" to "Ethan"
                )
                PhraseCategory.IDIOM -> listOf(
                    "Clara" to "Ben",
                    "Sophia" to "Daniel",
                    "Grace" to "Owen",
                    "Lydia" to "Miles",
                    "Nora" to "Jack"
                )
                PhraseCategory.PHRASAL_VERB -> listOf(
                    "Hannah" to "Liam",
                    "Avery" to "Caleb",
                    "Maya" to "Omar",
                    "Claire" to "Ryan",
                    "Olivia" to "Luke"
                )
                PhraseCategory.GRE_VOCAB -> listOf("Maya" to "Jordan")
            }
            val (speakerA, speakerB) = speakerPairs[hash % speakerPairs.size]

            val leadPrompts = when (category) {
                PhraseCategory.WORKPLACE -> listOf(
                    "How should we frame this before tomorrow's stakeholder review?",
                    "Quick check before we send the Q3 update out to the team—what's our read?",
                    "Before we wrap up this sync, any thoughts on how we should handle this?",
                    "How did leadership respond when you walked them through the plan?"
                )
                PhraseCategory.SLANG -> listOf(
                    "Be honest—what did you actually think about how that went?",
                    "Wait, fill me in on what happened this weekend!",
                    "So what's the verdict—are we doing this or switching plans?",
                    "You look like you've got a story to tell—what happened?"
                )
                PhraseCategory.IDIOM -> listOf(
                    "How are things shaping up now that you've had a chance to step back?",
                    "That sounds like quite a week—what's your honest takeaway?",
                    "Looking back on how that unfolded, how do you see it now?",
                    "Any advice before we decide on our next move?"
                )
                PhraseCategory.PHRASAL_VERB -> listOf(
                    "How did everything turn out once you got into the details?",
                    "Any update on how you're planning to handle that this week?",
                    "Wait—what happened after you talked it through with everyone?",
                    "How do you usually approach something like that in everyday speech?"
                )
                PhraseCategory.GRE_VOCAB -> listOf("Wait, fill me in on what happened!")
            }

            val followUps = when (category) {
                PhraseCategory.WORKPLACE -> listOf(
                    "That makes total sense—clear, grounded, and easy for everyone to act on.",
                    "Love that framing. Let's capture that right in our recap notes.",
                    "Agreed. Saying it that way keeps the tone collaborative and direct."
                )
                PhraseCategory.SLANG -> listOf(
                    "Haha, totally—I could not have said it better myself.",
                    "Honestly, that's such a great call. Count me in.",
                    "Real talk, that sums up the whole situation perfectly."
                )
                PhraseCategory.IDIOM -> listOf(
                    "True—sometimes that classic phrasing captures it better than a whole paragraph.",
                    "Well put. That puts the entire situation into much clearer focus.",
                    "Exactly. Hearing it in context makes the nuance click immediately."
                )
                PhraseCategory.PHRASAL_VERB -> listOf(
                    "Got it—hearing it in a natural sentence makes the rhythm feel effortless.",
                    "Good plan. Let me know how it goes once you wrap that up!",
                    "That sounds super natural—way smoother than a stiff textbook verb."
                )
                PhraseCategory.GRE_VOCAB -> listOf("Right? That makes total sense.")
            }

            listOf(
                DialogueTurn(speakerA, leadPrompts[hash % leadPrompts.size]),
                DialogueTurn(speakerB, exampleLine),
                DialogueTurn(speakerA, followUps[(hash / 7) % followUps.size])
            )
        }

        val synAntParts = synAntRaw.split("~")
        val synTerms = synAntParts.getOrNull(0)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val antonyms = synAntParts.getOrNull(1)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val synonyms = synTerms.mapIndexed { idx, s ->
            val contrast = when (idx) {
                0 -> "Closest everyday substitute in ${register.label.lowercase(Locale.US)} American English."
                1 -> "Slightly different rhythm or tone—great for avoiding repetition."
                else -> "Useful related alternative depending on audience and emphasis."
            }
            SynonymNuance(s, contrast)
        }

        val registerNote = when (register) {
            PhraseRegister.CASUAL -> "Best suited for friends, group chats, and relaxed social settings; swap for a neutral synonym in formal presentations."
            PhraseRegister.CONVERSATIONAL -> "Natural across everyday American speech, podcasts, and friendly workplace chat."
            PhraseRegister.PROFESSIONAL -> "Polished and well-calibrated for US meetings, Slack threads, client emails, and leadership updates."
            PhraseRegister.FORMAL -> "Elevated, crisp choice ideal for presentations, essays, journalism, and executive memos."
        }

        val etymology = "$nuance Widely recognized across modern American English (${cefr.label}) for conveying precise tone without sounding stiff."

        val usageNotes = listOf(
            registerNote,
            "Practice shadowing the dialogue at 0.75x and 1.0x speed to lock in natural American stress on \"$term\"."
        )

        return PhraseEntry(
            id = id,
            term = term,
            ipa = ipa,
            category = category,
            register = register,
            cefrLevel = cefr,
            definition = definition,
            nuanceSummary = nuance,
            dialogue = dialogue,
            etymology = etymology,
            synonyms = synonyms,
            antonyms = antonyms,
            usageNotes = usageNotes,
            source = PhraseSource.CURATED
        )
    }

    private fun makeStableId(
        category: PhraseCategory,
        term: String,
        idx: Int,
        existingIds: Set<String>
    ): String {
        val prefix = when (category) {
            PhraseCategory.SLANG -> "slang"
            PhraseCategory.IDIOM -> "idiom"
            PhraseCategory.PHRASAL_VERB -> "phrasal"
            PhraseCategory.WORKPLACE -> "work"
            PhraseCategory.GRE_VOCAB -> "gre"
        }
        val slug = term.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(50)
        val base = "${prefix}_${slug.ifEmpty { "entry_$idx" }}"
        if (base !in existingIds) return base
        return "${base}_$idx"
    }

    private fun normalizeKey(raw: String): String =
        raw.lowercase(Locale.US)
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
