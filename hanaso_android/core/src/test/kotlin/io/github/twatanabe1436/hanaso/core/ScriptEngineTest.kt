package io.github.twatanabe1436.hanaso.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptsTest {
    private val everyScenario = Catalog.scenarios + Catalog.freeTalks

    @Test
    fun everyScenarioHasAScript() {
        assertEquals(everyScenario.map { it.id }.toSet(), Scripts.all.keys)
        for (s in everyScenario) {
            val script = Scripts.forScenario(s)
            assertTrue("${s.id}: 日本語訳", script.openerJa.hasJapanese())
            assertTrue("${s.id}: お題が少ない", script.steps.size >= 4)
            for (step in script.steps) {
                val where = "${s.id} / ${step.taskJa}"
                assertTrue(where, step.taskJa.hasJapanese())
                assertTrue(where, step.answers.isNotEmpty())
                assertTrue(where, step.answers.all { it.en.isNotBlank() && !it.en.hasJapanese() && it.ja.hasJapanese() })
                assertTrue(where, step.reply.en.isNotBlank() && !step.reply.en.hasJapanese() && step.reply.ja.hasJapanese())
                assertTrue("$where: 自由回答でなければキーワードが必要", step.open || step.keywords.isNotEmpty())
                assertTrue("$where: 自由回答にキーワードは使わない", !step.open || step.keywords.isEmpty())
            }
        }
    }

    @Test
    fun missionsAreCoveredByTheScript() {
        for (s in Catalog.scenarios) {
            val stepMissions = Scripts.forScenario(s).steps.mapNotNull { it.mission }.toSet()
            assertEquals(s.id, s.missions.map { it.id }.toSet(), stepMissions)
        }
        for (s in Catalog.freeTalks) {
            assertTrue(s.id, Scripts.forScenario(s).steps.all { it.mission == null })
        }
    }

    @Test
    fun everyModelAnswerPassesItsOwnStep() {
        for (s in everyScenario) {
            for (step in Scripts.forScenario(s).steps) {
                for (answer in step.answers) {
                    val where = "${s.id} / ${step.taskJa} / ${answer.en}"
                    val j = ScriptEngine.judge(step, answer.en)
                    assertTrue(where, j.passed)
                    if (!step.open) {
                        assertTrue("$where: キーワード", ScriptEngine.keywordsSatisfied(step.keywords, answer.en))
                        assertEquals(where, Rating.GREAT, j.rating)
                        assertEquals(where, 100, j.match!!.score)
                    }
                }
            }
        }
    }

    @Test
    fun everyScriptLineCanBeTranslated() {
        for (s in everyScenario) {
            val script = Scripts.forScenario(s)
            assertEquals(s.id, script.openerJa, ScriptEngine.lookupJa(s.opener))
            for (step in script.steps) {
                assertNotNull(step.reply.en, ScriptEngine.lookupJa(step.reply.en))
                step.answers.forEach { assertNotNull(it.en, ScriptEngine.lookupJa(it.en)) }
            }
        }
    }
}

class ScriptEngineTest {
    private val engine = ScriptEngine(delayMs = 0)
    private val cafe = Catalog.find("cafe")!!
    private val cafeSteps = Scripts.forScenario(cafe).steps

    private fun talk(scenario: Scenario, vararg said: String): Conversation {
        val history = mutableListOf(Line(Speaker.AI, scenario.opener))
        for (text in said) {
            history += Line(Speaker.LEARNER, text)
            history += Line(Speaker.AI, engine.nextLine(Conversation(scenario, Level.BEGINNER, history)).en)
        }
        return Conversation(scenario, Level.BEGINNER, history)
    }

    /** 最後の AI のセリフを外して「学習者が話した直後」にする */
    private fun Conversation.beforeReply() = copy(history = history.dropLast(1))

    @Test
    fun keywordMatching() {
        assertTrue(ScriptEngine.keywordsSatisfied(listOf("latte|coffee", "medium|large"), "One large coffee, please"))
        assertFalse(ScriptEngine.keywordsSatisfied(listOf("latte|coffee", "medium|large"), "One coffee, please"))
        // 複数語・前方一致・短縮形・数字・時刻
        assertTrue(ScriptEngine.keywordsSatisfied(listOf("how long"), "How LONG does it take?"))
        assertFalse(ScriptEngine.keywordsSatisfied(listOf("how long"), "How is it? Long?"))
        assertTrue(ScriptEngine.keywordsSatisfied(listOf("recommend*"), "Which one is recommended?"))
        assertTrue(ScriptEngine.keywordsSatisfied(listOf("why don't we"), "Why do not we try it?"))
        assertTrue(ScriptEngine.keywordsSatisfied(listOf("two|o'clock"), "Let's meet at 2:00"))
        assertFalse(ScriptEngine.keywordsSatisfied(listOf("ac"), "The A/C is broken"))
        assertTrue(ScriptEngine.keywordsSatisfied(listOf("ac|a c"), "the a/c is broken"))
    }

    @Test
    fun judgeTargetStep() {
        val drink = cafeSteps[0]
        // 自分なりの言い方でも、キーワード (ドリンク + サイズ) があれば合格
        val own = ScriptEngine.judge(drink, "one large cappuccino")
        assertTrue(own.passed)
        assertEquals(Rating.GOOD, own.rating)
        // 形は似ていても、肝心の内容がなければ不合格
        val empty = ScriptEngine.judge(drink, "Can I get a, please?")
        assertFalse(empty.passed)
        assertEquals(Rating.FIX, empty.rating)
        // いちばん近いお手本と比べる
        // 音声認識でよくある、句読点なし・短い言い方
        val hotel = Scripts.forScenario(Catalog.find("hotel")!!).steps
        assertTrue(ScriptEngine.judge(hotel[0], "check in please tanaka").passed)
        assertTrue(ScriptEngine.judge(hotel[3], "the AC is not working").passed)
        assertTrue(ScriptEngine.judge(Scripts.forScenario(Catalog.find("weekend")!!).steps[1], "2:30").passed)
        assertFalse(ScriptEngine.judge(drink, "i want coffee").passed) // サイズがない

        val close = ScriptEngine.judge(drink, "i'd like a medium latte")
        assertEquals("I'd like a medium latte, please.", close.closest.en)
        assertEquals(Rating.GREAT, close.rating)
        assertEquals(listOf(true, true, true, true, true, false), close.match!!.words.map { it.ok })
    }

    @Test
    fun judgeOpenStep() {
        val step = Scripts.forScenario(Catalog.find("free:hobbies")!!).steps[0]
        assertFalse(ScriptEngine.judge(step, "Guitar.").passed)
        assertEquals(Rating.GOOD, ScriptEngine.judge(step, "I like guitar.").rating)
        assertEquals(Rating.GREAT, ScriptEngine.judge(step, "I like playing the guitar with my friends.").rating)
        // 例と同じなら短くても Great
        assertEquals(Rating.GREAT, ScriptEngine.judge(step, "I like playing the guitar.").rating)
    }

    @Test
    fun correctAnswerAdvancesWithTheScriptedReply() = runBlocking {
        val c = talk(cafe, "Can I get a medium latte, please?")
        assertEquals(cafeSteps[0].reply.en, c.history.last().text)
        assertEquals(cafeSteps[0].reply.en, engine.reply(c.beforeReply()).toList().joinToString(""))

        val fb = engine.feedback(c.beforeReply())
        assertEquals(Rating.GREAT, fb.rating)
        assertEquals(listOf("drink"), fb.completedMissions)
        assertEquals(100, fb.matchScore!!.score)
        assertTrue(fb.explanationJa.contains("💡"))

        val task = engine.task(c)
        assertEquals(2, task.number)
        assertEquals(cafeSteps.size, task.total)
        assertEquals(cafeSteps[1].taskJa, task.taskJa)
        assertEquals(0, task.retries)
        assertFalse(task.finished)
    }

    @Test
    fun wrongAnswerAsksAgainThenMovesOn() = runBlocking {
        val once = talk(cafe, "Hello")
        assertEquals(ScriptEngine.RETRY_LINES[0].en, once.history.last().text)
        assertEquals(1, engine.task(once).number)
        assertEquals(1, engine.task(once).retries)
        val fb = engine.feedback(once.beforeReply())
        assertEquals(Rating.FIX, fb.rating)
        assertEquals(cafeSteps[0].answers[0].en, fb.natural)
        assertTrue(fb.completedMissions.isEmpty())

        val twice = talk(cafe, "Hello", "Good morning")
        assertEquals(ScriptEngine.RETRY_LINES[1].en, twice.history.last().text)
        assertEquals(2, engine.task(twice).retries)

        // 言い直しを使い切ると、お手本を見せて次のお題へ (ミッションは未達成のまま)
        val thrice = talk(cafe, "Hello", "Good morning", "Nice weather")
        assertEquals(cafeSteps[0].reply.en, thrice.history.last().text)
        assertEquals(2, engine.task(thrice).number)
        val last = engine.feedback(thrice.beforeReply())
        assertEquals(Rating.FIX, last.rating)
        assertTrue(last.explanationJa.contains("次のお題に進みます"))
        assertTrue(last.completedMissions.isEmpty())

        // 言い直してから正解すれば達成
        val retried = talk(cafe, "Hello", "A medium latte, please.")
        assertEquals(listOf("drink"), engine.feedback(retried.beforeReply()).completedMissions)
        assertEquals(2, engine.task(retried).number)
    }

    @Test
    fun openStepAsksForMore() {
        val free = Catalog.find("free:today")!!
        val c = talk(free, "Nothing.")
        assertEquals(ScriptEngine.OPEN_RETRY_LINES[0].en, c.history.last().text)
        val ok = talk(free, "Nothing.", "I went to the park with my dog.")
        assertEquals(Scripts.forScenario(free).steps[0].reply.en, ok.history.last().text)
    }

    @Test
    fun fullPlaythroughOfEveryScript() = runBlocking {
        for (s in Catalog.scenarios + Catalog.freeTalks) {
            val steps = Scripts.forScenario(s).steps
            val c = talk(s, *steps.map { it.answers.first().en }.toTypedArray())
            assertEquals(s.id, steps.last().reply.en, c.history.last().text)
            val task = engine.task(c)
            assertTrue(s.id, task.finished)
            assertEquals(steps.size, task.number)
            val fb = engine.feedback(c.beforeReply())
            assertEquals(s.id, s.missions.map { it.id }.toSet(), fb.completedMissions.toSet())

            val sum = engine.summary(c, fb.completedMissions.toSet())
            assertEquals(s.id, 100, sum.score)
            assertTrue(sum.headlineJa.contains("すばらしい"))
            assertTrue(sum.goodPointsJa.first().contains("${steps.size} 個のお題のうち ${steps.size} 個"))
            assertTrue(sum.improvePoints.isNotEmpty())
            assertTrue(sum.keyPhrases.isNotEmpty())
        }
    }

    @Test
    fun talkingAfterTheEnd() = runBlocking {
        val answers = cafeSteps.map { it.answers.first().en }
        val c = talk(cafe, *(answers + "Bye!").toTypedArray())
        assertEquals(ScriptEngine.AFTER_END.en, c.history.last().text)
        val fb = engine.feedback(c.beforeReply())
        assertEquals("Bye!", fb.natural)
        assertEquals(setOf("drink", "custom", "food"), fb.completedMissions.toSet())
        assertTrue(engine.task(c).finished)
    }

    @Test
    fun partialSessionSummary() = runBlocking {
        val c = talk(cafe, "Hello", "one large cappuccino")
        val sum = engine.summary(c, setOf("drink"))
        // お題 1: 言い直して合格 (85 - 10)、残り 4 問は未回答 → 75 / 5 = 15
        assertEquals(15, sum.score)
        assertTrue(sum.headlineJa.contains("1 個のお題をクリア"))
        assertEquals(cafeSteps[0].answers[0].en, sum.improvePoints.first().exampleEn)
        assertEquals(cafe.keyPhrases, sum.keyPhrases)
    }

    @Test
    fun hintsAndTranslation() = runBlocking {
        val first = engine.hint(talk(cafe), null)
        assertEquals(listOf("お手本", "言い換え", "言い換え"), first.map { it.labelJa })
        assertEquals(cafeSteps[0].answers.map { it.en }, first.map { it.en })
        assertEquals(cafeSteps[1].answers[0].en, engine.hint(talk(cafe, "A medium latte, please."), "ミルクを変えて")[0].en)

        val open = engine.hint(talk(Catalog.find("free:work")!!), null)
        assertEquals("例1", open[0].labelJa)

        assertEquals(Scripts.forScenario(cafe).openerJa, engine.translate(cafe.opener).ja)
        assertEquals(cafeSteps[0].reply.ja, engine.translate(cafeSteps[0].reply.en.lowercase()).ja)
        assertTrue(engine.translate("Completely unrelated sentence.").ja.contains("翻訳できません"))
        assertNull(ScriptEngine.lookupJa("Completely unrelated sentence."))
    }

    @Test
    fun modes() {
        assertEquals(EngineMode.SCRIPT, engine.mode)
        assertEquals(EngineMode.DEMO, MockEngine().mode)
    }
}
