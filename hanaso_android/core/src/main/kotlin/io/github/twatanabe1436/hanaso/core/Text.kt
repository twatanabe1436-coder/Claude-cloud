package io.github.twatanabe1436.hanaso.core

/**
 * ストリーミングで届くテキストを、文が完成するたびに取り出す (文単位で読み上げを始めるため)。
 */
class SentenceChunker {
    private var buf = StringBuilder()

    /** @return 完成した文 */
    fun push(delta: String): List<String> {
        buf.append(delta)
        val text = buf.toString()
        val out = mutableListOf<String>()
        var start = 0
        for (m in SENTENCE_END.findAll(text)) {
            val end = m.range.last + 1
            val piece = text.substring(start, end).trim()
            if (ABBREVIATION.containsMatchIn(piece)) continue // Dr. などの略語の後では区切らない
            if (piece.isNotEmpty()) out += piece
            start = end
        }
        buf = StringBuilder(text.substring(start))
        return out
    }

    /** 残りをすべて取り出す */
    fun flush(): List<String> {
        val rest = buf.toString().trim()
        buf = StringBuilder()
        return if (rest.isEmpty()) emptyList() else listOf(rest)
    }

    private companion object {
        val SENTENCE_END = Regex("""[.!?]+["')\]]*\s+""")
        val ABBREVIATION = Regex("""\b(mr|mrs|ms|dr|st|vs|etc|e\.g|i\.e|jr|sr|no)\.$""", RegexOption.IGNORE_CASE)
    }
}

/** 発音チェックの結果。words はお手本の単語ごとに、言えたかどうか。 */
data class SpeechScore(val score: Int, val words: List<WordResult>) {
    data class WordResult(val text: String, val ok: Boolean)
}

/**
 * お手本の文と音声認識の結果を単語単位で突き合わせて採点する。
 * 大文字小文字・句読点は無視し、短縮形 (I'm = I am) や数字 (2 = two) の違いも同一視する。
 */
object SpeechScorer {
    private val CONTRACTIONS = mapOf(
        "i'm" to "i am", "you're" to "you are", "we're" to "we are", "they're" to "they are",
        "he's" to "he is", "she's" to "she is", "it's" to "it is", "that's" to "that is",
        "what's" to "what is", "where's" to "where is", "there's" to "there is", "here's" to "here is",
        "how's" to "how is", "let's" to "let us",
        "i've" to "i have", "you've" to "you have", "we've" to "we have", "they've" to "they have",
        "i'll" to "i will", "you'll" to "you will", "we'll" to "we will", "they'll" to "they will",
        "he'll" to "he will", "she'll" to "she will", "it'll" to "it will",
        "i'd" to "i would", "you'd" to "you would", "we'd" to "we would", "they'd" to "they would",
        "he'd" to "he would", "she'd" to "she would",
        "don't" to "do not", "doesn't" to "does not", "didn't" to "did not", "isn't" to "is not",
        "aren't" to "are not", "wasn't" to "was not", "weren't" to "were not", "can't" to "can not",
        "couldn't" to "could not", "won't" to "will not", "wouldn't" to "would not",
        "shouldn't" to "should not", "haven't" to "have not", "hasn't" to "has not",
        "cannot" to "can not", "gonna" to "going to", "wanna" to "want to",
    )

    private val NUMBERS = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
    )

    private fun normalizeWord(word: String): List<String> {
        val w = word.lowercase().replace('’', '\'').replace('‘', '\'').trim('\'')
        CONTRACTIONS[w]?.let { return it.split(" ") }
        if (w.isNotEmpty() && w.all { it.isDigit() } && w.length <= 2 && w.toInt() <= 20) return listOf(NUMBERS[w.toInt()])
        val clean = w.filter { it in 'a'..'z' || it in '0'..'9' || it == '\'' }
        return if (clean.isEmpty()) emptyList() else listOf(clean)
    }

    private fun words(text: String) = text.split(Regex("""\s+""")).filter { it.isNotEmpty() }

    private fun spokenTokens(text: String) = words(text).flatMap { normalizeWord(it) }

    /** 大文字小文字と句読点を無視して同じ文か */
    fun sameSentence(a: String, b: String) = spokenTokens(a) == spokenTokens(b)

    fun score(target: String, spoken: String): SpeechScore {
        val targetWords = words(target)
        // 短縮形 (I'll) は比較用に2トークン (i, will) になるが、表示は1単語のまま
        val tokens = targetWords.flatMapIndexed { index, w -> normalizeWord(w).map { it to index } }
        if (tokens.isEmpty()) return SpeechScore(0, targetWords.map { SpeechScore.WordResult(it, false) })
        val matched = matchedFlags(tokens.map { it.first }, spokenTokens(spoken))
        val okByWord = BooleanArray(targetWords.size) { true }
        tokens.forEachIndexed { i, (_, wordIndex) -> if (!matched[i]) okByWord[wordIndex] = false }
        return SpeechScore(
            score = Math.round(matched.count { it } * 100.0 / tokens.size).toInt(),
            words = targetWords.mapIndexed { i, w -> SpeechScore.WordResult(w, okByWord[i]) },
        )
    }

    /** 最長共通部分列で、お手本のどのトークンが発話に含まれていたかを求める */
    private fun matchedFlags(target: List<String>, spoken: List<String>): BooleanArray {
        val n = target.size
        val m = spoken.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (target[i] == spoken[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val flags = BooleanArray(n)
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                target[i] == spoken[j] -> { flags[i] = true; i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> i++
                else -> j++
            }
        }
        return flags
    }
}

/** ひらがな・カタカナ・漢字が含まれていれば日本語とみなす */
fun String.hasJapanese(): Boolean = any { c ->
    c in '぀'..'ヿ' || c in '㐀'..'鿿' || c in 'ｦ'..'ﾟ'
}
