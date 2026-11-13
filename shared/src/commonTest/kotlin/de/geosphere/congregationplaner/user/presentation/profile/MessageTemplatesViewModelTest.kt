package de.geosphere.congregationplaner.user.presentation.profile

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
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
        assertFalse(state.canSave)
    }

    @Test
    fun `make default moves the template to the top and save stores the order`() {
        viewModel.makeDefault(2)
        assertEquals(DEFAULT_MESSAGE_TEMPLATES[2], state.form.first())
        assertTrue(state.canSave)

        viewModel.save()

        assertEquals(DEFAULT_MESSAGE_TEMPLATES[2], repository.saved.single().second.first())
        assertEquals("Gespeichert.", state.message)
        assertFalse(state.isDirty)
    }

    @Test
    fun `add stops at four and remove keeps at least one`() {
        viewModel.addTemplate()
        viewModel.addTemplate()
        assertEquals(MessageTemplate.MAX_TEMPLATES, state.form.size)
        assertFalse(state.canAdd)

        repeat(5) { viewModel.removeTemplate(0) }
        assertEquals(1, state.form.size)
        assertFalse(state.canRemove)
    }

    @Test
    fun `edits are cut to the limits and an empty text cannot be saved`() {
        viewModel.onTemplateChange(0, MessageTemplate("x".repeat(60), ""))

        assertEquals(MessageTemplate.MAX_NAME_LENGTH, state.form.first().name.length)
        assertFalse(state.canSave)
    }

    @Test
    fun `unsaved edits survive a new saved state and failures are shown`() {
        viewModel.onTemplateChange(0, MessageTemplate("Neu", "Text"))
        repository.templates.value = listOf(MessageTemplate("Fremd", "Gerät"))
        assertEquals("Neu", state.form.first().name)

        repository.saveResult = Result.failure(IllegalStateException("offline"))
        viewModel.save()
        assertEquals("offline", state.message)
        assertTrue(state.isError)
    }
}
