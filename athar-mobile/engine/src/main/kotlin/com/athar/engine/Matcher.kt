package com.athar.engine

/**
 * Deterministic on-device matcher for Athar Mobile.
 *
 * Rules (same spirit as athar-ledger engine):
 * - Verdicts come from counted word overlap, never from an LLM.
 * - "Verbatim" only when every spoken word in the span agrees (exact or ASR-fold).
 * - "Not found" means absent from the loaded pack — not a ruling on authenticity.
 */
class Matcher(
    private val corpus: Corpus,
    private val minWords: Int = 3,
    private val verbatimRatio: Double = 0.92,
    private val partialRatio: Double = 0.55,
    private val maxCandidates: Int = 8
) {
    fun lookup(text: String, max: Int = maxCandidates): LookupResult {
        val query = text.trim()
        val qTok = ArabicText.tokenize(query)
        val qFold = qTok.map { ArabicText.fold(it) }
        if (qTok.size < 2) {
            return LookupResult(query, emptyList())
        }
        val ids = corpus.candidateIds(qFold, maxPassages = 120)
        val hits = ids.mapNotNull { scoreAgainst(qTok, qFold, it) }
            .sortedWith(
                compareByDescending<MatchHit> { it.status.rank }
                    .thenByDescending { it.evidence }
                    .thenBy { it.passage.collectionRank }
            )
            .distinctBy { it.passage.ref }
            .take(max)
        return LookupResult(query, hits)
    }

    /**
     * Scan a transcript for Qur'an / hadith citations using overlapping windows.
     * Lighter than the full web engine, but offline and on-device.
     */
    fun analyze(transcript: String): AnalyzeResult {
        val t0 = System.currentTimeMillis()
        val words = ArabicText.tokenize(transcript)
        val folded = words.map { ArabicText.fold(it) }
        if (words.isEmpty()) {
            return AnalyzeResult(emptyList(), 0, 0)
        }

        val ledger = mutableListOf<LedgerEntry>()
        val covered = BooleanArray(words.size)
        val windows = listOf(40, 24, 16, 10, 6)

        for (win in windows) {
            var i = 0
            while (i < words.size) {
                while (i < words.size && covered[i]) i++
                if (i >= words.size) break
                val end = minOf(words.size, i + win)
                if (end - i < minWords) {
                    i++
                    continue
                }
                val slice = words.subList(i, end)
                val sliceF = folded.subList(i, end)
                val hit = corpus.candidateIds(sliceF, maxPassages = 40)
                    .mapNotNull { scoreAgainst(slice, sliceF, it) }
                    .maxWithOrNull(
                        compareByDescending<MatchHit> { it.status.rank }
                            .thenByDescending { it.evidence }
                    )
                if (hit != null && hit.status.rank >= MatchStatus.PARTIAL.rank && hit.queryLength >= minWords) {
                    val absStart = i + hit.queryStart
                    val absEnd = minOf(words.size, absStart + hit.queryLength)
                    if (absStart < absEnd && !(absStart until absEnd).any { covered[it] }) {
                        for (k in absStart until absEnd) covered[k] = true
                        ledger += LedgerEntry(
                            status = hit.status,
                            passage = hit.passage,
                            startWord = absStart,
                            endWord = absEnd,
                            spokenSpan = words.subList(absStart, absEnd).joinToString(" "),
                            evidence = hit.evidence,
                            diffs = hit.diffs,
                            grade = hit.grade
                        )
                        i = absEnd
                    } else {
                        i++
                    }
                } else {
                    i++
                }
            }
        }

        ledger.sortBy { it.startWord }
        return AnalyzeResult(ledger, words.size, System.currentTimeMillis() - t0)
    }

    private fun scoreAgainst(
        qTok: List<String>,
        qFold: List<String>,
        pid: Int
    ): MatchHit? {
        val src = corpus.tokensOf(pid)
        val srcF = corpus.foldedOf(pid)
        if (src.size < 2) return null

        val best = bestWindowAlign(qTok, qFold, src, srcF) ?: return null
        val (qStart, sStart, length, exactish, evidence, diffs) = best
        if (length < minWords) return null

        val ratio = exactish.toDouble() / length
        val status = when {
            ratio >= verbatimRatio && diffs.none { it.kind == WordMatch.DIFF } -> MatchStatus.VERBATIM
            ratio >= partialRatio -> MatchStatus.PARTIAL
            evidence >= 4 -> MatchStatus.MEANING
            else -> return null
        }

        val passage = corpus.passages[pid]
        return MatchHit(
            status = status,
            passage = passage,
            evidence = evidence,
            overlap = ratio,
            spokenSpan = qTok.subList(qStart, qStart + length).joinToString(" "),
            sourceSpan = src.subList(sStart, minOf(src.size, sStart + length)).joinToString(" "),
            queryStart = qStart,
            queryLength = length,
            diffs = diffs,
            grade = corpus.gradeOf(passage.ref)
        )
    }

    private data class Align(
        val qStart: Int,
        val sStart: Int,
        val length: Int,
        val exactish: Int,
        val evidence: Double,
        val diffs: List<WordDiff>
    )

    private fun bestWindowAlign(
        qTok: List<String>,
        qFold: List<String>,
        src: List<String>,
        srcF: List<String>
    ): Align? {
        var best: Align? = null
        val maxLen = minOf(qTok.size, src.size)
        if (maxLen < minWords) return null

        // Prefer longer spans; slide query against source.
        for (len in maxLen downTo minWords) {
            for (qi in 0..qTok.size - len) {
                for (si in 0..src.size - len) {
                    var exactish = 0
                    var evidence = 0.0
                    val diffs = ArrayList<WordDiff>(len)
                    var solid = true
                    for (k in 0 until len) {
                        val kind = ArabicText.compareWords(qTok[qi + k], src[si + k])
                        when (kind) {
                            WordMatch.EXACT -> {
                                exactish++; evidence += 1.0
                            }
                            WordMatch.ASR -> {
                                exactish++; evidence += 0.85
                                diffs += WordDiff(qTok[qi + k], src[si + k], kind)
                            }
                            WordMatch.NEAR -> {
                                evidence += 0.45
                                diffs += WordDiff(qTok[qi + k], src[si + k], kind)
                                solid = false
                            }
                            WordMatch.DIFF -> {
                                // allow fold equality already handled; else miss
                                if (qFold[qi + k] == srcF[si + k]) {
                                    exactish++; evidence += 0.85
                                    diffs += WordDiff(qTok[qi + k], src[si + k], WordMatch.ASR)
                                } else {
                                    evidence += 0.0
                                    diffs += WordDiff(qTok[qi + k], src[si + k], kind)
                                    solid = false
                                }
                            }
                        }
                    }
                    val ratio = exactish.toDouble() / len
                    if (ratio < partialRatio && evidence < 4) continue
                    val cand = Align(qi, si, len, exactish, evidence, diffs)
                    if (best == null ||
                        cand.evidence > best.evidence + 0.01 ||
                        (kotlin.math.abs(cand.evidence - best.evidence) < 0.01 && cand.length > best.length) ||
                        (cand.length == best.length && cand.exactish > best.exactish)
                    ) {
                        best = cand
                        // early win: long verbatim
                        if (solid && ratio >= verbatimRatio && len >= minOf(12, maxLen)) {
                            return best
                        }
                    }
                }
            }
            // if we already have a strong long match, stop shrinking
            if (best != null && best.length >= 10 && best.exactish.toDouble() / best.length >= verbatimRatio) {
                break
            }
        }
        return best
    }
}
