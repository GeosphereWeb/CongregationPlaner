package de.geosphere.congregationplaner.auth.presentation

import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelectionUiState

/** Was der Kopf und der Inhalt der Anmeldung im aktuellen Schritt zeigen. */
data class LoginStep(
    val stepLabel: String,
    val title: String,
    val previousSteps: List<String> = emptyList(),
    /** Die Versammlung steht fest; jetzt folgen die Zugangsdaten. */
    val showCredentials: Boolean = false,
    val canSignIn: Boolean = false,
    /** Eine neue Versammlung fügt zwischen Kreis und Zugangsdaten den Schritt für den Namen ein. */
    val isNewCongregationFlow: Boolean = false,
)

/** Leitet den Schritt der Anmeldung aus der Versammlungsauswahl ab; [globalLogin] überspringt die Auswahl. */
fun loginStep(selection: CongregationSelectionUiState, globalLogin: Boolean): LoginStep {
    val isNewFlow = !globalLogin && selection.newCongregation != null
    val showCredentials = showsCredentials(selection, globalLogin)
    return LoginStep(
        stepLabel = stepLabel(showCredentials, isNewFlow),
        title = when {
            showCredentials -> congregationName(selection, globalLogin) ?: "Anmelden"
            isNewFlow -> "Neue Versammlung"
            else -> "Welche Versammlung?"
        },
        previousSteps = if (!globalLogin && (showCredentials || isNewFlow)) circuitStep(selection) else emptyList(),
        showCredentials = showCredentials,
        canSignIn = globalLogin || selection.activeSelection != null || selection.isNewCongregationReady,
        isNewCongregationFlow = isNewFlow,
    )
}

/** Die Versammlung steht fest (oder entfällt für globale Admins); jetzt folgen die Zugangsdaten. */
private fun showsCredentials(selection: CongregationSelectionUiState, globalLogin: Boolean): Boolean = globalLogin ||
    selection.isNewCongregationConfirmed ||
    (selection.activeSelection != null && !selection.isPicking)

private fun stepLabel(showCredentials: Boolean, isNewFlow: Boolean): String {
    val totalSteps = if (isNewFlow) NEW_CONGREGATION_STEPS else SELECTION_STEPS
    val step = when {
        showCredentials -> totalSteps
        isNewFlow -> NEW_CONGREGATION_NAME_STEP
        else -> 1
    }
    return "SCHRITT $step / $totalSteps"
}

private fun congregationName(selection: CongregationSelectionUiState, globalLogin: Boolean): String? =
    if (globalLogin) "Globale Verwaltung" else selection.newCongregation?.name ?: selection.selected?.displayName

private fun circuitStep(selection: CongregationSelectionUiState): List<String> =
    (selection.newCongregation?.circuitName ?: selection.selected?.circuitDisplayName)
        ?.let { listOf("Kreis $it") }
        .orEmpty()

private const val SELECTION_STEPS = 2
private const val NEW_CONGREGATION_STEPS = 3
private const val NEW_CONGREGATION_NAME_STEP = 2
