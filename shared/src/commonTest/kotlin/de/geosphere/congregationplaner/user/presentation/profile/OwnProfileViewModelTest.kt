package de.geosphere.congregationplaner.user.presentation.profile

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.usecase.UpdateOwnProfileUseCase
import de.geosphere.congregationplaner.user.presentation.contact.canAddContact
import de.geosphere.congregationplaner.user.presentation.contact.canAddEmail
import de.geosphere.congregationplaner.user.presentation.contact.emailFields
import de.geosphere.congregationplaner.user.presentation.contact.withAddedContact
import de.geosphere.congregationplaner.user.presentation.contact.withAddedEmail
import de.geosphere.congregationplaner.user.presentation.contact.withContact
import de.geosphere.congregationplaner.user.presentation.contact.withEmail
import de.geosphere.congregationplaner.user.presentation.contact.withoutContact
import de.geosphere.congregationplaner.user.presentation.contact.withoutEmail
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
        profile = MemberProfile(
            lastName = "Muster",
            firstName = "Max",
            mobilePhone = "0170",
            street = "Hauptstr. 1",
            postalCode = "10115",
            city = "Berlin",
            emergencyContacts = listOf(EmergencyContact("Anna", "Ehefrau", listOf("0171"))),
        ),
    )

    private lateinit var users: FakeUserRepository
    private lateinit var viewModel: OwnProfileViewModel

    private val state get() = viewModel.uiState.value
    private val form get() = state.form

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        users = FakeUserRepository()
        viewModel = OwnProfileViewModel(UpdateOwnProfileUseCase(users))
        viewModel.onUserChanged(user)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the form starts with the saved profile and nothing to save`() {
        assertEquals(user.profile, form)
        assertFalse(state.isDirty)
        assertFalse(state.canSave)
    }

    @Test
    fun `an edit can be saved only while both names are filled`() {
        viewModel.onFormChange(form.copy(landlinePhone = "030 1"))
        assertTrue(state.canSave)

        viewModel.onFormChange(form.copy(firstName = " "))
        assertFalse(state.canSave)
    }

    @Test
    fun `phone and address are saved together in the member document`() {
        viewModel.onFormChange(form.copy(mobilePhone = "0172", city = "Potsdam"))

        viewModel.save()

        val saved = users.updatedOwnProfiles.single().profile
        assertEquals("0172", saved.mobilePhone)
        assertEquals("Potsdam", saved.city)
        assertFalse(state.isDirty)
        assertEquals("Gespeichert.", state.message)
    }

    @Test
    fun `contacts can be added edited and removed up to the limit`() {
        viewModel.onFormChange(form.withAddedContact())
        viewModel.onFormChange(form.withContact(1, EmergencyContact("Ben", "", listOf("030 2"))))
        assertEquals(EmergencyContact("Ben", "", listOf("030 2")), form.emergencyContacts[1])

        repeat(5) { viewModel.onFormChange(form.withAddedContact()) }
        assertEquals(MemberProfile.MAX_EMERGENCY_CONTACTS, form.emergencyContacts.size)
        assertFalse(form.canAddContact)

        viewModel.onFormChange(form.withoutContact(0))
        assertEquals("Ben", form.emergencyContacts.first().name)
    }

    @Test
    fun `empty new contacts are dropped when saving`() {
        viewModel.onFormChange(form.withAddedContact().copy(street = "Nebenstr. 2"))

        viewModel.save()

        assertEquals(user.profile.emergencyContacts, users.updatedOwnProfiles.single().profile.emergencyContacts)
        assertEquals(user.profile.emergencyContacts, form.emergencyContacts)
        assertFalse(state.isDirty)
    }

    @Test
    fun `an incomplete contact keeps the input and shows the error`() {
        viewModel.onFormChange(form.withAddedContact())
        viewModel.onFormChange(form.withContact(1, EmergencyContact(name = "Ben", phones = listOf(""))))

        viewModel.save()

        assertTrue(state.isError)
        assertTrue(state.isDirty)
        assertTrue(users.updatedOwnProfiles.isEmpty())
    }

    @Test
    fun `a new stored profile does not overwrite unsaved input but takes the fields of the admin`() {
        viewModel.onFormChange(form.copy(city = "Potsdam"))

        viewModel.onUserChanged(
            user.copy(profile = user.profile.copy(street = "Neu 1", category = MemberCategory.BROTHER)),
        )

        assertEquals("Potsdam", form.city)
        assertEquals("Hauptstr. 1", form.street)
        assertEquals(MemberCategory.BROTHER, form.category)
        assertEquals("Neu 1", state.saved.street)
    }

    @Test
    fun `the first e-mail field is always there and two more can be added and removed`() {
        assertEquals(listOf(""), form.emailFields)

        viewModel.onFormChange(form.withEmail(0, "max@beispiel.de"))
        repeat(3) { viewModel.onFormChange(form.withAddedEmail()) }
        assertEquals(3, form.emailFields.size)
        assertFalse(form.canAddEmail)

        viewModel.onFormChange(form.withoutEmail(0))
        assertEquals("max@beispiel.de", form.emailFields.first())
        viewModel.onFormChange(form.withoutEmail(2))
        viewModel.onFormChange(form.withoutEmail(1))
        viewModel.save()

        assertEquals(listOf("max@beispiel.de"), users.updatedOwnProfiles.single().profile.emails)
        assertEquals(listOf("max@beispiel.de"), form.emails)
        assertFalse(state.isDirty)
    }

    @Test
    fun `adding a second field without a first address keeps the first field`() {
        viewModel.onFormChange(form.withAddedEmail())

        assertEquals(listOf("", ""), form.emailFields)

        viewModel.onFormChange(form.withEmail(0, "a"))
        viewModel.onFormChange(form.withEmail(0, ""))
        assertEquals(2, form.emailFields.size)
    }

    @Test
    fun `clearing the only e-mail field leaves no address`() {
        viewModel.onFormChange(form.withEmail(0, "a"))
        viewModel.onFormChange(form.withEmail(0, ""))

        assertEquals(emptyList(), form.emails)
        assertFalse(state.isDirty)
    }

    @Test
    fun `saving without a change does nothing`() {
        viewModel.save()

        assertTrue(users.updatedOwnProfiles.isEmpty())
        assertNull(state.message)
    }
}
