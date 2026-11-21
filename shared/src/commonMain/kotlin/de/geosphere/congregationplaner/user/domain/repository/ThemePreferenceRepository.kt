package de.geosphere.congregationplaner.user.domain.repository

import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemeStyle
import kotlinx.coroutines.flow.StateFlow

/** The design chosen on this device; without a choice the night-blue design. */
data class ThemePreference(val style: ThemeStyle = ThemeStyle.NIGHT, val mode: ThemeMode = ThemeMode.SYSTEM)

/** Stores the [ThemePreference] on the device; [preference] holds the current value from the start. */
interface ThemePreferenceRepository {
    val preference: StateFlow<ThemePreference>

    suspend fun save(preference: ThemePreference)
}
