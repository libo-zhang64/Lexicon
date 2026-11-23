package com.example.data.catalog

import com.example.data.model.CategoryFilter
import com.example.data.model.PhraseCategory
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType

/**
 * Unseen transfer-learning quiz challenges featuring authentic B2-C2 American English
 * expressions intentionally NOT included in the learning library, training contextual
 * inference, register calibration, and transfer mastery.
 */
object TransferQuizBank {
    val questions: List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "tr_01",
            phraseId = "transfer_rubber_hits_the_road",
            phraseTerm = "where the rubber meets the road",
            category = PhraseCategory.IDIOM,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Infer the idiom that fits this real-world moment of truth.",
            contextSnippet = "Elena: \"Our slide deck looks great, but next week's live pilot is ________.\"\nDevon: \"Agreed—real customer usage will prove whether the workflow holds up.\"",
            options = listOf(
                "where the rubber meets the road",
                "where the grass is greener",
                "where the chips are stacked",
                "where the ink dries fastest"
            ),
            correctIndex = 0,
            explanation = "'Where the rubber meets the road' (unseen transfer idiom) marks the point where theory or planning faces practical, real-world testing."
        ),
        QuizQuestion(
            id = "tr_02",
            phraseId = "transfer_monday_morning_quarterback",
            phraseTerm = "Monday-morning quarterback",
            category = PhraseCategory.WORKPLACE,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: Your coworker says, 'I don't want to be a Monday-morning quarterback, but we should have launched Tuesday.' What does this mean?",
            contextSnippet = "Tone: Self-aware, diplomatic retrospective comment in a US team postmortem.",
            options = listOf(
                "Someone who criticizes past decisions only after seeing the outcome",
                "A team lead who sets weekly priorities every Monday morning",
                "An executive who reverses a decision without consulting engineering",
                "A colleague who volunteers early before anyone else speaks"
            ),
            correctIndex = 0,
            explanation = "From American football fans critiquing Sunday's game on Monday: a 'Monday-morning quarterback' judges choices with the unfair advantage of hindsight."
        ),
        QuizQuestion(
            id = "tr_03",
            phraseId = "transfer_punt_on",
            phraseTerm = "punt on",
            category = PhraseCategory.PHRASAL_VERB,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Choose the natural US workplace phrasal verb for deferring a tough decision.",
            contextSnippet = "Priya: \"Should we pick our Q4 cloud vendor today?\"\nMarcus: \"We're missing pricing from two bidders—let's ________ that decision until Thursday.\"",
            options = listOf(
                "punt on",
                "bowl over",
                "coast through",
                "turn in"
            ),
            correctIndex = 0,
            explanation = "Borrowed from American football, 'punt (on something)' means deliberately postponing or handing off a decision when conditions aren't ready."
        ),
        QuizQuestion(
            id = "tr_04",
            phraseId = "transfer_spitball",
            phraseTerm = "spitball",
            category = PhraseCategory.SLANG,
            questionType = QuizQuestionType.REGISTER,
            prompt = "Transfer Challenge: You want teammates to share rough, unpolished ideas freely without fear of criticism. Which line sounds most natural in a US brainstorm?",
            contextSnippet = "Setting: Informal creative kickoff with product and design peers.",
            options = listOf(
                "\"Let's just spitball a few rough concepts before we worry about budget.\"",
                "\"Let's rubber-stamp a few rough concepts before we worry about budget.\"",
                "\"Let's stonewall a few rough concepts before we worry about budget.\"",
                "\"Let's whitewash a few rough concepts before we worry about budget.\""
            ),
            correctIndex = 0,
            explanation = "'Spitball' (informal American brainstorming verb) means tossing out rough, tentative ideas purely to spark discussion."
        ),
        QuizQuestion(
            id = "tr_05",
            phraseId = "transfer_brass_tacks",
            phraseTerm = "get down to brass tacks",
            category = PhraseCategory.IDIOM,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Complete this dialogue about moving from small talk to essential facts.",
            contextSnippet = "Sam: \"Great catching up, everyone! Now let's ________—how much runway do we actually have left?\"",
            options = listOf(
                "get down to brass tacks",
                "keep it under our hat",
                "pass the hat around",
                "jump the shark"
            ),
            correctIndex = 0,
            explanation = "'Get down to brass tacks' means focusing directly on the core, practical facts and numbers of a matter."
        ),
        QuizQuestion(
            id = "tr_06",
            phraseId = "transfer_jump_the_shark",
            phraseTerm = "jump the shark",
            category = PhraseCategory.SLANG,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: A critic writes, 'The streaming series jumped the shark in season five.' What are they saying?",
            contextSnippet = "Pop culture & product commentary register.",
            options = listOf(
                "It relied on an absurd gimmick that signaled its creative decline",
                "It took a bold creative risk that won over skeptical viewers",
                "It quietly ended production before resolving its main plot",
                "It unexpectedly went viral among younger audiences"
            ),
            correctIndex = 0,
            explanation = "Named after a famous 1977 'Happy Days' episode where Fonzie water-skied over a shark, 'jump the shark' marks the moment a show or product resorts to desperation gimmicks."
        ),
        QuizQuestion(
            id = "tr_07",
            phraseId = "transfer_hash_it_out",
            phraseTerm = "hash out",
            category = PhraseCategory.PHRASAL_VERB,
            questionType = QuizQuestionType.REGISTER,
            prompt = "Transfer Challenge: Two teams disagree on API ownership. Pick the most constructive US workplace phrasing.",
            contextSnippet = "Goal: Suggest an open, candid 30-minute conversation to resolve the disagreement.",
            options = listOf(
                "\"Let's grab a room this afternoon and hash out the ownership boundaries.\"",
                "\"Let's grab a room this afternoon and lash out at the ownership boundaries.\"",
                "\"Let's grab a room this afternoon and chicken out of the ownership boundaries.\"",
                "\"Let's grab a room this afternoon and fizzle out the ownership boundaries.\""
            ),
            correctIndex = 0,
            explanation = "'Hash out' means talking through messy or disputed details thoroughly until both sides reach a clear agreement."
        ),
        QuizQuestion(
            id = "tr_08",
            phraseId = "transfer_litmus_test",
            phraseTerm = "litmus test",
            category = PhraseCategory.WORKPLACE,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Infer the decisive diagnostic metaphor.",
            contextSnippet = "Director: \"For me, the real ________ for our onboarding redesign is whether a first-time user finishes setup without opening help docs.\"",
            options = listOf(
                "litmus test",
                "acid rain",
                "blank check",
                "red tape"
            ),
            correctIndex = 0,
            explanation = "From chemistry pH paper, a 'litmus test' is a single decisive indicator that quickly reveals success, quality, or true stance."
        ),
        QuizQuestion(
            id = "tr_09",
            phraseId = "transfer_sandbag",
            phraseTerm = "sandbag",
            category = PhraseCategory.WORKPLACE,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: In a US sales forecast meeting, someone asks, 'Are you sandbagging your Q3 numbers?' What are they implying?",
            contextSnippet = "Register: Candid American corporate & competitive sports slang.",
            options = listOf(
                "Intentionally understating your forecast so you can easily beat it later",
                "Padding your forecast with unrealistic deals to impress leadership",
                "Rushing contracts through legal without proper compliance review",
                "Sharing confidential competitor pricing with another team"
            ),
            correctIndex = 0,
            explanation = "'Sandbagging' means deliberately hiding strength or setting artificially low expectations to gain an advantage or guarantee an easy win later."
        ),
        QuizQuestion(
            id = "tr_10",
            phraseId = "transfer_hold_water",
            phraseTerm = "hold water",
            category = PhraseCategory.IDIOM,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Select the idiom that evaluates logical soundness.",
            contextSnippet = "Reviewer: \"Their cost-savings argument sounds tidy on paper, but it doesn't ________ once you factor in customer support hours.\"",
            options = listOf(
                "hold water",
                "tread water",
                "test the waters",
                "muddy the waters"
            ),
            correctIndex = 0,
            explanation = "An argument that 'doesn't hold water' fails under close scrutiny—just like a leaky bucket. Contrast with 'muddy the waters' (confuse) or 'test the waters' (probe carefully)."
        ),
        QuizQuestion(
            id = "tr_11",
            phraseId = "transfer_nickel_and_dime",
            phraseTerm = "nickel-and-dime",
            category = PhraseCategory.SLANG,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: A traveler complains, 'Budget airlines really nickel-and-dime you on every step.' What does this express?",
            contextSnippet = "Everyday American consumer frustration.",
            options = listOf(
                "Charging lots of irritating small extra fees that quietly add up",
                "Refusing to accept cash payments at the boarding counter",
                "Offering tiny promotional discounts to loyal frequent flyers",
                "Negotiating aggressively for lower fuel contracts"
            ),
            correctIndex = 0,
            explanation = "Named after US 5-cent and 10-cent coins, to 'nickel-and-dime' someone means wearing them down with endless petty charges or trivial demands."
        ),
        QuizQuestion(
            id = "tr_12",
            phraseId = "transfer_ferret_out",
            phraseTerm = "ferret out",
            category = PhraseCategory.PHRASAL_VERB,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Choose the C2 phrasal verb for persistently uncovering hidden facts.",
            contextSnippet = "Investigator: \"It took three weeks of log audits, but the security team finally managed to ________ the root vulnerability.\"",
            options = listOf(
                "ferret out",
                "peter out",
                "bow out",
                "branch out"
            ),
            correctIndex = 0,
            explanation = "'Ferret out' means searching persistently until you uncover hidden, buried, or elusive information—like a ferret flushing prey from a burrow."
        ),
        QuizQuestion(
            id = "tr_13",
            phraseId = "transfer_rubber_stamp",
            phraseTerm = "rubber-stamp",
            category = PhraseCategory.WORKPLACE,
            questionType = QuizQuestionType.REGISTER,
            prompt = "Transfer Challenge: You want to warn that the review board should evaluate a proposal critically rather than approving it automatically. Which phrasing is sharpest?",
            contextSnippet = "Setting: Governance or design-review meeting.",
            options = listOf(
                "\"Let's make sure we examine the risks rather than just rubber-stamping the request.\"",
                "\"Let's make sure we examine the risks rather than just earmarking the request.\"",
                "\"Let's make sure we examine the risks rather than just green-lighting the weather.\"",
                "\"Let's make sure we examine the risks rather than just piggybacking the request.\""
            ),
            correctIndex = 0,
            explanation = "To 'rubber-stamp' means granting routine, unquestioning approval without real scrutiny."
        ),
        QuizQuestion(
            id = "tr_14",
            phraseId = "transfer_throw_cold_water",
            phraseTerm = "throw cold water on",
            category = PhraseCategory.IDIOM,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: 'I hate to throw cold water on the road-trip idea, but the mountain pass closes in November.' What is the speaker doing?",
            contextSnippet = "Conversational softener before sharing a realistic obstacle.",
            options = listOf(
                "Politely dampening enthusiasm by pointing out a practical problem",
                "Volunteering to cover the cost of winter tires for the group",
                "Accusing the group of ignoring weather forecasts on purpose",
                "Changing the subject abruptly to avoid an argument"
            ),
            correctIndex = 0,
            explanation = "'Throw cold water on (an idea)' means discouraging or cooling down excitement about a plan—often prefaced with 'I hate to...' to sound considerate."
        ),
        QuizQuestion(
            id = "tr_15",
            phraseId = "transfer_peter_out",
            phraseTerm = "peter out",
            category = PhraseCategory.PHRASAL_VERB,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Complete this description of momentum fading gradually to nothing.",
            contextSnippet = "Hannah: \"The book club started with twenty members, didn't it?\"\nLeo: \"Yeah, enthusiasm started to ________ around chapter four, and now there are three of us.\"",
            options = listOf(
                "peter out",
                "pan out",
                "spell out",
                "flesh out"
            ),
            correctIndex = 0,
            explanation = "A 19th-century US mining term (from an ore vein thinning away), 'peter out' means gradually losing strength, energy, or numbers until nothing remains."
        ),
        QuizQuestion(
            id = "tr_16",
            phraseId = "transfer_flesh_out",
            phraseTerm = "flesh out",
            category = PhraseCategory.PHRASAL_VERB,
            questionType = QuizQuestionType.REGISTER,
            prompt = "Transfer Challenge: A colleague shares a rough 3-bullet outline. How do you ask them to add concrete detail in natural American workplace English?",
            contextSnippet = "Tone: Encouraging, collaborative feedback.",
            options = listOf(
                "\"Love the direction—could you flesh out the timeline and staffing details before Friday?\"",
                "\"Love the direction—could you flush out the timeline and staffing details before Friday?\"",
                "\"Love the direction—could you air out the timeline and staffing details before Friday?\"",
                "\"Love the direction—could you thin out the timeline and staffing details before Friday?\""
            ),
            correctIndex = 0,
            explanation = "'Flesh out' means adding substance and concrete detail to a bare-bones sketch. Watch the common trap: 'flush out' means driving something hidden into the open!"
        ),
        QuizQuestion(
            id = "tr_17",
            phraseId = "transfer_silver_bullet",
            phraseTerm = "silver bullet",
            category = PhraseCategory.WORKPLACE,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: An architect says, 'Microservices aren't a silver bullet for slow deployments.' What nuance is being conveyed?",
            contextSnippet = "Engineering & strategy realism.",
            options = listOf(
                "It is not a simple, magical cure-all for a complex underlying problem",
                "It is far too expensive for mid-sized engineering organizations",
                "It only works during rare emergency production incidents",
                "It is an outdated approach that has already been deprecated"
            ),
            correctIndex = 0,
            explanation = "From folklore about slaying werewolves with one magic shot, a 'silver bullet' (usually negated) refers to an unrealistically simple fix for a multifaceted problem."
        ),
        QuizQuestion(
            id = "tr_18",
            phraseId = "transfer_split_hairs",
            phraseTerm = "split hairs",
            category = PhraseCategory.IDIOM,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Identify the idiom for arguing over overly tiny, pedantic distinctions.",
            contextSnippet = "Maya: \"Wait, is this button 'slate blue' or 'steel blue'?\"\nJordan: \"Let's not ________—both pass contrast checks and we ship in an hour.\"",
            options = listOf(
                "split hairs",
                "pull strings",
                "burn bridges",
                "call dibs"
            ),
            correctIndex = 0,
            explanation = "'Split hairs' means making excessively fine or trivial distinctions that slow down practical progress."
        ),
        QuizQuestion(
            id = "tr_19",
            phraseId = "transfer_call_dibs",
            phraseTerm = "call dibs on",
            category = PhraseCategory.SLANG,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge: Pick the playful American slang for playfully claiming first right to something.",
            contextSnippet = "Ben: \"Someone brought a fresh box of warm cinnamon rolls to the kitchen!\"\nTessa: \"I ________ the corner one with extra icing!\"",
            options = listOf(
                "call dibs on",
                "take stock of",
                "put tabs on",
                "lay siege to"
            ),
            correctIndex = 0,
            explanation = "'Call dibs on (something)' is classic casual American slang for informally claiming first claim to an item, seat, or opportunity."
        ),
        QuizQuestion(
            id = "tr_20",
            phraseId = "transfer_pass_muster",
            phraseTerm = "pass muster",
            category = PhraseCategory.IDIOM,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge: 'The prototype looks sleek, but it won't pass muster with our accessibility auditors yet.' What does 'pass muster' mean?",
            contextSnippet = "Register: Crisp C1/C2 editorial and professional English.",
            options = listOf(
                "Meet the required standards during a formal inspection or review",
                "Generate enough viral excitement to justify a marketing campaign",
                "Receive funding approval before the end of the fiscal quarter",
                "Be completed faster than the competing vendor's submission"
            ),
            correctIndex = 0,
            explanation = "From military roll-call inspections ('musters'), to 'pass muster' means proving good enough to satisfy rigorous review criteria."
        ),
        QuizQuestion(
            id = "tr_21",
            phraseId = "transfer_trenchant",
            phraseTerm = "trenchant",
            category = PhraseCategory.GRE_VOCAB,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge (GRE): 'Her trenchant critique exposed the hidden flaw in the forecast within two minutes.' What does 'trenchant' mean?",
            contextSnippet = "Register: C2 / GRE editorial and academic precision.",
            options = listOf(
                "Vigorous, incisive, and sharply clear in expression or analysis",
                "Overly cautious and hesitant to draw a firm conclusion",
                "Lengthy, meandering, and difficult for readers to follow",
                "Polite, flattering, and intended solely to boost morale"
            ),
            correctIndex = 0,
            explanation = "'Trenchant' (from Old French for 'cutting') describes analysis or wit that slices straight to the core with razor-sharp clarity."
        ),
        QuizQuestion(
            id = "tr_22",
            phraseId = "transfer_quixotic",
            phraseTerm = "quixotic",
            category = PhraseCategory.GRE_VOCAB,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge (GRE): Pick the precise word for a noble yet wildly impractical, overly idealistic venture.",
            contextSnippet = "Reviewer: \"Critics called his plan to build a solar rail line across the desert ________, yet his passion inspired hundreds of engineers.\"",
            options = listOf(
                "quixotic",
                "perfunctory",
                "scrupulous",
                "soporific"
            ),
            correctIndex = 0,
            explanation = "Named after Cervantes' Don Quixote, 'quixotic' describes romantic, chivalrous idealism that disregards practical obstacles."
        ),
        QuizQuestion(
            id = "tr_23",
            phraseId = "transfer_verisimilitude",
            phraseTerm = "verisimilitude",
            category = PhraseCategory.GRE_VOCAB,
            questionType = QuizQuestionType.MEANING,
            prompt = "Transfer Challenge (GRE): 'Set designers consulted 1940s archives to give the courtroom drama remarkable verisimilitude.' What does 'verisimilitude' mean?",
            contextSnippet = "Register: C2 literary criticism and film analysis.",
            options = listOf(
                "The appearance of being true, lifelike, or convincingly real",
                "A deliberate break from historical facts for comic effect",
                "An overly sentimental ending designed to please test audiences",
                "Rapid pacing that prevents viewers from noticing plot holes"
            ),
            correctIndex = 0,
            explanation = "From Latin 'verum' (truth) + 'similis' (similar), 'verisimilitude' refers to lifelike realism and convincing authenticity in fiction or art."
        ),
        QuizQuestion(
            id = "tr_24",
            phraseId = "transfer_redoubtable",
            phraseTerm = "redoubtable",
            category = PhraseCategory.GRE_VOCAB,
            questionType = QuizQuestionType.REGISTER,
            prompt = "Transfer Challenge (GRE): Which sentence uses 'redoubtable' with the right meaning and tone?",
            contextSnippet = "Careful! 'Redoubtable' does NOT mean 'doubtful.'",
            options = listOf(
                "Even seasoned negotiators respected the union's redoubtable lead attorney.",
                "His redoubtable claim lacked any credible sources or documentation.",
                "We felt redoubtable about whether the rain would clear by noon.",
                "The redoubtable prototype broke apart on its very first drop test."
            ),
            correctIndex = 0,
            explanation = "'Redoubtable' is a classic GRE trap word—it means formidable and commanding deep respect (especially as an opponent), never 'doubtful.'"
        ),
        QuizQuestion(
            id = "tr_25",
            phraseId = "transfer_perspicacious",
            phraseTerm = "perspicacious",
            category = PhraseCategory.GRE_VOCAB,
            questionType = QuizQuestionType.CLOZE,
            prompt = "Transfer Challenge (GRE): Choose the best word for someone with penetrating mental insight and keen judgment.",
            contextSnippet = "Mentor: \"That was a ________ observation—nobody else noticed how seasonal returns were skewing our retention curve.\"",
            options = listOf(
                "perspicacious",
                "pusillanimous",
                "parsimonious",
                "peripatetic"
            ),
            correctIndex = 0,
            explanation = "'Perspicacious' (from Latin 'to see through') praises keen discernment and razor-sharp understanding of subtle patterns."
        )
    )

    fun pickQuestions(
        categoryFilter: CategoryFilter = CategoryFilter.ALL,
        count: Int = 5
    ): List<QuizQuestion> {
        val pool = questions.filter { categoryFilter.matches(it.category) }
            .ifEmpty { questions }
        return pool.shuffled().take(count.coerceAtMost(pool.size))
    }
}
