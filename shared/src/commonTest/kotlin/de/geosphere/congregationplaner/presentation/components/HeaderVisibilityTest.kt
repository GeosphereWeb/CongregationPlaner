package de.geosphere.congregationplaner.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HeaderVisibilityTest {
    @Test
    fun scrollingListUpHidesHeader() = assertFalse(nextHeaderVisible(current = true, scrollDeltaY = -5f))

    @Test
    fun fastScrollDownShowsHeader() =
        assertTrue(nextHeaderVisible(current = false, scrollDeltaY = SHOW_HEADER_MIN_DELTA))

    @Test
    fun slowScrollDownKeepsHeaderHidden() =
        assertFalse(nextHeaderVisible(current = false, scrollDeltaY = SHOW_HEADER_MIN_DELTA - 1f))

    @Test
    fun noMovementKeepsState() {
        assertTrue(nextHeaderVisible(current = true, scrollDeltaY = 0f))
        assertFalse(nextHeaderVisible(current = false, scrollDeltaY = 0f))
    }

    @Test
    fun scrollConnectionHidesOnScrollUpAndShowsOnFastScrollDown() {
        val state = CollapsingHeaderState()
        assertTrue(state.visible)
        state.connection.onPreScroll(Offset(0f, -10f), NestedScrollSource.UserInput)
        assertFalse(state.visible)
        state.connection.onPreScroll(Offset(0f, SHOW_HEADER_MIN_DELTA), NestedScrollSource.UserInput)
        assertTrue(state.visible)
    }

    @Test
    fun listStartMeansFirstItemWithoutOffset() {
        assertTrue(isAtListStart(0, 0))
        assertFalse(isAtListStart(0, 1))
        assertFalse(isAtListStart(1, 0))
    }

    @Test
    fun headerIsShownAgainWhenListStartIsReached() = runTest {
        val state = CollapsingHeaderState()
        state.connection.onPreScroll(Offset(0f, -10f), NestedScrollSource.UserInput)
        state.showWhenAtListStart(flowOf(false))
        assertFalse(state.visible)
        state.showWhenAtListStart(flowOf(false, true))
        assertTrue(state.visible)
    }

    @Test
    fun scrollModifierIsAttached() = assertNotEquals(Modifier, Modifier.collapsingHeaderScroll(CollapsingHeaderState()))
}
