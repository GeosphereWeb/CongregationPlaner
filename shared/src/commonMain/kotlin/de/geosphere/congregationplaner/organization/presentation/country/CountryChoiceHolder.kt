package de.geosphere.congregationplaner.organization.presentation.country

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.CountryOption
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.organization.domain.model.preferredFor
import de.geosphere.congregationplaner.organization.domain.usecase.ListCountriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Die Länder zur Auswahl und das gewählte; [errorMessage], wenn sie nicht geladen werden konnten. */
data class CountryChoiceState(
    val countries: List<CountryOption> = emptyList(),
    val selected: CountryRef? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedOption: CountryOption? get() = countries.firstOrNull { it.ref == selected }
}

/**
 * Lädt die Länder einmal und hält die Auswahl, z. B. für die Pflege der Vortragsliste durch den globalen Admin
 * (ADR-0025, ADR-0034). [onSelected] erfährt jedes neu gewählte Land.
 */
class CountryChoiceHolder(
    private val scope: CoroutineScope,
    private val listCountries: ListCountriesUseCase,
    private val read: () -> CountryChoiceState,
    private val write: (CountryChoiceState) -> Unit,
    private val onSelected: (CountryRef?) -> Unit = {},
) {
    private var loadJob: Job? = null
    private var congregation: CongregationRef? = null

    /** Lädt die Länder, falls noch nicht geschehen; vorgewählt wird das Land von [ownCongregation]. */
    fun load(ownCongregation: CongregationRef?) {
        congregation = ownCongregation
        val state = read()
        if (loadJob?.isActive == true || state.countries.isNotEmpty()) return
        write(state.copy(isLoading = true, errorMessage = null))
        loadJob = scope.launch {
            listCountries()
                .onSuccess { countries ->
                    val selected = countries.preferredFor(congregation)
                    write(CountryChoiceState(countries, selected))
                    onSelected(selected)
                }.onFailure { exception ->
                    val message = exception.message ?: "Die Länder konnten nicht geladen werden."
                    write(read().copy(isLoading = false, errorMessage = message))
                }
        }
    }

    /** Lädt nach einem Fehler erneut. */
    fun retry() = load(congregation)

    fun select(country: CountryRef) {
        val state = read()
        if (state.selected == country || state.countries.none { it.ref == country }) return
        write(state.copy(selected = country))
        onSelected(country)
    }
}
