package com.example.data.catalog

import com.example.data.model.CefrLevel
import com.example.data.model.DialogueTurn
import com.example.data.model.PhraseCategory
import com.example.data.model.PhraseEntry
import com.example.data.model.PhraseRegister
import com.example.data.model.SynonymNuance

internal val curatedPhrasalAndWorkplace: List<PhraseEntry> = listOf(
    // --- COMPLEX PHRASAL VERBS (10) ---
    PhraseEntry(
        id = "pan_out",
        term = "pan out",
        ipa = "/pæn aʊt/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To turn out well, succeed, or develop in a particular way over time (very often used in the negative or questions).",
        nuanceSummary = "Focuses on the eventual outcome of a hopeful bet, plan, or hypothesis after waiting to see results.",
        dialogue = listOf(
            DialogueTurn("Gabe", "How did that partnership pitch with the Boston clinic go?"),
            DialogueTurn("Farah", "It didn't pan out this quarter, but they invited us back for their fall budget cycle."),
            DialogueTurn("Gabe", "Still a great warm lead to keep alive.")
        ),
        etymology = "Born in the 1849 California Gold Rush! Prospectors swirled gravel and water in shallow metal pans; if gold flecks remained at the bottom, the claim 'panned out'—it yielded value.",
        synonyms = listOf(
            SynonymNuance("work out", "Broader and simpler; used for schedules, relationships, and plans alike."),
            SynonymNuance("bear fruit", "More formal and literary; highlights tangible rewards from long effort."),
            SynonymNuance("materialize", "Formal; emphasizes whether something anticipated actually happens.")
        ),
        antonyms = listOf("fall through", "fizzle out", "backfire"),
        usageNotes = listOf(
            "Intransitive and inseparable: a plan pans out (you never 'pan out a plan').",
            "Very natural in uncertain future contexts: 'Let's see how the pilot pans out before scaling.'"
        )
    ),
    PhraseEntry(
        id = "iron_out",
        term = "iron out",
        ipa = "/ˈaɪərn aʊt/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To resolve remaining minor problems, disagreements, or logistical wrinkles so something runs smoothly.",
        nuanceSummary = "Reassuring and collaborative—frames remaining issues as solvable details rather than fatal blockers.",
        dialogue = listOf(
            DialogueTurn("Elena", "Are we ready to sign the vendor contract tomorrow?"),
            DialogueTurn("Chris", "Almost—legal just needs an hour to iron out a few data-retention details."),
            DialogueTurn("Elena", "Perfect, ping me once those wrinkles are settled.")
        ),
        etymology = "A domestic laundry metaphor from the 19th century: pressing creases flat with a hot iron makes fabric crisp and smooth, just as careful negotiation smooths rough draft agreements.",
        synonyms = listOf(
            SynonymNuance("hammer out", "Implies tougher, noisier, more intense back-and-forth negotiation."),
            SynonymNuance("hash out", "Focuses on talking through messy details openly in a meeting."),
            SynonymNuance("resolve", "Clean formal verb for written legal or technical documents.")
        ),
        antonyms = listOf("complicate", "leave unresolved", "gloss over"),
        usageNotes = listOf(
            "Separable transitive verb: 'iron out the kinks' OR 'iron them out' (pronouns must go in the middle).",
            "Common objects: 'details', 'kinks', 'wrinkles', 'differences', 'edge cases'."
        )
    ),
    PhraseEntry(
        id = "follow_through_on",
        term = "follow through (on)",
        ipa = "/ˈfɑːloʊ θruː ɑːn/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To carry a commitment, promise, or plan all the way to completion rather than stopping halfway.",
        nuanceSummary = "A high-trust American workplace virtue—distinct from 'follow up' (checking in).",
        dialogue = listOf(
            DialogueTurn("Maya", "Why did you nominate Luis to lead the migration?"),
            DialogueTurn("Kenji", "Because whenever he commits to a milestone, he always follows through on it."),
            DialogueTurn("Maya", "That kind of reliability is rare on cross-team projects.")
        ),
        etymology = "Derived from 19th-century golf, tennis, and baseball instruction, where continuing the swing smoothly *after* striking the ball ('the follow-through') ensures accuracy and power.",
        synonyms = listOf(
            SynonymNuance("deliver on", "Emphasizes producing promised business outcomes or value."),
            SynonymNuance("see through", "Highlights perseverance through a long or arduous process."),
            SynonymNuance("follow up", "FALSE FRIEND: means checking status or sending a reminder—not completing the task!")
        ),
        antonyms = listOf("drop the ball", "flake on", "abandon halfway"),
        usageNotes = listOf(
            "Crucial C1 distinction: 'follow up' = check in/ping someone; 'follow through (on)' = actually complete what you promised.",
            "Can be used with 'on' + noun ('follow through on a promise') or as a hyphenated noun ('strong follow-through')."
        )
    ),
    PhraseEntry(
        id = "chalk_up_to",
        term = "chalk (something) up to",
        ipa = "/tʃɔːk ʌp tuː/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C2,
        definition = "To attribute an outcome—usually a mistake, setback, or surprising result—to a particular cause.",
        nuanceSummary = "Graceful, non-defensive phrasing Americans use to accept a lesson without dwelling on blame.",
        dialogue = listOf(
            DialogueTurn("Brianna", "Our first podcast episode had a rough audio echo in the intro."),
            DialogueTurn("Sam", "Let's chalk it up to a learning experience and test the mic gain next time."),
            DialogueTurn("Brianna", "Agreed—episode two already sounds way cleaner.")
        ),
        etymology = "Originates from 19th-century British and American taverns and billiard halls, where bartenders kept customers' tabs and game scores by writing tally marks with chalk on a slate board.",
        synonyms = listOf(
            SynonymNuance("attribute to", "Formal and analytical; ideal for postmortems and research papers."),
            SynonymNuance("put down to", "More common in British English than American English."),
            SynonymNuance("blame on", "Harsher and assigns fault rather than calm reflection.")
        ),
        antonyms = listOf("rule out", "disregard", "separate from"),
        usageNotes = listOf(
            "Always separable with the outcome in the middle: 'chalk it up to experience / bad timing / nerves'.",
            "Extremely common collocation: 'chalk it up to experience'."
        )
    ),
    PhraseEntry(
        id = "brush_up_on",
        term = "brush up on",
        ipa = "/brʌʃ ʌp ɑːn/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To quickly refresh or practice a skill or body of knowledge you previously knew but haven't used lately.",
        nuanceSummary = "Specifically implies prior familiarity—never use it for learning a brand-new subject from scratch.",
        dialogue = listOf(
            DialogueTurn("Diego", "I'm interviewing for a data role next week, so I'm brushing up on SQL window functions."),
            DialogueTurn("Tanya", "Smart—those always show up in live technical screens."),
            DialogueTurn("Diego", "It's all coming back faster than I expected.")
        ),
        etymology = "A 17th-century physical care metaphor: brushing dust off a velvet coat or polishing a dull surface restores its original shine without remaking it.",
        synonyms = listOf(
            SynonymNuance("bone up on", "Playful, older American slang meaning to cram or study intensively."),
            SynonymNuance("refresh your memory on", "Slightly longer, neutral phrasing."),
            SynonymNuance("get up to speed on", "Used when catching up on *new* developments rather than old skills.")
        ),
        antonyms = listOf("get rusty at", "fall out of practice", "neglect"),
        usageNotes = listOf(
            "Three-word inseparable phrasal verb: always 'brush up on + noun' (never 'brush on up').",
            "Only fits skills you already learned once before (languages, math, coding, driving rules)."
        )
    ),
    PhraseEntry(
        id = "fall_through",
        term = "fall through",
        ipa = "/fɔːl θruː/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To fail to happen or collapse before completion, especially regarding a deal, trip, or arrangement.",
        nuanceSummary = "Neutral and matter-of-fact—places focus on the canceled arrangement rather than blaming one party.",
        dialogue = listOf(
            DialogueTurn("Alicia", "Weren't you closing on that condo in Portland this week?"),
            DialogueTurn("Evan", "The financing fell through at the last minute, so we're back to looking."),
            DialogueTurn("Alicia", "I'm sorry to hear that—something better will turn up.")
        ),
        etymology = "Metaphor of something slipping straight through the gaps of a floorboard or sieve instead of holding firm; attested in English business writing since the late 1700s.",
        synonyms = listOf(
            SynonymNuance("collapse", "More dramatic; used for major treaties, mergers, or negotiations."),
            SynonymNuance("fizzle out", "Describes losing momentum gradually rather than failing cleanly."),
            SynonymNuance("fall apart", "Implies internal chaos or conflict broke the plan.")
        ),
        antonyms = listOf("go through", "pan out", "come together"),
        usageNotes = listOf(
            "Intransitive: plans, deals, trips, and funding 'fall through' (never 'we fell through the deal').",
            "Pairs cleanly with 'at the last minute' or 'due to...'."
        )
    ),
    PhraseEntry(
        id = "zero_in_on",
        term = "zero in on",
        ipa = "/ˈzɪroʊ ɪn ɑːn/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To direct sharp, concentrated attention toward one specific target, cause, or priority.",
        nuanceSummary = "Conveys crisp analytical focus—great in product, engineering, and strategy meetings.",
        dialogue = listOf(
            DialogueTurn("Priya", "We have forty customer feedback items from this month's survey."),
            DialogueTurn("Marcus", "Let's zero in on the top three onboarding friction points first."),
            DialogueTurn("Priya", "Agreed—fixing those will move retention fastest.")
        ),
        etymology = "Entered American English during World War II from artillery and rifle calibration, where 'zeroing in' meant adjusting sights to zero error on a distant target.",
        synonyms = listOf(
            SynonymNuance("hone in on", "Very common American variant (originally 'home in on'), nearly identical in daily speech."),
            SynonymNuance("pinpoint", "Focuses on identifying an exact location or root cause."),
            SynonymNuance("drill down into", "Workplace idiom for inspecting deeper layers of data.")
        ),
        antonyms = listOf("broaden out", "lose focus", "boil the ocean"),
        usageNotes = listOf(
            "Inseparable three-word phrasal verb: 'zero in on + target'.",
            "Works well when narrowing a broad brainstorm into concrete action."
        )
    ),
    PhraseEntry(
        id = "phase_out",
        term = "phase out",
        ipa = "/feɪz aʊt/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To discontinue a product, policy, or legacy system gradually in planned stages rather than abruptly.",
        nuanceSummary = "Communicates orderly, predictable transition—essential for roadmap and policy announcements.",
        dialogue = listOf(
            DialogueTurn("Jordan", "Are we shutting off the v1 authentication endpoint next month?"),
            DialogueTurn("Sonia", "No, we'll phase it out over two quarters so enterprise clients have time to migrate."),
            DialogueTurn("Jordan", "That timeline feels much safer.")
        ),
        etymology = "Grew out of mid-20th-century American manufacturing and electrical engineering, adapting 'phase' (a stage in a cycle) into a verb for staged rollouts ('phase in') and retirements ('phase out').",
        synonyms = listOf(
            SynonymNuance("sunset", "Popular US tech/corporate verb for gracefully retiring a feature or service."),
            SynonymNuance("wind down", "Emphasizes slowing operations toward closure."),
            SynonymNuance("scrap", "Informal and abrupt—implies sudden cancellation.")
        ),
        antonyms = listOf("phase in", "roll out", "ramp up"),
        usageNotes = listOf(
            "Separable: 'phase out legacy plans' or 'phase them out gradually'.",
            "Frequently used in passive voice for policy updates: 'Paper tickets are being phased out.'"
        )
    ),
    PhraseEntry(
        id = "step_up_to_the_plate",
        term = "step up (to the plate)",
        ipa = "/stɛp ʌp tuː ðə pleɪt/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.CONVERSATIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To rise to a challenge and voluntarily take responsibility when a moment demands leadership.",
        nuanceSummary = "High-energy American praise for initiative—short form 'step up' is equally common in workplaces.",
        dialogue = listOf(
            DialogueTurn("Claire", "Who covered the client workshop while Devon was out sick?"),
            DialogueTurn("Liam", "Keiko really stepped up to the plate and ran the whole session seamlessly."),
            DialogueTurn("Claire", "I'll make sure to shout her out at all-hands.")
        ),
        etymology = "Pure American baseball heritage: a batter walks up to home plate knowing the game's outcome rests on their swing. Figurative workplace usage surged in the 1990s.",
        synonyms = listOf(
            SynonymNuance("rise to the occasion", "More formal and dignified; fits speeches and performance reviews."),
            SynonymNuance("take ownership", "Modern corporate phrasing focused on accountability."),
            SynonymNuance("pitch in", "Means helping casually as part of a group rather than leading.")
        ),
        antonyms = listOf("shirk responsibility", "sit on the sidelines", "pass the buck"),
        usageNotes = listOf(
            "Use the full idiom ('step up to the plate') for big moments, or just 'step up' ('step up as interim lead').",
            "Note: 'step up' + object also means 'increase intensity' ('step up our security efforts')."
        )
    ),
    PhraseEntry(
        id = "gloss_over",
        term = "gloss over",
        ipa = "/ɡlɔːs ˈoʊvər/",
        category = PhraseCategory.PHRASAL_VERB,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C2,
        definition = "To treat an inconvenient fact, flaw, or complex issue superficially in order to move past it quickly.",
        nuanceSummary = "A sharp, diplomatic critique in American discussions when someone skips uncomfortable details.",
        dialogue = listOf(
            DialogueTurn("Nadia", "The quarterly deck looked upbeat, didn't it?"),
            DialogueTurn("Tariq", "A little too upbeat—they completely glossed over the shipping delays in the Midwest."),
            DialogueTurn("Nadia", "Good point. Let's ask for the real fulfillment metrics in Q&A.")
        ),
        etymology = "Combines 'gloss' (a shiny surface polish or veneer, from Scandinavian/Germanic roots) with 'over': painting a bright coat over cracks hides imperfections without fixing them.",
        synonyms = listOf(
            SynonymNuance("brush aside", "Dismisses a concern more openly and impatiently."),
            SynonymNuance("sweep under the rug", "Implies actively hiding a serious scandal or problem."),
            SynonymNuance("skim over", "Neutral; means reading or covering lightly due to time limits, not evasion.")
        ),
        antonyms = listOf("unpack", "dig into", "address head-on"),
        usageNotes = listOf(
            "Inseparable: 'gloss over the risks' (never 'gloss the risks over').",
            "Polite yet incisive in C2-level meetings: 'I don't want to gloss over the migration cost.'"
        )
    ),

    // --- WORKPLACE & PROFESSIONAL NUANCE (10) ---
    PhraseEntry(
        id = "circle_back",
        term = "circle back",
        ipa = "/ˈsɜːrkəl bæk/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To revisit a topic or follow up with someone later once more information or time is available.",
        nuanceSummary = "Quintessential American corporate shorthand—polite when paired with a concrete date, frustrating if used vaguely.",
        dialogue = listOf(
            DialogueTurn("Hannah", "Do we have final numbers from finance on the conference budget?"),
            DialogueTurn("Owen", "Not yet—let me check with Sandra and circle back to you by 3 PM."),
            DialogueTurn("Hannah", "Awesome, appreciate it.")
        ),
        etymology = "Adapted from aviation and wilderness navigation ('circling back' along a route to retrieve something left behind) into US corporate consulting jargon in the late 1990s.",
        synonyms = listOf(
            SynonymNuance("follow up", "Universally clear and slightly less buzzwordy than 'circle back'."),
            SynonymNuance("revisit", "Crisp and formal; great for meeting agendas and written notes."),
            SynonymNuance("touch base", "Focuses on a brief check-in conversation rather than returning to an open question.")
        ),
        antonyms = listOf("close the loop", "settle on the spot", "drop"),
        usageNotes = listOf(
            "Always attach a clear timeframe ('circle back on Thursday') so it sounds reliable rather than evasive.",
            "Patterns: 'circle back with [person]', 'circle back to [topic/person]', 'circle back on [issue]'."
        )
    ),
    PhraseEntry(
        id = "bandwidth",
        term = "bandwidth",
        ipa = "/ˈbændwɪdθ/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "A person's or team's available time, energy, and mental capacity to take on additional work.",
        nuanceSummary = "The gold-standard diplomatic way in American offices to say 'I am too busy' without sounding unwilling.",
        dialogue = listOf(
            DialogueTurn("Rachel", "Could you take a look at the Q3 landing page copy this afternoon?"),
            DialogueTurn("Mateo", "I don't have the bandwidth today with the launch prep, but I can review it first thing tomorrow."),
            DialogueTurn("Rachel", "Tomorrow morning works great—thanks for being upfront.")
        ),
        etymology = "Borrowed from telecommunications engineering (the maximum data transmission capacity of a channel) into Silicon Valley office English in the 1990s before spreading nationwide.",
        synonyms = listOf(
            SynonymNuance("capacity", "More formal and preferred in operations, HR, and executive planning."),
            SynonymNuance("on my plate", "Warmer, everyday idiom ('I have a lot on my plate right now')."),
            SynonymNuance("swamped", "Informal adjective signaling urgent overload.")
        ),
        antonyms = listOf("stretched thin", "at capacity", "maxed out"),
        usageNotes = listOf(
            "Standard patterns: 'have the bandwidth for X', 'limited bandwidth', 'free up bandwidth'.",
            "Saying 'I don't have the bandwidth this week, could we...' is far more professional than 'I'm too busy'."
        )
    ),
    PhraseEntry(
        id = "table_a_discussion",
        term = "table (a discussion)",
        ipa = "/ˈteɪbəl/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C2,
        definition = "In American English: to postpone or set aside a topic/proposal for later consideration.",
        nuanceSummary = "CRITICAL US vs. UK TRAP: In the US, 'table it' means PAUSE/POSTPONE; in the UK, 'table a motion' means INTRODUCE it now!",
        dialogue = listOf(
            DialogueTurn("Derek", "We only have five minutes left and still haven't finalized the vendor vote."),
            DialogueTurn("Aisha", "Let's table the logo refresh until next Tuesday so we can close out the vendor decision."),
            DialogueTurn("Derek", "Good call—I'll move it to next week's agenda.")
        ),
        etymology = "Comes from US parliamentary procedure ('lay on the table'), where placing a bill onto the clerk's table suspends debate indefinitely—opposite to British Parliament, where laying a paper on the table brings it forward for debate.",
        synonyms = listOf(
            SynonymNuance("put a pin in it", "Warm, conversational US workplace equivalent for pausing a topic briefly."),
            SynonymNuance("park it", "Informal meeting shorthand ('let's park that in the parking lot')."),
            SynonymNuance("defer", "Unambiguous formal verb safe across international US/UK teams.")
        ),
        antonyms = listOf("bring to the floor", "tackle now", "expedite"),
        usageNotes = listOf(
            "Warning for global calls with British or Commonwealth colleagues: clarify with 'postpone' or 'revisit next week' to prevent opposite interpretations!",
            "In American boardrooms, 'table' is crisp, decisive, and respected when keeping meetings on time."
        )
    ),
    PhraseEntry(
        id = "move_the_needle",
        term = "move the needle",
        ipa = "/muːv ðə ˈniːdəl/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "To create a noticeable, measurable impact on an important metric or business outcome.",
        nuanceSummary = "Separates high-leverage work from busywork—very common in product reviews and executive Q&A.",
        dialogue = listOf(
            DialogueTurn("Grant", "Should we spend two weeks tweaking the footer icon colors?"),
            DialogueTurn("Nina", "Honestly, that won't move the needle on conversion. Let's simplify checkout instead."),
            DialogueTurn("Grant", "Fair point—let's focus where the impact is.")
        ),
        etymology = "Evokes analog gauges, speedometers, and studio VU meters: a tiny tweak barely jiggles the dial, whereas meaningful action visibly pushes the indicator needle upward.",
        synonyms = listOf(
            SynonymNuance("make a dent in", "Usually used for reducing a huge backlog or problem."),
            SynonymNuance("drive measurable impact", "Formal alternative for written business cases."),
            SynonymNuance("be a game-changer", "Stronger hype; implies a transformative breakthrough.")
        ),
        antonyms = listOf("be a drop in the bucket", "spin your wheels", "marginal tweak"),
        usageNotes = listOf(
            "Often used in questions or negatives to challenge low-impact ideas: 'Will this actually move the needle on retention?'",
            "Pair with 'on + metric' ('move the needle on revenue / engagement')."
        )
    ),
    PhraseEntry(
        id = "take_it_offline",
        term = "take it offline",
        ipa = "/teɪk ɪt ˌɔːfˈlaɪn/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "To pause a detailed side discussion during a group meeting and continue it privately afterward with only the relevant people.",
        nuanceSummary = "Does NOT mean 'without the internet'—it means 'outside this group meeting' (even over Slack or a 1:1 Zoom!).",
        dialogue = listOf(
            DialogueTurn("Ben", "Wait—does the billing webhook retry on 429 errors or exponential backoff?"),
            DialogueTurn("Tessa", "Great question, Ben—let's take that offline right after standup so we don't hold up the designers."),
            DialogueTurn("Ben", "Sounds good, I'll message you on Slack.")
        ),
        etymology = "Adapted from 1980s computing terminology ('offline' = disconnected from the main active system) into American corporate meeting etiquette during the 1990s.",
        synonyms = listOf(
            SynonymNuance("follow up 1:1", "Direct and friendly modern phrasing."),
            SynonymNuance("sync separately", "Common in remote US tech teams."),
            SynonymNuance("sidebar", "Used as a noun or verb ('quick sidebar after this') for a brief side chat.")
        ),
        antonyms = listOf("hash out in the room", "keep on the main agenda", "escalate to the group"),
        usageNotes = listOf(
            "A respectful meeting-facilitation tool—frame it around protecting everyone's time, not shutting someone down.",
            "Remember: 'taking it offline' usually happens online (Slack DM or smaller video call)!"
        )
    ),
    PhraseEntry(
        id = "boil_the_ocean",
        term = "boil the ocean",
        ipa = "/bɔɪl ði ˈoʊʃən/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C2,
        definition = "To attempt an impossibly vast, overly ambitious, or poorly scoped undertaking that wastes time and effort.",
        nuanceSummary = "A diplomatic, memorable way to push back against scope creep without attacking a colleague's enthusiasm.",
        dialogue = listOf(
            DialogueTurn("Amara", "For the Q2 pilot, we could redesign billing, search, analytics, and mobile push all at once."),
            DialogueTurn("Victor", "Let's not try to boil the ocean—if we nail search first, we can ship in six weeks."),
            DialogueTurn("Amara", "You're right. Search alone solves 70% of support tickets.")
        ),
        etymology = "Attributed to humorist Will Rogers during World War I (who joked the Navy could defeat U-boats by boiling the ocean) and popularized by American management consultants in the late 20th century.",
        synonyms = listOf(
            SynonymNuance("over-engineer", "Specifically refers to adding unnecessary technical complexity."),
            SynonymNuance("bite off more than you can chew", "More personal and everyday; less specific to project scoping."),
            SynonymNuance("scope creep", "Noun describing gradual, unplanned expansion of project requirements.")
        ),
        antonyms = listOf("start small", "ship an MVP", "focus on quick wins"),
        usageNotes = listOf(
            "Almost always used in the negative ('Let's not boil the ocean', 'We don't need to boil the ocean here').",
            "Particularly resonant in American tech, product, and strategy teams."
        )
    ),
    PhraseEntry(
        id = "low_hanging_fruit",
        term = "low-hanging fruit",
        ipa = "/ˌloʊˈhæŋɪŋ fruːt/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.B2,
        definition = "The easiest, quickest wins or tasks that deliver immediate value with minimal effort.",
        nuanceSummary = "Signals pragmatic prioritization—grabbing fast improvements before tackling harder structural work.",
        dialogue = listOf(
            DialogueTurn("Sofia", "Our page load time is up to four seconds—where should we start?"),
            DialogueTurn("Daniel", "Compressing those uncompressed hero images is the low-hanging fruit. We can ship that today."),
            DialogueTurn("Sofia", "Love it—quick win first, then we profile the bundle.")
        ),
        etymology = "An orchard metaphor entering American business writing in the 1960s: fruit on lower branches can be harvested effortlessly without ladders.",
        synonyms = listOf(
            SynonymNuance("quick wins", "Crisp, modern alternative if you want to avoid corporate clichés."),
            SynonymNuance("easy pickings", "Casual and slightly predatory—not suitable for constructive team planning."),
            SynonymNuance("no-brainer fixes", "More conversational.")
        ),
        antonyms = listOf("heavy lift", "uphill battle", "moonshot"),
        usageNotes = listOf(
            "Usually treated as an uncountable noun ('plenty of low-hanging fruit', not 'many low-hanging fruits').",
            "Frequently contrasted with 'heavy lift' (a high-effort, complex initiative)."
        )
    ),
    PhraseEntry(
        id = "stakeholder_buy_in",
        term = "buy-in",
        ipa = "/ˈbaɪ ɪn/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "Genuine agreement, commitment, and active support from key decision-makers or teammates for a proposal.",
        nuanceSummary = "Goes deeper than passive permission—means stakeholders believe in the plan and will back it.",
        dialogue = listOf(
            DialogueTurn("Jenna", "We're ready to roll out the new support escalation policy next Monday."),
            DialogueTurn("Omar", "Have you gotten buy-in from the regional managers yet? Without them, adoption will stall."),
            DialogueTurn("Jenna", "Good point—I'll walk them through the draft tomorrow.")
        ),
        etymology = "Originated in poker (paying the 'buy-in' chips to join a game, giving you a real stake in the outcome) and entered US corporate leadership vocabulary in the 1980s.",
        synonyms = listOf(
            SynonymNuance("alignment", "Emphasizes shared understanding and coordinated direction across teams."),
            SynonymNuance("green light", "Informal idiom meaning formal approval to start—less about emotional enthusiasm."),
            SynonymNuance("sign-off", "Formal procedural approval, often documented in writing.")
        ),
        antonyms = listOf("pushback", "resistance", "siloed decision"),
        usageNotes = listOf(
            "Always hyphenated as a noun ('get buy-in from leadership', 'build stakeholder buy-in').",
            "As a two-word verb without a hyphen, 'buy in / buy into' means to believe or accept an idea."
        )
    ),
    PhraseEntry(
        id = "pushback",
        term = "pushback",
        ipa = "/ˈpʊʃbæk/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "Constructive resistance, skepticism, or objection to a proposal, deadline, or policy.",
        nuanceSummary = "Polite, non-confrontational American business term—lets you discuss disagreement calmly without sounding combative.",
        dialogue = listOf(
            DialogueTurn("Trevor", "How did engineering react to moving the beta launch up by two weeks?"),
            DialogueTurn("Paula", "We got some fair pushback on testing coverage, so we cut two secondary features."),
            DialogueTurn("Trevor", "Glad they spoke up early.")
        ),
        etymology = "Originally an aviation term for tugs pushing aircraft away from airport gates; adopted in 1990s US journalism and corporate culture for diplomatic opposition.",
        synonyms = listOf(
            SynonymNuance("objection", "More formal and legalistic."),
            SynonymNuance("friction", "Describes systemic or process slowdowns rather than spoken disagreement."),
            SynonymNuance("backlash", "Much stronger—implies widespread angry public or employee reaction.")
        ),
        antonyms = listOf("buy-in", "enthusiastic support", "smooth sailing"),
        usageNotes = listOf(
            "Uncountable noun as one word ('expect some pushback', 'handle pushback'); verb is two words ('push back on a deadline').",
            "Encouraging healthy pushback ('I'd love your candid pushback on this') signals psychological safety in US teams."
        )
    ),
    PhraseEntry(
        id = "heavy_lift",
        term = "heavy lift",
        ipa = "/ˈhɛvi lɪft/",
        category = PhraseCategory.WORKPLACE,
        register = PhraseRegister.PROFESSIONAL,
        cefrLevel = CefrLevel.C1,
        definition = "A task or project requiring substantial effort, coordination, or technical resources to execute.",
        nuanceSummary = "Helps teams transparently negotiate realistic timelines without sounding like they are complaining.",
        dialogue = listOf(
            DialogueTurn("Hector", "Can we support real-time offline sync before the conference demo?"),
            DialogueTurn("Chloe", "Real-time conflict resolution is a pretty heavy lift—it's at least a six-week effort."),
            DialogueTurn("Hector", "Got it, let's stick to read-only caching for the demo then.")
        ),
        etymology = "Borrowed from aerospace and industrial crane engineering ('heavy-lift launch vehicles' built to carry massive payloads) into American management speech in the 2000s.",
        synonyms = listOf(
            SynonymNuance("major undertaking", "Formal, classic phrasing for reports and grant proposals."),
            SynonymNuance("heavy lifting", "Refers to the hard work itself ('the compiler does the heavy lifting')."),
            SynonymNuance("tall order", "Conversational idiom for a demanding, hard-to-meet request.")
        ),
        antonyms = listOf("low-hanging fruit", "light lift", "quick win"),
        usageNotes = listOf(
            "Countable noun: 'a heavy lift' vs. 'a light lift' (an easy, low-effort task).",
            "Distinguish 'a heavy lift' (a big project) from 'do the heavy lifting' (perform the hardest share of the work)."
        )
    )
)
