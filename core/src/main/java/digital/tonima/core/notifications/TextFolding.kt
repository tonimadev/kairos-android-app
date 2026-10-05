package digital.tonima.core.notifications

import java.text.Normalizer

/**
 * Makes text comparable across scripts without changing its length, so match positions found in the
 * folded text still point at the same characters of the original: digits of any script become 0-9,
 * letters are lower-cased and lose their accents (á -> a, ñ -> n, full-width forms -> ASCII).
 */
internal object TextFolding {
    private val unspacedScripts =
        setOf(
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
        )

    fun fold(text: String): String {
        val folded = StringBuilder(text.length)
        text.forEach { folded.append(foldChar(it)) }
        return folded.toString()
    }

    private fun foldChar(c: Char): Char {
        val digit = Character.digit(c, DECIMAL)
        if (digit >= 0) return '0' + digit
        val decomposed = Normalizer.normalize(c.toString(), Normalizer.Form.NFKD)
        return (decomposed.firstOrNull() ?: c).lowercaseChar()
    }

    /** Scripts written without spaces (Chinese, Japanese) cannot be matched on word boundaries. */
    fun isSpaced(c: Char): Boolean = Character.UnicodeScript.of(c.code) !in unspacedScripts

    /**
     * Regex alternation of [words], longest first. Words in spaced scripts only match at the start of a
     * word (so stems like "segunda" also catch "segunda-feira"), and also at the end of one when
     * [wholeWord] is set.
     */
    fun alternation(
        words: Collection<String>,
        wholeWord: Boolean,
    ): String =
        words
            .map { fold(it) }
            .distinct()
            .sortedByDescending { it.length }
            .joinToString("|", prefix = "(?:", postfix = ")") { word ->
                val start = if (isSpaced(word.first())) "(?<!\\p{L})" else ""
                val end = if (wholeWord && isSpaced(word.last())) "(?!\\p{L})" else ""
                start + Regex.escape(word) + end
            }

    private const val DECIMAL = 10
}
