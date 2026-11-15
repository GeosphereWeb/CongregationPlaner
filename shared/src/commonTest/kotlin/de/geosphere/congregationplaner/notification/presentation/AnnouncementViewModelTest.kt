package de.geosphere.congregationplaner.notification.presentation

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.permission.NotificationPermissionPolicy
import de.geosphere.congregationplaner.notification.domain.usecase.SendNotificationUseCase
import de.geosphere.congregationplaner.notification.testing.FakeNotificationRepository
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementViewModelTest {
    private val repository = FakeNotificationRepository()
    private val viewModel = AnnouncementViewModel(
        SendNotificationUseCase(repository, NotificationPermissionPolicy(TalkSchedulePermissionPolicy())),
        NotificationPermissionPolicy(TalkSchedulePermissionPolicy()),
    )
    private val sender = AppUser(
        uid = "sender",
        approved = true,
        roles = setOf(CongregationRole.NOTIFICATION_SENDER),
        congregation = testCongregation,
    )
    private val state get() = viewModel.uiState.value
    private val draft =
        AnnouncementDraft(title = "Kongress", body = "Bitte anmelden", groups = setOf(EventAudience.ALL))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `only members with the sender role may send`() {
        viewModel.onCurrentUserChanged(sender.copy(roles = setOf(CongregationRole.CONGREGATION_ADMIN)))
        viewModel.updateDraft(draft)
        assertFalse(state.canSend)
        assertFalse(state.canSubmit)
        viewModel.send()
        assertTrue(repository.sent.isEmpty())

        viewModel.onCurrentUserChanged(sender)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `sending sends an announcement clears the form and confirms`() {
        viewModel.onCurrentUserChanged(sender)
        viewModel.updateDraft(draft)

        viewModel.send()

        assertEquals(
            "sender" to AppNotification(
                NotificationCategory.ANNOUNCEMENT,
                title = "Kongress",
                body = "Bitte anmelden",
                groups = setOf(EventAudience.ALL),
            ),
            repository.sent.single(),
        )
        assertEquals(AnnouncementDraft(), state.draft)
        assertEquals("Mitteilung gesendet.", state.sentMessage)
        assertFalse(state.isSending)

        viewModel.updateDraft(AnnouncementDraft(title = "Neu"))
        assertNull(state.sentMessage)
    }

    @Test
    fun `a failure keeps the draft and shows the error until dismissed`() {
        repository.result = Result.failure(IllegalStateException("offline"))
        viewModel.onCurrentUserChanged(sender)
        viewModel.updateDraft(draft)

        viewModel.send()

        assertEquals(draft, state.draft)
        assertEquals("offline", state.errorMessage)
        viewModel.dismissError()
        assertNull(state.errorMessage)
    }

    @Test
    fun `a draft needs a title and recipients and is cut to the allowed length`() {
        viewModel.onCurrentUserChanged(sender)
        viewModel.updateDraft(draft.copy(title = " "))
        assertFalse(state.canSubmit)
        viewModel.updateDraft(draft.copy(groups = emptySet()))
        assertFalse(state.canSubmit)

        viewModel.updateDraft(draft.copy(title = "x".repeat(200), body = "y".repeat(600)))
        assertEquals(AppNotification.MAX_TITLE_LENGTH, state.draft.title.length)
        assertEquals(AppNotification.MAX_BODY_LENGTH, state.draft.body.length)
    }

    @Test
    fun `all excludes the other recipients and any other choice deselects all`() {
        val start =
            AnnouncementDraft(groups = setOf(EventAudience.SISTERS), roles = setOf(CongregationRole.TALK_HELPER))

        val all = start.toggled(EventAudience.ALL)
        assertEquals(setOf(EventAudience.ALL), all.groups)
        assertTrue(all.roles.isEmpty())

        assertEquals(setOf(EventAudience.ELDERS), all.toggled(EventAudience.ELDERS).groups)
        val withRole = all.toggled(CongregationRole.TASK_PLANNER)
        assertTrue(withRole.groups.isEmpty())
        assertEquals(setOf(CongregationRole.TASK_PLANNER), withRole.roles)

        assertTrue(all.toggled(EventAudience.ALL).groups.isEmpty())
        assertTrue(withRole.toggled(CongregationRole.TASK_PLANNER).roles.isEmpty())
    }
}
