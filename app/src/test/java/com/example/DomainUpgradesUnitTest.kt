package com.example

import com.example.data.catalog.CuratedCatalog
import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseProgressDoc
import com.example.data.model.RecallRating
import com.example.domain.EbbinghausScheduler
import com.example.domain.TransferScenarioEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainUpgradesUnitTest {

    @Test
    fun ebbinghausScheduleAdvancesAndResetsCorrectly() {
        val now = 1_700_000_000_000L
        val enrolled = EbbinghausScheduler.initialEnrollment(now)
        assertEquals(1L, enrolled.stage)
        assertEquals(1L, enrolled.intervalDays)

        val stage1Doc = PhraseProgressDoc(
            phraseId = "p1",
            lessonCompleted = true,
            ebbinghausStage = enrolled.stage,
            intervalDays = enrolled.intervalDays,
            lastReviewAtMillis = enrolled.lastReviewAtMillis,
            nextReviewAtMillis = enrolled.nextReviewAtMillis
        )

        val stage2 = EbbinghausScheduler.computeNextSchedule(stage1Doc, RecallRating.GOOD, now)
        assertEquals(2L, stage2.stage)
        assertEquals(3L, stage2.intervalDays)
        assertEquals(1L, stage2.reviewCount)

        val stage2Doc = stage1Doc.copy(
            ebbinghausStage = stage2.stage,
            intervalDays = stage2.intervalDays,
            lastReviewAtMillis = stage2.lastReviewAtMillis,
            nextReviewAtMillis = stage2.nextReviewAtMillis,
            reviewCount = stage2.reviewCount
        )

        val stage4 = EbbinghausScheduler.computeNextSchedule(stage2Doc, RecallRating.EASY, now)
        assertEquals(4L, stage4.stage)
        assertEquals(14L, stage4.intervalDays)

        val stage4Doc = stage2Doc.copy(
            ebbinghausStage = stage4.stage,
            intervalDays = stage4.intervalDays,
            reviewCount = stage4.reviewCount
        )

        val lapsed = EbbinghausScheduler.computeNextSchedule(stage4Doc, RecallRating.FORGOT, now)
        assertEquals(0L, lapsed.stage)
        assertEquals(1L, lapsed.lapseCount)
        assertEquals("6 hours", lapsed.nextIntervalLabel)
    }

    @Test
    fun ebbinghausBootstrapsLegacyStudiedPhrasesIntoDueQueue() {
        val sample = CuratedCatalog.entries.take(3)
        val now = 1_700_000_000_000L
        val legacyProgress = mapOf(
            sample[0].id to PhraseProgressDoc(
                phraseId = sample[0].id,
                lessonCompleted = true,
                lastReviewAtMillis = now - 2L * EbbinghausScheduler.DAY_MILLIS,
                nextReviewAtMillis = 0L
            )
        )

        val dashboard = EbbinghausScheduler.computeDashboard(sample, legacyProgress, now)
        assertEquals(1, dashboard.totalTrackedCount)
        assertEquals(1, dashboard.dueNowCount)
        assertEquals(sample[0].id, dashboard.dueItems.first().phrase.id)
    }

    @Test
    fun transferScenarioEngineTestsOnlyLearnedPhrasesInFreshContexts() {
        val phrases = CuratedCatalog.entries.take(40)
        val learnedIds = phrases.take(5).map { it.id }.toSet()
        val progress = learnedIds.associateWith { id ->
            PhraseProgressDoc(phraseId = id, lessonCompleted = true)
        }

        // With 0 learned phrases, returns empty (never tests unlearned phrases)
        val emptyQuestions = TransferScenarioEngine.buildTransferQuestionsForLearned(
            allPhrases = phrases,
            progressMap = emptyMap<String, PhraseProgressDoc>(),
            categoryFilter = CategoryFilter.ALL,
            questionCount = 5
        )
        assertTrue(emptyQuestions.isEmpty())

        // With 5 learned phrases, only those learned phrases are tested in fresh contexts
        val transferQuestions = TransferScenarioEngine.buildTransferQuestionsForLearned(
            allPhrases = phrases,
            progressMap = progress,
            categoryFilter = CategoryFilter.ALL,
            questionCount = 5
        )
        assertEquals(5, transferQuestions.size)
        transferQuestions.forEach { q ->
            assertTrue("Expected learned phrase ID, got ${q.phraseId}", q.phraseId in learnedIds)
            assertFalse("Expected non-blank transfer domain label", q.transferDomainLabel.isNullOrBlank())
            val originalLines = phrases.first { it.id == q.phraseId }.dialogue.joinToString(" ") { it.line }
            assertNotEquals("Transfer prompt should differ from original lesson dialogue", originalLines, q.prompt)
        }
    }

    @Test
    fun greAssetCatalogHasSubstantialEntriesAndNoRoboticTemplates() {
        val greFile = java.io.File("src/main/assets/catalog/gre.tsv")
        assertTrue("Expected gre.tsv asset to exist", greFile.exists())
        val rows = greFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
        assertTrue("Expected substantial GRE catalog, found ${rows.size}", rows.size >= 400)
        rows.forEach { row ->
            assertFalse("Found robotic speaker in GRE row", row.contains("Dr. Patel"))
            assertFalse("Found robotic speaker in GRE row", row.contains("Prof. Lin"))
            assertFalse("Found meta-linguistic template in GRE row", row.contains("The editorial chose the word"))
        }
    }

    @Test
    fun ebbinghausReviewQuizTargetsDuePhrasesOnly() {
        val all = CuratedCatalog.entries
        val dueSubset = all.take(4)
        val questions = com.example.domain.CuratedQuizGenerator.buildEbbinghausReviewQuiz(
            reviewPhrases = dueSubset,
            allPhrases = all,
            questionCount = 4
        )
        assertEquals(4, questions.size)
        val dueIds = dueSubset.map { it.id }.toSet()
        questions.forEach { q ->
            assertTrue("Expected due phrase ID in review quiz", q.phraseId in dueIds)
        }
    }
}
