package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkUseCase
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
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
class TalkCatalogManagementViewModelTest {
    private val admin = AppUser(uid = "admin", isGlobalAdmin = true)
    private val congregationAdmin = AppUser(uid = "ca", approved = true, role = UserRole.CONGREGATION_ADMIN)
    private val first = Talk(1, "Erster Vortrag")
    private val second = Talk(2, "Zweiter Vortrag", active = false)

    private lateinit var catalog: FakeTalkCatalogRepository
    private lateinit var viewModel: TalkCatalogManagementViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        catalog = FakeTalkCatalogRepository(listOf(second, first))
        val policy = TalkReferenceDataPolicy()
        viewModel = TalkCatalogManagementViewModel(
            ObserveTalkCatalogUseCase(catalog),
            SaveTalkUseCase(catalog, policy),
            DeleteTalkUseCase(catalog, policy),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `global admin sees the sorted talk list`() {
        viewModel.onCurrentUserChanged(admin)

        assertTrue(state.canManage)
        assertFalse(state.isLoading)
        assertEquals(listOf(first, second), state.talks)
    }

    @Test
    fun `other users neither load nor edit`() {
        viewModel.onCurrentUserChanged(congregationAdmin)
        viewModel.newTalk()

        assertFalse(state.canManage)
        assertTrue(state.talks.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.editor)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `a new talk takes at most four digits and can be saved`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newTalk()
        assertTrue(state.editor!!.isNew)

        viewModel.updateEditor(state.editor!!.copy(numberText = "1a2b3456789", title = "  Neu  "))
        assertEquals("1234", state.editor!!.numberText)

        viewModel.save()

        val saved = Talk(1234, "Neu")
        assertEquals(listOf(saved), catalog.savedTalks)
        assertEquals(saved, state.editor!!.original)
        assertFalse(state.editor!!.isNew)
        assertFalse(state.isSaving)
    }

    @Test
    fun `the number of an existing talk cannot change`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editTalk(first)

        viewModel.updateEditor(state.editor!!.copy(numberText = "99", title = "Anders", active = false))

        assertEquals("1", state.editor!!.numberText)
        assertEquals(Talk(1, "Anders", active = false), state.editor!!.talk)
        assertTrue(state.editor!!.isDirty)

        viewModel.save()
        assertEquals(listOf(Talk(1, "Anders", active = false)), catalog.savedTalks)
    }

    @Test
    fun `a missing or already used number is refused`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newTalk()
        viewModel.updateEditor(state.editor!!.copy(numberText = "", title = "Titel"))

        viewModel.save()
        assertEquals("Bitte gib eine Vortragsnummer ein.", state.errorMessage)

        viewModel.dismissError()
        viewModel.updateEditor(state.editor!!.copy(numberText = "1"))
        viewModel.save()
        assertEquals("Die Nummer 1 gibt es schon.", state.errorMessage)
        assertTrue(catalog.savedTalks.isEmpty())
    }

    @Test
    fun `invalid input and repository failures show a message and keep the editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newTalk()
        viewModel.updateEditor(state.editor!!.copy(numberText = "5", title = " "))

        viewModel.save()
        assertNotNull(state.errorMessage)
        assertFalse(state.isSaving)

        viewModel.dismissError()
        viewModel.updateEditor(state.editor!!.copy(title = "Titel"))
        catalog.saveResult = Result.failure(IllegalStateException("offline"))
        viewModel.save()
        assertEquals("offline", state.errorMessage)
        assertTrue(state.editor!!.isNew)
    }

    @Test
    fun `deleting the open talk closes the editor and failures are reported`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editTalk(first)

        viewModel.delete(first)

        assertEquals(listOf(1), catalog.deletedNumbers)
        assertNull(state.editor)
        assertEquals(listOf(second), state.talks)

        catalog.deleteResult = Result.failure(IllegalStateException("denied"))
        viewModel.delete(second)
        assertEquals("denied", state.errorMessage)
    }

    @Test
    fun `closing the editor discards it and a load error is retried when dismissed`() {
        catalog.observeError = IllegalStateException("denied")
        viewModel.onCurrentUserChanged(admin)
        assertEquals("denied", state.errorMessage)
        assertFalse(state.isLoading)

        catalog.observeError = null
        viewModel.dismissError()
        assertNull(state.errorMessage)
        assertEquals(listOf(first, second), state.talks)

        assertFalse(state.hasDetail)
        viewModel.newTalk()
        assertTrue(state.hasDetail)
        viewModel.cancelEditing()
        assertNull(state.editor)
        assertFalse(state.hasDetail)
    }

    @Test
    fun `saving and deleting update the list before the snapshot flow delivers`() {
        catalog.publishesChanges = false
        viewModel.onCurrentUserChanged(admin)
        viewModel.newTalk()
        viewModel.updateEditor(state.editor!!.copy(numberText = "3", title = "Dritter"))

        viewModel.save()
        assertEquals(listOf(first, second, Talk(3, "Dritter")), state.talks)

        viewModel.delete(first)
        assertEquals(listOf(second, Talk(3, "Dritter")), state.talks)
    }

    @Test
    fun `losing the permission clears the list and the editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editTalk(first)

        viewModel.onCurrentUserChanged(congregationAdmin)

        assertTrue(state.talks.isEmpty())
        assertNull(state.editor)
    }
}
