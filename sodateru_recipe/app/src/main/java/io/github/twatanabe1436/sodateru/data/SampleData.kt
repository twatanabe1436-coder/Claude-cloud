package io.github.twatanabe1436.sodateru.data

import io.github.twatanabe1436.sodateru.core.IngredientRoles
import io.github.twatanabe1436.sodateru.core.model.AppData
import io.github.twatanabe1436.sodateru.core.model.BakeRecord
import io.github.twatanabe1436.sodateru.core.model.BakeScores
import io.github.twatanabe1436.sodateru.core.model.BreadProcess
import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.CookLog
import io.github.twatanabe1436.sodateru.core.model.Ingredient
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import io.github.twatanabe1436.sodateru.core.model.Recipe
import io.github.twatanabe1436.sodateru.core.model.RecipeVersion

/** 使い方を試すためのサンプル (設定画面・最初の画面から追加できる)。 */
object SampleData {

    private const val DAY = 24 * 60 * 60 * 1000L

    private fun ing(name: String, amount: String, unit: String = "g", note: String = "") =
        Ingredient(name, amount, unit, note, IngredientRoles.guess(name))

    private fun breadIngredients(water: String) = listOf(
        ing("強力粉", "250"),
        ing("砂糖", "17"),
        ing("塩", "5"),
        ing("スキムミルク", "6"),
        ing("インスタントドライイースト", "2.5"),
        ing("水", water),
        ing("無塩バター", "12"),
    )

    fun create(now: Long = System.currentTimeMillis()): AppData {
        val breadId = "sample-shokupan"
        val bread = Recipe(
            id = breadId,
            title = "山型食パン（サンプル）",
            category = Category.BREAD,
            tags = listOf("食パン", "毎週"),
            source = "サンプル",
            createdAt = now - 120 * DAY,
            updatedAt = now - 30 * DAY,
            versions = listOf(
                RecipeVersion(
                    number = 1,
                    createdAt = now - 120 * DAY,
                    servings = "1斤型",
                    ingredients = breadIngredients("180"),
                    steps = listOf(
                        "バター以外の材料をボウルに入れ、ひとまとまりになるまで混ぜる",
                        "台に出して10分こね、バターを加えてさらに10分こねる",
                        "こね上げ温度を測り、28℃前後で一次発酵（2倍になるまで）",
                        "2分割して丸め、ベンチタイム15分",
                        "成形して型に入れ、型の9分目まで二次発酵",
                        "190℃に予熱したオーブンで28分焼く",
                    ),
                    memo = "こね上げ温度は27℃が目標",
                    process = BreadProcess(
                        targetDoughTemp = 27.0,
                        mixingMethod = MixingMethod.HAND,
                        firstProofMinutes = 60,
                        firstProofTemp = 28.0,
                        benchMinutes = 15,
                        secondProofMinutes = 45,
                        secondProofTemp = 35.0,
                        bakeTemp = 190,
                        bakeMinutes = 28,
                    ),
                ),
                RecipeVersion(
                    number = 2,
                    createdAt = now - 30 * DAY,
                    servings = "1斤型",
                    ingredients = breadIngredients("172"),
                    steps = listOf(
                        "バター以外の材料をボウルに入れ、ひとまとまりになるまで混ぜる",
                        "台に出して10分こね、バターを加えてさらに10分こねる",
                        "こね上げ温度を測り、28℃前後で一次発酵（2倍になるまで）",
                        "2分割して丸め、ベンチタイム15分",
                        "成形して型に入れ、型の9分目まで二次発酵",
                        "200℃に予熱し、190℃に下げて28分焼く",
                    ),
                    memo = "こね上げ温度は27℃が目標。湿度が高い日は水を少し残して様子を見る",
                    changeNote = "梅雨どきにべたついたので加水を下げた。予熱を高めに",
                    fromLogId = "sample-bake-4",
                    process = BreadProcess(
                        targetDoughTemp = 27.0,
                        mixingMethod = MixingMethod.HAND,
                        firstProofMinutes = 55,
                        firstProofTemp = 28.0,
                        benchMinutes = 15,
                        secondProofMinutes = 45,
                        secondProofTemp = 35.0,
                        bakeTemp = 190,
                        bakeMinutes = 28,
                    ),
                ),
            ),
        )

        data class B(
            val daysAgo: Int, val version: Int, val rating: Int, val room: Double, val humidity: Double,
            val water: String, val waterTemp: Double, val dough: Double, val first: Int, val second: Int,
            val scores: BakeScores, val arrangement: String, val notes: String, val weather: String,
        )

        val bakes = listOf(
            B(110, 1, 4, 18.0, 45.0, "180", 36.0, 26.5, 75, 55, BakeScores(4, 4, 4, 4), "", "冬なので水温高め。ちょうどよい", "晴れ"),
            B(95, 1, 5, 20.0, 50.0, "180", 33.0, 27.0, 65, 50, BakeScores(5, 4, 5, 5), "", "理想的な膨らみ", "晴れ"),
            B(80, 1, 3, 23.0, 62.0, "180", 27.0, 27.5, 60, 45, BakeScores(3, 3, 4, 4), "", "少しべたついた", "くもり"),
            B(60, 1, 2, 26.0, 78.0, "180", 20.0, 28.5, 50, 40, BakeScores(2, 2, 3, 3), "水を8g減らせばよかった", "梅雨。生地がだれて横に広がった", "雨"),
            B(45, 1, 4, 27.0, 75.0, "172", 17.0, 27.0, 50, 40, BakeScores(4, 4, 4, 4), "水を180g→172gに減らした", "扱いやすかった", "雨"),
            B(20, 2, 5, 28.0, 72.0, "170", 15.0, 27.0, 45, 40, BakeScores(5, 5, 4, 5), "さらに水を2g減らした", "今までで一番きれいな山", "くもり"),
            B(7, 2, 4, 24.0, 60.0, "172", 24.0, 27.5, 55, 45, BakeScores(4, 4, 4, 4), "", "安定", "晴れ"),
        )
        val bakeLogs = bakes.mapIndexed { i, b ->
            CookLog(
                id = "sample-bake-${i + 1}",
                recipeId = breadId,
                versionNumber = b.version,
                date = now - b.daysAgo * DAY,
                rating = b.rating,
                arrangement = b.arrangement,
                notes = b.notes,
                appliedVersion = if (i == 3 || i == 4) 2 else null,
                bake = BakeRecord(
                    roomTemp = b.room,
                    humidity = b.humidity,
                    flourTemp = b.room - 1,
                    waterTemp = b.waterTemp,
                    targetDoughTemp = 27.0,
                    doughTemp = b.dough,
                    mixingMethod = MixingMethod.HAND,
                    mixingMinutes = 20,
                    ingredients = breadIngredients(b.water),
                    firstProofMinutes = b.first,
                    firstProofTemp = 28.0,
                    benchMinutes = 15,
                    secondProofMinutes = b.second,
                    secondProofTemp = 35.0,
                    bakeTemp = 190,
                    bakeMinutes = 28,
                    weather = b.weather,
                    scores = b.scores,
                ),
            )
        }

        val gingerId = "sample-shogayaki"
        val ginger = Recipe(
            id = gingerId,
            title = "豚の生姜焼き（サンプル）",
            category = Category.COOKING,
            tags = listOf("主菜", "定番"),
            source = "料理本",
            createdAt = now - 90 * DAY,
            updatedAt = now - 10 * DAY,
            versions = listOf(
                RecipeVersion(
                    number = 1,
                    createdAt = now - 90 * DAY,
                    servings = "2人分",
                    ingredients = listOf(
                        ing("豚ロース薄切り", "250"),
                        ing("玉ねぎ", "1/2", "個", "薄切り"),
                        ing("醤油", "2", "大さじ"),
                        ing("みりん", "2", "大さじ"),
                        ing("酒", "1", "大さじ"),
                        ing("砂糖", "1", "大さじ"),
                        ing("しょうが", "1", "かけ", "すりおろし"),
                    ),
                    steps = listOf(
                        "調味料としょうがを混ぜてタレを作る",
                        "豚肉にタレの半量をもみこんで10分おく",
                        "フライパンで玉ねぎと豚肉を焼く",
                        "残りのタレを加えて絡める",
                    ),
                ),
                RecipeVersion(
                    number = 2,
                    createdAt = now - 10 * DAY,
                    servings = "2人分",
                    ingredients = listOf(
                        ing("豚ロース薄切り", "250"),
                        ing("玉ねぎ", "1/2", "個", "薄切り"),
                        ing("醤油", "2", "大さじ"),
                        ing("みりん", "2", "大さじ"),
                        ing("酒", "1", "大さじ"),
                        ing("砂糖", "1/2", "大さじ"),
                        ing("しょうが", "2", "かけ", "すりおろし"),
                        ing("片栗粉", "1", "小さじ", "肉にまぶす"),
                    ),
                    steps = listOf(
                        "調味料としょうがを混ぜてタレを作る",
                        "豚肉に片栗粉を薄くまぶす",
                        "フライパンで玉ねぎと豚肉を焼く",
                        "タレを加えて絡める",
                    ),
                    changeNote = "甘さ控えめ・しょうが多め。片栗粉でタレがよく絡むように",
                    fromLogId = "sample-cook-2",
                ),
            ),
        )
        val cookLogs = listOf(
            CookLog(
                id = "sample-cook-1", recipeId = gingerId, versionNumber = 1, date = now - 60 * DAY, rating = 3,
                arrangement = "", notes = "少し甘い",
            ),
            CookLog(
                id = "sample-cook-2", recipeId = gingerId, versionNumber = 1, date = now - 12 * DAY, rating = 4,
                arrangement = "砂糖を半分に、しょうがを倍に。肉に片栗粉をまぶした", notes = "家族に好評。この味で固定",
                appliedVersion = 2,
            ),
            CookLog(
                id = "sample-cook-3", recipeId = gingerId, versionNumber = 2, date = now - 3 * DAY, rating = 5,
                arrangement = "玉ねぎを1個に増やした", notes = "玉ねぎ多めもおいしい。次もこれで",
            ),
        )
        return AppData(recipes = listOf(bread, ginger), logs = bakeLogs + cookLogs)
    }
}
