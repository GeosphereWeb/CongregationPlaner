package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.data.KeyValueStorage
import de.geosphere.congregationplaner.organization.data.createKeyValueStorage
import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemeStyle
import de.geosphere.congregationplaner.user.domain.repository.ThemePreference
import de.geosphere.congregationplaner.user.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// One instance for the whole app: the theme root and the settings page come from different graphs.
private val sharedThemePreferenceRepository by lazy { KeyValueThemePreferenceRepository(createKeyValueStorage()) }

fun createThemePreferenceRepository(): ThemePreferenceRepository = sharedThemePreferenceRepository

internal class KeyValueThemePreferenceRepository(private val storage: KeyValueStorage) : ThemePreferenceRepository {
    private val state = MutableStateFlow(
        ThemePreference(
            style = ThemeStyle.entries.find { it.name == storage.getString(KEY_STYLE) } ?: ThemeStyle.NIGHT,
            mode = ThemeMode.entries.find { it.name == storage.getString(KEY_MODE) } ?: ThemeMode.SYSTEM,
        ),
    )
    override val preference: StateFlow<ThemePreference> = state.asStateFlow()

    override suspend fun save(preference: ThemePreference) {
        storage.putString(KEY_STYLE, preference.style.name)
        storage.putString(KEY_MODE, preference.mode.name)
        state.value = preference
    }

    private companion object {
        const val KEY_STYLE = "theme_style"
        const val KEY_MODE = "theme_mode"
    }
}
