package de.geosphere.congregationplaner.talk.presentation.requests

import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.AcceptSpeakerRequestUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.AddContactAttemptUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.AssignRequestToDateUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeclineSpeakerRequestUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveAllTalkSlotsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalSpeakersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveOpenRequestsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkMembersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveSpeakerRequestUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeSpeakerRequestRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class SpeakerRequestsViewModelTest {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val helper = planner.copy(uid = "h", roles = setOf(CongregationRole.TALK_HELPER))

    private val requestedSlot = TalkSlot(
        today.plus(DatePeriod(days = 14)),
        LocalTime(10, 0),
        speakerLastName = "Gast",
        speakerFirstName = "",
        requestId = "r1",
    )
    private val freeSlot = TalkSlot(today.plus(DatePeriod(days = 21)), LocalTime(10, 0))
    private val dated = SpeakerRequest("r1", today)
    private val undated = SpeakerRequest("u1", today, speakerLastName = "Huber", speakerFirstName = "")

    private lateinit var schedule: FakeTalkScheduleRepository
    private lateinit var requests: FakeSpeakerRequestRepository
    private lateinit var viewModel: SpeakerRequestsViewModel

    private val state get() = viewModel.uiState.value

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        schedule = FakeTalkScheduleRepository(listOf(requestedSlot, freeSlot))
        requests = FakeSpeakerRequestRepository(listOf(dated, undated), schedule)
        val policy = TalkSchedulePermissionPolicy()
        val externals = FakeExternalSpeakerDirectoryRepository(
            listOf(ExternalCircuit("c1", "Kreis Nord")),
            listOf(ExternalCongregation("g1", "c1", "Nord")),
            listOf(VisitingSpeaker("e1", "g1", "Extern", "Eva")),
        )
        viewModel = SpeakerRequestsViewModel(
            ObserveOpenRequestsUseCase(requests, schedule, policy),
            ObserveAllTalkSlotsUseCase(schedule),
            SpeakerRequestChangeUseCases(
                SaveSpeakerRequestUseCase(requests, policy),
                AddContactAttemptUseCase(requests, policy),
                AcceptSpeakerRequestUseCase(requests, policy),
                DeclineSpeakerRequestUseCase(requests, policy),
                AssignRequestToDateUseCase(requests, policy),
            ),
            RequestReferenceUseCases(
                ObserveTalkMembersUseCase(ObserveMembersUseCase(FakeUserRepository())),
                ObserveExternalSpeakersUseCase(ObserveExternalDirectoryUseCase(externals)),
                ObserveTalkCatalogUseCase(FakeTalkCatalogRepository(listOf(Talk(12, "Zwölf")))),
            ),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun open(id: String) = state.items.first { it.request.id == id }

    @Test
    fun `a planner sees the open requests and may change them`() {
        viewModel.onCurrentUserChanged(planner)

        assertEquals(setOf("r1", "u1"), state.items.map { it.request.id }.toSet())
        assertTrue(state.canEdit)
        assertFalse(state.isLoading)
        assertEquals(listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord")), state.externalSpeakers)
    }

    @Test
    fun `a helper sees the requests but may not change them`() {
        viewModel.onCurrentUserChanged(helper)

        assertEquals(2, state.items.size)
        assertFalse(state.canEdit)
        viewModel.changes.openContact(open("u1"))
        assertNull(state.dialog)
    }

    @Test
    fun `a contact attempt is added through the dialog`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.openContact(open("u1"))
        viewModel.changes.saveContact(ContactKind.VOICEMAIL, "zurückrufen")

        assertNull(state.dialog)
        assertEquals(
            listOf(ContactAttempt(today, ContactKind.VOICEMAIL, "zurückrufen")),
            requests.requests.value.first { it.id == "u1" }.contactAttempts,
        )
    }

    @Test
    fun `accepting clears the request on the slot`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.accept(open("r1"))

        assertNull(schedule.slots.value.first { it.date == requestedSlot.date }.requestId)
        assertEquals(listOf("u1"), state.items.map { it.request.id })
    }

    @Test
    fun `declining asks first`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.askDecline(open("u1"))
        assertIs<RequestDialog.Decline>(state.dialog)
        assertTrue(requests.writes.isEmpty())

        viewModel.changes.confirmDecline()

        assertNull(state.dialog)
        assertEquals(listOf("r1"), state.items.map { it.request.id })
    }

    @Test
    fun `assigning offers free days and puts the request on the chosen one`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.openAssign(open("u1"))
        assertEquals(listOf(freeSlot), state.assignableSlots)
        viewModel.changes.assign(freeSlot)

        assertNull(state.dialog)
        assertEquals("u1", schedule.slots.value.first { it.date == freeSlot.date }.requestId)
    }

    @Test
    fun `a new request needs a speaker and is saved with the draft`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.draft.newRequest()
        viewModel.draft.saveNew()
        assertNotNull(state.errorMessage)

        viewModel.dismissError()
        viewModel.draft.selectExternalSpeaker(ExternalSpeaker("e1", "Extern", "Eva", "Nord"))
        viewModel.draft.selectTalk(Talk(12, "Zwölf"))
        viewModel.draft.updateNote("Frühjahr")
        viewModel.draft.saveNew()

        assertNull(state.dialog)
        val saved = requests.requests.value.first { it.id !in setOf("r1", "u1") }
        assertEquals("e1", saved.externalSpeakerId)
        assertEquals(12, saved.talkNumber)
        assertEquals("Frühjahr", saved.note)
    }

    @Test
    fun `two contacts in a row keep both while the list is not refreshed`() {
        requests.frozen = true
        schedule.frozen = true
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.openContact(open("u1"))
        viewModel.changes.saveContact(ContactKind.CALL, "")
        viewModel.changes.openContact(open("u1"))
        viewModel.changes.saveContact(ContactKind.VOICEMAIL, "")

        assertEquals(
            listOf(ContactKind.CALL, ContactKind.VOICEMAIL),
            requests.requests.value.first { it.id == "u1" }.contactAttempts.map { it.kind },
        )
    }

    @Test
    fun `an assigned day is taken at once while the list is not refreshed`() {
        requests.frozen = true
        schedule.frozen = true
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.openAssign(open("u1"))
        viewModel.changes.assign(freeSlot)

        assertEquals(freeSlot.date, open("u1").slot?.date)
        assertTrue(state.assignableSlots.isEmpty())
    }

    @Test
    fun `an accepted or declined request leaves the list at once`() {
        requests.frozen = true
        schedule.frozen = true
        viewModel.onCurrentUserChanged(planner)

        viewModel.changes.accept(open("r1"))
        viewModel.changes.askDecline(open("u1"))
        viewModel.changes.confirmDecline()

        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `a failed change shows the error and keeps the dialog`() {
        viewModel.onCurrentUserChanged(planner)
        requests.commitResult = Result.failure(IllegalStateException("offline"))

        viewModel.changes.openContact(open("u1"))
        viewModel.changes.saveContact(ContactKind.CALL, "")

        assertNotNull(state.errorMessage)
        assertNotNull(state.dialog)
        assertFalse(state.isSaving)
    }
}
