package de.geosphere.congregationplaner.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.home.domain.model.HomeSummary
import de.geosphere.congregationplaner.home.domain.usecase.ObserveHomeSummaryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class HomeUiState(val summary: HomeSummary = HomeSummary(), val errorMessage: String? = null)

/** Hält die Daten der Startseite; die Blöcke ergeben sich aus den Rollen des angemeldeten Nutzers. */
@Inject
class HomeViewModel(private val observeSummary: ObserveHomeSummaryUseCase) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState = mutableUiState.asStateFlow()

    private var observeJob: Job? = null

    /**
     * Beginnt die Beobachtung für [user] neu, z. B. nach der Anmeldung oder einem Rollenwechsel; ohne Nutzer wird
     * alles geleert. shortcut: [today] gilt ab dem Start der Beobachtung und springt nicht über Mitternacht, bei
     * Bedarf alle paar Stunden neu starten.
     */
    fun onCurrentUserChanged(user: AppUser?, today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())) {
        observeJob?.cancel()
        mutableUiState.value = HomeUiState()
        if (user == null) return
        observeJob = viewModelScope.launch {
            observeSummary(user, today)
                .catch { exception ->
                    val message = exception.message ?: "Die Startseite konnte nicht geladen werden."
                    mutableUiState.update { it.copy(errorMessage = message) }
                }.collect { summary -> mutableUiState.update { HomeUiState(summary) } }
        }
    }
}
