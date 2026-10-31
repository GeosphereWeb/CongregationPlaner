package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.chip.StatusChip
import de.geosphere.congregationplaner.ui.chip.StatusTone
import de.geosphere.congregationplaner.ui.header.NightScreen
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState

/**
 * Wird angezeigt, solange ein angemeldeter Nutzer (noch) keinen Zugriff auf die App hat.
 */
@Composable
internal fun UserAccessGateScreen(
    state: UserAccessState,
    onRefresh: () -> Unit,
    onChangeCongregation: () -> Unit,
    onSignOut: () -> Unit,
) {
    when (state) {
        UserAccessState.Loading,
        UserAccessState.Unauthenticated,
        UserAccessState.CongregationRequired,
        is UserAccessState.Authorized,
        -> Box(
            modifier = Modifier.fillMaxSize().background(NightColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = NightColors.Lavender)
        }

        is UserAccessState.NeedsApproval -> GateContent(
            current = "Warten auf Freigabe",
            chipText = "Freigabe ausstehend",
            chipTone = StatusTone.Gold,
            title = "Fast geschafft",
            message = approvalMessage(state.user.email.orEmpty()),
            hint = "Sobald die Freigabe erteilt ist, geht es ohne neue Anmeldung weiter.",
            primaryAction = "Status prüfen",
            onPrimaryAction = onRefresh,
            onChangeCongregation = onChangeCongregation,
            onSignOut = onSignOut,
        )

        is UserAccessState.Error -> GateContent(
            current = "Zugriff nicht geprüft",
            chipText = "Fehler",
            chipTone = StatusTone.Coral,
            title = "Zugriff konnte nicht geprüft werden",
            message = AnnotatedString(state.message),
            hint = null,
            primaryAction = "Erneut versuchen",
            onPrimaryAction = onRefresh,
            onChangeCongregation = onChangeCongregation,
            onSignOut = onSignOut,
        )
    }
}

private fun approvalMessage(email: String) = buildAnnotatedString {
    append("Dein Konto ")
    withStyle(SpanStyle(color = NightColors.OnBackground, fontWeight = FontWeight.Medium)) { append(email) }
    append(" wurde angelegt, ist aber noch nicht freigegeben. Bitte wende dich an den Admin deiner Versammlung.")
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun GateContent(
    current: String,
    chipText: String,
    chipTone: StatusTone,
    title: String,
    message: AnnotatedString,
    hint: String?,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    onChangeCongregation: () -> Unit,
    onSignOut: () -> Unit,
) {
    NightScreen(stepLabel = "ZUGANG", current = current, previous = listOf("Konto angelegt")) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusChip(chipText, chipTone)
            Text(title, style = nightTextStyle(28.sp, FontWeight.Bold, letterSpacing = (-0.56).sp))
            Text(message, style = nightTextStyle(16.sp, color = NightColors.OnBackgroundBody, lineHeight = 24.sp))
            if (hint != null) HintCard(hint)
        }
        Spacer(Modifier.weight(1f))
        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(primaryAction, onPrimaryAction, Modifier.fillMaxWidth())
            SecondaryButton("Andere Versammlung wählen", onChangeCongregation, Modifier.fillMaxWidth())
            SecondaryButton("Abmelden", onSignOut, Modifier.fillMaxWidth(), outlined = false)
        }
    }
}

@Composable
@Suppress("MagicNumber")
private fun HintCard(text: String) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier.fillMaxWidth().background(NightColors.Surface, shape)
            .border(1.dp, NightColors.Border, shape).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val unit = size.width / 24f
            drawCircle(NightColors.Sky, radius = 9f * unit, style = Stroke(1.7f * unit, cap = StrokeCap.Round))
            // Ausrufezeichen-artiger Strich und Punkt des Info-Symbols
            for ((from, to) in listOf(11f to 16f, 7.5f to 8f)) {
                val start = Offset(12f * unit, from * unit)
                drawLine(NightColors.Sky, start, Offset(12f * unit, to * unit), 1.7f * unit, StrokeCap.Round)
            }
        }
        Text(text, style = nightTextStyle(14.sp, color = NightColors.OnBackgroundMuted, lineHeight = 20.sp))
    }
}

@Composable
private fun GatePreview(state: UserAccessState) = NightPhonePreview {
    UserAccessGateScreen(state = state, onRefresh = {}, onChangeCongregation = {}, onSignOut = {})
}

@ThemePreviews
@Composable
private fun UserAccessGateNeedsApprovalPreview() =
    GatePreview(UserAccessState.NeedsApproval(AppUser(uid = "1", email = "name@beispiel.de")))

@ThemePreviews
@Composable
private fun UserAccessGateErrorPreview() = GatePreview(UserAccessState.Error("Firestore is not configured for iOS."))
