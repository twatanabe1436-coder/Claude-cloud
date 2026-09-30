package io.github.twatanabe1436.sodateru.core

import io.github.twatanabe1436.sodateru.core.model.Category
import io.github.twatanabe1436.sodateru.core.model.Ingredient

/** 写真の文字起こしから作ったレシピの下書き。フォームに流し込んで、人が確認・修正する。 */
data class RecipeDraft(
    val title: String = "",
    val servings: String = "",
    val category: Category? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val memo: String = "",
    /** 読み取った文字をそのまま並べたもの。 */
    val transcript: String = "",
)

/**
 * 端末内 OCR が返したテキストを「材料」「作り方」「メモ」に振り分ける。
 * 見出し (材料・作り方・ポイント) があればそれに従い、なければ行の形 (量がある / 番号で始まる) から推測する。
 */
object RecipeTextParser {

    private const val QTY = "\\d+(?:\\.\\d+)?(?:と\\d+/\\d+|/\\d+)?(?:\\s*[〜~～-]\\s*\\d+(?:\\.\\d+)?(?:/\\d+)?)?"
    private const val SUFFIX_UNITS =
        "kg|g|ml|mL|ML|cc|l|L|個|本|枚|片|かけ|束|袋|缶|合|切れ|房|玉|丁|尾|杯|粒|つまみ|パック|株|cm|センチ|粒|さや|ケ"
    private const val KEYWORDS = "少々|適量|適宜|少量|ひとつまみ|ひとつかみ|お好みで|好みで|ひとかけ"
    private const val PREFIX_UNITS = "大さじ|小さじ|大匙|小匙|大サジ|小サジ|カップ"

    private val bullet = Regex("^[・●○◯◎◆◇■□☆★\\-*•＊※]+\\s*")
    private val paren = Regex("[（(]([^（）()]*)[）)]")
    private val separators = Regex("…+|‥+|\\.{2,}|・{2,}|：|:|\\s{2,}|\\t")
    private val keywordLine = Regex("^(.*?)\\s*($KEYWORDS)$")
    private val prefixUnitLine = Regex("^(.*?)\\s*($PREFIX_UNITS)\\s*($QTY)$")
    private val suffixUnitLine = Regex("^(.*?)\\s*($QTY)\\s*($SUFFIX_UNITS)?$")
    private val groupLabel = Regex("^[\\[（(【<＜〈]?[A-Za-z][\\]）)】>＞〉]?$")
    private val groupPrefix = Regex("^[\\[（(【<＜〈]?[A-Z][\\]）)】>＞〉]?\\s+")

    private val stepLine = Regex(
        "^(?:(?:STEP|Step|step|手順)\\s*\\d+[.)、．]?|\\d{1,2}\\s*[.)．、）]|[(（]\\d{1,2}[)）]|[\\u2460-\\u2473])\\s*(.*)$",
    )
    private val stepLooseLine = Regex("^\\d{1,2}\\s+(\\D.*)$")

    private val headerDecoration = "[【\\[<〈《■◆●□◇＜]?\\s*"
    private val ingredientHeader = Regex("^$headerDecoration(材料|ざいりょう|用意するもの)")
    private val stepHeader = Regex("^$headerDecoration(作り方|つくり方|作りかた|つくりかた|手順|工程)")
    private val memoHeader = Regex("^$headerDecoration(ポイント|コツ|メモ|MEMO|Memo|memo|備考|アレンジ|保存)")
    private val servingsPattern = Regex(
        "(\\d+(?:\\s*[〜~～-]\\s*\\d+)?\\s*(?:人分|人前|個分|枚分|本分|切れ分|台分|斤分|斤|皿分|杯分)|\\d+cm[^\\s)）]*型[^\\s)）]*)",
    )

    private enum class Mode { NONE, INGREDIENTS, STEPS, MEMO }

    /** 1 行を材料として読む。量が読み取れなければ null。 */
    fun parseIngredientLine(raw: String): Ingredient? {
        var s = Amounts.normalize(raw).replace(bullet, "")
        val notes = paren.findAll(s).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
        s = s.replace(paren, " ").replace(separators, " ").replace(Regex("\\s+"), " ").trim()
        s = s.replace(groupPrefix, "")
        if (s.isEmpty()) return null

        fun make(name: String, amount: String, unit: String): Ingredient? {
            val n = name.trim().trimEnd('.', '・', '…', '　')
            if (n.isEmpty() || n.all { it.isDigit() || it == '.' || it == '/' }) return null
            return Ingredient(
                name = n,
                amount = amount.replace(" ", ""),
                unit = normalizeUnit(unit),
                note = notes.joinToString("・"),
                role = IngredientRoles.guess(n),
            )
        }

        keywordLine.matchEntire(s)?.let { m -> return make(m.groupValues[1], m.groupValues[2], "") }
        prefixUnitLine.matchEntire(s)?.let { m -> return make(m.groupValues[1], m.groupValues[3], m.groupValues[2]) }
        suffixUnitLine.matchEntire(s)?.let { m -> return make(m.groupValues[1], m.groupValues[2], m.groupValues[3]) }
        return null
    }

    /** 量だけの行 (「250g」「大さじ1」「少々」)。表の列が分かれて読まれたときに使う。 */
    private fun parseQuantityOnly(line: String): Pair<String, String>? {
        val s = Amounts.normalize(line).replace(bullet, "").replace(Regex("\\s+"), " ").trim()
        Regex("^($KEYWORDS)$").matchEntire(s)?.let { return it.groupValues[1] to "" }
        Regex("^($PREFIX_UNITS)\\s*($QTY)$").matchEntire(s)?.let {
            return it.groupValues[2].replace(" ", "") to normalizeUnit(it.groupValues[1])
        }
        Regex("^($QTY)\\s*($SUFFIX_UNITS)$").matchEntire(s)?.let {
            return it.groupValues[1].replace(" ", "") to normalizeUnit(it.groupValues[2])
        }
        return null
    }

    private fun normalizeUnit(unit: String): String = when (unit) {
        "大匙", "大サジ" -> "大さじ"
        "小匙", "小サジ" -> "小さじ"
        "mL", "ML" -> "ml"
        "ケ" -> "個"
        else -> unit
    }

    private fun stepText(line: String): String? {
        val s = Amounts.normalize(line)
        stepLine.matchEntire(s)?.let { return it.groupValues[1].trim() }
        stepLooseLine.matchEntire(s)?.let { return it.groupValues[1].trim() }
        return null
    }

    fun parse(text: String): RecipeDraft {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var mode = Mode.NONE
        var title = ""
        var servings = ""
        val ingredients = mutableListOf<Ingredient>()
        val steps = mutableListOf<String>()
        val memo = mutableListOf<String>()

        fun addStep(textPart: String) {
            if (textPart.isNotEmpty()) steps += textPart
        }

        for (line in lines) {
            val normalized = Amounts.normalize(line)
            if (servings.isEmpty()) servingsPattern.find(normalized)?.let { servings = it.value.replace(" ", "") }

            val isShort = normalized.length <= 24
            when {
                isShort && ingredientHeader.containsMatchIn(normalized) -> { mode = Mode.INGREDIENTS; continue }
                isShort && stepHeader.containsMatchIn(normalized) -> { mode = Mode.STEPS; continue }
                isShort && memoHeader.containsMatchIn(normalized) -> { mode = Mode.MEMO; continue }
            }
            if (groupLabel.matches(normalized)) continue

            when (mode) {
                Mode.NONE -> {
                    val step = stepText(line)
                    val ing = parseIngredientLine(line)
                    when {
                        step != null -> { addStep(step); mode = Mode.STEPS }
                        ing != null -> { ingredients += ing; mode = Mode.INGREDIENTS }
                        title.isEmpty() && servingsPattern.matchEntire(normalized) == null -> title = normalized
                        servingsPattern.matchEntire(normalized) != null -> Unit
                        else -> memo += normalized
                    }
                }
                Mode.INGREDIENTS -> {
                    val qty = parseQuantityOnly(line)
                    val pending = ingredients.indexOfFirst { it.amount.isEmpty() }
                    val step = stepText(line)
                    when {
                        qty != null && pending >= 0 ->
                            ingredients[pending] = ingredients[pending].copy(amount = qty.first, unit = qty.second)
                        qty != null -> Unit
                        step != null -> { addStep(step); mode = Mode.STEPS }
                        else -> {
                            val ing = parseIngredientLine(line)
                            if (ing != null) {
                                ingredients += ing
                            } else if (servingsPattern.matchEntire(normalized) == null) {
                                val name = normalized.replace(bullet, "").trim()
                                ingredients += Ingredient(name = name, role = IngredientRoles.guess(name))
                            }
                        }
                    }
                }
                Mode.STEPS -> {
                    val step = stepText(line)
                    when {
                        step != null -> addStep(step)
                        steps.isEmpty() -> addStep(normalized.replace(bullet, ""))
                        else -> steps[steps.lastIndex] = steps.last() + normalized
                    }
                }
                Mode.MEMO -> memo += normalized
            }
        }

        if (title.isEmpty() && memo.isNotEmpty() && ingredients.isEmpty() && steps.isEmpty()) {
            title = memo.removeAt(0)
        }
        return RecipeDraft(
            title = title,
            servings = servings,
            ingredients = ingredients,
            steps = steps,
            memo = memo.joinToString("\n"),
            transcript = text.trim(),
        )
    }
}
