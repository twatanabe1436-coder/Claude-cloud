package io.github.twatanabe1436.sodateru

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.twatanabe1436.sodateru.core.model.AppData
import io.github.twatanabe1436.sodateru.data.SampleData
import io.github.twatanabe1436.sodateru.data.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 実機/エミュレータで画面を操作して、主要な流れがクラッシュせずに動くことを確かめる。
 * 途中の画面は /data/local/tmp/sodateru_shots に保存し、CI で取り出して確認する。
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: SodateruApp get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        shell("mkdir -p $SHOT_DIR")
        runBlocking {
            app.container.repository.loaded.first { it }
            app.container.repository.replaceAll(AppData())
        }
        instrumentation.runOnMainSync { app.container.settings.update { Settings() } }
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun launch(withSample: Boolean) {
        if (withSample) runBlocking { app.container.repository.replaceAll(SampleData.create()) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun emptyStateThenSample() {
        launch(withSample = false)
        waitForText("レシピを育てよう")
        screenshot("01_empty")
        compose.onNodeWithText("サンプルを入れて試してみる").performClick()
        waitForText("山型食パン（サンプル）")
        compose.onNodeWithText("豚の生姜焼き（サンプル）").assertExists()
        screenshot("02_recipes")
        assertEquals(2, app.container.repository.data.value.recipes.size)
    }

    @Test
    fun cookingRecipeDetailAndDiff() {
        launch(withSample = true)
        waitForText("豚の生姜焼き（サンプル）")
        compose.onNodeWithText("豚の生姜焼き（サンプル）").performClick()
        waitForText("v2で変えたこと")
        screenshot("03_detail_cooking")
        scrollTo(hasText("育ちの記録"))
        screenshot("04_detail_timeline")
        scrollTo(hasContentDescription("違いを見る"))
        compose.onNodeWithContentDescription("違いを見る").performClick()
        waitForText("版を比べる")
        screenshot("05_diff")
    }

    @Test
    fun breadDetailAndTodaysSuggestion() {
        launch(withSample = true)
        waitForText("山型食パン（サンプル）")
        compose.onNodeWithText("山型食パン（サンプル）").performClick()
        waitForText("今日の条件で仕込む")
        screenshot("06_detail_bread")
        compose.onNodeWithText("今日の条件で仕込む").performClick()
        waitForText("今日の仕込み")
        field("室温").performTextReplacement("27")
        field("湿度").performTextReplacement("74")
        hideKeyboard()
        waitForText("今日の提案")
        screenshot("07_bread_lab")
        scrollTo(hasText("この内容で焼成ログをつける"))
        screenshot("08_bread_lab_advice")
        scrollTo(hasText("条件が近かった過去の回"))
        screenshot("08b_bread_lab_refs")
        scrollTo(hasText("この内容で焼成ログをつける"))
        compose.onNodeWithText("この内容で焼成ログをつける").performClick()
        waitForText("記録する")
        screenshot("09_log_prefilled")
    }

    @Test
    fun analysisChart() {
        launch(withSample = true)
        waitForText("山型食パン（サンプル）")
        compose.onNodeWithText("パン研究").performClick()
        waitForText("今日の仕込み")
        scrollTo(hasText("研究ノート"))
        compose.onNodeWithText("研究ノート").performClick()
        waitForText("湿度 と 加水率")
        screenshot("10_analysis")
        compose.onNodeWithText("室温×一次発酵の時間").performClick()
        waitForText("室温 と 一次発酵の時間")
        screenshot("11_analysis_proof")
    }

    @Test
    fun addRecipeCookAndGrow() {
        launch(withSample = true)
        waitForDescription("レシピを追加")
        compose.onNodeWithContentDescription("レシピを追加").performClick()
        compose.onNodeWithText("手で入力する").performClick()
        waitForText("料理名")
        field("料理名").performTextReplacement("テストのカレー")
        field("材料").performTextReplacement("玉ねぎ")
        field("量").performTextReplacement("1")
        field("単位").performTextReplacement("個")
        scrollTo(hasText("手順 1") and hasSetTextAction())
        field("手順 1").performTextReplacement("玉ねぎを炒める")
        hideKeyboard()
        screenshot("12_new_recipe")
        compose.onNodeWithText("保存").performClick()
        waitForDescription("作った！記録する")

        compose.onNodeWithContentDescription("作った！記録する").performClick()
        waitForText("アレンジしたこと")
        field("アレンジしたこと").performTextReplacement("玉ねぎを2個にした")
        field("感想・次回へのメモ").performTextReplacement("甘みが増しておいしい")
        hideKeyboard()
        screenshot("13_new_log")
        compose.onNodeWithText("保存").performClick()
        waitForText("レシピに反映しますか？")
        compose.onNodeWithText("反映する").performClick()
        waitForText("v2 に育てる")
        field("量").performTextReplacement("2")
        hideKeyboard()
        screenshot("14_new_version")
        compose.onNodeWithText("保存").performClick()
        waitForText("v2で変えたこと")
        screenshot("15_grown")

        val recipe = app.container.repository.data.value.recipes.first { it.title == "テストのカレー" }
        assertEquals(2, recipe.versions.size)
        assertEquals("2", recipe.current.ingredients.first().amount)
        val log = app.container.repository.data.value.logs.first { it.recipeId == recipe.id }
        assertEquals(2, log.appliedVersion)
    }

    @Test
    fun logsAndSettingsScreens() {
        launch(withSample = true)
        waitForText("山型食パン（サンプル）")
        compose.onNodeWithText("記録").performClick()
        waitForText("アレンジ: 玉ねぎを1個に増やした")
        screenshot("16_logs")
        compose.onNodeWithText("設定").performClick()
        waitForText("写真の文字起こし")
        screenshot("17_settings")
        compose.onNodeWithText("キーを入力").performClick()
        waitForText("Claude の API キー")
        screenshot("18_api_key_dialog")
        compose.onNodeWithText("キャンセル").performClick()
    }

    @Test
    fun scanScreenAndCalculator() {
        launch(withSample = true)
        waitForDescription("レシピを追加")
        compose.onNodeWithContentDescription("レシピを追加").performClick()
        compose.onNodeWithText("写真から読み取る").performClick()
        waitForText("読み取り方")
        screenshot("19_scan")
        compose.onNodeWithContentDescription("戻る").performClick()
        compose.onNodeWithText("パン研究").performClick()
        waitForText("今日の仕込み")
        scrollTo(hasText("計算ツール"))
        compose.onNodeWithText("計算ツール").performClick()
        waitForText("仕込み水温")
        field("室温").performTextReplacement("25")
        hideKeyboard()
        waitForText("仕込み水", substring = true)
        screenshot("20_calc_water")
        compose.onNodeWithText("配合（ベーカーズ%）").performClick()
        waitForText("元の配合")
        scrollTo(hasText("計算結果"))
        screenshot("21_calc_bakers")
    }

    /** 端末内の日本語 OCR (ML Kit、モデル同梱) が動くこと。 */
    @Test
    fun onDeviceJapaneseOcr() {
        val bitmap = Bitmap.createBitmap(900, 400, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 64f }
            drawText("材料", 40f, 100f, paint)
            drawText("強力粉 250g", 40f, 200f, paint)
            drawText("砂糖 大さじ2", 40f, 300f, paint)
        }
        val file = File(app.cacheDir, "ocr_test.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val text = runBlocking { app.container.textReader.readTextOnDevice(listOf(Uri.fromFile(file))) }
        assertTrue("OCR result: $text", "250" in text)
        assertTrue("OCR result: $text", "粉" in text || "砂糖" in text)
    }

    private fun hideKeyboard() {
        scenario?.onActivity { activity ->
            activity.getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        compose.waitForIdle()
        // キーボードが閉じるアニメーションを待つ
        Thread.sleep(700)
    }

    private fun waitForDescription(description: String, timeoutMs: Long = 8_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** 画面の縦スクロール (LazyColumn) を、条件に合う項目が見えるところまで動かす。 */
    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(matcher)
        compose.waitForIdle()
    }

    private fun field(label: String): SemanticsNodeInteraction =
        compose.onAllNodes(hasText(label) and hasSetTextAction()).onFirst()

    private fun waitForText(text: String, timeoutMs: Long = 8_000, substring: Boolean = false) {
        try {
            compose.waitUntil(timeoutMs) {
                compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: ComposeTimeoutException) {
            // 失敗したときの画面と、表示されている文字を残す
            shell("screencap -p $SHOT_DIR/fail_${text.take(12).replace(Regex("[^\\p{L}\\p{N}]"), "_")}.png")
            val visible = compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .flatMap { it.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { t -> t.text } }
                .distinct()
            throw AssertionError("「$text」が見つかりません。表示中の文字: $visible", e)
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        Thread.sleep(600)
        shell("screencap -p $SHOT_DIR/$name.png")
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes() }
    }

    private companion object {
        const val SHOT_DIR = "/data/local/tmp/sodateru_shots"
    }
}
