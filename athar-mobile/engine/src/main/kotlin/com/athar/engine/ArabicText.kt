package com.athar.engine

/**
 * Arabic normalisation + phonetic fold — mirrors athar-ledger `public/js/text.js`
 * so offline mobile verdicts stay aligned with the web engine.
 */
object ArabicText {
    private val DIAC = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val HAS_AR = Regex("[\\u0621-\\u064A]")
    private val AR_DIGITS = Regex("[\\u0660-\\u0669\\u06F0-\\u06F9]")
    private val FOREIGN = mapOf(
        '\u06CC' to 'ي', '\u06D2' to 'ي', '\u06A9' to 'ك',
        '\u06C1' to 'ه', '\u06BE' to 'ه', '\u06C0' to 'ه',
        '\u06C3' to 'ه', '\u06D5' to 'ه'
    )
    private val FOLD = mapOf(
        'ص' to 'س', 'ث' to 'س', 'ض' to 'د', 'ظ' to 'ز', 'ذ' to 'ز',
        'ط' to 'ت', 'ق' to 'ك', 'ح' to 'ه', 'ع' to 'ا', 'ء' to 'ا',
        'ئ' to 'ا', 'ؤ' to 'و', 'غ' to 'ك', 'خ' to 'ه'
    )

    fun wordText(x: Any?): String = when (x) {
        null -> ""
        is Map<*, *> -> x["w"]?.toString().orEmpty()
        else -> x.toString()
    }

    /** Strip diacritics, unify alef/yeh/teh-marbuta, keep Arabic letters. */
    fun norm(s: String): String =
        s.replace('ی', 'ي').replace('ک', 'ك')
            .replace(DIAC, "")
            .replace(Regex("[إأآٱ]"), "ا")
            .replace('ى', 'ي').replace('ة', 'ه')
            .replace(Regex("[^ء-ي ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun normEn(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    fun normMixed(w: Any?): String {
        var s = wordText(w)
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC)
        s = buildString {
            for (ch in s) append(FOREIGN[ch] ?: ch)
        }
        s = AR_DIGITS.replace(s) { m ->
            (m.value[0].code and 15).toString()
        }
        return if (HAS_AR.containsMatchIn(s)) norm(s) else normEn(s)
    }

    fun fold(s: String): String = buildString {
        for (ch in s) append(FOLD[ch] ?: ch)
    }

    fun tokenize(text: String): List<String> =
        text.trim().split(Regex("\\s+")).map { normMixed(it) }.filter { it.isNotEmpty() }

    fun tokenizeFolded(text: String): List<String> =
        tokenize(text).map { fold(it) }

    /** exact / asr-fold / near(1 edit) / diff — used for word-level ledger diffs. */
    fun compareWords(spoken: String, source: String): WordMatch {
        if (spoken == source) return WordMatch.EXACT
        if (fold(spoken) == fold(source)) return WordMatch.ASR
        if (within1(spoken, source) || (spoken.length >= 6 && editDistance(spoken, source, 2) <= 2)) {
            return WordMatch.NEAR
        }
        return WordMatch.DIFF
    }

    fun within1(a: String, b: String): Boolean {
        val la = a.length
        val lb = b.length
        val d = la - lb
        if (d > 1 || d < -1) return false
        val m = minOf(la, lb)
        var i = 0
        while (i < m && a[i] == b[i]) i++
        if (i == m) return true
        var x = if (d >= 0) i + 1 else i
        var y = if (d <= 0) i + 1 else i
        while (x < la && y < lb && a[x] == b[y]) {
            x++; y++
        }
        return x == la && y == lb
    }

    fun editDistance(a: String, b: String, max: Int = 3): Int {
        if (a == b) return 0
        val la = a.length
        val lb = b.length
        if (kotlin.math.abs(la - lb) > max) return max + 1
        var prev = IntArray(lb + 1) { it }
        var cur = IntArray(lb + 1)
        for (i in 1..la) {
            cur[0] = i
            var rowMin = cur[0]
            val ca = a[i - 1]
            for (j in 1..lb) {
                val c = if (ca == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + c)
                if (cur[j] < rowMin) rowMin = cur[j]
            }
            if (rowMin > max) return max + 1
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[lb]
    }
}

enum class WordMatch { EXACT, ASR, NEAR, DIFF }
