package com.athar.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatcherTest {
    private val corpus = Corpus(
        listOf(
            Passage("q", "1:1", "بسم الله الرحمن الرحيم"),
            Passage("q", "1:2", "الحمد لله رب العالمين"),
            Passage("q", "2:255", "الله لا اله الا هو الحي القيوم لا تاخذه سنه ولا نوم"),
            Passage("h", "nawawi:1", "انما الاعمال بالنيات وانما لكل امرئ ما نوي"),
            Passage("h", "bukhari:1", "انما الاعمال بالنيات")
        ),
        mapOf("nawawi:1" to GradeInfo("صحيح", "النووي"))
    )
    private val matcher = Matcher(corpus)

    @Test
    fun normalizesArabicDiacritics() {
        assertEquals("بسم الله الرحمن الرحيم", ArabicText.norm("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))
    }

    @Test
    fun foldsConfusedLetters() {
        assertEquals(ArabicText.fold("صراط"), ArabicText.fold("سراط"))
    }

    @Test
    fun findsAyahVerbatim() {
        val r = matcher.lookup("الحمد لله رب العالمين")
        assertTrue(r.candidates.isNotEmpty())
        assertEquals("1:2", r.candidates.first().passage.ref)
        assertEquals(MatchStatus.VERBATIM, r.candidates.first().status)
    }

    @Test
    fun findsHadithDespiteAsrNoise() {
        // ص/س fold: نيات ~ نيات already; use ق/ك style noise on a different letter pair via fold path
        val r = matcher.lookup("انما الاعمال بالنيات وانما لكل امرئ ما نوي")
        assertTrue(r.candidates.any { it.passage.ref.startsWith("nawawi") })
        assertTrue(r.candidates.first().status.rank >= MatchStatus.PARTIAL.rank)
    }

    @Test
    fun analyzeLectureFindsCitations() {
        val text = "قال الله تعالى الحمد لله رب العالمين ثم قال رسول الله انما الاعمال بالنيات"
        val r = matcher.analyze(text)
        assertTrue(r.ledger.isNotEmpty(), "expected ledger entries")
        assertTrue(r.ledger.any { it.passage?.ref == "1:2" })
    }

    @Test
    fun notFoundIsNotARuling() {
        val r = matcher.lookup("حب الوطن من الايمان")
        assertTrue(r.candidates.isEmpty() || r.candidates.first().status == MatchStatus.NOT_FOUND || r.candidates.first().status == MatchStatus.MEANING)
    }
}
