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

    @Test
    fun synthesizedDialoguesAndTransferScenesContainZeroFormulaicMetaLanguage() {
        val bannedPhrases = listOf(
            "sounds super natural",
            "i'd call it",
            "would call it",
            "natural expression",
            "native speakers",
            "stiff textbook",
            "hearing it in context",
            "hearing it in a natural sentence",
            "classic phrasing",
            "love that framing",
            "captures that best",
            "sums up the whole situation",
            "classic american idiom",
            "in everyday speech"
        )
        val samples = listOf(
            Triple(PhraseCategory.SLANG, "out of pocket", "Acting wildly inappropriate and rude"),
            Triple(PhraseCategory.SLANG, "ate that", "Executed a performance or move flawlessly"),
            Triple(PhraseCategory.SLANG, "say less", "Quick casual way to say I completely understand"),
            Triple(PhraseCategory.IDIOM, "fall on your sword", "Take full public blame to protect your team"),
            Triple(PhraseCategory.IDIOM, "a piece of the pie", "A lucrative share of profits or opportunity"),
            Triple(PhraseCategory.IDIOM, "above and beyond", "Going noticeably further than expected"),
            Triple(PhraseCategory.PHRASAL_VERB, "thrash out", "To debate tough disagreements until reaching firm terms"),
            Triple(PhraseCategory.PHRASAL_VERB, "confide in", "To entrust private feelings to a trusted friend"),
            Triple(PhraseCategory.PHRASAL_VERB, "blow over", "To subside naturally without lasting damage"),
            Triple(PhraseCategory.WORKPLACE, "put a pin in it", "Pause a side topic temporarily during a meeting"),
            Triple(PhraseCategory.WORKPLACE, "booked solid", "Having every hour of one's calendar filled"),
            Triple(PhraseCategory.WORKPLACE, "critical path", "The longest chain of dependent tasks dictating completion"),
            Triple(PhraseCategory.GRE_VOCAB, "abate", "Become less intense, strong, or widespread"),
            Triple(PhraseCategory.GRE_VOCAB, "acerbic", "Sharp, biting, and forthright in tone or wit")
        )

        samples.forEachIndexed { idx, (category, term, def) ->
            val lessonLines = com.example.data.catalog.NaturalDialogueSynthesizer
                .buildNaturalLessonDialogue(category, term, def, idx + 7)
                .joinToString(" ") { it.line }
                .lowercase()
            val transfer = com.example.data.catalog.NaturalDialogueSynthesizer
                .buildTransferScene(category, term, def, idx + 13)
            val transferLines = "${transfer.leadLine} ${transfer.replyWithTarget} ${transfer.replyWithBlank}".lowercase()

            bannedPhrases.forEach { banned ->
                assertFalse("Lesson dialogue for $term contained banned '$banned': $lessonLines", lessonLines.contains(banned))
                assertFalse("Transfer scene for $term contained banned '$banned': $transferLines", transferLines.contains(banned))
            }
            assertTrue("Lesson dialogue for $term should use the expression directly", lessonLines.contains(term.lowercase()))
            assertTrue("Transfer reply for $term should contain blank", transfer.replyWithBlank.contains("________"))
        }

        // Also assert every question generated by TransferScenarioEngine (CLOZE, REGISTER, MEANING) is free of meta-language
        val all = CuratedCatalog.entries
        val learnedProgress = all.take(9).associate { it.id to PhraseProgressDoc(phraseId = it.id, lessonCompleted = true) }
        val quizQuestions = TransferScenarioEngine.buildTransferQuestionsForLearned(
            allPhrases = all,
            progressMap = learnedProgress,
            categoryFilter = CategoryFilter.ALL,
            questionCount = 9
        )
        assertEquals(9, quizQuestions.size)
        quizQuestions.forEach { q ->
            assertFalse(
                "Transfer question snippet contained meta-language: ${q.contextSnippet}",
                com.example.data.catalog.NaturalDialogueSynthesizer.containsMetaLanguage(q.contextSnippet)
            )
        }
    }
}
