package de.geosphere.congregationplaner.presentation.navigation

import androidx.lifecycle.ViewModel
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Der Navigationszustand der angemeldeten App. [congregationSection] und [globalSection] sind `null`, solange die
 * Übersicht des Hauptbereichs offen ist.
 */
data class AppNavigationState(
    val visible: VisibleNavigation = VisibleNavigation(),
    val destination: TopLevelDestination = TopLevelDestination.HOME,
    val congregationSection: CongregationSection? = null,
    val globalSection: GlobalSection? = null,
)

/** Hält die Auswahl und setzt sie auf Home bzw. die Übersicht zurück, wenn ein Bereich nicht mehr sichtbar ist. */
@Inject
class AppNavigationViewModel(private val access: NavigationAccess) : ViewModel() {
    private val mutableState = MutableStateFlow(AppNavigationState())
    val uiState = mutableState.asStateFlow()

    /** Berechnet die sichtbaren Bereiche neu, z. B. nach der Anmeldung oder einem Rollenwechsel. */
    fun onUserChanged(user: AppUser?) {
        val visible = user?.let(access::visibleFor) ?: VisibleNavigation()
        mutableState.update { state ->
            state.copy(
                visible = visible,
                destination = state.destination.takeIf { it in visible.topLevel } ?: TopLevelDestination.HOME,
                congregationSection = state.congregationSection?.takeIf { it in visible.congregationSections },
                globalSection = state.globalSection?.takeIf { it in visible.globalSections },
            )
        }
    }

    /** Öffnet einen Hauptbereich mit seiner Übersicht; nicht sichtbare Bereiche werden ignoriert. */
    fun select(destination: TopLevelDestination) {
        mutableState.update { state ->
            if (destination !in state.visible.topLevel) {
                state
            } else {
                state.copy(destination = destination, congregationSection = null, globalSection = null)
            }
        }
    }

    fun openCongregationSection(section: CongregationSection?) {
        mutableState.update { state ->
            if (section != null && section !in state.visible.congregationSections) {
                state
            } else {
                state.copy(destination = TopLevelDestination.CONGREGATION, congregationSection = section)
            }
        }
    }

    fun openGlobalSection(section: GlobalSection?) {
        mutableState.update { state ->
            if (section != null && section !in state.visible.globalSections) {
                state
            } else {
                state.copy(destination = TopLevelDestination.GLOBAL_ADMIN, globalSection = section)
            }
        }
    }
}
