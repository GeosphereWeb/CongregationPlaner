package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.usecase.ObserveOwnPrivateDetailsUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveOwnPrivateDetailsUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateOwnProfileUseCase
import de.geosphere.congregationplaner.user.testing.FakeMemberPrivateRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OwnProfileViewModelTest {
    private val user = AppUser(
        uid = "u1",
        approved = true,
        congregation = testCongregation,
        profile = MemberProfile(lastName = "Muster", firstName = "Max", mobilePhone = "0170"),
    )
    private val stored = MemberPrivateDetails(
        street = "Hauptstr. 1",
        postalCode = "10115",
        city = "Berlin",
        emergencyContacts = listOf(EmergencyContact("Anna", "Ehefrau", listOf("0171"))),
    )

    private lateinit var users: FakeUserRepository
    private lateinit var privateDetails: FakeMemberPrivateRepository
    private lateinit var viewModel: OwnProfileViewModel

    private val state get() = viewModel.uiState.value

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        users = FakeUserRepository()
        privateDetails = FakeMemberPrivateRepository(mapOf("u1" to stored))
        viewModel = OwnProfileViewModel(
            UpdateOwnProfileUseCase(users),
            ObserveOwnPrivateDetailsUseCase(privateDetails),
            SaveOwnPrivateDetailsUseCase(privateDetails),
        )
        viewModel.onUserChanged(user)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the form starts with the saved profile and private details and nothing to save`() {
        assertEquals("Muster", state.form.lastName)
        assertEquals("0170", state.form.mobilePhone)
        assertEquals(stored, state.form.details)
        assertTrue(state.detailsLoaded)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
    }

    @Test
    fun `an edit can be saved only while both names are filled`() {
        viewModel.onLandlinePhoneChange("030 1")
        assertTrue(state.canSave)

        viewModel.onFirstNameChange(" ")
        assertFalse(state.canSave)
    }

    @Test
    fun `changing only the phone writes the member document and leaves the private details alone`() {
        viewModel.onMobilePhoneChange("0172")

        viewModel.save()

        assertEquals("0172", users.updatedOwnProfiles.single().profile.mobilePhone)
        assertTrue(privateDetails.savedUids.isEmpty())
        assertFalse(state.isDirty)
        assertEquals("Gespeichert.", state.message)
    }

    @Test
    fun `changing only the address writes the private details`() {
        viewModel.onCityChange("Potsdam")

        viewModel.save()

        assertTrue(users.updatedOwnProfiles.isEmpty())
        assertEquals("Potsdam", privateDetails.details.value["u1"]?.city)
        assertFalse(state.isDirty)
    }

    @Test
    fun `contacts can be added edited and removed up to the limit`() {
        viewModel.addContact()
        viewModel.onContactChange(1, EmergencyContact("Ben", "", listOf("030 2")))
        assertEquals(EmergencyContact("Ben", "", listOf("030 2")), state.form.details.emergencyContacts[1])

        repeat(5) { viewModel.addContact() }
        assertEquals(MemberPrivateDetails.MAX_EMERGENCY_CONTACTS, state.form.details.emergencyContacts.size)
        assertFalse(state.canAddContact)

        viewModel.removeContact(0)
        assertEquals("Ben", state.form.details.emergencyContacts.first().name)
    }

    @Test
    fun `empty new contacts are dropped when saving`() {
        viewModel.addContact()
        viewModel.onStreetChange("Nebenstr. 2")

        viewModel.save()

        assertEquals(stored.emergencyContacts, privateDetails.details.value["u1"]?.emergencyContacts)
        assertEquals(stored.emergencyContacts, state.form.details.emergencyContacts)
        assertFalse(state.isDirty)
    }

    @Test
    fun `an incomplete contact keeps the input and shows the error`() {
        viewModel.addContact()
        viewModel.onContactChange(1, EmergencyContact(name = "Ben", phones = listOf("")))

        viewModel.save()

        assertTrue(state.isError)
        assertTrue(state.isDirty)
        assertTrue(privateDetails.savedUids.isEmpty())
    }

    @Test
    fun `a failed private save keeps the saved member document`() {
        privateDetails.saveResult = Result.failure(IllegalStateException("denied"))
        viewModel.onMobilePhoneChange("0172")
        viewModel.onCityChange("Potsdam")

        viewModel.save()

        assertEquals("denied", state.message)
        assertEquals("0172", state.saved.mobilePhone)
        assertEquals("Berlin", state.saved.details.city)
        assertTrue(state.isDirty)
    }

    @Test
    fun `new stored details do not overwrite unsaved input`() {
        viewModel.onCityChange("Potsdam")

        privateDetails.details.value = mapOf("u1" to stored.copy(street = "Neu 1"))

        assertEquals("Potsdam", state.form.details.city)
        assertEquals("Hauptstr. 1", state.form.details.street)
        assertEquals("Neu 1", state.saved.details.street)
    }

    @Test
    fun `saving without a change does nothing`() {
        viewModel.save()

        assertTrue(users.updatedOwnProfiles.isEmpty())
        assertTrue(privateDetails.savedUids.isEmpty())
        assertNull(state.message)
    }
}
