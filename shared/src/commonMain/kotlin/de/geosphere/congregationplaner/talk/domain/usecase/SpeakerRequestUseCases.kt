package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.hasAssignment
import de.geosphere.congregationplaner.talk.domain.model.newRequestId
import de.geosphere.congregationplaner.talk.domain.model.openRequestsOf
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

/** Die offenen Anfragen für die Übersicht; Planer und Helfer sehen sie, alle anderen eine leere Liste. */
@Inject
class ObserveOpenRequestsUseCase(
    private val requests: SpeakerRequestRepository,
    private val schedule: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    operator fun invoke(user: AppUser, today: LocalDate): Flow<List<OpenRequest>> {
        val congregation = user.congregation
        if (congregation == null || !policy.canViewTalkPlanning(user)) return flowOf(emptyList())
        return combine(requests.observeRequests(congregation), schedule.observeAll(congregation)) { open, slots ->
            openRequestsOf(open, slots, today)
        }
    }
}

/** Legt eine Anfrage ohne Tag an oder ändert sie; eine leere ID bekommt eine neue ID und [today] als Anlagetag. */
@Inject
class SaveSpeakerRequestUseCase(
    private val repository: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, request: SpeakerRequest, today: LocalDate): Result<SpeakerRequest> =
        planner(user, policy).mapCatching { congregation ->
            if (!request.hasSpeaker) throw TalkScheduleException.InvalidSpeaker()
            val fields = request.asSlotFields.checked().getOrThrow()
            val note = request.note?.trim()?.ifEmpty { null }
            if ((note?.length ?: 0) > SpeakerRequest.MAX_NOTE_LENGTH) throw TalkScheduleException.InvalidNote()
            val saved = request.copy(
                id = request.id.ifBlank { newRequestId() },
                createdOn = if (request.id.isBlank()) today else request.createdOn,
                note = note,
                speakerLastName = fields.speakerLastName,
                speakerFirstName = fields.speakerFirstName,
                speakerCongregation = fields.speakerCongregation,
                talkTitle = fields.talkTitle,
            )
            repository.commit(congregation, ScheduleWrite(saveRequests = listOf(saved))).getOrThrow()
            saved
        }
}

/**
 * Hängt einen Kontaktversuch an die Anfrage an (Notiz getrimmt, höchstens [SpeakerRequest.MAX_ATTEMPTS]) und liefert
 * die gespeicherte Anfrage.
 */
@Inject
class AddContactAttemptUseCase(
    private val repository: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(
        user: AppUser,
        request: SpeakerRequest,
        attempt: ContactAttempt,
    ): Result<SpeakerRequest> = planner(user, policy).mapCatching { congregation ->
        val cleaned = attempt.copy(note = attempt.note?.trim()?.ifEmpty { null })
        if ((cleaned.note?.length ?: 0) > SpeakerRequest.MAX_ATTEMPT_NOTE_LENGTH ||
            request.contactAttempts.size >= SpeakerRequest.MAX_ATTEMPTS
        ) {
            throw TalkScheduleException.InvalidContactAttempt()
        }
        val updated = request.copy(contactAttempts = request.contactAttempts + cleaned)
        repository.commit(congregation, ScheduleWrite(saveRequests = listOf(updated))).getOrThrow()
        updated
    }
}

/** Der Redner hat zugesagt: der Termin verliert den Verweis, die Anfrage wird gelöscht. Nur für Anfragen mit Tag. */
@Inject
class AcceptSpeakerRequestUseCase(
    private val repository: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, open: OpenRequest): Result<Unit> =
        planner(user, policy).mapCatching { congregation ->
            val slot = open.slot ?: throw TalkScheduleException.InvalidRequest()
            val write = ScheduleWrite(
                saveSlots = listOf(slot.copy(requestId = null)),
                deleteRequestIds = listOf(open.request.id),
            )
            repository.commit(congregation, write).getOrThrow()
        }
}

/**
 * Der Redner hat abgesagt: mit Tag werden Vortrag, Redner und Verweis am Termin geleert (Uhrzeit, Ereignis, Vorsitz und
 * Notiz bleiben), und die Anfrage wird gelöscht; ohne Tag wird nur die Anfrage gelöscht.
 */
@Inject
class DeclineSpeakerRequestUseCase(
    private val repository: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, open: OpenRequest): Result<Unit> =
        planner(user, policy).mapCatching { congregation ->
            val write = ScheduleWrite(
                saveSlots = listOfNotNull(open.slot?.withoutAssignment()),
                deleteRequestIds = listOf(open.request.id),
            )
            repository.commit(congregation, write).getOrThrow()
        }
}

/**
 * Legt eine Anfrage ohne Tag auf den Termin [target]: Redner und Vortrag wandern in den Termin, der auf die Anfrage
 * verweist; Notiz und Kontaktversuche bleiben in der Anfrage. Das Ergebnis ist der angefragte Termin.
 */
@Inject
class AssignRequestToDateUseCase(
    private val repository: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, request: SpeakerRequest, target: TalkSlot): Result<TalkSlot> =
        planner(user, policy).mapCatching { congregation ->
            when {
                target.hasAssignment -> throw TalkScheduleException.TargetTaken()
                target.talkCancelled -> throw TalkScheduleException.TargetCancelled()
            }
            val slot = target.copy(
                talkNumber = request.talkNumber,
                talkTitle = request.talkTitle,
                speakerMemberId = request.speakerMemberId,
                speakerLastName = request.speakerLastName,
                speakerFirstName = request.speakerFirstName,
                externalSpeakerId = request.externalSpeakerId,
                speakerCongregation = request.speakerCongregation,
                confirmed = false,
                requestId = request.id,
            )
            val dated = SpeakerRequest(request.id, request.createdOn, request.note, request.contactAttempts)
            repository.commit(congregation, ScheduleWrite(saveSlots = listOf(slot), saveRequests = listOf(dated)))
                .getOrThrow()
            slot
        }
}

private fun planner(user: AppUser, policy: TalkSchedulePermissionPolicy): Result<CongregationRef> {
    val congregation = user.congregation
    return if (congregation == null || !policy.canPlanTalks(user)) {
        Result.failure(TalkScheduleException.NotAllowed())
    } else {
        Result.success(congregation)
    }
}

private fun TalkSlot.withoutAssignment(): TalkSlot = copy(
    talkNumber = null,
    talkTitle = null,
    speakerMemberId = null,
    speakerLastName = null,
    speakerFirstName = null,
    externalSpeakerId = null,
    speakerCongregation = null,
    confirmed = false,
    requestId = null,
)
