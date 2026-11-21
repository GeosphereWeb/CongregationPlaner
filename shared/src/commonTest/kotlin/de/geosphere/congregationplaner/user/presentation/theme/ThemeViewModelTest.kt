package de.geosphere.congregationplaner.user.presentation.theme

import de.geosphere.congregationplaner.organization.data.InMemoryKeyValueStorage
import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemeStyle
import de.geosphere.congregationplaner.user.data.KeyValueThemePreferenceRepository
import de.geosphere.congregationplaner.user.domain.repository.ThemePreference
import de.geosphere.congregationplaner.user.domain.usecase.ObserveThemePreferenceUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveThemePreferenceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    private val repository = KeyValueThemePreferenceRepository(InMemoryKeyValueStorage())
    private val viewModel by lazy {
        ThemeViewModel(ObserveThemePreferenceUseCase(repository), SaveThemePreferenceUseCase(repository))
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `style and mode are changed independently and saved`() {
        viewModel.selectMode(ThemeMode.LIGHT)
        viewModel.selectStyle(ThemeStyle.BLUE)

        val expected = ThemePreference(ThemeStyle.BLUE, ThemeMode.LIGHT)
        assertEquals(expected, viewModel.preference.value)
        assertEquals(expected, repository.preference.value)
    }
}
