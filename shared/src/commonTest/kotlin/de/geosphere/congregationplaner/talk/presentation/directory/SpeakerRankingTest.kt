package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.ui.rating.StarFill.Empty
import de.geosphere.congregationplaner.ui.rating.StarFill.Full
import de.geosphere.congregationplaner.ui.rating.StarFill.Half
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakerRankingTest {
    @Test
    fun tappingAStarFillsUpToItThenHalvesItThenClears() {
        assertEquals(6, nextRanking(current = 0, tapped = 3))
        assertEquals(5, nextRanking(current = 6, tapped = 3))
        assertEquals(0, nextRanking(current = 5, tapped = 3))
    }

    @Test
    fun tappingAnotherStarAlwaysFillsUpToIt() {
        assertEquals(8, nextRanking(current = 5, tapped = 4))
        assertEquals(2, nextRanking(current = 10, tapped = 1))
        assertEquals(10, nextRanking(current = 7, tapped = 5))
    }

    @Test
    fun starsAreFilledInHalfSteps() {
        assertEquals(listOf(Full, Full, Full, Half, Empty), rankingStars(7))
        assertEquals(listOf(Full, Full, Empty, Empty, Empty), rankingStars(4))
        assertEquals(List(5) { Empty }, rankingStars(0))
        assertEquals(List(5) { Full }, rankingStars(10))
    }
}
