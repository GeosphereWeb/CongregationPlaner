package de.geosphere.congregationplaner.user.presentation.access

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import de.geosphere.congregationplaner.theming.NightColors

/** Hinweistext für Konten, die auf die Freigabe des Versammlungsadmins warten; die E-Mail ist hervorgehoben. */
internal fun approvalMessage(email: String): AnnotatedString = accountMessage(
    email,
    " wurde angelegt, ist aber noch nicht freigegeben. Bitte wende dich an den Admin deiner Versammlung.",
)

/** Hinweistext für Konten, die sich als globaler Admin angemeldet haben, aber nicht eingetragen sind. */
internal fun globalAdminMessage(email: String): AnnotatedString = accountMessage(
    email,
    " ist noch nicht als globaler Admin eingetragen. Globale Admins gehören zu keiner Versammlung; " +
        "eintragen kann dich nur der Betreiber der App. Gehörst du zu einer Versammlung, " +
        "melde dich ab und wähle sie bei der Anmeldung.",
)

private fun accountMessage(email: String, rest: String): AnnotatedString = buildAnnotatedString {
    append("Dein Konto ")
    withStyle(SpanStyle(color = NightColors.OnBackground, fontWeight = FontWeight.Medium)) { append(email) }
    append(rest)
}
