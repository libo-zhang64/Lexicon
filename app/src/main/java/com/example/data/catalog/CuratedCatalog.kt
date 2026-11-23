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
        val result = ArrayList<PhraseEntry>(1024)
        val seenIds = HashSet<String>(1100)
        val seenTerms = HashSet<String>(1100)

        // Exclude Daily Spotlight & Transfer Quiz Bank headwords so they stay strictly distinct!
        val excludedTerms = HashSet<String>().apply {
            DailySpotlightCatalog.entries.forEach { add(normalizeKey(it.term)) }
            TransferQuizBank.questions.forEach { add(normalizeKey(it.phraseTerm)) }
        }

        // 1. Always preserve the 40 richly hand-crafted flagship entries first
        for (entry in flagshipEntries) {
            result.add(entry)
            seenIds.add(entry.id)
            seenTerms.add(normalizeKey(entry.term))
        }

        // 2. Load the 5 category TSV asset files (~250 per track -> ~1,250 entries total)
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
                if (parts.size < 8) continue

                val term = parts[0].trim()
                val normalized = normalizeKey(term)
                if (term.isEmpty() || normalized in excludedTerms || normalized in seenTerms) continue

                val ipa = parts[1].trim()
                val register = PhraseRegister.fromRaw(parts[2].trim())
                val cefr = CefrLevel.fromRaw(parts[3].trim())
                val definition = parts[4].trim()
                val nuance = parts[5].trim()
                val exampleLine = parts[6].trim()
                val synAntRaw = parts[7].trim()

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
                    synAntRaw = synAntRaw
                )
                result.add(entry)
            }
        }

        return result
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
        synAntRaw: String
    ): PhraseEntry {
        val hash = abs(term.hashCode())
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
            PhraseCategory.GRE_VOCAB -> listOf(
                "Dr. Patel" to "Julian",
                "Claire" to "Prof. Lin",
                "Ananya" to "Victor",
                "Elena" to "Nathan",
                "Miriam" to "David"
            )
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
            PhraseCategory.GRE_VOCAB -> listOf(
                "What stood out to you most when you finished reading the report?",
                "How would you summarize the author's main observation for our seminar?",
                "Give me your crispest editorial take on how that unfolded.",
                "How can we express that idea with more precision in the final essay?"
            )
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
            PhraseCategory.GRE_VOCAB -> listOf(
                "Exactly—choosing one precise word elevates the entire argument immediately.",
                "Sharp phrasing. That captures the nuance far better than a generic adjective.",
                "Spot on—pairing that word with concrete evidence makes the point unforgettable."
            )
        }

        val dialogue = listOf(
            DialogueTurn(speakerA, leadPrompts[hash % leadPrompts.size]),
            DialogueTurn(speakerB, exampleLine),
            DialogueTurn(speakerA, followUps[(hash / 7) % followUps.size])
        )

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
