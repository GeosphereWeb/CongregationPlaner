package de.geosphere.congregationplaner.event.presentation

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.usecase.AdoptEventTypeBlueprintUseCase
import de.geosphere.congregationplaner.event.domain.usecase.DeleteEventTypeUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveEventTypesUseCase
import de.geosphere.congregationplaner.event.domain.usecase.SaveEventTypeUseCase
import de.geosphere.congregationplaner.event.testing.FakeEventTypeRepository
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
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
class EventTypeManagementViewModelTest {
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )
    private val member = admin.copy(uid = "m", roles = emptySet())
    private val congress = EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo")

    private lateinit var types: FakeEventTypeRepository
    private lateinit var blueprint: FakeWeekEventRepository
    private lateinit var viewModel: EventTypeManagementViewModel

    private val state get() = viewModel.uiState.value

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        types = FakeEventTypeRepository(listOf(congress))
        blueprint = FakeWeekEventRepository(listOf(WeekEvent("c1", "Kreiskongress"), WeekEvent("c2", "Dienstwoche")))
        val policy = EventTypePolicy()
        viewModel = EventTypeManagementViewModel(
            ObserveEventTypesUseCase(types, policy),
            SaveEventTypeUseCase(types, policy),
            DeleteEventTypeUseCase(types, policy),
            AdoptEventTypeBlueprintUseCase(types, blueprint, policy),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the admin creates a type with a chosen icon`() {
        viewModel.onCurrentUserChanged(admin)
        assertTrue(state.canManage)
        assertEquals(listOf(congress), state.types)

        viewModel.editType(null)
        val editor = state.editor!!
        assertTrue(editor.isNew)
        assertEquals(EventType.DEFAULT_ICON, editor.type.icon)
        viewModel.updateEditor(editor.copy(type = editor.type.copy(name = "Abendmahl", icon = "ic_cp_wine_bread_duo")))
        viewModel.save()

        val saved = EventType("type-1", "Abendmahl", icon = "ic_cp_wine_bread_duo")
        assertEquals(listOf(saved), types.savedTypes)
        assertEquals(listOf(saved, congress), state.types)
        assertFalse(state.editor!!.isDirty)
    }

    @Test
    fun `an invalid name keeps the editor open with a message`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editType(congress)
        assertFalse(state.editor!!.isDirty)

        viewModel.updateEditor(state.editor!!.copy(type = congress.copy(name = " ")))
        viewModel.save()

        assertNotNull(state.errorMessage)
        assertNotNull(state.editor)
        assertTrue(types.savedTypes.isEmpty())
    }

    @Test
    fun `deleting asks first and closes the open editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editType(congress)

        viewModel.requestDelete(congress)
        assertEquals(congress, state.pendingDelete)
        viewModel.requestDelete(null)
        assertNull(state.pendingDelete)
        assertTrue(types.deletedIds.isEmpty())

        viewModel.requestDelete(congress)
        viewModel.confirmDelete()

        assertEquals(listOf("kk"), types.deletedIds)
        assertEquals(emptyList(), state.types)
        assertNull(state.editor)
        assertNull(state.pendingDelete)
    }

    @Test
    fun `a failed delete is reported`() {
        types.deleteResult = Result.failure(IllegalStateException("offline"))
        viewModel.onCurrentUserChanged(admin)

        viewModel.requestDelete(congress)
        viewModel.confirmDelete()

        assertEquals("offline", state.errorMessage)
        assertFalse(state.isSaving)
    }

    @Test
    fun `adopting the blueprint reports how many types were added`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.adoptBlueprint()
        assertEquals(blueprintMessage(1), state.infoMessage)
        assertEquals(listOf("Dienstwoche"), types.savedTypes.map { it.name })

        viewModel.dismissMessages()
        viewModel.adoptBlueprint()
        assertEquals(blueprintMessage(0), state.infoMessage)
    }

    @Test
    fun `members neither manage nor change anything`() {
        viewModel.onCurrentUserChanged(member)
        assertFalse(state.canManage)

        viewModel.editType(null)
        viewModel.requestDelete(congress)
        viewModel.adoptBlueprint()

        assertNull(state.editor)
        assertNull(state.pendingDelete)
        assertEquals("Dafür fehlt dir die Berechtigung.", state.errorMessage)
        assertTrue(types.savedTypes.isEmpty())
    }

    @Test
    fun `a load error is shown and dismissing it loads again`() {
        types.observeError = IllegalStateException("kaputt")
        viewModel.onCurrentUserChanged(admin)
        assertEquals("kaputt", state.errorMessage)

        types.observeError = null
        viewModel.dismissMessages()

        assertNull(state.errorMessage)
        assertEquals(listOf(congress), state.types)
    }

    @Test
    fun `texts for delete hint and blueprint result`() {
        assertTrue(deleteEventTypeHint(congress).startsWith("„Kreiskongress“ wird gelöscht."))
        assertEquals("Alle Arten der Vorlage sind schon vorhanden.", blueprintMessage(0))
        assertEquals("1 Art aus der Vorlage ergänzt.", blueprintMessage(1))
        assertEquals("3 Arten aus der Vorlage ergänzt.", blueprintMessage(3))
    }

    @Test
    fun `icon search matches all words of the label and keeps the order`() {
        val icons = listOf("ic_cp_arena_duo", "ic_cp_arena_fill", "ic_cp_wine_bread_duo")

        assertEquals(icons, icons.matchingIcons(" "))
        assertEquals(listOf("ic_cp_arena_duo"), icons.matchingIcons("Arena duo"))
        assertEquals(listOf("ic_cp_wine_bread_duo"), icons.matchingIcons("bread"))
        assertEquals(emptyList(), icons.matchingIcons("zelt"))
        assertEquals("arena duo", iconLabel("ic_cp_arena_duo"))
        assertTrue("ic_cp_arena_duo" in eventIconNames)
        assertTrue(eventIconNames.all { it.startsWith("ic_cp_") })
    }
}
