package com.athar.engine

/**
 * Offline mobile corpus. Load from JSON maps produced by `scripts/build_mobile_pack.py`
 * (full Qur'an + Nawawi / Qudsi / short Bukhari sample).
 */
class Corpus(
    val passages: List<Passage>,
    val grades: Map<String, GradeInfo> = emptyMap(),
    val meta: Map<String, Any?> = emptyMap()
) {
    private val tokens: List<List<String>> = passages.map { ArabicText.tokenize(it.text) }
    private val folded: List<List<String>> = tokens.map { toks -> toks.map { ArabicText.fold(it) } }

    /** inverted index: folded trigram hash -> passage indices */
    private val trigramIndex: Map<Int, IntArray> = buildTrigramIndex()

    val size: Int get() = passages.size
    val quranCount: Int get() = passages.count { it.type == "q" }
    val hadithCount: Int get() = passages.count { it.type == "h" }

    fun tokensOf(i: Int): List<String> = tokens[i]
    fun foldedOf(i: Int): List<String> = folded[i]
    fun gradeOf(ref: String): GradeInfo? = grades[ref]

    fun candidateIds(queryFolded: List<String>, maxPassages: Int = 80): List<Int> {
        if (queryFolded.size < 2) return emptyList()
        val scores = HashMap<Int, Int>()
        val n = queryFolded.size
        for (i in 0 until n - 1) {
            val bi = biHash(queryFolded[i], queryFolded[i + 1])
            trigramIndex[bi]?.forEach { pid -> scores[pid] = (scores[pid] ?: 0) + 1 }
            if (i + 2 < n) {
                val tri = triHash(queryFolded[i], queryFolded[i + 1], queryFolded[i + 2])
                trigramIndex[tri]?.forEach { pid -> scores[pid] = (scores[pid] ?: 0) + 3 }
            }
        }
        return scores.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }
                .thenBy { passages[it.key].collectionRank }
                .thenBy { it.key })
            .take(maxPassages)
            .map { it.key }
    }

    private fun buildTrigramIndex(): Map<Int, IntArray> {
        val buckets = HashMap<Int, MutableList<Int>>()
        folded.forEachIndexed { pid, toks ->
            if (toks.size < 2) return@forEachIndexed
            val seen = HashSet<Int>()
            for (i in 0 until toks.size - 1) {
                val bi = biHash(toks[i], toks[i + 1])
                if (seen.add(bi)) buckets.getOrPut(bi) { mutableListOf() }.add(pid)
                if (i + 2 < toks.size) {
                    val tri = triHash(toks[i], toks[i + 1], toks[i + 2])
                    if (seen.add(tri)) buckets.getOrPut(tri) { mutableListOf() }.add(pid)
                }
            }
        }
        return buckets.mapValues { (_, v) -> v.toIntArray() }
    }

    companion object {
        fun biHash(a: String, b: String): Int = fnv("$a $b")
        fun triHash(a: String, b: String, c: String): Int = fnv("$a $b $c")

        fun fnv(s: String): Int {
            var h = -0x7ee3623b // 0x811c9dc5 as signed
            for (b in s.encodeToByteArray()) {
                h = h xor (b.toInt() and 0xff)
                h *= 0x01000193
            }
            return h
        }

        @Suppress("UNCHECKED_CAST")
        fun fromJsonMaps(
            passageMaps: List<Map<String, Any?>>,
            gradeMap: Map<String, Any?> = emptyMap(),
            meta: Map<String, Any?> = emptyMap()
        ): Corpus {
            val passages = passageMaps.map { m ->
                Passage(
                    type = m["t"]?.toString() ?: "h",
                    ref = m["r"]?.toString().orEmpty(),
                    text = m["n"]?.toString().orEmpty(),
                    collectionRank = (m["m"] as? Number)?.toInt() ?: 1
                )
            }.filter { it.ref.isNotEmpty() && it.text.isNotEmpty() }

            val grades = gradeMap.mapNotNull { (ref, raw) ->
                when (raw) {
                    is String -> ref to GradeInfo(raw)
                    is Map<*, *> -> {
                        val label = raw["g"]?.toString() ?: raw["label"]?.toString() ?: return@mapNotNull null
                        val scholar = raw["s"]?.toString() ?: raw["scholar"]?.toString()
                        ref to GradeInfo(label, scholar)
                    }
                    else -> null
                }
            }.toMap()

            return Corpus(passages, grades, meta)
        }
    }
}
