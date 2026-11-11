package de.geosphere.congregationplaner.ui.rating

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/** Bewertung in Sternen; [stars] sagt je Stern, ob er gefüllt ist. [onTap] bekommt die Nummer des Sterns (ab 1). */
@Composable
fun StarRating(stars: List<Boolean>, label: String = "Bewertung", onTap: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row {
            stars.forEachIndexed { index, filled ->
                TextButton(
                    onClick = { onTap(index + 1) },
                    modifier = Modifier.semantics { contentDescription = "${index + 1} Sterne" },
                ) { Text(if (filled) "★" else "☆", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun StarRatingPreview() = PreviewThemeWrapper {
    Column {
        StarRating(listOf(true, true, true, false, false)) {}
        StarRating(List(5) { false }, label = "Ohne Bewertung") {}
    }
}
