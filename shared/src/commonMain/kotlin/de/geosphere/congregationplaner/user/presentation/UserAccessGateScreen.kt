package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState

/**
 * Wird angezeigt, solange ein angemeldeter Nutzer (noch) keinen Zugriff auf die App hat.
 */
@Composable
internal fun UserAccessGateScreen(state: UserAccessState, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(brush = Brush.backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            UserAccessState.Loading,
            UserAccessState.Unauthenticated,
            is UserAccessState.Authorized,
            -> CircularProgressIndicator()

            is UserAccessState.NeedsApproval -> GateCard(
                title = "Freigabe ausstehend",
                message = "Dein Konto ${state.user.email.orEmpty()} wurde angelegt, ist aber noch nicht " +
                    "freigegeben. Bitte wende dich an einen Administrator.",
                primaryAction = "Status prüfen",
                onPrimaryAction = onRefresh,
                onSignOut = onSignOut,
            )

            is UserAccessState.Error -> GateCard(
                title = "Zugriff konnte nicht geprüft werden",
                message = state.message,
                primaryAction = "Erneut versuchen",
                onPrimaryAction = onRefresh,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun GateCard(
    title: String,
    message: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    onSignOut: () -> Unit,
) {
    Card(
        modifier = Modifier.widthIn(min = 300.dp, max = 500.dp).padding(24.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth()) {
                Text(primaryAction)
            }
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Abmelden")
            }
        }
    }
}

@ThemePreviews
@Composable
private fun UserAccessGateNeedsApprovalPreview() = PreviewThemeWrapper {
    UserAccessGateScreen(
        state = UserAccessState.NeedsApproval(AppUser(uid = "1", email = "neu@congregationplaner.de")),
        onRefresh = {},
        onSignOut = {},
    )
}

@ThemePreviews
@Composable
private fun UserAccessGateErrorPreview() = PreviewThemeWrapper {
    UserAccessGateScreen(
        state = UserAccessState.Error("Firestore is not configured for iOS."),
        onRefresh = {},
        onSignOut = {},
    )
}
