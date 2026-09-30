package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.core.model.Recipe
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 今日の仕込み条件。室温以外は分かるものだけでよい。 */
data class TodayConditions(
    val roomTemp: Double,
    val humidity: Double? = null,
    /** 粉の温度。空なら室温と同じとみなす。 */
    val flourTemp: Double? = null,
    /** 発酵器・オーブンの発酵機能などを使うときの温度。空ならレシピの工程の発酵温度、それもなければ室温。 */
    val firstProofTemp: Double? = null,
    val secondProofTemp: Double? = null,
    val mixingMethod: MixingMethod? = null,
    val targetDoughTemp: Double? = null,
    /** 種 (中種・ルヴァンなど) の温度。種を使うときだけ。 */
    val prefermentTemp: Double? = null,
)

/** 似た条件だった過去の回。 */
data class Reference(
    val sample: BakeSample,
    /** 条件の近さ (0 が同じ条件。室温 3℃ または湿度 15% の差がそれぞれ 1 に相当)。 */
    val distance: Double,
)

data class NumberSuggestion(
    val value: Double,
    val basis: String,
    val samples: Int,
)

enum class Confidence(val label: String) {
    NONE("記録がまだありません"),
    LOW("参考程度"),
    MEDIUM("まずまず"),
    HIGH("信頼できる"),
}

data class BakeAdvice(
    val targetDoughTemp: Double,
    val rise: RiseEstimate,
    val water: WaterTempResult,
    /** レシピ (今の版) の加水率。 */
    val baseHydration: Double?,
    val hydration: NumberSuggestion?,
    /** 今の版の材料を、おすすめの加水率に合わせて水分だけ増減したもの。 */
    val adjustedIngredients: List<Ingredient>?,
    val firstProof: NumberSuggestion?,
    val secondProof: NumberSuggestion?,
    val references: List<Reference>,
    val trends: List<String>,
    val notes: List<String>,
    val confidence: Confidence,
)

/**
 * 「今日の室温・湿度だと、どう仕込めばよいか」を過去の焼成ログから提案する。
 *
 * - 仕込み水温: こね上げ温度の式に、記録から学習した上昇温度を入れて計算
 * - 加水率: 条件が近く評価の高い回ほど重く見た加重平均 (近さはガウス重み、評価は★5=3, ★4=2, ★3=0.7)
 * - 発酵時間: 各回の時間を「温度が10℃上がると発酵が約2倍速くなる (Q10=2)」目安で今日の温度に補正し、同じ重みで平均
 */
object BakeAdvisor {

    const val TEMP_SCALE = 3.0
    const val HUMIDITY_SCALE = 15.0
    const val Q10 = 2.0

    fun distance(bake: BakeRecord, today: TodayConditions): Double? {
        val t = bake.roomTemp ?: return null
        var d2 = ((t - today.roomTemp) / TEMP_SCALE).pow(2)
        val h = bake.humidity
        val h0 = today.humidity
        if (h != null && h0 != null) d2 += ((h - h0) / HUMIDITY_SCALE).pow(2)
        return sqrt(d2)
    }

    fun ratingWeight(rating: Int): Double = when (rating) {
        5 -> 3.0
        4 -> 2.0
        3 -> 0.7
        else -> 0.0
    }

    /** [fromTemp] ℃ で [minutes] 分かかった発酵を [toTemp] ℃ で行うときの目安時間。 */
    fun adjustProofMinutes(minutes: Double, fromTemp: Double, toTemp: Double): Double =
        minutes * Q10.pow((fromTemp - toTemp) / 10)

    /**
     * @param samples このレシピの焼成ログ
     * @param allRecords 全パンの焼成ログ (上昇温度の学習に使う。こね方の癖はレシピによらないため)
     */
    fun advise(
        recipe: Recipe?,
        samples: List<BakeSample>,
        allRecords: List<BakeRecord>,
        today: TodayConditions,
    ): BakeAdvice {
        val process = recipe?.current?.process
        val target = today.targetDoughTemp ?: process?.targetDoughTemp ?: DoughTemperature.DEFAULT_TARGET
        val method = today.mixingMethod ?: process?.mixingMethod
        val rise = DoughTemperature.learnedRise(allRecords, method)
        val water = DoughTemperature.waterTemp(
            targetDoughTemp = target,
            roomTemp = today.roomTemp,
            flourTemp = today.flourTemp ?: today.roomTemp,
            rise = rise.value,
            prefermentTemp = today.prefermentTemp,
        )

        data class Weighted(val sample: BakeSample, val distance: Double, val weight: Double)

        val rated = samples.mapNotNull { s ->
            val d = distance(s.bake, today) ?: return@mapNotNull null
            if (s.rating <= 0) return@mapNotNull null
            Weighted(s, d, exp(-d * d / 2) * ratingWeight(s.rating))
        }
        val useful = rated.filter { it.weight > 0 }
        val good = useful.filter { it.sample.rating >= 4 }

        val references = rated.sortedWith(compareBy<Weighted> { it.distance }.thenByDescending { it.sample.rating })
            .take(3)
            .map { Reference(it.sample, it.distance) }

        val basisPrefix = if (good.isNotEmpty()) {
            "条件の近い★4以上の${good.size}回を中心にした加重平均"
        } else {
            "★3の記録からの推定（参考程度）"
        }

        // 加水率
        val baseIngredients = recipe?.current?.ingredients.orEmpty()
        val baseHydration = BakersMath.hydration(baseIngredients)
        val withHydration = useful.filter { it.sample.hydration != null }
        val hydration = Stats.weightedMean(
            withHydration.map { it.sample.hydration!! },
            withHydration.map { it.weight },
        )?.let { NumberSuggestion(it, basisPrefix, withHydration.size) }
        val adjusted = if (hydration != null && baseHydration != null && abs(hydration.value - baseHydration) >= 0.05) {
            BakersMath.withHydration(baseIngredients, hydration.value)
        } else {
            null
        }

        // 発酵時間。今日の発酵温度が空なら、いつもの発酵温度 (レシピの工程) で発酵させるとみなし、それもなければ室温
        val firstTodayTemp = today.firstProofTemp ?: process?.firstProofTemp ?: today.roomTemp
        val secondTodayTemp = today.secondProofTemp ?: process?.secondProofTemp ?: today.roomTemp
        val firstProof = proofSuggestion(
            useful.mapNotNull { w ->
                val m = w.sample.bake.firstProofMinutes ?: return@mapNotNull null
                val t = w.sample.firstProofTemp ?: return@mapNotNull null
                adjustProofMinutes(m.toDouble(), t, firstTodayTemp) to w.weight
            },
            basisPrefix,
            fallbackMinutes = process?.firstProofMinutes,
            fallbackTemp = process?.firstProofTemp,
            todayTemp = firstTodayTemp,
        )
        val secondProof = proofSuggestion(
            useful.mapNotNull { w ->
                val m = w.sample.bake.secondProofMinutes ?: return@mapNotNull null
                val t = w.sample.secondProofTemp ?: return@mapNotNull null
                adjustProofMinutes(m.toDouble(), t, secondTodayTemp) to w.weight
            },
            basisPrefix,
            fallbackMinutes = process?.secondProofMinutes,
            fallbackTemp = process?.secondProofTemp,
            todayTemp = secondTodayTemp,
        )

        val nearest = rated.minOfOrNull { it.distance }
        val confidence = when {
            useful.isEmpty() -> Confidence.NONE
            good.size >= 5 && nearest != null && nearest <= 1.0 -> Confidence.HIGH
            good.size >= 2 && nearest != null && nearest <= 1.5 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        val notes = buildList {
            if (samples.isEmpty()) {
                add("このレシピの焼成ログがまだありません。今日の条件と結果を記録すると、次から過去の回をもとに提案できるようになります。")
            } else if (useful.isEmpty()) {
                add("室温と総合評価（★3以上）が入った記録がまだないため、配合と発酵時間はレシピの目安のままです。")
            }
            if (nearest != null && nearest > 1.5) {
                add("過去の記録とは条件がかなり違います。提案は参考程度にして、生地の様子を見ながら調整してください。")
            }
            water.warning?.let(::add)
            if (rise.isDefault) {
                val label = method?.label ?: "こね方"
                add("上昇温度は${label}の目安（+${Amounts.formatNumber(rise.value)}℃）です。仕込み水温とこね上げ温度を測って記録すると、あなたのこね方に合わせて学習します。")
            }
            val h = today.humidity
            if (h != null && h >= 70) {
                add("湿度が高い日です。水は5%ほど残しておき、生地の様子を見ながら足すと失敗しにくくなります。")
            } else if (h != null && h <= 35) {
                add("乾燥している日です。発酵中に生地の表面が乾かないよう、ラップや濡れ布巾をかけましょう。")
            }
        }

        return BakeAdvice(
            targetDoughTemp = target,
            rise = rise,
            water = water,
            baseHydration = baseHydration,
            hydration = hydration,
            adjustedIngredients = adjusted,
            firstProof = firstProof,
            secondProof = secondProof,
            references = references,
            trends = trends(samples),
            notes = notes,
            confidence = confidence,
        )
    }

    private fun proofSuggestion(
        adjusted: List<Pair<Double, Double>>,
        basis: String,
        fallbackMinutes: Int?,
        fallbackTemp: Double?,
        todayTemp: Double,
    ): NumberSuggestion? {
        Stats.weightedMean(adjusted.map { it.first }, adjusted.map { it.second })?.let {
            return NumberSuggestion(roundTo5(it), "$basis（温度で補正）", adjusted.size)
        }
        val m = fallbackMinutes ?: return null
        return if (fallbackTemp != null) {
            NumberSuggestion(roundTo5(adjustProofMinutes(m.toDouble(), fallbackTemp, todayTemp)), "レシピの目安を温度で補正", 0)
        } else {
            NumberSuggestion(m.toDouble(), "レシピの目安", 0)
        }
    }

    private fun roundTo5(minutes: Double): Double = ((minutes / 5).roundToInt() * 5).coerceAtLeast(5).toDouble()

    /** 評価の高い回に見られる傾向 (記録が 4 回以上たまってから)。 */
    fun trends(samples: List<BakeSample>): List<String> {
        val good = samples.filter { it.rating >= 4 }
        val pairs = listOf(
            BakeMetric.HUMIDITY to BakeMetric.HYDRATION,
            BakeMetric.ROOM_TEMP to BakeMetric.HYDRATION,
            BakeMetric.FIRST_PROOF_TEMP to BakeMetric.FIRST_PROOF,
            BakeMetric.DOUGH_TEMP to BakeMetric.FIRST_PROOF,
        )
        return pairs.mapNotNull { (x, y) ->
            val cmp = BakeMetrics.compare(good, x, y)
            val fit = cmp.fit ?: return@mapNotNull null
            if (cmp.points.size < 4 || abs(fit.r) < 0.4) return@mapNotNull null
            "うまくいった回では、" + cmp.describe(x, y)
        }
    }
}
