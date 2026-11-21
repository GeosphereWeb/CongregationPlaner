package de.geosphere.congregationplaner.user.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemeStyle
import de.geosphere.congregationplaner.user.domain.repository.ThemePreference
import de.geosphere.congregationplaner.user.domain.usecase.ObserveThemePreferenceUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveThemePreferenceUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The design of the app: read by the theme root, changed in the settings ("Allgemein" → "Darstellung"). */
@Inject
class ThemeViewModel(observe: ObserveThemePreferenceUseCase, private val save: SaveThemePreferenceUseCase) :
    ViewModel() {
    val preference: StateFlow<ThemePreference> = observe()

    fun selectStyle(style: ThemeStyle) = update(preference.value.copy(style = style))

    fun selectMode(mode: ThemeMode) = update(preference.value.copy(mode = mode))

    private fun update(preference: ThemePreference) {
        viewModelScope.launch { save(preference) }
    }
}
