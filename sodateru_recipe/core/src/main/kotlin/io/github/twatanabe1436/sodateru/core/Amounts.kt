package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.IngredientRole
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * 材料の量の表記 (「200」「1/2」「1と1/2」「2〜3」「少々」) を数値として扱うためのユーティリティ。
 */
object Amounts {

    /** 「大さじ1」のように単位を量の前に書くもの。 */
    val PREFIX_UNITS = listOf("大さじ", "小さじ", "カップ")

    /** 分数で書くのが自然な単位 (「大さじ1/2」「1/2個」)。 */
    private val FRACTION_UNITS = PREFIX_UNITS + listOf(
        "個", "本", "枚", "片", "かけ", "束", "袋", "缶", "合", "切れ", "房", "玉", "丁", "尾", "杯", "株", "パック",
    )

    private val UNICODE_FRACTIONS = mapOf(
        '½' to "1/2", '⅓' to "1/3", '⅔' to "2/3", '¼' to "1/4", '¾' to "3/4",
    )

    private val RANGE_SEPARATOR = Regex("\\s*[〜~～]\\s*|\\s*-\\s*(?=\\d)")

    /** 全角の英数字・記号を半角にし、前後の空白を取る。 */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text.trim()) {
            when (c) {
                in '０'..'９' -> sb.append('0' + (c - '０'))
                in 'Ａ'..'Ｚ' -> sb.append('A' + (c - 'Ａ'))
                in 'ａ'..'ｚ' -> sb.append('a' + (c - 'ａ'))
                '．' -> sb.append('.')
                '／' -> sb.append('/')
                '，' -> sb.append(',')
                '　' -> sb.append(' ')
                else -> {
                    val frac = UNICODE_FRACTIONS[c]
                    if (frac != null) {
                        // 「1½」→「1と1/2」
                        if (sb.isNotEmpty() && sb.last().isDigit()) sb.append('と')
                        sb.append(frac)
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    /** 1 つの数量を読む。「少々」や範囲 (「2〜3」) は null。 */
    fun parse(amount: String): Double? {
        val s = normalize(amount).replace(" ", "").removeSuffix("強").removeSuffix("弱")
        if (s.isEmpty()) return null
        Regex("^(\\d+)と(\\d+)/(\\d+)$").matchEntire(s)?.let { m ->
            val (w, n, d) = m.destructured
            val den = d.toDouble()
            return if (den == 0.0) null else w.toDouble() + n.toDouble() / den
        }
        Regex("^(\\d+(?:\\.\\d+)?)/(\\d+(?:\\.\\d+)?)$").matchEntire(s)?.let { m ->
            val (n, d) = m.destructured
            val den = d.toDouble()
            return if (den == 0.0) null else n.toDouble() / den
        }
        Regex("^\\d+(?:\\.\\d+)?$").matchEntire(s)?.let { return s.toDouble() }
        Regex("^\\.\\d+$").matchEntire(s)?.let { return s.toDouble() }
        return null
    }

    /** 「2〜3」を (2, 3) として読む。範囲でなければ null。 */
    fun parseRange(amount: String): Pair<Double, Double>? {
        val parts = normalize(amount).split(RANGE_SEPARATOR)
        if (parts.size != 2) return null
        val a = parse(parts[0]) ?: return null
        val b = parse(parts[1]) ?: return null
        return a to b
    }

    fun usesFractions(unit: String): Boolean = FRACTION_UNITS.any { unit.trim() == it }

    /** 数量を表示用の文字列にする。分数向きの単位なら 1/2・1/3 などに丸める。 */
    fun format(value: Double, unit: String = ""): String {
        if (usesFractions(unit)) formatFraction(value)?.let { return it }
        return formatNumber(value)
    }

    fun formatNumber(value: Double): String {
        val a = abs(value)
        val rounded = when {
            a >= 100 -> value.roundToLong().toDouble()
            a >= 1 -> (value * 10).roundToLong() / 10.0
            else -> (value * 100).roundToLong() / 100.0
        }
        return if (rounded == floor(rounded)) rounded.toLong().toString() else rounded.toString()
    }

    private fun formatFraction(value: Double): String? {
        if (value <= 0) return null
        val whole = floor(value).toLong()
        val frac = value - whole
        val candidates = listOf(
            0.0 to "", 0.25 to "1/4", 1.0 / 3 to "1/3", 0.5 to "1/2", 2.0 / 3 to "2/3", 0.75 to "3/4", 1.0 to "",
        )
        val (target, label) = candidates.minBy { abs(it.first - frac) }
        if (abs(target - frac) > 0.04) return null
        val w = if (target == 1.0) whole + 1 else whole
        return when {
            label.isEmpty() -> w.toString()
            w == 0L -> label
            else -> "${w}と$label"
        }
    }

    /** 量を factor 倍した表記を返す。数値として読めない表記 (「少々」) はそのまま。 */
    fun scale(amount: String, factor: Double, unit: String = ""): String {
        parse(amount)?.let { return format(it * factor, unit) }
        parseRange(amount)?.let { (a, b) -> return "${format(a * factor, unit)}〜${format(b * factor, unit)}" }
        return amount
    }

    fun scale(ingredient: Ingredient, factor: Double): Ingredient =
        ingredient.copy(amount = scale(ingredient.amount, factor, ingredient.unit))

    /** 「大さじ1」「200g」「1個」「少々」のような表示。 */
    fun display(ingredient: Ingredient): String {
        val amount = ingredient.amount.trim()
        val unit = ingredient.unit.trim()
        return when {
            amount.isEmpty() -> unit
            unit in PREFIX_UNITS -> unit + amount
            else -> amount + unit
        }
    }

    /**
     * グラム換算。g・kg と、水分なら ml・cc・大さじ・小さじも 1ml = 1g として換算する。
     * 換算できなければ null。
     */
    fun grams(ingredient: Ingredient): Double? {
        val value = parse(ingredient.amount) ?: return null
        val unit = normalize(ingredient.unit).lowercase()
        return when (unit) {
            "g", "ｇ", "グラム", "gr" -> value
            "kg", "ｋｇ", "キロ" -> value * 1000
            "ml", "ｍｌ", "cc", "ｃｃ", "ミリリットル" -> value
            "大さじ" -> if (ingredient.role == IngredientRole.LIQUID) value * 15 else null
            "小さじ" -> if (ingredient.role == IngredientRole.LIQUID) value * 5 else null
            "カップ" -> if (ingredient.role == IngredientRole.LIQUID) value * 200 else null
            else -> null
        }
    }
}
