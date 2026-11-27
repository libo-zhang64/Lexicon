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

    @Test
    fun bespokeClozeMaskingSucceedsOnFlagshipAndAlmostAll8ColTsvEntries() {
        // 1. All 40 flagship entries must mask cleanly in their own dialogue
        val flagshipFailures = mutableListOf<String>()
        CuratedCatalog.entries.forEach { entry ->
            val masked = entry.dialogue.any { turn ->
                com.example.domain.CuratedQuizGenerator.maskTermInLine(entry.term, turn.line).contains("________")
            }
            if (!masked) flagshipFailures.add(entry.term)
        }
        assertTrue("Flagship entries failed cloze masking: $flagshipFailures", flagshipFailures.isEmpty())

        // 2. Scan all 5 TSV files for 8-column bespoke entries
        val catalogFiles = listOf("slang.tsv", "idioms.tsv", "phrasal.tsv", "workplace.tsv", "gre.tsv")
        var totalBespoke = 0
        var maskedCount = 0
        val failures = mutableListOf<String>()
        catalogFiles.forEach { fileName ->
            val file = java.io.File("src/main/assets/catalog/$fileName")
            assertTrue("Expected $fileName to exist", file.exists())
            file.readLines().forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val parts = line.split("|")
                if (parts.size >= 8 && parts[6].trim().isNotBlank()) {
                    totalBespoke++
                    val term = parts[0].trim()
                    val example = parts[6].trim()
                    val result = com.example.domain.CuratedQuizGenerator.maskTermInLine(term, example)
                    if (result.contains("________")) {
                        maskedCount++
                    } else {
                        failures.add("$fileName: $term -> $example")
                    }
                }
            }
        }
        assertTrue("Expected at least 1,200 bespoke entries, found $totalBespoke", totalBespoke >= 1200)
        val passRate = maskedCount.toDouble() / totalBespoke.toDouble()
        assertTrue(
            "Expected >=98% bespoke cloze masking rate, got ${(passRate * 100).toInt()}% ($maskedCount/$totalBespoke). Sample failures: ${failures.take(10)}",
            passRate >= 0.98
        )
    }

    @Test
    fun semanticIntentRoutingMatchesExactWordSenseAndPolarity() {
        val synth = com.example.data.catalog.NaturalDialogueSynthesizer
        val classifier = com.example.data.catalog.SemanticIntentClassifier

        // Polysemous slang words must get their exact slang grammar & sense
        assertEquals(
            com.example.data.catalog.NaturalDialogueSynthesizer.ExpressionGrammar.THING_NOUN,
            synth.classifyGrammar(PhraseCategory.SLANG, "cap", "Modern slang meaning a lie, exaggeration, or false boast")
        )
        assertEquals(
            com.example.data.catalog.NaturalDialogueSynthesizer.ExpressionGrammar.ADJECTIVE,
            synth.classifyGrammar(PhraseCategory.SLANG, "fire", "Exceptionally good, stylish, or exciting.")
        )
        assertEquals(
            com.example.data.catalog.NaturalDialogueSynthesizer.ExpressionGrammar.UNCOUNTABLE_NOUN,
            synth.classifyGrammar(PhraseCategory.SLANG, "tea", "Juicy, piping-hot gossip and behind-the-scenes drama")
        )

        // Person vs thing noun separation + negative polarity for sycophant
        assertEquals(
            com.example.data.catalog.NaturalDialogueSynthesizer.ExpressionGrammar.PERSON_NOUN,
            synth.classifyGrammar(PhraseCategory.GRE_VOCAB, "sycophant", "A person who acts obsequiously toward someone important in order to gain advantage")
        )
        val sycophantDialogue = synth.buildNaturalLessonDialogue(
            PhraseCategory.GRE_VOCAB,
            "sycophant",
            "A person who acts obsequiously toward someone important in order to gain advantage",
            hash = 123
        ).joinToString(" ") { it.line }.lowercase()
        assertFalse("Sycophant should never get a drink/corner place scene", sycophantDialogue.contains("corner place"))
        assertFalse("Sycophant should never get a toddler praise scene", sycophantDialogue.contains("toddlers"))

        // Out of pocket (slang rude sense) must route to CRITICISM_NEGATIVE, never positive praise
        assertEquals(
            com.example.data.catalog.SemanticIntentClassifier.SemanticIntent.CRITICISM_NEGATIVE,
            classifier.classifySemanticIntent(PhraseCategory.SLANG, "out of pocket", "Acting wildly inappropriate and rude, or temporarily unavailable/paying personally")
        )
        val oopScene = synth.buildTransferScene(
            PhraseCategory.SLANG,
            "out of pocket",
            "Acting wildly inappropriate and rude, or temporarily unavailable/paying personally",
            seed = 7
        )
        assertFalse("Rude 'out of pocket' must not get a helpful mechanic scene", oopScene.leadLine.lowercase().contains("mechanic"))

        // Directional & domain precision checks
        assertEquals(
            com.example.data.catalog.SemanticIntentClassifier.SemanticIntent.REDUCE_LESSEN,
            classifier.classifySemanticIntent(PhraseCategory.GRE_VOCAB, "abate", "Become less intense, strong, or widespread")
        )
        assertEquals(
            com.example.data.catalog.SemanticIntentClassifier.SemanticIntent.INCREASE_GROW,
            classifier.classifySemanticIntent(PhraseCategory.GRE_VOCAB, "exacerbate", "Make a problem, bad situation, or negative feeling worse")
        )
        assertEquals(
            com.example.data.catalog.SemanticIntentClassifier.SemanticIntent.BORING_OVERUSED,
            classifier.classifySemanticIntent(PhraseCategory.GRE_VOCAB, "hackneyed", "Lacking significance through having been overused; unoriginal and trite")
        )
        assertEquals(
            com.example.data.catalog.SemanticIntentClassifier.SemanticIntent.BRIEF_TEMPORARY,
            classifier.classifySemanticIntent(PhraseCategory.GRE_VOCAB, "ephemeral", "Lasting for a very short time")
        )
        assertEquals(
            com.example.data.catalog.SemanticIntentClassifier.SemanticIntent.TIME_SCHEDULE,
            classifier.classifySemanticIntent(PhraseCategory.WORKPLACE, "booked solid", "Having every hour of one's calendar filled")
        )

        // Cut corners must never get a concert speeding scene
        val cutCornersDialogue = synth.buildNaturalLessonDialogue(
            PhraseCategory.SLANG,
            "cut corners",
            "To do something hastily, cheaply, or carelessly by skipping important steps to save time or money.",
            hash = 0
        ).joinToString(" ") { it.line }.lowercase()
        assertFalse("cut corners must not get concert traffic scene", cutCornersDialogue.contains("concert"))
    }

    @Test
    fun quizDistractorsExcludeTargetSynonyms() {
        val all = CuratedCatalog.entries
        val target = all.first { it.id == "table_a_discussion" } // synonym includes "put a pin in it"
        val syntheticSynonymEntry = target.copy(
            id = "syn_put_a_pin",
            term = "put a pin in it",
            definition = "Pause a side topic temporarily"
        )
        val pool = all + syntheticSynonymEntry
        repeat(20) {
            val distractors = com.example.domain.CuratedQuizGenerator.selectSafeDistractors(target, pool, count = 3)
            assertFalse(
                "Distractors must never contain a synonym of target",
                distractors.any { it.id == "syn_put_a_pin" }
            )
        }
    }
}
