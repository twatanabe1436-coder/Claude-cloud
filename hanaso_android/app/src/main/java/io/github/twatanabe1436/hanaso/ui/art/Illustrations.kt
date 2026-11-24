package io.github.twatanabe1436.hanaso.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.twatanabe1436.hanaso.R
import io.github.twatanabe1436.hanaso.ui.Emoji
import kotlin.math.max

// イラストの画像は art/build.py で作る (人物・場面の描き方は art/people.py と art/scenes.py)

/** 相手の表情 */
enum class Mood { NORMAL, HAPPY, PUZZLED }

/** 絵の中の人物の位置 (x, y, 大きさ。その絵の座標) */
internal data class Placement(val person: String, val x: Float, val y: Float, val size: Float)

/** フリートーク (相手は Alex) の絵を出すときに使う id */
const val FREE_TALK_ART = "free:"

/** シナリオ id → 場面 id (フリートークはどのトピックも同じ場面) */
fun sceneOf(scenarioId: String): String = if (scenarioId.startsWith("free:")) "free" else scenarioId

/** その場面にイラストがあるか */
fun hasArt(scenarioId: String): Boolean = ArtResources.sceneBackground(sceneOf(scenarioId)) != null

// 場面の絵は 720x400。人物 (200x200 の絵) はこの位置にこの大きさで置く (art/scenes.py と同じ値)
private const val SCENE_W = 720f
private const val SCENE_H = 400f
private const val CHAR_X = 230f
private const val CHAR_Y = 96f
private const val CHAR_SIZE = 260f

// リーグの絵は 720x320
private const val LEAGUE_W = 720f
private const val LEAGUE_H = 320f

/**
 * 場面のイラスト (背景・相手・前景を重ねたもの)。枠いっぱいに広げ、はみ出た分は切る。
 * @param focusY 縦にはみ出たときに見せる位置 (0 = 上端、1 = 下端)
 * @param overlay いちばん上に重ねる絵 (紙吹雪など。720x400)
 */
@Composable
fun SceneIllustration(
    scenarioId: String,
    mood: Mood,
    modifier: Modifier = Modifier,
    focusY: Float = 0.5f,
    overlay: Int? = null,
) {
    val scene = sceneOf(scenarioId)
    val bgRes = ArtResources.sceneBackground(scene) ?: return
    val bg = painterResource(bgRes)
    val fg = ArtResources.sceneForeground(scene)?.let { painterResource(it) }
    val person = ArtResources.partner[scene]?.let { ArtResources.character(it, mood) }?.let { painterResource(it) }
    val top = overlay?.let { painterResource(it) }
    Canvas(modifier.clipToBounds()) {
        val s = max(size.width / SCENE_W, size.height / SCENE_H)
        translate((size.width - SCENE_W * s) / 2, (size.height - SCENE_H * s) * focusY) {
            val full = Size(SCENE_W * s, SCENE_H * s)
            paint(bg, 0f, 0f, full)
            person?.let { paint(it, CHAR_X * s, CHAR_Y * s, Size(CHAR_SIZE * s, CHAR_SIZE * s)) }
            fg?.let { paint(it, 0f, 0f, full) }
            top?.let { paint(it, 0f, 0f, full) }
        }
    }
}

/** シナリオの絵 (角丸の場面)。イラストがなければ絵文字 */
@Composable
fun ScenarioPicture(scenarioId: String, emoji: String, modifier: Modifier = Modifier, emojiSize: Int = 30, corner: Dp = 12.dp) {
    if (hasArt(scenarioId)) {
        SceneIllustration(scenarioId, Mood.NORMAL, modifier.clip(RoundedCornerShape(corner)), focusY = 0.6f)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) { Emoji(emoji, size = emojiSize) }
    }
}

/** 相手の顔 (丸)。イラストがない場面では fallback の文字 (名前の頭文字など) */
@Composable
fun PartnerAvatar(scenarioId: String, mood: Mood, size: Dp, modifier: Modifier = Modifier, fallback: String = "") {
    val res = ArtResources.partner[sceneOf(scenarioId)]?.let { ArtResources.character(it, mood) }
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (res == null) {
            Text(fallback, color = scheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
        } else {
            val face = painterResource(res)
            Canvas(Modifier.fillMaxSize()) {
                // 顔が真ん中に来るよう、少し拡大する (頭の中心は 200x200 の絵の (100, 86))
                val s = this.size.width / 200f * 1.25f
                paint(face, this.size.width / 2 - 100f * s, this.size.height / 2 - 92f * s, Size(200f * s, 200f * s))
            }
        }
    }
}

/** リーグ画面の絵 (表彰台で喜ぶ 3 人) */
@Composable
fun LeagueIllustration(modifier: Modifier = Modifier) {
    val bg = painterResource(R.drawable.art_league_bg)
    val fg = painterResource(R.drawable.art_league_fg)
    val people = ArtResources.podium.mapNotNull { p -> ArtResources.character(p.person, Mood.HAPPY)?.let { p to painterResource(it) } }
    Canvas(modifier.clipToBounds()) {
        val s = max(size.width / LEAGUE_W, size.height / LEAGUE_H)
        translate((size.width - LEAGUE_W * s) / 2, (size.height - LEAGUE_H * s) / 2) {
            val full = Size(LEAGUE_W * s, LEAGUE_H * s)
            paint(bg, 0f, 0f, full)
            people.forEach { (p, painter) -> paint(painter, p.x * s, p.y * s, Size(p.size * s, p.size * s)) }
            paint(fg, 0f, 0f, full)
        }
    }
}

private fun DrawScope.paint(painter: Painter, x: Float, y: Float, size: Size) {
    translate(x, y) { with(painter) { draw(size) } }
}
