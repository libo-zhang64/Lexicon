package com.example.data.catalog

import com.example.data.model.CefrLevel
import com.example.data.model.DialogueTurn
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseRegister
import com.example.data.model.SynonymNuance

internal val curatedSlangAndIdioms: List<PhraseEntry> = listOf(
    // --- DAILY SLANG (10) ---
    PhraseEntry(
        id = "low_key",
        term = "low-key",
        ipa = "/ˌloʊˈkiː/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CASUAL,
        cefrLevel = CefrLevel.B2,
        definition = "Quietly, subtly, or somewhat; used either as an adjective for understated events or as an adverb meaning 'secretly / kind of'.",
        nuanceSummary = "In contemporary American speech, adverbial 'low-key' softens an admission or confession without sounding overly dramatic.",
        dialogue = listOf(
            DialogueTurn("Maya", "Are you excited about the team offsite in Austin next week?"),
            DialogueTurn("Jordan", "Honestly, I'm low-key relieved we get a break from back-to-back Zoom calls."),
            DialogueTurn("Maya", "Same. Let's keep dinner Friday night pretty low-key, though.")
        ),
        etymology = "Originated in 19th-century music and photography ('low-key lighting' uses predominantly dark, restrained tones). Its modern adverbial slang sense emerged in African American Vernacular English (AAVE) in the 2000s before entering mainstream US speech.",
        synonyms = listOf(
            SynonymNuance("subtly", "More formal; lacks the playful, candid tone of 'low-key'."),
            SynonymNuance("quietly", "Literal and neutral; does not carry the 'secretly kind of' adverbial sense."),
            SynonymNuance("understated", "Refined adjective for style or events; never used as an adverb.")
        ),
        antonyms = listOf("high-key", "over-the-top", "flashy"),
        usageNotes = listOf(
            "Works both as an adjective ('a low-key dinner') and as a casual adverb before adjectives or verbs ('I low-key love this song').",
            "Appropriate with colleagues your age in Slack or casual chats, but avoid the adverbial slang sense in formal written reports."
        )
    ),
    PhraseEntry(
        id = "no_brainer",
        term = "no-brainer",
        ipa = "/ˌnoʊˈbreɪnər/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "A decision or choice so obviously advantageous that it requires almost no deliberation.",
        nuanceSummary = "Widely used across everyday American conversation and informal business meetings to signal strong consensus.",
        dialogue = listOf(
            DialogueTurn("Elena", "Did you accept the transfer to the Seattle office?"),
            DialogueTurn("Marcus", "It was a total no-brainer—better role, shorter commute, and they covered moving costs."),
            DialogueTurn("Elena", "That really does check every box.")
        ),
        etymology = "Coined in American English in the late 1950s (first documented in sports journalism and casual campus speech) to describe tasks requiring 'no brain' power to solve.",
        synonyms = listOf(
            SynonymNuance("slam dunk", "Emphasizes guaranteed success rather than ease of decision."),
            SynonymNuance("obvious choice", "Neutral and formal; suitable for executive memos."),
            SynonymNuance("given", "Focuses on an accepted fact rather than an active choice.")
        ),
        antonyms = listOf("tough call", "coin toss", "dilemma"),
        usageNotes = listOf(
            "Always takes an indefinite article: 'a no-brainer' (never 'it is no-brainer').",
            "Acceptable in spoken US workplace discussions, though swap for 'clear choice' in formal board decks."
        )
    ),
    PhraseEntry(
        id = "play_it_by_ear",
        term = "play it by ear",
        ipa = "/pleɪ ɪt baɪ ɪr/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To decide how to proceed spontaneously as a situation unfolds, rather than following a fixed plan.",
        nuanceSummary = "Signals relaxed flexibility—Americans often use it to keep weekend plans open without committing.",
        dialogue = listOf(
            DialogueTurn("Sam", "Should we book museum tickets for Saturday afternoon?"),
            DialogueTurn("Priya", "The forecast looks rainy, so let's just play it by ear after brunch."),
            DialogueTurn("Sam", "Works for me—we can always pivot to a coffee shop.")
        ),
        etymology = "Borrowed from musicians who play a melody purely from listening ('by ear') without sheet music. By the 1950s, American political and sports writers extended it to improvising strategy in real time.",
        synonyms = listOf(
            SynonymNuance("wing it", "More informal; implies improvising without preparation, sometimes with risk."),
            SynonymNuance("go with the flow", "Focuses on passively adapting to others' choices rather than making real-time calls."),
            SynonymNuance("improvise", "More formal and clinical; fits technical or creative contexts.")
        ),
        antonyms = listOf("plan to a T", "lock in", "stick to the script"),
        usageNotes = listOf(
            "Almost always keeps the fixed pronoun 'it' ('play it by ear', not 'play the evening by ear').",
            "Common in both personal life and agile workplace conversations when awaiting new data."
        )
    ),
    PhraseEntry(
        id = "heads_up",
        term = "heads-up",
        ipa = "/ˈhɛdz ʌp/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "An advance warning, brief notice, or helpful reminder before something happens.",
        nuanceSummary = "One of the most frequent American politeness softeners on Slack and email when delivering small schedule shifts.",
        dialogue = listOf(
            DialogueTurn("Devon", "Quick heads-up: the client pushed our design review back by thirty minutes."),
            DialogueTurn("Claire", "Thanks for the heads-up! That gives me time to polish the prototype."),
            DialogueTurn("Devon", "Sure thing—I updated the calendar invite.")
        ),
        etymology = "Originated in 19th-century American baseball and construction sites, where shouting 'Heads up!' warned bystanders to look up for a stray ball or falling object.",
        synonyms = listOf(
            SynonymNuance("advance notice", "Formal administrative phrasing for policies and contracts."),
            SynonymNuance("tip-off", "Implies confidential or insider information."),
            SynonymNuance("fair warning", "Carries a sterner or slightly cautionary undertone.")
        ),
        antonyms = listOf("blindside", "surprise", "afterthought"),
        usageNotes = listOf(
            "Used as a singular countable noun ('give someone a heads-up', 'just a quick heads-up').",
            "As a hyphenated adjective before a noun, 'heads-up play' means alert, quick-thinking action."
        )
    ),
    PhraseEntry(
        id = "ballpark",
        term = "ballpark (figure)",
        ipa = "/ˈbɔːlpɑːrk/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "A rough, approximate estimate—or as a verb, to give a quick approximate number.",
        nuanceSummary = "Essential in American budgeting, contracting, and everyday planning when precision isn't yet possible.",
        dialogue = listOf(
            DialogueTurn("Hannah", "How much will the kitchen tile repair run us?"),
            DialogueTurn("Leo", "The contractor hasn't sent the quote yet, but he ballparked it around twelve hundred."),
            DialogueTurn("Hannah", "Okay, that's in the ballpark of what we budgeted.")
        ),
        etymology = "Sprang from 1950s–60s American baseball and NASA spaceflight jargon: a rocket landing 'in the ballpark' (inside the stadium) was close enough to the target zone to be counted a success.",
        synonyms = listOf(
            SynonymNuance("rough estimate", "Neutral and globally understood across all English dialects."),
            SynonymNuance("back-of-the-envelope", "Emphasizes a fast, informal mental or scratch-paper calculation."),
            SynonymNuance("guesstimate", "More playful and implies less rigor than 'ballpark'.")
        ),
        antonyms = listOf("exact figure", "to the penny", "pinpoint"),
        usageNotes = listOf(
            "Common patterns: 'in the ballpark (of)', 'a ballpark figure/estimate', and verb 'to ballpark something'.",
            "Saying 'not even in the same ballpark' means two things are completely incomparable."
        )
    ),
    PhraseEntry(
        id = "cut_corners",
        term = "cut corners",
        ipa = "/kʌt ˈkɔːrnərz/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To do something hastily, cheaply, or carelessly by skipping important steps to save time or money.",
        nuanceSummary = "Almost always carries negative judgment—implies compromised quality or integrity.",
        dialogue = listOf(
            DialogueTurn("Nadia", "Why did the new release ship with three broken checkout flows?"),
            DialogueTurn("Owen", "The vendor cut corners on QA testing to hit last Friday's deadline."),
            DialogueTurn("Nadia", "That shortcut is going to cost us twice as much time now.")
        ),
        etymology = "Dates to 19th-century American carriage driving and road travel, where impatient drivers literally steered diagonally across street corners, damaging lawns or risking collisions to shave seconds off a trip.",
        synonyms = listOf(
            SynonymNuance("skimp on", "Focuses specifically on spending too little money, materials, or effort."),
            SynonymNuance("take shortcuts", "Can be neutral or clever in tech; 'cut corners' is almost always critical."),
            SynonymNuance("phone it in", "Casual slang emphasizing minimal enthusiasm or lazy execution.")
        ),
        antonyms = listOf("go the extra mile", "dot every i", "do by the book"),
        usageNotes = listOf(
            "Often paired with 'on': 'cut corners on safety / quality / testing'.",
            "Unlike 'streamline' (positive efficiency), 'cut corners' warns that essential standards were sacrificed."
        )
    ),
    PhraseEntry(
        id = "rain_check",
        term = "take a rain check",
        ipa = "/teɪk ə ˈreɪn tʃɛk/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To politely decline an invitation now while expressing genuine interest in accepting it at a later time.",
        nuanceSummary = "A warm, face-saving American social phrase that softens a 'no' without sounding dismissive.",
        dialogue = listOf(
            DialogueTurn("Tessa", "A bunch of us are grabbing tacos after work—want to join?"),
            DialogueTurn("Ben", "I'm swamped with a deadline tonight, so can I take a rain check?"),
            DialogueTurn("Tessa", "Of course! We'll catch you next Thursday.")
        ),
        etymology = "Invented in 1880s American baseball: when a game was rained out, stadiums issued cardboard 'rain checks' allowing fans to attend a rescheduled game for free. Retailers later adopted it for out-of-stock sale items.",
        synonyms = listOf(
            SynonymNuance("reschedule", "Functional and professional; best for formal calendar meetings."),
            SynonymNuance("pass for now", "More casual and slightly less warm about re-booking later."),
            SynonymNuance("pencil in another time", "Emphasizes tentative scheduling.")
        ),
        antonyms = listOf("RSVP yes", "turn down flat", "lock in"),
        usageNotes = listOf(
            "Always say 'take a rain check (on something)', or as a host, 'I'll give you a rain check'.",
            "Signifies goodwill—do not use it if you never intend to see the person again."
        )
    ),
    PhraseEntry(
        id = "ghost_someone",
        term = "ghost (someone)",
        ipa = "/ɡoʊst/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CASUAL,
        cefrLevel = CefrLevel.B2,
        definition = "To abruptly end all communication with someone without any explanation, vanishing like a ghost.",
        nuanceSummary = "Started in dating culture but is now common across American job hunting ('the recruiter ghosted me') and friendships.",
        dialogue = listOf(
            DialogueTurn("Chloe", "Did you ever hear back after your final interview with that startup?"),
            DialogueTurn("Rami", "Not a word—it's been three weeks, so I'm pretty sure they ghosted me."),
            DialogueTurn("Chloe", "Ugh, radio silence after four rounds is the worst.")
        ),
        etymology = "Entered mainstream American digital slang in the mid-2000s alongside texting and dating apps, extending the older verb 'ghost' (to depart silently) into online communication.",
        synonyms = listOf(
            SynonymNuance("go radio silent", "Politer and more workplace-appropriate when describing an unresponsive client."),
            SynonymNuance("leave on read", "Specifically refers to seeing a text message without replying."),
            SynonymNuance("cut off contact", "More deliberate and formal; often implies a stated boundary.")
        ),
        antonyms = listOf("follow up", "give closure", "stay in the loop"),
        usageNotes = listOf(
            "Transitive verb: 'ghost someone' or passive 'get ghosted by a recruiter'.",
            "Keep it to casual chats with friends or peers; in formal emails use 'unresponsive' or 'ceased communication'."
        )
    ),
    PhraseEntry(
        id = "in_a_nutshell",
        term = "in a nutshell",
        ipa = "/ɪn ə ˈnʌtʃɛl/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "Summarized in the fewest, clearest words possible.",
        nuanceSummary = "A crisp verbal signpost Americans use before delivering a one-sentence executive summary.",
        dialogue = listOf(
            DialogueTurn("Liam", "I missed the two-hour architecture review—what did they decide?"),
            DialogueTurn("Sofia", "In a nutshell, we're keeping the current API and upgrading the database first."),
            DialogueTurn("Liam", "Got it, thanks for the quick recap.")
        ),
        etymology = "Traces back to Roman author Pliny the Elder (1st century AD), who wrote that Homer's Iliad had been copied in such tiny handwriting that it fit inside a walnut shell. Shakespeare popularized it in Hamlet.",
        synonyms = listOf(
            SynonymNuance("long story short", "More casual; best for summarizing personal anecdotes."),
            SynonymNuance("the bottom line is", "More emphatic and business-oriented; highlights the decisive takeaway."),
            SynonymNuance("to sum up", "Neutral academic or presentation transition.")
        ),
        antonyms = listOf("at length", "in granular detail", "blown out"),
        usageNotes = listOf(
            "Works smoothly at the start ('In a nutshell, ...') or end ('That's the plan in a nutshell.').",
            "Keep your actual summary brief after saying it—otherwise it sounds unintentionally ironic."
        )
    ),
    PhraseEntry(
        id = "read_the_room",
        term = "read the room",
        ipa = "/riːd ðə ruːm/",
        category = PhraseCategory.SLANG,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To accurately sense the unspoken mood, sensitivities, and social dynamics of a group before speaking or acting.",
        nuanceSummary = "Frequently used as a sharp critique ('He really couldn't read the room') when someone makes a tone-deaf remark.",
        dialogue = listOf(
            DialogueTurn("Aisha", "Did he seriously brag about his bonus right after the layoff announcement?"),
            DialogueTurn("Grant", "Yeah, zero self-awareness. You've got to read the room before saying something like that."),
            DialogueTurn("Aisha", "Everyone went completely silent.")
        ),
        etymology = "Originated in 20th-century American stand-up comedy and live theater, where performers had to gauge an audience's energy within seconds to adjust their timing and material.",
        synonyms = listOf(
            SynonymNuance("gauge the mood", "More formal and analytical; suits leadership coaching."),
            SynonymNuance("pick up on social cues", "Clinical/psychological framing for interpersonal awareness."),
            SynonymNuance("sense the vibe", "More casual and youth-oriented.")
        ),
        antonyms = listOf("be tone-deaf", "put your foot in your mouth", "miss the mark"),
        usageNotes = listOf(
            "Used both as advice ('Read the room first') and criticism ('She failed to read the room').",
            "Applies equally to video meetings and group chat threads—not just physical rooms."
        )
    ),

    // --- IDIOMS (10) ---
    PhraseEntry(
        id = "bite_the_bullet",
        term = "bite the bullet",
        ipa = "/baɪt ðə ˈbʊlɪt/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To force yourself to endure an unpleasant, difficult, or costly task that you have been putting off.",
        nuanceSummary = "Implies stoic acceptance after hesitation—you stop procrastinating and face the inevitable.",
        dialogue = listOf(
            DialogueTurn("Noah", "Your laptop fan sounds like a jet engine. Are you finally replacing it?"),
            DialogueTurn("Grace", "Yeah, I'm going to bite the bullet this weekend and buy a new one."),
            DialogueTurn("Noah", "Honestly, you'll be glad once it's done.")
        ),
        etymology = "Popularized in 19th-century military accounts—including Rudyard Kipling's 1891 novel 'The Light That Failed'—referring to wounded soldiers biting down on a lead bullet during battlefield surgery before anesthesia existed.",
        synonyms = listOf(
            SynonymNuance("rip off the Band-Aid", "Emphasizes doing something painful quickly to get it over with."),
            SynonymNuance("face the music", "Specifically means accepting consequences or accountability for a mistake."),
            SynonymNuance("take the plunge", "Used for bold, exciting commitments rather than unpleasant chores.")
        ),
        antonyms = listOf("put off", "kick the can down the road", "chickening out"),
        usageNotes = listOf(
            "Often followed by 'and + verb' ('bite the bullet and call the dentist').",
            "Do not confuse with 'dodge a bullet', which means narrowly escaping a bad outcome."
        )
    ),
    PhraseEntry(
        id = "read_between_the_lines",
        term = "read between the lines",
        ipa = "/riːd bɪˈtwiːn ðə laɪnz/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To infer an unstated, subtle, or hidden meaning from what someone says or writes.",
        nuanceSummary = "Vital for decoding polite American workplace indirectness (e.g., 'Interesting idea' meaning 'We won't do this').",
        dialogue = listOf(
            DialogueTurn("Keiko", "The VP said my proposal was 'ambitious for this quarter.' Was that praise?"),
            DialogueTurn("Daniel", "If you read between the lines, he's politely telling you to trim the scope."),
            DialogueTurn("Keiko", "Good catch—I'll send a leaner phase-one option tomorrow.")
        ),
        etymology = "Stems from 19th-century cryptography and invisible-ink correspondence, where secret messages were literally written in the blank spaces between visible lines of text.",
        synonyms = listOf(
            SynonymNuance("pick up on the subtext", "More analytical and formal; common in media and leadership."),
            SynonymNuance("connect the dots", "Focuses on linking separate clues rather than decoding indirect wording."),
            SynonymNuance("infer", "Concise academic verb without the vivid imagery.")
        ),
        antonyms = listOf("take at face value", "take literally", "miss the subtext"),
        usageNotes = listOf(
            "Frequently contrasted with 'take something at face value' (accepting the surface meaning).",
            "In American business culture, softening criticism is common—reading between the lines is a key C1/C2 skill."
        )
    ),
    PhraseEntry(
        id = "burn_the_midnight_oil",
        term = "burn the midnight oil",
        ipa = "/bɜːrn ðə ˈmɪdnaɪt ɔɪl/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To work, study, or write diligently late into the night.",
        nuanceSummary = "Carries an admiring, slightly literary tone of dedicated effort rather than chaotic burnout.",
        dialogue = listOf(
            DialogueTurn("Victor", "You look exhausted—long week?"),
            DialogueTurn("Amara", "We've been burning the midnight oil to get the grant application in before Friday."),
            DialogueTurn("Victor", "Take a real break once you hit submit.")
        ),
        etymology = "Coined by English poet Francis Quarles in 1635, evoking scholars studying by the glow of oil lamps long past midnight before electricity existed.",
        synonyms = listOf(
            SynonymNuance("pull an all-nighter", "Casual American campus/work slang for staying awake the entire night without sleeping."),
            SynonymNuance("burn the candle at both ends", "Warns of overexerting yourself early and late to the point of exhaustion."),
            SynonymNuance("work overtime", "Literal and administrative.")
        ),
        antonyms = listOf("call it a day", "clock out early", "take it easy"),
        usageNotes = listOf(
            "Almost always used in continuous or habitual tenses ('burning the midnight oil', 'burned the midnight oil').",
            "Fits naturally in both spoken American English and polished editorial writing."
        )
    ),
    PhraseEntry(
        id = "silver_lining",
        term = "silver lining",
        ipa = "/ˈsɪlvər ˈlaɪnɪŋ/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "A hopeful, comforting, or unexpectedly positive aspect hidden inside a disappointing situation.",
        nuanceSummary = "Reflects characteristic American pragmatic optimism—reframing a setback without denying its difficulty.",
        dialogue = listOf(
            DialogueTurn("Mateo", "I can't believe our flight to Chicago got canceled."),
            DialogueTurn("Rachel", "The silver lining is we finally have time to see your cousin in Denver tonight."),
            DialogueTurn("Mateo", "True—I haven't caught up with him in two years.")
        ),
        etymology = "Derived from John Milton's 1634 masque 'Comus', describing a dark cloud whose hidden edge gleams with silver sunlight ('turn forth her silver lining on the night').",
        synonyms = listOf(
            SynonymNuance("bright side", "More casual and direct ('look on the bright side')."),
            SynonymNuance("upside", "Concise business-friendly noun for advantages or positive tradeoffs."),
            SynonymNuance("blessing in disguise", "Implies the bad event later turns out to be overwhelmingly good.")
        ),
        antonyms = listOf("downside", "catch", "dark cloud"),
        usageNotes = listOf(
            "Common collocations: 'find the silver lining', 'every cloud has a silver lining', 'the one silver lining is...'.",
            "Avoid overusing it during acute personal grief—it works best for everyday setbacks or retrospectives."
        )
    ),
    PhraseEntry(
        id = "hit_the_nail_on_the_head",
        term = "hit the nail on the head",
        ipa = "/hɪt ðə neɪl ɑːn ðə hɛd/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To pinpoint the exact truth, root cause, or most accurate analysis of a problem.",
        nuanceSummary = "High-praise validation in discussions—tells someone their diagnosis was dead-on accurate.",
        dialogue = listOf(
            DialogueTurn("Omar", "I think users aren't abandoning onboarding because it's long—they just don't trust the permissions screen."),
            DialogueTurn("Jenna", "You hit the nail on the head. The drop-off spikes right on that step."),
            DialogueTurn("Omar", "Let's rewrite the privacy copy first and test it.")
        ),
        etymology = "A carpentry metaphor recorded in English since the 1500s: striking a nail squarely on its flat head drives it cleanly home in one blow, whereas glancing blows bend it.",
        synonyms = listOf(
            SynonymNuance("spot on", "Short, crisp praise (originally British, now widely used in US speech too)."),
            SynonymNuance("nail it", "Casual American exclamation celebrating flawless execution, not just analysis."),
            SynonymNuance("zero in on the root cause", "Formal workplace phrasing.")
        ),
        antonyms = listOf("miss the mark", "bark up the wrong tree", "be way off base"),
        usageNotes = listOf(
            "Often used in past tense right after someone speaks: 'You hit the nail on the head.'",
            "Keep the exact wording—don't say 'hit the nail on its top'."
        )
    ),
    PhraseEntry(
        id = "elephant_in_the_room",
        term = "elephant in the room",
        ipa = "/ˈɛlɪfənt ɪn ðə ruːm/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "An obvious, major, or uncomfortable issue that everyone notices but deliberately avoids mentioning.",
        nuanceSummary = "Used constructively in American meetings ('Let's address the elephant in the room') to show candor and leadership.",
        dialogue = listOf(
            DialogueTurn("Tariq", "Before we discuss Q4 hiring, let's address the elephant in the room—our churn rate doubled last month."),
            DialogueTurn("Leslie", "Thank you for bringing it up. We can't plan headcount until we fix retention."),
            DialogueTurn("Tariq", "Exactly. Let's put the retention numbers on screen first.")
        ),
        etymology = "Echoes Ivan Krylov's 1814 fable 'The Inquisitive Man', about a museum visitor who notices tiny insects yet overlooks an elephant. The exact phrase gained prominence in US journalism in the 1950s.",
        synonyms = listOf(
            SynonymNuance("unspoken issue", "Neutral, restrained wording for formal written memos."),
            SynonymNuance("taboo subject", "Implies social prohibition rather than awkward collective avoidance."),
            SynonymNuance("white elephant", "FALSE FRIEND: means an expensive, burdensome possession—not an ignored issue!")
        ),
        antonyms = listOf("open secret", "transparent agenda", "out in the open"),
        usageNotes = listOf(
            "Standard verbs: 'address', 'name', 'acknowledge', or 'ignore' the elephant in the room.",
            "Watch the classic false friend: 'elephant in the room' (obvious awkward truth) vs. 'white elephant' (costly useless item)."
        )
    ),
    PhraseEntry(
        id = "bark_up_the_wrong_tree",
        term = "bark up the wrong tree",
        ipa = "/bɑːrk ʌp ðə rɔːŋ triː/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To pursue a mistaken line of inquiry, blame the wrong cause, or ask the wrong person for help.",
        nuanceSummary = "A colorful, non-hostile way to redirect someone's assumptions or debugging effort.",
        dialogue = listOf(
            DialogueTurn("Caleb", "I've spent two hours checking the CSS grid for that layout bug."),
            DialogueTurn("Mei", "You might be barking up the wrong tree—the API started returning null titles this morning."),
            DialogueTurn("Caleb", "Oh! That explains why the header collapsed.")
        ),
        etymology = "Uniquely American frontier idiom from the early 1800s: hunting dogs chasing raccoons at night would sometimes bark excitedly at the base of a tree after the animal had already leaped to a neighboring branch.",
        synonyms = listOf(
            SynonymNuance("on the wrong track", "Politer and more professional for formal reviews."),
            SynonymNuance("chase a red herring", "Implies being actively misled by a distracting clue."),
            SynonymNuance("miss the point", "Direct and blunter.")
        ),
        antonyms = listOf("on the right track", "hit the nail on the head", "warm on the trail"),
        usageNotes = listOf(
            "Most commonly used in continuous form ('You're barking up the wrong tree') or hedged with 'might be'.",
            "Because it's playful, it softens disagreement well among American coworkers."
        )
    ),
    PhraseEntry(
        id = "bend_over_backward",
        term = "bend over backward",
        ipa = "/bɛnd ˈoʊvər ˈbækwərd/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To make an extraordinary, strenuous effort to accommodate or please someone.",
        nuanceSummary = "Often hints that the effort felt slightly excessive or went underappreciated.",
        dialogue = listOf(
            DialogueTurn("Sonia", "Why is Alex frustrated with that enterprise account?"),
            DialogueTurn("Derek", "He bent over backward to deliver their custom export on Sunday, and they still complained."),
            DialogueTurn("Sonia", "We need firmer scope boundaries next quarter.")
        ),
        etymology = "An early 20th-century American acrobatic metaphor—literally picturing a contortionist arching backward as far as humanly possible to reach an awkward target.",
        synonyms = listOf(
            SynonymNuance("go above and beyond", "Positive praise for excellence; lacks the overstretched undertone."),
            SynonymNuance("move heaven and earth", "More dramatic and emphasizes overcoming monumental obstacles."),
            SynonymNuance("accommodate", "Neutral, formal business verb.")
        ),
        antonyms = listOf("do the bare minimum", "lift a finger (negative)", "stonewall"),
        usageNotes = listOf(
            "In American English, both 'bend over backward' (preferred in US style guides) and 'bend over backwards' are heard.",
            "Always followed by 'to + infinitive' ('bent over backward to help us')."
        )
    ),
    PhraseEntry(
        id = "throw_in_the_towel",
        term = "throw in the towel",
        ipa = "/θroʊ ɪn ðə ˈtaʊəl/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To admit defeat and stop trying after a prolonged struggle.",
        nuanceSummary = "Usually follows a visible, hard-fought effort—distinct from casually flaking out before trying.",
        dialogue = listOf(
            DialogueTurn("Paula", "Did you manage to restore your old sourdough starter?"),
            DialogueTurn("Hector", "After four failed loaves, I finally threw in the towel and started fresh."),
            DialogueTurn("Paula", "Smart move—baking should be fun, not a battle.")
        ),
        etymology = "Comes directly from 19th- and early 20th-century boxing: when a fighter was outmatched, their corner trainer literally tossed a towel (earlier, a sponge) into the ring to signal surrender to the referee.",
        synonyms = listOf(
            SynonymNuance("call it quits", "More neutral; can mean ending a job or project voluntarily without defeat."),
            SynonymNuance("pull the plug", "Workplace/tech idiom meaning leadership decides to cancel a project."),
            SynonymNuance("give up", "Plain everyday phrasing without the endurance nuance.")
        ),
        antonyms = listOf("stick it out", "double down", "stay the course"),
        usageNotes = listOf(
            "Often paired with 'ready to' or 'not ready to': 'Don't throw in the towel just yet.'",
            "Note the phrasing is 'throw in the towel' (not 'throw out the towel')."
        )
    ),
    PhraseEntry(
        id = "get_wind_of",
        term = "get wind of",
        ipa = "/ɡɛt wɪnd ʌv/",
        category = PhraseCategory.IDIOM,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To hear unofficial rumors or indirect hints about something kept quiet or confidential.",
        nuanceSummary = "Signals indirect discovery—someone caught the rumor before any formal announcement.",
        dialogue = listOf(
            DialogueTurn("Nina", "How did the sales team already know about the pricing change?"),
            DialogueTurn("Trevor", "Someone must have gotten wind of it during the beta call yesterday."),
            DialogueTurn("Nina", "Let's send the official FAQ out today so rumors don't spin up.")
        ),
        etymology = "Draws from hunting dogs catching the scent ('wind') of distant game carried on the breeze before the animal comes into view; documented in English since the 18th century.",
        synonyms = listOf(
            SynonymNuance("catch wind of", "Interchangeable American variant with identical meaning."),
            SynonymNuance("hear through the grapevine", "Emphasizes informal gossip passing person-to-person."),
            SynonymNuance("learn of", "Formal and neutral; does not imply secrecy.")
        ),
        antonyms = listOf("be kept in the dark", "be blindsided", "miss the memo"),
        usageNotes = listOf(
            "In American English, the past participle is typically 'gotten wind of' (whereas British English uses 'got wind of').",
            "Used almost exclusively for news, rumors, plans, or secrets—not concrete facts from a textbook."
        )
    )
)
