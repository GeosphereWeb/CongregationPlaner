package de.geosphere.congregationplaner.user.presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import de.geosphere.congregationplaner.theming.NightColors

/** Hinweistext für Konten, die auf die Freigabe des Versammlungsadmins warten; die E-Mail ist hervorgehoben. */
internal fun approvalMessage(email: String): AnnotatedString = buildAnnotatedString {
    append("Dein Konto ")
    withStyle(SpanStyle(color = NightColors.OnBackground, fontWeight = FontWeight.Medium)) { append(email) }
    append(" wurde angelegt, ist aber noch nicht freigegeben. Bitte wende dich an den Admin deiner Versammlung.")
}
