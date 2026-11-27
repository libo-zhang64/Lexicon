package com.example.data.catalog

import android.content.Context
import com.example.data.model.CefrLevel
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

        // 2. Load the 5 category TSV asset files in TWO passes:
        //    Pass 1: ALL bespoke 8-column entries (with hand-written, meaning-accurate examples) first,
        //            so compact 5-column duplicates in earlier files never shadow richer bespoke entries.
        //    Pass 2: Remaining 5-column compact entries.
        val assetMap = listOf(
            "catalog/slang.tsv" to PhraseCategory.SLANG,
            "catalog/idioms.tsv" to PhraseCategory.IDIOM,
            "catalog/phrasal.tsv" to PhraseCategory.PHRASAL_VERB,
            "catalog/workplace.tsv" to PhraseCategory.WORKPLACE,
            "catalog/gre.tsv" to PhraseCategory.GRE_VOCAB
        )

        val assetRows: List<Pair<PhraseCategory, List<String>>> = assetMap.map { (assetPath, category) ->
            val lines = runCatching {
                context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readLines() }
            }.getOrDefault(emptyList())
            category to lines
        }

        fun ingestPass(bespokeOnly: Boolean) {
            for ((category, lines) in assetRows) {
                for ((idx, rawLine) in lines.withIndex()) {
                    val line = rawLine.trim()
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val parts = line.split("|")
                    if (parts.size < 5) continue

                    val isBespoke = parts.size >= 8 && parts[6].trim().isNotBlank()
                    if (bespokeOnly != isBespoke) continue

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
                        exampleLine = explicitExample
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
        }

        ingestPass(bespokeOnly = true)
        ingestPass(bespokeOnly = false)

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
            PhraseCategory.SLANG -> "Used in ${register.label.lowercase(Locale.US)} American speech to mean: $defLower."
            PhraseCategory.IDIOM -> "American figurative expression (${register.label.lowercase(Locale.US)}) meaning: $defLower."
            PhraseCategory.PHRASAL_VERB -> "Common ${register.label.lowercase(Locale.US)} multi-word verb meaning: $defLower."
            PhraseCategory.WORKPLACE -> "US workplace phrasing (${register.label.lowercase(Locale.US)}) signaling: $defLower."
            PhraseCategory.GRE_VOCAB -> "Precise GRE vocabulary (${register.label.lowercase(Locale.US)}) expressing: $defLower."
        }
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

        val hash = abs(term.hashCode())
        val dialogue = if (hasCustomExample && exampleLine.isNotBlank()) {
            NaturalDialogueSynthesizer.wrapBespokeExampleInDialogue(
                category = category,
                exampleLine = exampleLine,
                hash = hash,
                definition = definition
            )
        } else {
            NaturalDialogueSynthesizer.buildNaturalLessonDialogue(
                category = category,
                term = term,
                definition = definition,
                hash = hash,
                synonyms = synTerms
            )
        }

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
            PhraseRegister.CONVERSATIONAL -> "Common across everyday American speech, podcasts, and friendly workplace chat."
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
