package io.github.twatanabe1436.hanaso.core

/**
 * 音声認識の結果を整える。
 *
 * - 文頭を大文字に、i を I に、文末に . か ? を付ける
 * - いまのお題で言うはずの語 (台本のお手本・キーワード、見ているヒント) に綴りがとても近い語は、
 *   聞き間違いとみなして直す (例: lotte → latte、penicilin → penicillin)
 * - 相手のセリフなどに出てきた固有名詞は、その書き方にそろえる (例: tanaka → Tanaka)
 *
 * 文法の間違い (a が抜けている、時制が違う、複数形の s がないなど) は直さない。学習者の間違いまで
 * 消してしまわないように、よく使う語や短い語は変えず、候補が 1 つに決まるときだけ直す。
 */
object Transcript {

    /** 聞き間違いとして直さない、よく使う語 (別の語に化けたとしても学習者の言い間違いかもしれない) */
    private val COMMON = setOf(
        "a", "an", "the", "i", "you", "he", "she", "it", "we", "they", "me", "my", "your", "our", "their", "his", "her",
        "its", "us", "them", "is", "am", "are", "was", "were", "be", "been", "being", "do", "does", "did", "done",
        "have", "has", "had", "can", "could", "will", "would", "shall", "should", "may", "might", "must", "to", "of",
        "in", "on", "at", "for", "with", "by", "from", "up", "down", "out", "off", "over", "into", "and", "or", "but",
        "so", "if", "not", "no", "yes", "this", "that", "these", "those", "there", "here", "what", "where", "when",
        "why", "how", "who", "which", "get", "got", "go", "went", "gone", "like", "want", "need", "please", "thank",
        "thanks", "some", "any", "one", "two", "three", "all", "very", "too", "also", "just", "now", "then", "than",
        "about", "as", "ok", "okay", "hi", "hello", "bye", "good", "well", "really", "much", "more", "most", "make",
        "made", "take", "took", "taken", "see", "saw", "seen", "say", "said", "tell", "told", "know", "knew", "known",
        "think", "thought", "let", "let's", "it's", "i'm", "i'd", "i'll", "i've", "don't", "can't", "that's",
        "there's", "what's",
        // 不規則な変化 (時制や数の間違いを聞き間違いとして直さないように)
        "eat", "ate", "eaten", "buy", "bought", "bring", "brought", "come", "came", "run", "ran", "write", "wrote",
        "written", "give", "gave", "given", "feel", "felt", "leave", "left", "meet", "met", "pay", "paid", "send",
        "sent", "spend", "spent", "find", "found", "keep", "kept", "begin", "began", "begun", "child", "children",
        "man", "men", "woman", "women", "person", "people", "better", "best", "worse", "worst",
    )

    /** 語形変化の語尾 (watch と watched、friend と friends は別の語として扱わない) */
    private val SUFFIXES = setOf("s", "es", "ies", "ed", "d", "ied", "ing", "er", "est", "ly", "'s")

    /**
     * これより短い語は綴りで直さない。認識結果はたいてい実在の語で、短い語 (cold と hold など) は
     * 学習者が本当にそう言った可能性が高い
     */
    private const val MIN_FUZZY = 5

    private val QUESTION_START = Regex(
        """^(what|where|when|why|how|who|which|whose|can|could|would|will|shall|should|do|does|did|is|are|am|was|were|have|has|may)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val WHITESPACE = Regex("""\s+""")

    private val EDGE_PUNCTUATION = Regex("""^([^\p{L}\p{N}']*)(.*?)([^\p{L}\p{N}']*)$""")

    private val SENTENCE_END = Regex("""[.!?:;"”]$""")

    /** . で終わるが文の終わりではない語 */
    private val ABBREVIATIONS = setOf("mr.", "mrs.", "ms.", "dr.", "st.", "prof.", "mt.", "jr.", "sr.")

    /**
     * @param expected いまのお題で言うはずの英語 (台本のお手本・キーワード、見ているヒント)。綴りが近い語はこれに直す
     * @param context ほかに出てきた英語 (相手のセリフ、キーフレーズなど)。固有名詞の書き方だけそろえる
     */
    fun fix(raw: String, expected: List<String> = emptyList(), context: List<String> = emptyList()): String {
        val words = raw.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return ""
        val targets = vocabulary(expected)
        val names = vocabulary(context + expected).filter { (lower, form) -> form != lower }
        var text = words.joinToString(" ") { fixWord(it, targets, names) }
        text = text.replaceFirstChar { it.uppercaseChar() }
        if (text.last() !in ".?!") text += if (QUESTION_START.containsMatchIn(text)) "?" else "."
        return text
    }

    /**
     * 音声認識の候補 (確からしい順) から 1 つ選ぶ。お題で使いそうな語 (よく使う語を除く) を
     * 先頭の候補より多く含む候補があればそれを、なければ先頭を選ぶ。
     * 文法の違い (a のあるなしなど) は数えないので、学習者の間違いを消す方向には選ばない。
     */
    fun pick(alternatives: List<String>, expected: List<String>): String {
        val first = alternatives.firstOrNull() ?: return ""
        if (alternatives.size == 1 || expected.isEmpty()) return first
        val vocabulary = vocabulary(expected).keys.filter { it !in COMMON }.toSet()
        fun score(text: String) = words(text).count { it in vocabulary }
        val best = alternatives.maxBy(::score)
        return if (score(best) > score(first)) best else first
    }

    /**
     * AI が直した聞き取り (repaired) を使ってよいか。聞き間違いの直しだけで、文法の直しが混ざっていないこと:
     * 語の入れ替えがあり (足すだけ・消すだけは文法の直し)、入れ替えた語によく使う語がなく、
     * 語形変化 (go → goes など) でもなく、入れ替えが 3 語以内。
     */
    fun acceptRepair(original: String, repaired: String): Boolean {
        val before = words(original)
        val after = words(repaired)
        if (before == after || after.isEmpty()) return false
        val removed = before.toMutableList().apply { after.forEach { remove(it) } }
        val added = after.toMutableList().apply { before.forEach { remove(it) } }
        return when {
            removed.isEmpty() || added.isEmpty() -> false
            removed.size > 3 || added.size > 3 -> false
            (removed + added).any { it in COMMON } -> false
            else -> removed.none { r -> added.any { a -> inflected(r, a) } }
        }
    }

    /** 語が変わったか (大文字・小文字や句読点だけの違いは数えない) */
    fun wordsChanged(before: String, after: String): Boolean = words(before) != words(after)

    /** 変わった箇所 (直す前 → 直した後)。例: [lotte → latte]、[lot day → latte] */
    fun corrections(raw: String, fixed: String): List<Pair<String, String>> {
        val a = raw.split(WHITESPACE).map(::core).filter { it.isNotEmpty() }
        val b = fixed.split(WHITESPACE).map(::core).filter { it.isNotEmpty() }
        fun same(i: Int, j: Int) = a[i].equals(b[j], ignoreCase = true)
        // 最長共通部分列で対応をとり、そろわない部分をまとめる
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            lcs[i][j] = if (same(i, j)) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val result = mutableListOf<Pair<String, String>>()
        val removed = mutableListOf<String>()
        val added = mutableListOf<String>()
        fun flush() {
            if (removed.isNotEmpty() || added.isNotEmpty()) {
                result += removed.joinToString(" ").ifEmpty { "…" } to added.joinToString(" ").ifEmpty { "…" }
            }
            removed.clear()
            added.clear()
        }
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            when {
                i < a.size && j < b.size && same(i, j) -> {
                    flush()
                    i++
                    j++
                }
                j >= b.size || (i < a.size && lcs[i + 1][j] >= lcs[i][j + 1]) -> removed += a[i++]
                else -> added += b[j++]
            }
        }
        flush()
        return result
    }

    private fun words(text: String): List<String> =
        text.split(WHITESPACE).map { core(it).lowercase() }.filter { it.isNotEmpty() }

    /** 小文字の語 → 書く形 (固有名詞は大文字で始まる形: Tanaka、Wednesday など) */
    private fun vocabulary(sentences: List<String>): Map<String, String> {
        val forms = HashMap<String, String>()
        for (sentence in sentences) {
            val tokens = sentence.split(WHITESPACE).filter { it.isNotEmpty() }
            tokens.forEachIndexed { i, token ->
                val word = core(token)
                if (word.isEmpty()) return@forEachIndexed
                val lower = word.lowercase()
                // 文の途中で大文字なら固有名詞。文頭 (前の語が . ? ! などで終わる) の大文字は文頭だからなので小文字で覚える
                val sentenceStart = i == 0 ||
                    (SENTENCE_END.containsMatchIn(tokens[i - 1]) && tokens[i - 1].lowercase() !in ABBREVIATIONS)
                val proper = !sentenceStart && word[0].isUpperCase() && lower != "i" && !lower.startsWith("i'")
                if (proper) forms[lower] = word else forms.putIfAbsent(lower, lower)
            }
        }
        return forms
    }

    private fun core(token: String): String = EDGE_PUNCTUATION.matchEntire(token)?.groupValues?.get(2) ?: token

    private fun fixWord(token: String, targets: Map<String, String>, names: Map<String, String>): String {
        val match = EDGE_PUNCTUATION.matchEntire(token) ?: return token
        val (lead, word, trail) = match.destructured
        if (word.isEmpty()) return token
        val lower = word.lowercase()
        val fixed = when {
            lower == "i" -> "I"
            lower.startsWith("i'") -> "I" + word.substring(1)
            lower in names -> names.getValue(lower)
            lower in targets || lower in COMMON || lower.length < MIN_FUZZY || lower.any { it.isDigit() } -> word
            else -> closest(lower, targets) ?: word
        }
        return lead + fixed + trail
    }

    /** 綴りがとても近い語がひとつだけあれば、その語 (同じ距離の候補が複数なら直さない) */
    private fun closest(word: String, vocabulary: Map<String, String>): String? {
        val tolerance = if (word.length <= 6) 1 else 2
        var best: String? = null
        var bestDistance = Int.MAX_VALUE
        var tie = false
        for ((candidate, form) in vocabulary) {
            if (candidate.length < MIN_FUZZY - 1 || candidate in COMMON || candidate.any { it.isDigit() }) continue
            // 語形変化 (watched と watch など) は文法なので直さない
            if (inflected(word, candidate)) continue
            val d = distance(word, candidate, tolerance)
            // 2 文字以上違うなら、少なくとも頭の文字は同じであること
            if (d > tolerance || (d >= 2 && word[0] != candidate[0])) continue
            when {
                d < bestDistance -> {
                    best = form
                    bestDistance = d
                    tie = false
                }
                d == bestDistance && form != best -> tie = true
            }
        }
        return if (tie) null else best
    }

    /** 片方がもう片方の語形変化か (watch / watched、study / studies、stop / stopped、make / making) */
    internal fun inflected(a: String, b: String): Boolean {
        val (short, long) = if (a.length <= b.length) a to b else b to a
        val prefix = short.commonPrefixWith(long).length
        val shortRest = short.substring(prefix)
        val longRest = long.substring(prefix)
        if (shortRest.length > 1 || longRest.isEmpty()) return false
        if (longRest in SUFFIXES) return true
        // 子音を重ねる変化 (stop → stopped)
        return shortRest.isEmpty() && longRest.length >= 2 && longRest[0] == short.last() && longRest.substring(1) in SUFFIXES
    }

    /** 編集距離 (limit を超えたら打ち切って limit + 1 を返す) */
    internal fun distance(a: String, b: String, limit: Int = Int.MAX_VALUE): Int {
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                rowMin = minOf(rowMin, current[j])
            }
            if (rowMin > limit) return limit + 1
            previous = current
        }
        return previous[b.length]
    }
}
