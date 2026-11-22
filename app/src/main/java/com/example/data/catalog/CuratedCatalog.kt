package com.example.data.catalog

import com.example.data.model.PhraseEntry
import java.util.Calendar
import java.util.Locale

object CuratedCatalog {
    val entries: List<PhraseEntry> by lazy {
        curatedSlangAndIdioms + curatedPhrasalAndWorkplace
    }

    private val byId: Map<String, PhraseEntry> by lazy {
        entries.associateBy { it.id }
    }

    fun findById(id: String): PhraseEntry? = byId[id]

    fun phraseOfTheDay(allEntries: List<PhraseEntry> = entries): PhraseEntry {
        val pool = allEntries.ifEmpty { entries }
        val dayOfYear = Calendar.getInstance(Locale.US).get(Calendar.DAY_OF_YEAR)
        return pool[dayOfYear % pool.size]
    }
}
