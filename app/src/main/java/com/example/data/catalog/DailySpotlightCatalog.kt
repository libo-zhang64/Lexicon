package com.example.data.catalog

import java.util.Calendar
import java.util.Locale

data class DailySpotlightEntry(
    val id: String,
    val term: String,
    val ipa: String,
    val partOfSpeech: String,
    val tagline: String,
    val definition: String,
    val example: String,
    val funFact: String
)

/**
 * Curated bonus "Word / Phrase of the Day" discoveries intentionally kept outside
 * the 1,000-entry learning curriculum so learners encounter a fresh cultural curiosity daily.
 */
object DailySpotlightCatalog {
    val entries: List<DailySpotlightEntry> = listOf(
        DailySpotlightEntry(
            id = "spot_boondoggle",
            term = "boondoggle",
            ipa = "/ˈbuːndɑːɡəl/",
            partOfSpeech = "noun",
            tagline = "American Political & Office Lore",
            definition = "An expensive, showy, or time-consuming project that delivers little or no real value.",
            example = "\"The committee's $2 million software rebrand turned into a classic boondoggle.\"",
            funFact = "Coined in 1929 by Boy Scout leader Robert Link for braided leather lanyards; New York reporters popularized it in 1935 to mock make-work projects."
        ),
        DailySpotlightEntry(
            id = "spot_persnickety",
            term = "persnickety",
            ipa = "/pərˈsnɪkəti/",
            partOfSpeech = "adjective",
            tagline = "Colorful Everyday American English",
            definition = "Overly fussy about tiny, trivial details; hard to please.",
            example = "\"Our copy editor is wonderfully persnickety about Oxford commas and em-dashes.\"",
            funFact = "An 1890s American remodeling of the Scottish word 'pernickety', adding an extra 's' that gave it its snappy US rhythm."
        ),
        DailySpotlightEntry(
            id = "spot_skedaddle",
            term = "skedaddle",
            ipa = "/skɪˈdædəl/",
            partOfSpeech = "verb",
            tagline = "Playful Americana",
            definition = "To depart hurriedly or run off in a lively rush.",
            example = "\"Storm clouds are rolling in—let's grab the picnic blanket and skedaddle.\"",
            funFact = "Surged into American newspapers during the 1861 Civil War as playful slang for beating a hasty retreat."
        ),
        DailySpotlightEntry(
            id = "spot_kerfuffle",
            term = "kerfuffle",
            ipa = "/kərˈfʌfəl/",
            partOfSpeech = "noun",
            tagline = "Polite Understatement",
            definition = "A noisy commotion, fuss, or minor public disagreement over something relatively small.",
            example = "\"There was a brief kerfuffle over parking passes, but facilities sorted it out by noon.\"",
            funFact = "Evolved from Scottish 'curfuffle' (to ruffle or muss) and became a beloved diplomatic word across North America."
        ),
        DailySpotlightEntry(
            id = "spot_discombobulated",
            term = "discombobulated",
            ipa = "/ˌdɪskəmˈbɑːbjəleɪtɪd/",
            partOfSpeech = "adjective",
            tagline = "19th-Century American Invention",
            definition = "Thoroughly confused, flustered, or thrown off balance.",
            example = "\"After flying across three time zones, I felt completely discombobulated at breakfast.\"",
            funFact = "Invented purely for comedic effect in the 1830s US press as a mock-Latin exaggeration of 'discompose'."
        ),
        DailySpotlightEntry(
            id = "spot_shenanigans",
            term = "shenanigans",
            ipa = "/ʃəˈnænɪɡənz/",
            partOfSpeech = "noun (plural)",
            tagline = "Playful Mischief",
            definition = "Secret, silly, or slightly sneaky mischief, pranks, or questionable behavior.",
            example = "\"No more late-night Slack shenanigans—we have a client demo at 8 AM.\"",
            funFact = "First recorded in 1850s San Francisco gold-rush newspapers; linguists still debate whether it stems from Irish 'sionnachuighim' (to play the fox)."
        ),
        DailySpotlightEntry(
            id = "spot_lollygag",
            term = "lollygag",
            ipa = "/ˈlɑːliɡæɡ/",
            partOfSpeech = "verb",
            tagline = "Classic American Vernacular",
            definition = "To dawdle, waste time aimlessly, or move at a painfully slow pace.",
            example = "\"Stop lollygagging by the gate—our boarding group was just called!\"",
            funFact = "First appeared in mid-1800s US newspapers; famously immortalized in the baseball movie 'Bull Durham'."
        ),
        DailySpotlightEntry(
            id = "spot_finagle",
            term = "finagle",
            ipa = "/fɪˈneɪɡəl/",
            partOfSpeech = "verb",
            tagline = "Clever Resourcefulness",
            definition = "To obtain or arrange something through clever persuasion, improvisation, or slightly sneaky maneuvering.",
            example = "\"She somehow finagled two last-minute front-row tickets to the sold-out show.\"",
            funFact = "Entered American English around 1920 from an older English dialect word 'fainaigue' meaning to cheat at cards."
        ),
        DailySpotlightEntry(
            id = "spot_gobbledygook",
            term = "gobbledygook",
            ipa = "/ˈɡɑːbəldiɡʊk/",
            partOfSpeech = "noun",
            tagline = "Anti-Jargon Favorite",
            definition = "Pompous, overly technical, or bureaucratic language that sounds impressive yet means almost nothing.",
            example = "\"Cut the corporate gobbledygook and tell customers in one sentence what the app does.\"",
            funFact = "Coined in 1944 by Texas Congressman Maury Maverick, who said bloated federal memos reminded him of the gobbling sound of turkeys."
        ),
        DailySpotlightEntry(
            id = "spot_hullabaloo",
            term = "hullabaloo",
            ipa = "/ˌhʌləbəˈluː/",
            partOfSpeech = "noun",
            tagline = "Expressive Sound Word",
            definition = "A loud uproar, commotion, or dramatic public fuss.",
            example = "\"All that hullabaloo online was just about a tiny font change in the app header.\"",
            funFact = "Dates to 18th-century reduplicative rhyming slang—built purely to sound chaotic when spoken aloud."
        ),
        DailySpotlightEntry(
            id = "spot_bamboozle",
            term = "bamboozle",
            ipa = "/bæmˈbuːzəl/",
            partOfSpeech = "verb",
            tagline = "Vivid Everyday Verb",
            definition = "To trick, hoodwink, or completely puzzle someone with clever misdirection.",
            example = "\"Don't let the fine print bamboozle you into signing a three-year lock-in.\"",
            funFact = "Jonathan Swift hated this slang word in 1710, calling it a barbarous invention—yet it survived 300+ years to thrive in American English."
        ),
        DailySpotlightEntry(
            id = "spot_rigmarole",
            term = "rigmarole",
            ipa = "/ˈrɪɡməroʊl/",
            partOfSpeech = "noun",
            tagline = "Bureaucracy & Storytelling",
            definition = "A long, tedious, unnecessarily complicated procedure or rambling story.",
            example = "\"Renewing a parking permit shouldn't involve an hour of paperwork rigmarole.\"",
            funFact = "Descends from medieval 'Ragman Roll', a 1296 parchment scroll listing thousands of Scottish nobles' pledges."
        ),
        DailySpotlightEntry(
            id = "spot_schmooze",
            term = "schmooze",
            ipa = "/ʃmuːz/",
            partOfSpeech = "verb",
            tagline = "Networking Culture",
            definition = "To chat warmly and casually at a social or industry event, especially to build helpful connections.",
            example = "\"Spend twenty minutes after the keynote schmoozing with the speakers.\"",
            funFact = "Borrowed into American English via New York Yiddish 'shmuesn' (to converse), itself from Hebrew 'shemuot' (news/tidings)."
        ),
        DailySpotlightEntry(
            id = "spot_verklempt",
            term = "verklempt",
            ipa = "/fərˈklɛmpt/",
            partOfSpeech = "adjective",
            tagline = "Emotional Nuance",
            definition = "Overcome with tender emotion; choked up and momentarily unable to speak.",
            example = "\"Hearing the whole room cheer during her retirement toast left her a little verklempt.\"",
            funFact = "Entered mainstream US pop culture from Yiddish ('farklemt', literally 'clamped/squeezed') via classic Saturday Night Live sketches."
        ),
        DailySpotlightEntry(
            id = "spot_catawampus",
            term = "catawampus",
            ipa = "/ˌkætəˈwɑːmpəs/",
            partOfSpeech = "adjective",
            tagline = "Regional American Charm",
            definition = "Askew, crooked, diagonal, or hilariously out of alignment.",
            example = "\"After moving the bookshelf, the framed poster ended up completely catawampus.\"",
            funFact = "A playful 19th-century Southern and Midwestern American coinage originally describing an imaginary fierce wildcat."
        ),
        DailySpotlightEntry(
            id = "spot_hunky_dory",
            term = "hunky-dory",
            ipa = "/ˌhʌŋkiˈdɔːri/",
            partOfSpeech = "adjective",
            tagline = "Vintage US Optimism",
            definition = "Completely fine, smooth, and satisfactory (often used with gentle irony).",
            example = "\"Everyone thought the launch was hunky-dory until traffic tripled at midnight.\"",
            funFact = "Popularized in 1860s New York; 'hunky' came from Dutch-American kids' street games meaning 'safe at home base'."
        ),
        DailySpotlightEntry(
            id = "spot_flabbergasted",
            term = "flabbergasted",
            ipa = "/ˈflæbərɡæstɪd/",
            partOfSpeech = "adjective",
            tagline = "High-Impact Surprise",
            definition = "Utterly astonished or left speechless by something unexpected.",
            example = "\"I was flabbergasted when the tiny neighborhood bakery won a national James Beard award.\"",
            funFact = "First noted in a 1772 magazine listing trendy new British slang—likely blending 'flabby' and 'aghast'."
        ),
        DailySpotlightEntry(
            id = "spot_ramshackle",
            term = "ramshackle",
            ipa = "/ˈræmʃækəl/",
            partOfSpeech = "adjective",
            tagline = "Vivid Descriptive Prose",
            definition = "Rickety, poorly maintained, or hastily pieced together as if ready to fall apart.",
            example = "\"We replaced their ramshackle spreadsheet workflow with a clean automated tracker.\"",
            funFact = "Evolved from 'ransackle' (an old variant of 'ransack'), picturing a house left battered after being searched."
        ),
        DailySpotlightEntry(
            id = "spot_dillydally",
            term = "dillydally",
            ipa = "/ˈdɪlidæli/",
            partOfSpeech = "verb",
            tagline = "Rhythmic Everyday Speech",
            definition = "To waste time through indecision, hesitation, or leisurely wandering.",
            example = "\"Don't dillydally on accepting the offer—they have two other finalists waiting.\"",
            funFact = "A classic ablaut reduplication (switching 'i' to 'a', just like zigzag and chit-chat) recorded since the 1740s."
        ),
        DailySpotlightEntry(
            id = "spot_brouhaha",
            term = "brouhaha",
            ipa = "/ˈbruːhɑːhɑː/",
            partOfSpeech = "noun",
            tagline = "Editorial Journalism Staple",
            definition = "A noisy, overblown public uproar or media controversy.",
            example = "\"The airline quietly reversed the fee change after a weekend brouhaha on social media.\"",
            funFact = "Borrowed from 16th-century French theatrical slang—used for the offstage roar of an excited crowd."
        )
    )

    fun spotlightForOffset(dayOffset: Int = 0): DailySpotlightEntry {
        val dayOfYear = Calendar.getInstance(Locale.US).get(Calendar.DAY_OF_YEAR)
        val idx = ((dayOfYear + dayOffset) % entries.size + entries.size) % entries.size
        return entries[idx]
    }
}
