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
    val newCongregation = selection.newCongregation.takeUnless { globalLogin }
    val isNewFlow = newCongregation != null
    val selected = selection.activeSelection
    val showCredentials = globalLogin ||
        selection.isNewCongregationConfirmed ||
        (selected != null && !selection.isPicking)
    val congregationName = if (globalLogin) {
        "Globale Verwaltung"
    } else {
        newCongregation?.name ?: selection.selected?.displayName
    }
    val circuitName = newCongregation?.circuitName ?: selection.selected?.circuitDisplayName
    val totalSteps = if (isNewFlow) NEW_CONGREGATION_STEPS else SELECTION_STEPS
    val step = when {
        showCredentials -> totalSteps
        isNewFlow -> NEW_CONGREGATION_NAME_STEP
        else -> 1
    }
    return LoginStep(
        stepLabel = "SCHRITT $step / $totalSteps",
        title = when {
            showCredentials -> congregationName ?: "Anmelden"
            isNewFlow -> "Neue Versammlung"
            else -> "Welche Versammlung?"
        },
        previousSteps = if (!globalLogin && (showCredentials || isNewFlow)) {
            circuitName?.let { listOf("Kreis $it") }.orEmpty()
        } else {
            emptyList()
        },
        showCredentials = showCredentials,
        canSignIn = selected != null || globalLogin || selection.isNewCongregationReady,
        isNewCongregationFlow = isNewFlow,
    )
}

private const val SELECTION_STEPS = 2
private const val NEW_CONGREGATION_STEPS = 3
private const val NEW_CONGREGATION_NAME_STEP = 2
