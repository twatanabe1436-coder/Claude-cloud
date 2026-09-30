package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Recipe

/** 焼成ログ 1 回分と、その回の加水率。 */
data class BakeSample(
    val log: CookLog,
    val bake: BakeRecord,
    val hydration: Double?,
) {
    val rating: Int get() = log.rating

    /** 一次発酵をさせた温度 (記録がなければ室温)。 */
    val firstProofTemp: Double? get() = bake.firstProofTemp ?: bake.roomTemp

    val secondProofTemp: Double? get() = bake.secondProofTemp ?: bake.roomTemp
}

object BakeSamples {

    /** 焼成ログを持つ記録を集める。加水率はその回の配合、なければ作った版の材料から求める。 */
    fun from(recipes: List<Recipe>, logs: List<CookLog>): List<BakeSample> {
        val byId = recipes.associateBy { it.id }
        return logs.mapNotNull { log ->
            val bake = log.bake ?: return@mapNotNull null
            val ingredients = bake.ingredients.ifEmpty {
                byId[log.recipeId]?.version(log.versionNumber)?.ingredients.orEmpty()
            }
            BakeSample(log, bake, BakersMath.hydration(ingredients))
        }.sortedBy { it.log.date }
    }

    fun breadRecipes(recipes: List<Recipe>): List<Recipe> = recipes.filter { it.category == Category.BREAD }
}

/** 研究ノート (散布図) の軸にできる項目。 */
enum class BakeMetric(val label: String, val unit: String, private val extractor: (BakeSample) -> Double?) {
    ROOM_TEMP("室温", "℃", { it.bake.roomTemp }),
    HUMIDITY("湿度", "%", { it.bake.humidity }),
    FLOUR_TEMP("粉温", "℃", { it.bake.flourTemp }),
    WATER_TEMP("仕込み水温", "℃", { it.bake.waterTemp }),
    DOUGH_TEMP("こね上げ温度", "℃", { it.bake.doughTemp }),
    MIXING_RISE("こねによる上昇温度", "℃", { DoughTemperature.observedRise(it.bake) }),
    HYDRATION("加水率", "%", { it.hydration }),
    FIRST_PROOF("一次発酵の時間", "分", { it.bake.firstProofMinutes?.toDouble() }),
    FIRST_PROOF_TEMP("一次発酵の温度", "℃", { it.firstProofTemp }),
    SECOND_PROOF("二次発酵の時間", "分", { it.bake.secondProofMinutes?.toDouble() }),
    BAKE_TEMP("焼成温度", "℃", { it.bake.bakeTemp?.toDouble() }),
    BAKE_TIME("焼成時間", "分", { it.bake.bakeMinutes?.toDouble() }),
    RATING("総合評価", "★", { it.log.rating.takeIf { r -> r > 0 }?.toDouble() }),
    SCORE_RISE("ふくらみ", "★", { it.bake.scores.rise.takeIf { r -> r > 0 }?.toDouble() }),
    SCORE_CRUMB("内相", "★", { it.bake.scores.crumb.takeIf { r -> r > 0 }?.toDouble() }),
    SCORE_CRUST("皮", "★", { it.bake.scores.crust.takeIf { r -> r > 0 }?.toDouble() }),
    SCORE_TASTE("味", "★", { it.bake.scores.taste.takeIf { r -> r > 0 }?.toDouble() }),
    ;

    fun valueOf(sample: BakeSample): Double? = extractor(sample)
}

data class MetricPoint(val x: Double, val y: Double, val sample: BakeSample)

data class MetricComparison(
    val points: List<MetricPoint>,
    val fit: LinearFit?,
) {
    /** 「室温が1℃上がると一次発酵の時間が約6分短い傾向」のような説明。 */
    fun describe(x: BakeMetric, y: BakeMetric): String? {
        val f = fit ?: return null
        val step = when (x.unit) {
            "%" -> 10.0
            else -> 1.0
        }
        val delta = f.slope * step
        val dir = if (delta >= 0) "高い" else "低い"
        val xStep = "${Amounts.formatNumber(step)}${x.unit}"
        return "${x.label}が${xStep}上がると、${y.label}は約${Amounts.formatNumber(kotlin.math.abs(delta))}${y.unit}${dir}" +
            "傾向（${Stats.describeCorrelation(f.r)}・r=${"%.2f".format(f.r)}・${f.n}回）"
    }
}

object BakeMetrics {
    fun compare(samples: List<BakeSample>, x: BakeMetric, y: BakeMetric, minRating: Int = 0): MetricComparison {
        val points = samples.filter { it.rating >= minRating }.mapNotNull { s ->
            val xv = x.valueOf(s) ?: return@mapNotNull null
            val yv = y.valueOf(s) ?: return@mapNotNull null
            MetricPoint(xv, yv, s)
        }
        return MetricComparison(points, Stats.linearFit(points.map { it.x }, points.map { it.y }))
    }
}
