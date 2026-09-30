package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.MixingMethod

/** こねている間の上昇温度の見積もり。 */
data class RiseEstimate(
    val value: Double,
    /** 見積もりに使った記録の数。0 ならこね方ごとの初期値。 */
    val samples: Int,
    val method: MixingMethod?,
) {
    val isDefault: Boolean get() = samples == 0
}

/** 仕込み水温の計算結果と注意書き。 */
data class WaterTempResult(
    val waterTemp: Double,
    val warning: String?,
)

/**
 * こね上げ温度と仕込み水温の計算。
 *
 * ストレート法: 仕込み水温 = (目標こね上げ温度 − 上昇温度) × 3 − (室温 + 粉温)
 * 種 (中種・ポーリッシュ・ルヴァンなど) を使うとき: × 4 にして種の温度も引く。
 *
 * 上昇温度 (こねている間の摩擦や手の熱で生地が温まる分) はこね方で変わるので、
 * 記録から「実際のこね上げ温度 − (室温 + 粉温 + 水温) ÷ 3」を平均して学習する。
 */
object DoughTemperature {

    const val DEFAULT_TARGET = 27.0

    fun waterTemp(
        targetDoughTemp: Double,
        roomTemp: Double,
        flourTemp: Double,
        rise: Double,
        prefermentTemp: Double? = null,
    ): WaterTempResult {
        val water = if (prefermentTemp == null) {
            (targetDoughTemp - rise) * 3 - (roomTemp + flourTemp)
        } else {
            (targetDoughTemp - rise) * 4 - (roomTemp + flourTemp + prefermentTemp)
        }
        return WaterTempResult(water, warningFor(water))
    }

    fun warningFor(waterTemp: Double): String? = when {
        waterTemp < 0 -> "水温が0℃を下回ります。氷水を使い、粉も冷蔵庫で冷やしておきましょう"
        waterTemp < 5 -> "かなり冷たい水が必要です。氷水を使うと合わせやすくなります"
        waterTemp > 45 -> "45℃を超えるとイーストが弱ります。40℃程度にとどめ、発酵時間を長めに見てください"
        waterTemp > 40 -> "高めの水温です。イーストに直接かけないようにしましょう"
        else -> null
    }

    /** 1 回の記録から実際の上昇温度を求める。必要な値がそろっていなければ null。 */
    fun observedRise(record: BakeRecord): Double? {
        val dough = record.doughTemp ?: return null
        val room = record.roomTemp ?: return null
        val water = record.waterTemp ?: return null
        val flour = record.flourTemp ?: room
        return dough - (room + flour + water) / 3
    }

    /**
     * 記録から上昇温度を学習する。同じこね方の記録を優先し、なければ全記録、それもなければ初期値。
     * 極端な値 (測り間違いなど) は除く。
     */
    fun learnedRise(records: List<BakeRecord>, method: MixingMethod?): RiseEstimate {
        fun usable(r: BakeRecord) = observedRise(r)?.takeIf { it in -5.0..20.0 }
        val sameMethod = records.filter { method != null && it.mixingMethod == method }.mapNotNull(::usable)
        if (sameMethod.isNotEmpty()) return RiseEstimate(sameMethod.average(), sameMethod.size, method)
        val default = method?.defaultRise ?: MixingMethod.HAND.defaultRise
        if (method != null) return RiseEstimate(default, 0, method)
        val all = records.mapNotNull(::usable)
        if (all.isNotEmpty()) return RiseEstimate(all.average(), all.size, null)
        return RiseEstimate(default, 0, null)
    }
}
