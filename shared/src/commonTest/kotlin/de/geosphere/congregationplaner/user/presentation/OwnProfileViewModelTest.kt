package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.usecase.UpdateOwnNameUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OwnProfileViewModelTest {
    private val user = AppUser(
        uid = "u1",
        approved = true,
        congregation = testCongregation,
        profile = MemberProfile(lastName = "Muster", firstName = "Max"),
    )

    private lateinit var repository: FakeUserRepository
    private lateinit var viewModel: OwnProfileViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeUserRepository()
        viewModel = OwnProfileViewModel(UpdateOwnNameUseCase(repository))
        viewModel.onUserChanged(user)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the form starts with the saved names and nothing to save`() {
        val state = viewModel.uiState.value

        assertEquals("Muster", state.lastName)
        assertEquals("Max", state.firstName)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
    }

    @Test
    fun `an edit can be saved only while both names are filled`() {
        viewModel.onFirstNameChange("Maxi")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onFirstNameChange(" ")
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving writes the names and marks them as saved`() {
        viewModel.onFirstNameChange("Maxi")

        viewModel.save()

        assertEquals("Maxi", repository.updatedOwnNames.single().profile.firstName)
        val state = viewModel.uiState.value
        assertFalse(state.isDirty)
        assertFalse(state.isSaving)
        assertFalse(state.isError)
        assertEquals("Gespeichert.", state.message)
    }

    @Test
    fun `a failed save keeps the input and shows the error`() {
        repository.updateOwnNameResult = Result.failure(IllegalStateException("denied"))
        viewModel.onLastNameChange("Neu")

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals("Neu", state.lastName)
        assertTrue(state.isDirty)
        assertTrue(state.isError)
        assertEquals("denied", state.message)
        assertTrue(repository.updatedOwnNames.isEmpty())
    }

    @Test
    fun `a new stored state does not overwrite unsaved input`() {
        viewModel.onFirstNameChange("Maxi")

        viewModel.onUserChanged(user.copy(profile = MemberProfile(lastName = "Neu", firstName = "Max")))

        val state = viewModel.uiState.value
        assertEquals("Maxi", state.firstName)
        assertEquals("Muster", state.lastName)
        assertEquals("Neu" to "Max", state.saved)
    }

    @Test
    fun `a new stored state replaces the form while nothing was edited`() {
        viewModel.onUserChanged(user.copy(profile = MemberProfile(lastName = "Neu", firstName = "Anna")))

        val state = viewModel.uiState.value
        assertEquals("Neu", state.lastName)
        assertEquals("Anna", state.firstName)
        assertFalse(state.isDirty)
    }

    @Test
    fun `saving without a change does nothing`() {
        viewModel.save()

        assertTrue(repository.updatedOwnNames.isEmpty())
        assertNull(viewModel.uiState.value.message)
        assertNotNull(viewModel.uiState.value)
    }
}
