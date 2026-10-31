package com.athar.engine

data class Passage(
    val type: String,          // "q" | "h"
    val ref: String,           // "2:255" | "nawawi:1"
    val text: String,          // normalised source text (as stored in athar pack)
    val collectionRank: Int = 1
)

data class GradeInfo(
    val label: String,
    val scholar: String? = null
)

enum class MatchStatus(val ar: String, val rank: Int) {
    VERBATIM("مطابق حرفيًا", 4),
    PARTIAL("مطابق جزئيًا", 3),
    MEANING("بالمعنى — يحتاج تأكيدًا", 2),
    NOT_FOUND("لم يُعثر عليه في المصادر", 0)
}

data class WordDiff(
    val spoken: String,
    val source: String?,
    val kind: WordMatch
)

data class MatchHit(
    val status: MatchStatus,
    val passage: Passage,
    val evidence: Double,
    val overlap: Double,
    val spokenSpan: String,
    val sourceSpan: String,
    val queryStart: Int = 0,
    val queryLength: Int = 0,
    val diffs: List<WordDiff> = emptyList(),
    val grade: GradeInfo? = null
)

data class LookupResult(
    val query: String,
    val candidates: List<MatchHit>,
    val truncated: Boolean = false
)

data class LedgerEntry(
    val status: MatchStatus,
    val passage: Passage?,
    val startWord: Int,
    val endWord: Int,
    val spokenSpan: String,
    val evidence: Double,
    val diffs: List<WordDiff> = emptyList(),
    val grade: GradeInfo? = null
)

data class AnalyzeResult(
    val ledger: List<LedgerEntry>,
    val wordCount: Int,
    val elapsedMs: Long
)
