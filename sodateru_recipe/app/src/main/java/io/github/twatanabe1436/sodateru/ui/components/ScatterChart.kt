@file:OptIn(ExperimentalLayoutApi::class)

package io.github.twatanabe1436.sodateru.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.twatanabe1436.sodateru.core.Amounts
import io.github.twatanabe1436.sodateru.core.LinearFit
import io.github.twatanabe1436.sodateru.core.MetricPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

// 総合評価 ★1〜★5 の順序を 1 色相 (青) の明度で表す。ライト/ダークそれぞれの背景で検証済み。
private val RatingLight = listOf(Color(0xFF6DA7EC), Color(0xFF3987E5), Color(0xFF256ABF), Color(0xFF184F95), Color(0xFF0D366B))
private val RatingDark = listOf(Color(0xFF184F95), Color(0xFF256ABF), Color(0xFF3987E5), Color(0xFF6DA7EC), Color(0xFF9EC5F4))

@Composable
fun ratingColor(rating: Int): Color? {
    if (rating !in 1..5) return null
    return (if (isSystemInDarkTheme()) RatingDark else RatingLight)[rating - 1]
}

private fun niceStep(range: Double, targetTicks: Int = 5): Double {
    if (range <= 0) return 1.0
    val raw = range / targetTicks
    val mag = 10.0.pow(floor(log10(raw)))
    val norm = raw / mag
    val nice = when {
        norm < 1.5 -> 1.0
        norm < 3 -> 2.0
        norm < 7 -> 5.0
        else -> 10.0
    }
    return nice * mag
}

/**
 * 散布図。点の色は総合評価 (未評価は白抜き)、線は回帰直線。点をタップすると [onSelect] が呼ばれる。
 */
@Composable
fun ScatterChart(
    points: List<MetricPoint>,
    fit: LinearFit?,
    xUnit: String,
    yUnit: String,
    selected: MetricPoint?,
    onSelect: (MetricPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val dark = isSystemInDarkTheme()
    val palette = if (dark) RatingDark else RatingLight
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val axisText = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val hollow = MaterialTheme.colorScheme.outline
    val highlight = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(fontSize = 11.sp, color = axisText)

    if (points.isEmpty()) return

    val xs = points.map { it.x }
    val ys = points.map { it.y }
    fun bounds(values: List<Double>): Triple<Double, Double, Double> {
        var lo = values.min()
        var hi = values.max()
        if (hi - lo < 1e-9) {
            lo -= 1
            hi += 1
        }
        val step = niceStep(hi - lo)
        return Triple(floor(lo / step) * step, ceil(hi / step) * step, step)
    }
    val (x0, x1, xStep) = bounds(xs)
    val (y0, y1, yStep) = bounds(ys)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(260.dp)
            .semantics { contentDescription = "散布図 ${points.size}点" }
            .pointerInput(points) {
                detectTapGestures { tap ->
                    val left = 44.dp.toPx()
                    val bottom = size.height - 28.dp.toPx()
                    val top = 8.dp.toPx()
                    val right = size.width - 12.dp.toPx()
                    fun px(x: Double) = (left + (x - x0) / (x1 - x0) * (right - left)).toFloat()
                    fun py(y: Double) = (bottom - (y - y0) / (y1 - y0) * (bottom - top)).toFloat()
                    val nearest = points.minByOrNull { p ->
                        val dx = px(p.x) - tap.x
                        val dy = py(p.y) - tap.y
                        dx * dx + dy * dy
                    }
                    val hit = nearest?.let {
                        val dx = px(it.x) - tap.x
                        val dy = py(it.y) - tap.y
                        dx * dx + dy * dy <= (24.dp.toPx() * 24.dp.toPx())
                    } ?: false
                    onSelect(if (hit) nearest else null)
                }
            },
    ) {
        val left = 44.dp.toPx()
        val bottom = size.height - 28.dp.toPx()
        val top = 8.dp.toPx()
        val right = size.width - 12.dp.toPx()
        fun px(x: Double) = (left + (x - x0) / (x1 - x0) * (right - left)).toFloat()
        fun py(y: Double) = (bottom - (y - y0) / (y1 - y0) * (bottom - top)).toFloat()

        // 目盛りと補助線 (細い実線)
        var gx = x0
        while (gx <= x1 + xStep / 2) {
            val x = px(gx)
            drawLine(grid, Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
            val label = measurer.measure(Amounts.formatNumber(gx), labelStyle)
            drawText(label, topLeft = Offset(x - label.size.width / 2f, bottom + 4.dp.toPx()))
            gx += xStep
        }
        var gy = y0
        while (gy <= y1 + yStep / 2) {
            val y = py(gy)
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1f)
            val label = measurer.measure(Amounts.formatNumber(gy), labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
            gy += yStep
        }
        val xu = measurer.measure(xUnit, labelStyle)
        drawText(xu, topLeft = Offset(right - xu.size.width, bottom + 4.dp.toPx() + xu.size.height))
        val yu = measurer.measure(yUnit, labelStyle)
        drawText(yu, topLeft = Offset(left - yu.size.width - 6.dp.toPx(), top - 2.dp.toPx()))

        // 回帰直線
        if (fit != null) {
            val ya = fit.predict(x0)
            val yb = fit.predict(x1)
            drawLine(
                lineColor.copy(alpha = 0.8f),
                Offset(px(x0), py(ya).coerceIn(top, bottom)),
                Offset(px(x1), py(yb).coerceIn(top, bottom)),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 点 (背景色の縁取りで重なっても見分けられるようにする)
        val r = 6.dp.toPx()
        val ring = 2.dp.toPx()
        points.sortedBy { it.sample.rating }.forEach { p ->
            val c = Offset(px(p.x), py(p.y))
            drawCircle(surface, radius = r + ring, center = c)
            val rating = p.sample.rating
            if (rating in 1..5) {
                drawCircle(palette[rating - 1], radius = r, center = c)
            } else {
                drawCircle(hollow, radius = r - 1.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
            }
        }
        selected?.let { p ->
            drawCircle(highlight, radius = r + 5.dp.toPx(), center = Offset(px(p.x), py(p.y)), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/** 散布図の凡例 (評価の色)。 */
@Composable
fun RatingLegend(modifier: Modifier = Modifier) {
    val palette = if (isSystemInDarkTheme()) RatingDark else RatingLight
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        palette.forEachIndexed { i, color ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(4.dp))
                Text("★${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text("未評価", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(16.dp).height(2.dp).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(4.dp))
            Text("傾向線", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
