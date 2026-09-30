package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.IngredientRole

/** 材料名からベーカーズ%用の役割を推測する。 */
object IngredientRoles {

    private val SUGAR = listOf(
        "砂糖", "グラニュー糖", "上白糖", "きび糖", "てんさい糖", "黒糖", "粉糖", "三温糖", "和三盆",
        "はちみつ", "ハチミツ", "蜂蜜", "メープル", "水あめ", "水飴", "練乳", "コンデンス", "モラセス",
    )
    private val FAT = listOf(
        "バター", "オイル", "ショートニング", "マーガリン", "ラード", "油", "ギー",
    )
    private val FLOUR = listOf(
        "強力粉", "薄力粉", "中力粉", "準強力粉", "全粒粉", "ライ麦", "米粉", "小麦粉", "フランスパン用",
        "リスドォル", "カメリヤ", "はるゆたか", "キタノカオリ", "春よ恋", "ゆめちから", "イーグル",
        "タイプER", "スペルト", "グラハム", "デュラム", "セモリナ",
    )
    private val NOT_FLOUR = listOf("粉糖", "片栗粉", "きな粉", "ベーキング", "スキム", "脱脂粉乳", "粉チーズ", "打ち粉", "コーン")
    private val YEAST = listOf("イースト", "酵母", "ルヴァン", "サフ", "白神こだま", "パネトーネ種", "ホシノ")
    private val SALT_EXCLUDE = listOf("麹", "こうじ", "こしょう", "胡椒", "昆布", "バター")
    private val LIQUID = listOf("水", "牛乳", "豆乳", "ぬるま湯", "お湯", "湯", "ミルク")
    private val NOT_FAT = listOf("醤油", "しょう油", "油揚")
    private val NOT_LIQUID = listOf("水菜", "水煮", "水溶き", "水切り", "スキム", "粉ミルク", "脱脂")

    fun guess(name: String): IngredientRole {
        val n = name.trim()
        if (n.isEmpty()) return IngredientRole.OTHER
        return when {
            SUGAR.any { it in n } -> IngredientRole.SUGAR
            FAT.any { it in n } && NOT_FAT.none { it in n } -> IngredientRole.FAT
            FLOUR.any { it in n } && NOT_FLOUR.none { it in n } -> IngredientRole.FLOUR
            n.endsWith("粉") && NOT_FLOUR.none { it in n } -> IngredientRole.FLOUR
            YEAST.any { it in n } && "サフラン" !in n -> IngredientRole.YEAST
            "塩" in n && SALT_EXCLUDE.none { it in n } -> IngredientRole.SALT
            LIQUID.any { it in n } && NOT_LIQUID.none { it in n } -> IngredientRole.LIQUID
            else -> IngredientRole.OTHER
        }
    }
}

data class BakersLine(
    val ingredient: Ingredient,
    /** グラム換算できなければ null。 */
    val grams: Double?,
    /** 粉の合計を 100 としたときの割合。粉がなければ null。 */
    val percent: Double?,
)

data class BakersSummary(
    val flourGrams: Double,
    val totalGrams: Double,
    val lines: List<BakersLine>,
    /** 加水率 (水分 ÷ 粉 × 100)。 */
    val hydration: Double?,
    val salt: Double?,
    val yeast: Double?,
    val sugar: Double?,
    val fat: Double?,
) {
    val hasFlour: Boolean get() = flourGrams > 0

    /** グラムに換算できなかった行の数 (「卵 1個」など)。 */
    val unconvertedCount: Int get() = lines.count { it.grams == null && it.ingredient.amount.isNotBlank() }
}

/** ベーカーズ% (粉の合計を 100% とする配合表記) の計算と、配合の拡大縮小。 */
object BakersMath {

    fun summarize(ingredients: List<Ingredient>): BakersSummary {
        val grams = ingredients.map { Amounts.grams(it) }
        val flour = ingredients.indices.sumOf { i ->
            if (ingredients[i].role == IngredientRole.FLOUR) grams[i] ?: 0.0 else 0.0
        }
        fun pct(value: Double?): Double? = if (flour > 0 && value != null) value / flour * 100 else null
        fun roleTotal(role: IngredientRole): Double? {
            val idx = ingredients.indices.filter { ingredients[it].role == role }
            if (idx.isEmpty()) return null
            return idx.sumOf { grams[it] ?: 0.0 }
        }
        return BakersSummary(
            flourGrams = flour,
            totalGrams = grams.sumOf { it ?: 0.0 },
            lines = ingredients.indices.map { BakersLine(ingredients[it], grams[it], pct(grams[it])) },
            hydration = pct(roleTotal(IngredientRole.LIQUID)),
            salt = pct(roleTotal(IngredientRole.SALT)),
            yeast = pct(roleTotal(IngredientRole.YEAST)),
            sugar = pct(roleTotal(IngredientRole.SUGAR)),
            fat = pct(roleTotal(IngredientRole.FAT)),
        )
    }

    /** 加水率だけを求める。 */
    fun hydration(ingredients: List<Ingredient>): Double? = summarize(ingredients).hydration

    fun scale(ingredients: List<Ingredient>, factor: Double): List<Ingredient> =
        ingredients.map { Amounts.scale(it, factor) }

    /** 粉の合計が [targetFlourGrams] になるように全体を拡大縮小する。 */
    fun scaleToFlour(ingredients: List<Ingredient>, targetFlourGrams: Double): List<Ingredient> {
        val flour = summarize(ingredients).flourGrams
        if (flour <= 0 || targetFlourGrams <= 0) return ingredients
        return scale(ingredients, targetFlourGrams / flour)
    }

    /** 生地の総量 (g) が [targetTotalGrams] になるように拡大縮小する。 */
    fun scaleToTotal(ingredients: List<Ingredient>, targetTotalGrams: Double): List<Ingredient> {
        val total = summarize(ingredients).totalGrams
        if (total <= 0 || targetTotalGrams <= 0) return ingredients
        return scale(ingredients, targetTotalGrams / total)
    }

    /**
     * 加水率が [targetHydration] % になるように、水分の材料を比率を保ったまま増減する。
     * 粉がない・水分の材料がグラム換算できない場合はそのまま返す。
     */
    fun withHydration(ingredients: List<Ingredient>, targetHydration: Double): List<Ingredient> {
        val summary = summarize(ingredients)
        val current = summary.hydration ?: return ingredients
        if (!summary.hasFlour || current <= 0) return ingredients
        val factor = targetHydration / current
        return ingredients.map {
            if (it.role == IngredientRole.LIQUID && Amounts.grams(it) != null) Amounts.scale(it, factor) else it
        }
    }

    /** ベーカーズ% から材料のグラム数を作る (粉 [flourGrams] g のとき)。 */
    fun gramsFor(percent: Double, flourGrams: Double): Double = percent * flourGrams / 100
}
