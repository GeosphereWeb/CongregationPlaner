package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.data.InMemoryKeyValueStorage
import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemeStyle
import de.geosphere.congregationplaner.user.domain.repository.ThemePreference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyValueThemePreferenceRepositoryTest {
    @Test
    fun `starts with the night design without a stored choice`() {
        val repository = KeyValueThemePreferenceRepository(InMemoryKeyValueStorage())

        assertEquals(ThemePreference(ThemeStyle.NIGHT, ThemeMode.SYSTEM), repository.preference.value)
    }

    @Test
    fun `saved choice is emitted and restored by a new instance`() = runTest {
        val storage = InMemoryKeyValueStorage()
        val choice = ThemePreference(ThemeStyle.PURPLE, ThemeMode.DARK)

        KeyValueThemePreferenceRepository(storage).apply {
            save(choice)
            assertEquals(choice, preference.value)
        }

        assertEquals(choice, KeyValueThemePreferenceRepository(storage).preference.value)
    }

    @Test
    fun `unknown stored values fall back to the defaults`() {
        val storage = InMemoryKeyValueStorage()
        storage.putString("theme_style", "NEON")
        storage.putString("theme_mode", "")

        assertEquals(ThemePreference(), KeyValueThemePreferenceRepository(storage).preference.value)
    }
}
