package de.geosphere.congregationplaner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.navigation.arrowBackIcon
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.ui.button.SecondaryButton

private val WIDE_PADDING = PaddingValues(horizontal = 32.dp, vertical = 28.dp)

/**
 * Detailbereich einer Listen-/Detailansicht. Auf großen Fenstern (Liste sichtbar) liegt er auf `SurfaceDetail` mit
 * 28 × 32 dp Innenabstand; auf dem Handy ([showBack]) erscheint oben „Zurück zur Liste“, weil die Liste verdeckt ist.
 */
@Composable
fun DetailPane(
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .then(if (showBack) Modifier else Modifier.background(NightColors.SurfaceDetail))
            .verticalScroll(rememberScrollState())
            .padding(if (showBack) PaddingValues(0.dp) else WIDE_PADDING),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showBack) {
            SecondaryButton(
                text = "Zurück zur Liste",
                onClick = onBack,
                enabled = backEnabled,
                outlined = false,
                leading = { Icon(arrowBackIcon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        content()
    }
}

/** Ruhiger Hinweis mittig im Detailbereich, solange nichts ausgewählt ist. */
@Composable
fun DetailPlaceholder(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = NightColors.OnBackgroundMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Kopf einer Liste: Titel, optional ein Untertitel und – wenn [onPrevious] bzw. [onNext] gesetzt sind – zwei
 * Pfeile zum Blättern (z. B. Jahr zurück/vor), je 44 dp groß.
 */
@Composable
@Suppress("LongParameterList")
fun ListPaneHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onPrevious: (() -> Unit)? = null,
    previousDescription: String = "Zurück",
    onNext: (() -> Unit)? = null,
    nextDescription: String = "Vor",
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = NightColors.OnBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NightColors.OnBackgroundMuted)
            }
        }
        if (onPrevious != null) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                Icon(arrowBackIcon, contentDescription = previousDescription)
            }
        }
        if (onNext != null) {
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                // Der Pfeil nach rechts ist der gespiegelte Zurück-Pfeil.
                Icon(arrowBackIcon, nextDescription, Modifier.scale(scaleX = -1f, scaleY = 1f))
            }
        }
    }
}

@Preview(name = "Handy", widthDp = 360, heightDp = 400)
@Composable
private fun DetailPanePhonePreview() = AppTheme {
    DetailPane(showBack = true, onBack = {}) { DetailPlaceholder("Wähle einen Termin zum Bearbeiten aus.") }
}

@Preview(name = "Tablet", widthDp = 700, heightDp = 400)
@Composable
private fun DetailPaneWidePreview() = AppTheme {
    Column {
        ListPaneHeader("Planung am Wochenende", subtitle = "2026", onPrevious = {}, onNext = {})
        DetailPane(showBack = false, onBack = {}) { DetailPlaceholder("Wähle einen Termin zum Bearbeiten aus.") }
    }
}
