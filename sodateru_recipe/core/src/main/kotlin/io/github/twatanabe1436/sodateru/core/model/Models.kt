package io.github.twatanabe1436.sodateru.core.model

import kotlinx.serialization.Serializable

/** レシピの種類。パンは焼成ログ・ベーカーズ%・今日の提案の対象になる。 */
@Serializable
enum class Category(val label: String) {
    COOKING("料理"),
    BREAD("パン"),
    SWEETS("お菓子"),
}

/** ベーカーズ%の計算で使う材料の役割。 */
@Serializable
enum class IngredientRole(val label: String) {
    FLOUR("粉"),
    LIQUID("水分"),
    YEAST("酵母"),
    SALT("塩"),
    SUGAR("糖"),
    FAT("油脂"),
    OTHER("その他"),
}

/** こね方。こねている間に生地が温まる量 (上昇温度) の初期値が変わる。 */
@Serializable
enum class MixingMethod(val label: String, val defaultRise: Double) {
    HAND("手ごね", 3.0),
    BREAD_MACHINE("ホームベーカリー", 6.0),
    MIXER("ミキサー", 5.0),
    NO_KNEAD("こねない", 1.0),
}

/**
 * 材料 1 行。量は「200」「1/2」「少々」のような表記のまま持ち、計算するときに数値へ読み替える。
 * 単位は「g」「大さじ」「個」など。
 */
@Serializable
data class Ingredient(
    val name: String,
    val amount: String = "",
    val unit: String = "",
    val note: String = "",
    val role: IngredientRole = IngredientRole.OTHER,
)

/** パンのレシピの基本工程 (版ごとに持つ)。 */
@Serializable
data class BreadProcess(
    val targetDoughTemp: Double? = null,
    val mixingMethod: MixingMethod? = null,
    val firstProofMinutes: Int? = null,
    val firstProofTemp: Double? = null,
    val benchMinutes: Int? = null,
    val secondProofMinutes: Int? = null,
    val secondProofTemp: Double? = null,
    val bakeTemp: Int? = null,
    val bakeMinutes: Int? = null,
)

/**
 * レシピの 1 つの版。アレンジをレシピに反映するたびに新しい版が増え、古い版はそのまま残る。
 */
@Serializable
data class RecipeVersion(
    val number: Int,
    val createdAt: Long,
    val servings: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val memo: String = "",
    /** この版で何を変えたか (v1 は空)。 */
    val changeNote: String = "",
    /** この版のきっかけになった記録。 */
    val fromLogId: String? = null,
    val photos: List<String> = emptyList(),
    val process: BreadProcess? = null,
)

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val category: Category = Category.COOKING,
    val tags: List<String> = emptyList(),
    /** 出典 (本の名前・URL・「母から」など)。 */
    val source: String = "",
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    /** 番号順。最後が今の版。 */
    val versions: List<RecipeVersion>,
) {
    init {
        require(versions.isNotEmpty()) { "recipe must have at least one version" }
    }

    val current: RecipeVersion get() = versions.last()

    fun version(number: Int): RecipeVersion? = versions.firstOrNull { it.number == number }

    val coverPhoto: String?
        get() = versions.asReversed().firstNotNullOfOrNull { it.photos.firstOrNull() }
}

/** 焼き上がりの評価 (0 = 未評価, 1〜5)。 */
@Serializable
data class BakeScores(
    val rise: Int = 0,
    val crumb: Int = 0,
    val crust: Int = 0,
    val taste: Int = 0,
)

/**
 * パンを焼いたときの条件と工程。数値はすべて任意で、測ったものだけ入れればよい。
 * 温度は ℃、湿度は %、時間は分。
 */
@Serializable
data class BakeRecord(
    val roomTemp: Double? = null,
    val humidity: Double? = null,
    val flourTemp: Double? = null,
    val waterTemp: Double? = null,
    val targetDoughTemp: Double? = null,
    /** こね上げ温度 (実測)。 */
    val doughTemp: Double? = null,
    val mixingMethod: MixingMethod? = null,
    val mixingMinutes: Int? = null,
    /** 実際に使った配合。空ならその版の材料どおり。 */
    val ingredients: List<Ingredient> = emptyList(),
    val firstProofMinutes: Int? = null,
    /** 一次発酵をさせた場所の温度。空なら室温で発酵させたとみなす。 */
    val firstProofTemp: Double? = null,
    val benchMinutes: Int? = null,
    val secondProofMinutes: Int? = null,
    val secondProofTemp: Double? = null,
    val bakeTemp: Int? = null,
    val bakeMinutes: Int? = null,
    val weather: String = "",
    val scores: BakeScores = BakeScores(),
)

/**
 * 作った記録。料理なら「どうアレンジしたか・感想」、パンなら焼成ログ ([bake]) も持つ。
 */
@Serializable
data class CookLog(
    val id: String,
    val recipeId: String,
    val versionNumber: Int,
    /** 作った日時 (epoch ミリ秒)。 */
    val date: Long,
    /** 0 = 未評価, 1〜5。 */
    val rating: Int = 0,
    /** アレンジしたこと。 */
    val arrangement: String = "",
    /** 感想・次回へのメモ。 */
    val notes: String = "",
    val photos: List<String> = emptyList(),
    /** このアレンジを反映して作った版の番号。 */
    val appliedVersion: Int? = null,
    val bake: BakeRecord? = null,
    val createdAt: Long = date,
    val updatedAt: Long = date,
)

/** バックアップ・端末内保存の単位になる全データ。 */
@Serializable
data class AppData(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long = 0,
    val recipes: List<Recipe> = emptyList(),
    val logs: List<CookLog> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
