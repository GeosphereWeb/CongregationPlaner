package de.geosphere.congregationplaner.talk.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakerRankingTest {
    @Test
    fun tappingAnotherStarSetsItAndTappingTheCurrentOneClears() {
        assertEquals(4, nextRanking(current = 2, tapped = 4))
        assertEquals(0, nextRanking(current = 4, tapped = 4))
        assertEquals(1, nextRanking(current = 0, tapped = 1))
    }

    @Test
    fun starsAreFilledUpToTheRating() {
        assertEquals(listOf(true, true, true, false, false), rankingStars(3))
        assertEquals(List(5) { false }, rankingStars(0))
    }
}
