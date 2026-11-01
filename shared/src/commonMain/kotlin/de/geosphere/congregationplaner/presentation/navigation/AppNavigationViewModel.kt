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
) {
    /** Zurück führt vom Unterbereich zur Übersicht und von dort zu Home; auf Home gibt es nichts mehr. */
    val canGoBack: Boolean get() = destination != TopLevelDestination.HOME
}

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

    /**
     * Ein Schritt zurück: Unterbereich → Übersicht → Home. Liefert `false`, wenn die App schon auf Home steht und das
     * System-Zurück greifen soll.
     */
    fun back(): Boolean {
        val state = mutableState.value
        return when {
            state.destination == TopLevelDestination.CONGREGATION && state.congregationSection != null ->
                true.also { openCongregationSection(null) }

            state.destination == TopLevelDestination.GLOBAL_ADMIN && state.globalSection != null ->
                true.also { openGlobalSection(null) }

            state.destination != TopLevelDestination.HOME -> true.also { select(TopLevelDestination.HOME) }

            else -> false
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
