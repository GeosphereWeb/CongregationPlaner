package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalDirectoryViewModelTest {
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        permissions = setOf(UserPermission.PLAN_TALKS),
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)

    private val north = ExternalCircuit("c1", "Nord")
    private val altdorf = ExternalCongregation("g1", "c1", "Altdorf")
    private val berg = VisitingSpeaker("s1", "g1", "Berg", "Lea", talkNumbers = listOf(12))

    private lateinit var repository: FakeExternalSpeakerDirectoryRepository
    private lateinit var viewModel: ExternalDirectoryViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeExternalSpeakerDirectoryRepository(listOf(north), listOf(altdorf), listOf(berg))
        val policy = TalkSchedulePermissionPolicy()
        viewModel = ExternalDirectoryViewModel(
            ObserveExternalDirectoryUseCase(repository),
            SaveExternalCircuitUseCase(repository, policy),
            SaveExternalCongregationUseCase(repository, policy),
            SaveVisitingSpeakerUseCase(repository, policy),
            DeleteExternalCircuitUseCase(repository, policy),
            DeleteExternalCongregationUseCase(repository, policy),
            DeleteVisitingSpeakerUseCase(repository, policy),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `planner sees the directory`() {
        viewModel.onCurrentUserChanged(planner)

        assertTrue(state.canPlan)
        assertFalse(state.isLoading)
        assertEquals(listOf(north), state.directory.circuits)
        assertEquals(listOf(altdorf), state.directory.congregations)
        assertEquals(listOf(berg), state.directory.speakers)
    }

    @Test
    fun `member without permission sees nothing and cannot open an editor`() {
        viewModel.onCurrentUserChanged(member)
        viewModel.newCircuit()

        assertFalse(state.canPlan)
        assertFalse(state.isLoading)
        assertTrue(state.directory.circuits.isEmpty())
        assertNull(state.editor)
    }

    @Test
    fun `a new circuit is saved and stays open`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newCircuit()
        assertTrue(state.editor!!.isDirty)

        viewModel.updateEditor(ExternalDirectoryEditor.Circuit(ExternalCircuit(name = " Süd ")))
        viewModel.save()

        val editor = assertIs<ExternalDirectoryEditor.Circuit>(state.editor)
        assertEquals(ExternalCircuit("c-1", "Süd"), editor.circuit)
        assertEquals("c-1", editor.id)
        assertFalse(editor.isDirty)
        assertTrue(ExternalCircuit("c-1", "Süd") in state.directory.circuits)
        assertFalse(state.isSaving)
    }

    @Test
    fun `a new congregation and speaker are created below their parent`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.newCongregation("c1")
        val congregation = assertIs<ExternalDirectoryEditor.Congregation>(state.editor)
        viewModel.updateEditor(congregation.copy(congregation = congregation.congregation.copy(name = "Neustadt")))
        viewModel.save()
        assertEquals(ExternalCongregation("g-1", "c1", "Neustadt"), repository.savedCongregations.single())

        viewModel.newSpeaker("g-1")
        val speaker = assertIs<ExternalDirectoryEditor.Speaker>(state.editor)
        viewModel.updateEditor(speaker.copy(speaker = speaker.speaker.copy(lastName = "Neu"), talksText = "45, 12 45"))
        viewModel.save()

        val saved = repository.savedSpeakers.single()
        assertEquals("g-1", saved.congregationId)
        assertEquals(listOf(12, 45), saved.talkNumbers)
        assertEquals("12, 45", assertIs<ExternalDirectoryEditor.Speaker>(state.editor).talksText)
        assertTrue(saved in state.directory.speakers)
    }

    @Test
    fun `invalid talk numbers are not saved`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.select(berg)
        val editor = assertIs<ExternalDirectoryEditor.Speaker>(state.editor)

        viewModel.updateEditor(editor.copy(talksText = "12, abc"))
        viewModel.save()

        assertNotNull(state.errorMessage)
        assertTrue(repository.savedSpeakers.isEmpty())
    }

    @Test
    fun `a rejected save shows the error and keeps the editor`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newCircuit()
        viewModel.updateEditor(ExternalDirectoryEditor.Circuit(ExternalCircuit(name = "Süd")))
        repository.saveResult = Result.failure(IllegalStateException("offline"))

        viewModel.save()

        assertEquals("offline", state.errorMessage)
        assertNotNull(state.editor)
        assertFalse(state.isSaving)
    }

    @Test
    fun `an invalid name shows the validation error`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newCircuit()

        viewModel.save()

        assertNotNull(state.errorMessage)
        assertTrue(repository.savedCircuits.isEmpty())
    }

    @Test
    fun `discard resets a saved entry and closes a new one`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.select(north)
        viewModel.updateEditor(ExternalDirectoryEditor.Circuit(north.copy(name = "Geändert"), north))
        assertTrue(state.editor!!.isDirty)

        viewModel.discardChanges()
        assertEquals(ExternalDirectoryEditor.Circuit(north, north), state.editor)

        viewModel.newCircuit()
        viewModel.discardChanges()
        assertNull(state.editor)
    }

    @Test
    fun `deleting removes the entry at once and closes its editor`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.select(berg)

        viewModel.delete(berg)

        assertEquals(listOf("s1"), repository.deletedIds)
        assertTrue(state.directory.speakers.isEmpty())
        assertNull(state.editor)
    }

    @Test
    fun `a circuit with congregations cannot be deleted`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.delete(north)

        assertNotNull(state.errorMessage)
        assertEquals(listOf(north), state.directory.circuits)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `a failed delete shows the error`() {
        viewModel.onCurrentUserChanged(planner)
        repository.deleteResult = Result.failure(IllegalStateException("denied"))

        viewModel.delete(berg)

        assertEquals("denied", state.errorMessage)
        assertEquals(listOf(berg), state.directory.speakers)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `talk numbers are parsed from comma semicolon or space separated text`() {
        assertEquals(listOf(12, 45, 101), parseTalkNumbers("101, 12;45 12"))
        assertEquals(emptyList(), parseTalkNumbers("  "))
        assertNull(parseTalkNumbers("12, x"))
        assertEquals("12, 45", listOf(12, 45).toText())
    }
}
