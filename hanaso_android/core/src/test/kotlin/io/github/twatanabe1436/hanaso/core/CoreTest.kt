package io.github.twatanabe1436.hanaso.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun scenariosAreComplete() {
        assertEquals(16, Catalog.scenarios.size)
        assertEquals(Catalog.scenarios.size, Catalog.scenarios.map { it.id }.toSet().size)
        for (s in Catalog.scenarios) {
            assertNotNull(s.id, s.level)
            listOf(s.titleJa, s.setting, s.aiName, s.aiRole, s.userRole, s.userRoleJa, s.opener, s.descriptionJa)
                .forEach { assertTrue("${s.id} に空の項目がある", it.isNotBlank()) }
            assertTrue(s.id, s.missions.isNotEmpty())
            assertEquals("${s.id}: mission id 重複", s.missions.size, s.missions.map { it.id }.toSet().size)
            assertTrue(s.keyPhrases.all { it.en.isNotBlank() && it.ja.isNotBlank() })
        }
    }

    @Test
    fun everyLevelHasRolePlays() {
        val byLevel = Catalog.scenarios.groupBy { it.level }
        for (level in Level.entries) assertTrue("$level のシナリオが少ない", byLevel[level].orEmpty().size >= 2)
    }

    @Test
    fun levelsAndLegacyNames() {
        assertEquals(listOf("A1", "A2", "B1", "B2", "C1", "C2"), Level.entries.map { it.name })
        assertEquals("B2 中上級", Level.B2.displayJa)
        assertEquals('C', Level.C2.band)
        // 0.2.0 までの 3 段階の保存値
        assertEquals(Level.A2, Level.fromName("BEGINNER"))
        assertEquals(Level.B1, Level.fromName("INTERMEDIATE"))
        assertEquals(Level.C1, Level.fromName("ADVANCED"))
        assertEquals(Level.C2, Level.fromName("C2"))
        assertEquals(Level.DEFAULT, Level.fromName(null))
        assertTrue(Level.entries.all { it.label.startsWith("CEFR ${it.name}") && it.partnerGuide.isNotBlank() })
    }

    @Test
    fun freeTalks() {
        assertEquals(6, Catalog.freeTalks.size)
        val free = Catalog.find("free:hobbies")!!
        assertTrue(free.isFreeTalk)
        assertEquals("Alex", free.aiName)
        assertTrue(free.missions.isEmpty())
        assertNull(Catalog.find("free:nope"))
        assertNull(Catalog.find("nope"))
    }
}

class PromptsTest {
    private val cafe = Catalog.find("cafe")!!

    @Test
    fun partnerSystemIncludesRoleLevelAndGoals() {
        val sys = Prompts.partnerSystem(cafe, Level.A2)
        assertTrue(sys.contains(cafe.aiRole))
        assertTrue(sys.contains(Level.A2.partnerGuide))
        assertTrue(sys.contains("- [drink] ドリンクをサイズ付きで注文する"))
        assertTrue(sys.contains("text-to-speech"))
        assertFalse("trimMargin の | が残っていない", sys.lines().any { it.trimStart().startsWith("|") })

        val free = Prompts.partnerSystem(Catalog.find("free:free")!!, Level.C2)
        assertTrue(free.contains("free conversation"))
        assertTrue(free.contains(Level.C2.partnerGuide))
    }

    @Test
    fun partnerMessagesStartWithStartSignal() {
        val msgs = Prompts.partnerMessages(listOf(Line(Speaker.AI, "Hi!"), Line(Speaker.LEARNER, "hello")))
        assertEquals(listOf(Line(Speaker.LEARNER, "[start]"), Line(Speaker.AI, "Hi!"), Line(Speaker.LEARNER, "hello")), msgs)
    }

    @Test
    fun feedbackHintAndSummaryPrompts() {
        val hotel = Catalog.find("hotel")!!
        val c = Conversation(hotel, Level.B1, listOf(Line(Speaker.AI, hotel.opener), Line(Speaker.LEARNER, "yes i have reservation")))
        val fb = Prompts.feedbackPrompt(c)
        assertTrue(fb.contains("<utterance_to_review>\nyes i have reservation\n</utterance_to_review>"))
        assertTrue(fb.contains("${hotel.aiName}: ${hotel.opener}"))
        assertTrue(fb.contains("Learner: yes i have reservation"))
        assertTrue(fb.startsWith("<scene>"))

        assertFalse(Prompts.hintPrompt(c, null).contains("<learner_wants_to_say>"))
        assertTrue(Prompts.hintPrompt(c, "朝食は何時？").contains("朝食は何時？"))

        val sum = Prompts.summaryPrompt(c, setOf("checkin"))
        assertTrue(sum.contains("- [x] 予約名を伝えてチェックインする"))
        assertTrue(sum.contains("- [ ] 朝食の時間や場所を聞く"))
    }
}

class TextTest {
    @Test
    fun chunkerEmitsCompletedSentences() {
        val c = SentenceChunker()
        assertEquals(listOf("Hi there!"), c.push("Hi there! How are"))
        assertEquals(listOf("How are you today?"), c.push(" you today? I"))
        assertEquals(emptyList<String>(), c.push("'m Jamie"))
        assertEquals(listOf("I'm Jamie"), c.flush())
        assertEquals(emptyList<String>(), c.flush())
    }

    @Test
    fun chunkerKeepsAbbreviations() {
        assertEquals(
            listOf("Hi, I'm Dr. Lee.", "So, what brings you in?"),
            SentenceChunker().push("Hi, I'm Dr. Lee. So, what brings you in? "),
        )
    }

    @Test
    fun perfectMatchIs100() {
        val r = SpeechScorer.score("Could you make it with oat milk?", "could you make it with oat milk")
        assertEquals(100, r.score)
        assertTrue(r.words.all { it.ok })
    }

    @Test
    fun missedWordsAreMarked() {
        val r = SpeechScorer.score("I'd like a medium latte, please.", "I would like a latte please")
        assertEquals(listOf(true, true, true, false, true, true), r.words.map { it.ok })
        assertTrue(r.score in 71..99)
    }

    @Test
    fun contractionsAndNumbersAreEquivalent() {
        assertEquals(100, SpeechScorer.score("I'm staying for 2 weeks.", "I am staying for two weeks").score)
        assertEquals(100, SpeechScorer.score("I can't go.", "I cannot go").score)
        assertEquals(0, SpeechScorer.score("Hello there.", "").score)
    }

    @Test
    fun sameSentenceAndJapaneseDetection() {
        assertTrue(SpeechScorer.sameSentence("i want coffee", "I want coffee."))
        assertFalse(SpeechScorer.sameSentence("I want coffee.", "I'd like coffee."))
        assertTrue("砂糖なしで".hasJapanese())
        assertTrue("カフェ".hasJapanese())
        assertFalse("I'd like a latte.".hasJapanese())
    }
}

class MockEngineTest {
    private val cafe = Catalog.find("cafe")!!
    private val engine = MockEngine(delayMs = 0)

    @Test
    fun demoConversation() = runBlocking {
        val c = Conversation(cafe, Level.A2, listOf(Line(Speaker.AI, cafe.opener), Line(Speaker.LEARNER, "i want a latte")))
        assertEquals("I see! Could you tell me a little more about that?", engine.reply(c).toList().joinToString(""))
        val fb = engine.feedback(c)
        assertEquals(Rating.FIX, fb.rating)
        assertEquals("I'd like a latte.", fb.natural)
        assertEquals(listOf("drink"), fb.completedMissions)
        assertEquals(3, engine.hint(c).size)
        val summary = engine.summary(c, setOf("drink"))
        assertTrue(summary.score in 0..100)
        assertEquals(cafe.keyPhrases, summary.keyPhrases)
    }
}
