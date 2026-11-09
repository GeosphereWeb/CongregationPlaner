package de.geosphere.congregationplaner.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

/** Minimum downward movement (px per scroll event) that counts as a fast scroll and shows the header again. */
internal const val SHOW_HEADER_MIN_DELTA = 75f

/**
 * Decides whether the collapsible header is shown after a scroll step.
 * [scrollDeltaY] is the finger movement: negative = list scrolls up (hide), positive = scrolls down.
 * Slow downward scrolling keeps the header hidden so the full list stays visible; only a fast one shows it.
 */
internal fun nextHeaderVisible(current: Boolean, scrollDeltaY: Float): Boolean = when {
    scrollDeltaY < 0f -> false
    scrollDeltaY >= SHOW_HEADER_MIN_DELTA -> true
    else -> current
}

/** True when the list shows its very first pixel, i.e. nothing is scrolled away above it. */
internal fun isAtListStart(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

/** Scroll-driven visibility of a header above a list; shown again whenever [listState] is back at the top. */
@Stable
internal class CollapsingHeaderState {
    var visible by mutableStateOf(true)
        internal set

    internal val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            visible = nextHeaderVisible(visible, available.y)
            return Offset.Zero
        }
    }
}

/** Shows the header whenever [atListStart] emits true. */
internal suspend fun CollapsingHeaderState.showWhenAtListStart(atListStart: Flow<Boolean>) {
    atListStart.filter { it }.collect { visible = true }
}

@Composable
internal fun rememberCollapsingHeaderState(listState: LazyListState): CollapsingHeaderState {
    val state = remember { CollapsingHeaderState() }
    LaunchedEffect(listState) {
        state.showWhenAtListStart(
            snapshotFlow { isAtListStart(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) },
        )
    }
    return state
}

/**
 * Column with a [header] above a list that hides while the list scrolls up. Only a fast downward scroll or
 * reaching the top of [listState] shows it again; [forceVisible] keeps it shown (e.g. while searching).
 * [content] holds the list that uses [listState].
 */
@Composable
fun CollapsingHeaderColumn(
    listState: LazyListState,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    forceVisible: Boolean = false,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberCollapsingHeaderState(listState)
    Column(modifier = modifier.nestedScroll(state.connection), verticalArrangement = Arrangement.spacedBy(spacing)) {
        CollapsingHeader(visible = state.visible || forceVisible, spacing = spacing, content = header)
        content()
    }
}

@Composable
internal fun CollapsingHeader(visible: Boolean, modifier: Modifier = Modifier, spacing: Dp = 12.dp, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) { content() }
    }
}

@ThemePreviews
@Composable
private fun CollapsingHeaderColumnPreview() = PreviewThemeWrapper {
    Column {
        CollapsingHeader(visible = true) { Text("Header sichtbar") }
        CollapsingHeader(visible = false) { Text("Header ausgeblendet") }
        CollapsingHeaderColumn(listState = rememberLazyListState(), header = { Text("Header") }) {
            LazyColumn { items(count = 3) { Text("Eintrag $it") } }
        }
    }
}
