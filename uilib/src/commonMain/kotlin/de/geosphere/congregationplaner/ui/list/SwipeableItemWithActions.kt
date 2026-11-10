package de.geosphere.congregationplaner.ui.list

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ein Eintrag, den man zur Seite wischen kann, um Aktionen dahinter aufzudecken: nach links für [endActions], nach
 * rechts für [startActions]. Der Eintrag rastet ein, sobald die halbe Aktionsbreite aufgedeckt ist (siehe
 * [settleAnchor]).
 *
 * Wer mehrere Einträge zeigt, hält selbst fest, welcher offen ist: [isRevealed] `false` schließt den Eintrag,
 * [onRevealedChange] meldet, wenn er sich öffnet oder schließt. So bleibt immer nur einer offen. Ein Eintrag ohne
 * Aktionen auf einer Seite lässt sich dorthin nicht wischen. Der [content] sollte einen deckenden Hintergrund haben,
 * damit die Aktionen dahinter nicht durchscheinen.
 */
@Composable
fun SwipeableItemWithActions(
    isRevealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    startActions: (@Composable RowScope.() -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var startWidth by remember { mutableFloatStateOf(0f) }
    var endWidth by remember { mutableFloatStateOf(0f) }
    var anchor by remember { mutableStateOf(SwipeAnchor.Closed) }
    var offset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val onChange by rememberUpdatedState(onRevealedChange)

    suspend fun animateTo(target: SwipeAnchor) {
        anchor = target
        animate(offset, anchorOffset(target, startWidth, endWidth)) { value, _ -> offset = value }
    }

    // Der Aufrufer hat einen anderen Eintrag geöffnet oder will alles schließen.
    LaunchedEffect(isRevealed) { if (!isRevealed && anchor != SwipeAnchor.Closed) animateTo(SwipeAnchor.Closed) }
    // Ändert sich die Breite der Aktionen (z. B. kommt eine hinzu), bleibt der Eintrag am richtigen Rand.
    LaunchedEffect(startWidth, endWidth) { if (anchor != SwipeAnchor.Closed) animateTo(anchor) }

    val dragState = rememberDraggableState { delta -> offset = clampOffset(offset + delta, startWidth, endWidth) }

    Box(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Solange der Eintrag geschlossen ist, sind die Aktionen unsichtbar, werden aber gemessen.
        val actionsAlpha = if (offset == 0f) 0f else 1f
        startActions?.let { actions ->
            Row(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().graphicsLayer { alpha = actionsAlpha }
                    .onSizeChanged { startWidth = it.width.toFloat() }.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        endActions?.let { actions ->
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().graphicsLayer { alpha = actionsAlpha }
                    .onSizeChanged { endWidth = it.width.toFloat() }.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Box(
            Modifier.fillMaxWidth()
                .offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        scope.launch {
                            animateTo(settleAnchor(offset, startWidth, endWidth))
                            onChange(anchor != SwipeAnchor.Closed)
                        }
                    },
                ),
        ) { content() }
    }
}

/** Runder Icon-Button für eine Aktion hinter einem wischbaren Eintrag; 48 dp groß, damit er gut zu treffen ist. */
@Composable
fun SwipeActionButton(
    icon: ImageVector,
    contentDescription: String,
    tone: StatusTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(48.dp).clip(CircleShape).background(tone.container)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(rememberVectorPainter(icon), contentDescription = contentDescription, tint = tone.content)
    }
}

@ThemePreviews
@Composable
private fun SwipeableItemPreview() = NightPreviewWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwipeActionButton(previewIcon, "Anrufen", StatusTone.Sky, onClick = {})
            SwipeActionButton(previewIcon, "Nachricht", StatusTone.Mint, onClick = {})
        }
        SwipeableItemWithActions(
            isRevealed = false,
            onRevealedChange = {},
            endActions = {
                SwipeActionButton(previewIcon, "Anrufen", StatusTone.Sky, onClick = {})
                SwipeActionButton(previewIcon, "Nachricht", StatusTone.Mint, onClick = {})
            },
        ) {
            Box(Modifier.fillMaxWidth().background(StatusTone.Sky.container).padding(16.dp)) {
                Text("Nach links wischen")
            }
        }
    }
}

private val previewIcon: ImageVector = ImageVector.Builder(
    name = "preview",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).build()
