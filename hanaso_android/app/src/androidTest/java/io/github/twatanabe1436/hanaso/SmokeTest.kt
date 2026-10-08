package io.github.twatanabe1436.hanaso

import android.Manifest
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.twatanabe1436.hanaso.core.MockEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * エミュレータで画面を操作して、主要な流れがクラッシュせずに動くことを確かめる。
 * 相手はデモモード (台本モードのテストは API キーなしの本物)、音声認識・読み上げは偽物 (Fakes.kt) を使う。
 * 途中の画面は /data/local/tmp/hanaso_shots に保存し、CI で取り出して確認する。
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: HanasoApp get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var input: FakeSpeechInput
    private lateinit var speaker: FakeSpeaker

    @Before
    fun setUp() {
        // UiAutomation.grantRuntimePermission は Android 9 からなので、どの版でも使える pm grant で許可する
        shell("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
        shell("mkdir -p $SHOT_DIR")
        input = FakeSpeechInput()
        speaker = FakeSpeaker()
        onMain {
            app.closeTalk()
            app.store.resetAll()
            app.store.setApiKey("")
            app.engineOverride = MockEngine(delayMs = 5)
            app.speechInput = input
            app.speaker = speaker
            app.nav.tab(Screen.Home)
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
        onMain { app.closeTalk() }
    }

    @Test
    fun roleplayConversationAndSummary() {
        waitForText("今日のおすすめ")
        compose.onNodeWithText("🧪 デモモード").assertExists()
        screenshot("01_home")

        // シナリオ一覧 → カフェ → 説明シート → 開始
        compose.onNodeWithText("会話").performClick()
        compose.onNodeWithText("カフェで注文する").performScrollTo().performClick()
        waitForText("🔑 使えるフレーズ")
        screenshot("02_intro")
        compose.onNodeWithText("会話をはじめる").performScrollTo().performClick()

        val opener = "Hi there, good morning! What can I get started for you today?"
        waitForText(opener)
        // 会話の最初に場面の説明 (状況・自分の役・相手)
        compose.onNodeWithText("🎬 場面").assertExists()
        compose.onNodeWithText("あなた：お客さん").assertExists()
        waitUntil("最初のセリフが読み上げられない") { speaker.spoken.contains(opener) }

        // マイクで話す → 自動送信 → 返事 (読み上げ) → 添削 (修正ありは自動で開く) → ミッション
        speak("i want a medium latte with oat milk")
        waitForText("I see! Could you tell me a little more about that?")
        waitForText("修正あり")
        waitForText("I'd like a medium latte with oat milk.")
        waitUntil("返事が読み上げられない") { speaker.spoken.any { it.contains("Could you tell me a little more") } }
        compose.onNodeWithText("1 / 3").assertExists()
        screenshot("03_talk")

        // ヒント → そのまま送る
        waitUntil("返事が終わらない") { app.talk?.busy == false }
        compose.onNodeWithContentDescription("ヒント").performClick()
        waitForText("ヒント：何て言えばいい？")
        waitForText("そのまま送る")
        assertEquals(3, compose.onAllNodesWithText("そのまま送る").fetchSemanticsNodes().size)
        screenshot("04_hint")
        compose.onAllNodesWithText("そのまま送る").onFirst().performClick()
        waitForText("2 / 3")

        // キーボードで日本語 → 言い方の提案
        waitUntil("返事が終わらない") { app.talk?.busy == false }
        compose.onNodeWithContentDescription("入力").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("砂糖なしでお願いします")
        compose.onNode(hasSetTextAction()).performImeAction()
        waitForText("英語でどう言う？")
        onMain { app.talk?.closeHint() }

        // キーボードで英語 → 3つ目のミッション達成
        compose.onNode(hasSetTextAction()).performTextInput("Thank you so much")
        compose.onNode(hasContentDescription("送信")).performClick()
        waitForText("🎉 すべてのミッションを達成しました！", timeoutMs = 8_000)
        waitUntil("返事が終わらない") { app.talk?.busy == false }
        screenshot("05_complete")

        // 文字の入力欄は × で閉じられる
        compose.onNodeWithContentDescription("入力を閉じる").performClick()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)

        // 終了 → 振り返り
        compose.onNodeWithText("終了").performClick()
        waitForText("おつかれさまでした！")
        waitForText("スコア", timeoutMs = 8_000)
        screenshot("06_summary")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("すべて保存"))
        compose.onNodeWithText("すべて保存").performClick()
        waitUntil("フレーズが保存されない") { app.store.phrases.value.size >= 4 }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("ホームへ"))
        compose.onNodeWithText("ホームへ").performClick()
        waitForText("今日のおすすめ")
        compose.onNodeWithTag("home_list").performScrollToNode(hasText("最近の会話"))
        compose.onNodeWithText("最近の会話").assertExists()
        assertEquals(3, app.store.sessions.value.single().learnerTurns)
        assertTrue(app.store.sessions.value.single().score != null)
    }

    @Test
    fun phraseBookPracticeAndFlashcards() {
        onMain {
            app.store.addPhrase("Could you make it with oat milk?", "オーツミルクに変えてもらえますか？", "カフェで注文する")
            app.store.addPhrase("For here, please.", "店内で食べます。", "カフェで注文する")
        }
        compose.onNodeWithText("フレーズ帳").performClick()
        waitForText("2 フレーズ")

        input.queue.add("for here please")
        compose.onAllNodesWithText("言ってみる").onFirst().performClick()
        waitForText("for here please")
        compose.onNodeWithText("言い終わったらタップ").performClick()
        waitForText("すばらしい！")
        compose.onNodeWithText("100").assertExists()
        screenshot("07_phrases")

        input.queue.add("hello world")
        compose.onNodeWithText("🃏 フラッシュカード").performClick()
        waitForText("英語で言ってみよう")
        compose.onNodeWithContentDescription("話す").performClick()
        waitForText("hello world")
        compose.onNodeWithContentDescription("話し終わる").performClick()
        waitForText("聞き取り: “hello world”")
        screenshot("08_flashcard")
        compose.onNodeWithContentDescription("閉じる").performClick()
        waitUntil("練習の記録が残らない") { app.store.phrases.value.any { it.practiceCount >= 1 } }
    }

    @Test
    fun settingsAndHandsFreeFreeTalk() {
        compose.onNodeWithText("設定").performClick()
        waitForText("Claude API キー")
        screenshot("09_settings")
        compose.onNodeWithText("ハンズフリー会話").performScrollTo().performClick()
        waitUntil("ハンズフリーが保存されない") { app.store.settings.value.handsFree }

        // ハンズフリー: マイクを押さなくても、AI が話し終わるたびに聞き取りが始まる
        input.queue.add("i went to the gym today")
        input.queue.add("it was really fun")
        compose.onNodeWithText("ホーム").performClick()
        compose.onNodeWithText("今日の出来事").performScrollTo().performClick()
        compose.onNodeWithText("会話をはじめる").performScrollTo().performClick()
        waitForText("i went to the gym today", timeoutMs = 8_000)
        waitForText("it was really fun", timeoutMs = 8_000)
        waitUntil("3回目の聞き取りが始まらない", 8_000) { input.starts >= 3 }
        assertEquals("ハンズフリーは無音で自動的に区切る", false, input.lastUntilStopped)
        compose.onNodeWithText("🎯 ミッション").assertDoesNotExist()
        screenshot("10_handsfree")

        // 話した後の「やめる」は確認してから
        compose.onNodeWithContentDescription("やめる").performClick()
        waitForText("会話をやめますか？")
        compose.onNodeWithText("やめる").performClick()
        waitForText("今日のおすすめ")
    }

    @Test
    fun scriptModeConversation() {
        // API キーなし → 台本モード (AI を使わない本物の相手)
        onMain { app.engineOverride = null }
        waitForText("📖 台本モード（AI なし・無料）")

        compose.onNodeWithText("会話").performClick()
        compose.onNodeWithText("カフェで注文する").performScrollTo().performClick()
        waitForText("📖 台本のお題（全 5 問）")
        compose.onNodeWithText("会話をはじめる").performScrollTo().performClick()

        waitForText("1 / 5")
        waitForText("ラテの M サイズを注文しよう")
        // 台本モードでは相手のセリフに日本語訳が自動で付く
        waitForText("いらっしゃいませ、おはようございます！ご注文は何にしますか？")
        compose.onNodeWithText("お手本").performClick()
        waitForText("Can I get a medium latte, please?")

        // お題どおりに言う → 台本の返事が来て次のお題へ、ミッション達成
        // マイクをもう一度押すまでは、黙っても回答が確定しない
        input.queue.add("can I get a medium latte please")
        compose.onNodeWithContentDescription("話す").performClick()
        waitForText("can I get a medium latte please")
        Thread.sleep(1_500)
        assertTrue("もう一度押す前に確定してしまった", app.talk?.listening == true)
        assertEquals(true, input.lastUntilStopped)
        compose.onAllNodesWithText("Sure! Would you like regular milk, or would you prefer oat or almond milk?").assertCountEquals(0)
        screenshot("11_listening")
        compose.onNodeWithContentDescription("話し終わる").performClick()
        waitForText("Sure! Would you like regular milk, or would you prefer oat or almond milk?")
        waitForText("Great!")
        waitForText("2 / 5")
        waitForText("1 / 3")
        waitUntil("返事が読み上げられない") { speaker.spoken.any { it.contains("oat or almond milk") } }
        screenshot("12_script_talk")

        // 関係ないことを言う → 聞き返され、お手本つきで「もう一度」
        waitUntil("返事が終わらない") { app.talk?.busy == false }
        speak("hello")
        waitForText("Sorry, could you say that again?")
        waitForText("もう一度")
        waitForText("📖 お手本")
        screenshot("13_script_retry")

        // ヒント (お手本) をそのまま送る
        waitUntil("返事が終わらない") { app.talk?.busy == false }
        compose.onNodeWithContentDescription("ヒント").performClick()
        waitForText("ヒント：何て言えばいい？")
        waitForText("Could you make it with oat milk?")
        compose.onAllNodesWithText("そのまま送る").onFirst().performClick()
        waitForText("📖 お題 3 / 5")

        // 残りはキーボードで
        compose.onNodeWithContentDescription("入力").performClick()
        for (line in listOf("I'll also have a blueberry muffin.", "For here, please.", "I'll pay by card.")) {
            waitUntil("返事が終わらない") { app.talk?.busy == false }
            compose.onNode(hasSetTextAction()).performTextInput(line)
            compose.onNode(hasContentDescription("送信")).performClick()
            waitForText(line)
        }
        waitForText("🎉 台本を最後まで話せました！", timeoutMs = 8_000)
        waitForText("振り返りを見る")
        waitForText("3 / 3")
        screenshot("14_script_done")

        compose.onNodeWithText("振り返りを見る").performClick()
        waitForText("おつかれさまでした！")
        waitForText("スコア")
        waitForText("すばらしい！台本をほぼ完ぺきに話せました")
        screenshot("15_script_summary")
        val record = app.store.sessions.value.single()
        assertEquals(6, record.learnerTurns)
        assertEquals(3, record.missionsDone)
        // お題 2 は言い直し (-10)、お題 5 は自分なりの言い方 (85 点) → (100 + 90 + 100 + 100 + 85) / 5
        assertEquals(95, record.score)

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("ホームへ"))
        compose.onNodeWithText("ホームへ").performClick()
        waitForText("📖 台本モード（AI なし・無料）")
        screenshot("16_script_home")
    }

    /** マイクで話して、もう一度押して確定する (既定の「マイクをもう一度押すまで聞き続ける」) */
    private fun speak(text: String) {
        input.queue.add(text)
        compose.onNodeWithContentDescription("話す").performClick()
        waitForText(text)
        compose.onNodeWithContentDescription("話し終わる").performClick()
    }

    private fun waitForText(text: String, timeoutMs: Long = 5_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntil(message: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError(message)
            Thread.sleep(100)
        }
    }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // アニメーションや描画が終わるのを待つ
        Thread.sleep(800)
        shell("screencap -p $SHOT_DIR/$name.png")
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
    }

    private companion object {
        const val SHOT_DIR = "/data/local/tmp/hanaso_shots"
    }
}
