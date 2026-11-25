package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.newRequestId
import de.geosphere.congregationplaner.talk.domain.model.sameSpeakerAs
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/** Ein Termin aus dem Editor: [edited] ersetzt [original] vom Tag [from]; [requested] ist der Schalter „Angefragt“. */
data class SlotEdit(val from: LocalDate?, val original: TalkSlot?, val edited: TalkSlot, val requested: Boolean)

/**
 * Speichert einen im Editor bearbeiteten Termin samt Anfrage (ADR-0039) in einem Schreibvorgang:
 * [SlotEdit.requested] ist der Schalter „Angefragt“. Ein geänderter Tag verschiebt den Termin (der alte Tag wird
 * gelöscht); der Aufrufer muss wie bisher sicherstellen, dass am Zieltag kein anderer Termin liegt.
 *
 * shortcut: bearbeiten zwei Planer denselben Termin gleichzeitig, gewinnt der letzte Schreiber und eine Anfrage kann
 * verwaisen (ausgeblendet, später von [ObserveOpenRequestsUseCase] gelöscht). Bei Bedarf eine Transaktion, die die
 * gespeicherte `requestId` vergleicht.
 */
@Inject
class SaveEditedSlotUseCase(
    private val repository: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, edit: SlotEdit, today: LocalDate): Result<TalkSlot> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return edit.edited.checked()
            .mapCatching { planSlotEdit(edit.copy(edited = it), newRequestId(), today).getOrThrow() }
            .mapCatching { (write, saved) ->
                repository.commit(congregation, write).getOrThrow()
                saved
            }
    }
}

/**
 * Was beim Speichern von [SlotEdit.edited] geschrieben wird, und der gespeicherte Termin:
 * - angefragt ohne bisherige Anfrage oder mit anderem Redner → neue Anfrage [newId] (die alte wird gelöscht),
 * - angefragt mit demselben Redner → die Anfrage bleibt,
 * - nicht (mehr) angefragt → die bisherige Anfrage wird gelöscht (Zusage oder Redner entfernt),
 * - anderer Tag als [SlotEdit.from] → der alte Tag wird gelöscht.
 * Eine Anfrage braucht einen Redner und einen Vortrag, der nicht entfällt; „Informiert“ gilt erst nach der Zusage.
 */
fun planSlotEdit(edit: SlotEdit, newId: String, today: LocalDate): Result<Pair<ScheduleWrite, TalkSlot>> {
    val original = edit.original
    val edited = edit.edited
    val requested = edit.requested
    if (requested && (!edited.hasSpeaker || edited.talkCancelled)) {
        return Result.failure(TalkScheduleException.InvalidRequest())
    }
    val previousId = original?.requestId
    val keepId = previousId?.takeIf { requested && original.sameSpeakerAs(edited) }
    val requestId = keepId ?: newId.takeIf { requested }
    val saved = edited.copy(requestId = requestId, confirmed = edited.confirmed && requestId == null)
    val write = ScheduleWrite(
        saveSlots = listOf(saved),
        deleteSlotDates = listOfNotNull(edit.from?.takeIf { it != saved.date }),
        saveRequests = listOfNotNull(SpeakerRequest(newId, today).takeIf { requested && keepId == null }),
        deleteRequestIds = listOfNotNull(previousId?.takeIf { it != keepId }),
    )
    return Result.success(write to saved)
}
