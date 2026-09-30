package io.github.twatanabe1436.sodateru.core

import kotlin.math.sqrt

/** 回帰直線 y = intercept + slope * x と相関係数。 */
data class LinearFit(
    val slope: Double,
    val intercept: Double,
    /** ピアソンの相関係数 (重みなし)。 */
    val r: Double,
    val n: Int,
) {
    fun predict(x: Double): Double = intercept + slope * x
}

object Stats {

    fun mean(values: List<Double>): Double? = if (values.isEmpty()) null else values.average()

    /** 重み付き平均。重みの合計が 0 なら null。 */
    fun weightedMean(values: List<Double>, weights: List<Double>): Double? {
        require(values.size == weights.size)
        val w = weights.sum()
        if (w <= 0) return null
        return values.indices.sumOf { values[it] * weights[it] } / w
    }

    /**
     * 最小二乗法の回帰直線。点が 3 つ未満、または x がすべて同じなら null。
     * [weights] を渡すと重み付き最小二乗になる (相関係数は重みなしで計算)。
     */
    fun linearFit(xs: List<Double>, ys: List<Double>, weights: List<Double>? = null): LinearFit? {
        require(xs.size == ys.size)
        val n = xs.size
        if (n < 3) return null
        val w = weights ?: List(n) { 1.0 }
        val sw = w.sum()
        if (sw <= 0) return null
        val mx = xs.indices.sumOf { w[it] * xs[it] } / sw
        val my = ys.indices.sumOf { w[it] * ys[it] } / sw
        var sxx = 0.0
        var sxy = 0.0
        for (i in 0 until n) {
            sxx += w[i] * (xs[i] - mx) * (xs[i] - mx)
            sxy += w[i] * (xs[i] - mx) * (ys[i] - my)
        }
        if (sxx < 1e-12) return null
        val slope = sxy / sxx
        return LinearFit(slope = slope, intercept = my - slope * mx, r = correlation(xs, ys) ?: 0.0, n = n)
    }

    fun correlation(xs: List<Double>, ys: List<Double>): Double? {
        require(xs.size == ys.size)
        if (xs.size < 3) return null
        val mx = xs.average()
        val my = ys.average()
        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        for (i in xs.indices) {
            sxx += (xs[i] - mx) * (xs[i] - mx)
            syy += (ys[i] - my) * (ys[i] - my)
            sxy += (xs[i] - mx) * (ys[i] - my)
        }
        if (sxx < 1e-12 || syy < 1e-12) return null
        return sxy / sqrt(sxx * syy)
    }

    /** 相関の強さのことば。 */
    fun describeCorrelation(r: Double): String {
        val a = kotlin.math.abs(r)
        val strength = when {
            a >= 0.7 -> "強い"
            a >= 0.4 -> "中くらいの"
            a >= 0.2 -> "弱い"
            else -> return "ほとんど関係なし"
        }
        return if (r > 0) "${strength}正の相関" else "${strength}負の相関"
    }
}
