package de.geosphere.congregationplaner.user.presentation.profile

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.usecase.EditMessageTemplatesUseCase
import de.geosphere.congregationplaner.user.domain.usecase.MessageTemplateEdit
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMessageTemplatesUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveMessageTemplatesUseCase
import de.geosphere.congregationplaner.user.testing.FakeMessageTemplateRepository
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessageTemplatesViewModelTest {
    private val member = AppUser(uid = "u1", approved = true, congregation = testCongregation)
    private lateinit var repository: FakeMessageTemplateRepository
    private lateinit var viewModel: MessageTemplatesViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeMessageTemplateRepository()
        viewModel = MessageTemplatesViewModel(
            ObserveMessageTemplatesUseCase(repository),
            SaveMessageTemplatesUseCase(repository),
            EditMessageTemplatesUseCase(),
        )
        viewModel.onUserChanged(member)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `the defaults are shown until own templates are saved`() {
        assertEquals(DEFAULT_MESSAGE_TEMPLATES, state.form)
        assertTrue(state.canAdd && state.canRemove)
        assertFalse(state.canSave)
    }

    @Test
    fun `edits go through the use case and save stores the order`() {
        viewModel.edit(MessageTemplateEdit.MakeDefault(2))
        assertEquals(DEFAULT_MESSAGE_TEMPLATES[2], state.form.first())
        assertTrue(state.canSave)

        viewModel.save()

        assertEquals(DEFAULT_MESSAGE_TEMPLATES[2], repository.saved.single().second.first())
        assertEquals("Gespeichert.", state.message)
        assertFalse(state.isDirty)
    }

    @Test
    fun `the flags follow the use case limits`() {
        viewModel.edit(MessageTemplateEdit.Add)
        assertFalse(state.canAdd)

        repeat(5) { viewModel.edit(MessageTemplateEdit.Remove(0)) }
        assertEquals(1, state.form.size)
        assertFalse(state.canRemove)

        viewModel.edit(MessageTemplateEdit.Change(0, MessageTemplate("A", "")))
        assertFalse(state.canSave)
    }

    @Test
    fun `unsaved edits survive a new saved state and failures are shown`() {
        viewModel.edit(MessageTemplateEdit.Change(0, MessageTemplate("Neu", "Text")))
        repository.templates.value = listOf(MessageTemplate("Fremd", "Gerät"))
        assertEquals("Neu", state.form.first().name)

        repository.saveResult = Result.failure(IllegalStateException("offline"))
        viewModel.save()
        assertEquals("offline", state.message)
        assertTrue(state.isError)
    }
}
