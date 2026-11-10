package de.geosphere.congregationplaner.ui.list

import kotlin.test.Test
import kotlin.test.assertEquals

class SwipeAnchorsTest {
    private val start = 100f
    private val end = 120f

    @Test
    fun releasedBeforeHalfWayClosesAgain() {
        assertEquals(SwipeAnchor.Closed, settleAnchor(0f, start, end))
        assertEquals(SwipeAnchor.Closed, settleAnchor(49f, start, end))
        assertEquals(SwipeAnchor.Closed, settleAnchor(-59f, start, end))
    }

    @Test
    fun releasedFromHalfWayOnOpensThatSide() {
        assertEquals(SwipeAnchor.Start, settleAnchor(50f, start, end))
        assertEquals(SwipeAnchor.Start, settleAnchor(100f, start, end))
        assertEquals(SwipeAnchor.End, settleAnchor(-60f, start, end))
        assertEquals(SwipeAnchor.End, settleAnchor(-120f, start, end))
    }

    @Test
    fun aSideWithoutActionsNeverOpens() {
        assertEquals(SwipeAnchor.Closed, settleAnchor(80f, startWidth = 0f, endWidth = end))
        assertEquals(SwipeAnchor.Closed, settleAnchor(-80f, startWidth = start, endWidth = 0f))
        assertEquals(SwipeAnchor.Closed, settleAnchor(10f, startWidth = 0f, endWidth = 0f))
    }

    @Test
    fun anchorOffsetIsTheRevealedActionWidth() {
        assertEquals(0f, anchorOffset(SwipeAnchor.Closed, start, end))
        assertEquals(100f, anchorOffset(SwipeAnchor.Start, start, end))
        assertEquals(-120f, anchorOffset(SwipeAnchor.End, start, end))
    }

    @Test
    fun dragIsLimitedToTheRevealedArea() {
        assertEquals(100f, clampOffset(250f, start, end))
        assertEquals(-120f, clampOffset(-250f, start, end))
        assertEquals(30f, clampOffset(30f, start, end))
    }

    @Test
    fun dragTowardsASideWithoutActionsStaysAtZero() {
        assertEquals(0f, clampOffset(40f, startWidth = 0f, endWidth = end))
        assertEquals(0f, clampOffset(-40f, startWidth = start, endWidth = 0f))
    }
}
