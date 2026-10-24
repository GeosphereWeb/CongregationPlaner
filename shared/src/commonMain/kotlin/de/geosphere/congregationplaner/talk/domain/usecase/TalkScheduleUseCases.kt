package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Beobachtet die Termine des Jahres in der Versammlung des Nutzers; ohne Versammlung gibt es keine. */
@Inject
class ObserveTalkYearUseCase(private val repository: TalkScheduleRepository) {
    operator fun invoke(user: AppUser, year: Int): Flow<List<TalkSlot>> =
        user.congregation?.let { repository.observeYear(it, year) } ?: emptyFlow()
}

@Inject
class ObserveWeekEventsUseCase(private val repository: WeekEventRepository) {
    operator fun invoke(): Flow<List<WeekEvent>> =
        repository.observeWeekEvents().map { events -> events.sortedBy { it.name.lowercase() } }
}

/** Die Vortragsliste nach Nummer; auch nicht aktive Vorträge sind enthalten, die Auswahl filtert sie. */
@Inject
class ObserveTalkCatalogUseCase(private val repository: TalkCatalogRepository) {
    operator fun invoke(): Flow<List<Talk>> = repository.observeTalks().map { talks -> talks.sortedBy { it.number } }
}

/** Die Mitglieder, die im Termin als Redner und als Vorsitzender zur Auswahl stehen. */
data class TalkMembers(val speakers: List<Speaker>, val chairmen: List<Chairman>)

/**
 * Redner (Mitglieder mit `isSpeaker`) und Vorsitzende (Brüder mit Dienstamt) der Versammlung, jeweils mit
 * Familiennamen und nach Namen sortiert. Beide kommen aus einer gemeinsamen Beobachtung der Mitglieder. Das Lesen
 * der Mitglieder erfordert die Berechtigung `VIEW_MEMBERS` (oder Adminrechte), sonst lehnen die Rules ab.
 */
@Inject
class ObserveTalkMembersUseCase(private val observeMembers: ObserveMembersUseCase) {
    operator fun invoke(user: AppUser): Flow<TalkMembers> = user.congregation?.let { congregation ->
        observeMembers(congregation).map { members ->
            val named = members.filter { it.profile.lastName.isNotBlank() }
            TalkMembers(
                speakers = named.filter { it.profile.isSpeaker }.toRefs(),
                chairmen = named.filter { it.profile.canBeChairman }.toRefs(),
            )
        }
    } ?: emptyFlow()

    private fun List<AppUser>.toRefs(): List<MemberRef> =
        map { MemberRef(it.uid, it.profile.lastName, it.profile.firstName) }
            .sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))
}

/**
 * Speichert einen Termin in der Versammlung des Nutzers. Der Schlüssel ist das Datum, ein Termin pro Tag.
 * Der Wochentag wird bewusst nicht geprüft, weil sich der Versammlungstag kurzfristig verschieben kann.
 */
@Inject
class SaveTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, slot: TalkSlot): Result<TalkSlot> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val cleaned = slot.trimmed()
        cleaned.validationError()?.let { return Result.failure(it) }
        return repository.saveSlot(congregation, cleaned)
    }
}

private fun TalkSlot.trimmed(): TalkSlot = copy(
    weekEventName = weekEventName?.trim(),
    talkTitle = talkTitle?.trim(),
    speakerLastName = speakerLastName?.trim(),
    speakerFirstName = speakerFirstName?.trim(),
    chairmanLastName = chairmanLastName?.trim(),
    chairmanFirstName = chairmanFirstName?.trim(),
)

private fun TalkSlot.validationError(): TalkScheduleException? = when {
    !hasValidWeekEvent() -> TalkScheduleException.InvalidWeekEvent()
    !hasValidTalk() -> TalkScheduleException.InvalidTalk()
    !hasValidMember(speakerMemberId, speakerLastName, speakerFirstName) -> TalkScheduleException.InvalidSpeaker()
    !hasValidMember(chairmanMemberId, chairmanLastName, chairmanFirstName) -> TalkScheduleException.InvalidChairman()
    else -> null
}

private fun TalkSlot.hasValidWeekEvent(): Boolean = if (weekEventId == null) {
    weekEventName == null
} else {
    weekEventId.isNotBlank() &&
        !weekEventName.isNullOrEmpty() &&
        weekEventName.length <= TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH
}

private fun TalkSlot.hasValidTalk(): Boolean = if (talkNumber == null) {
    talkTitle == null
} else {
    talkNumber >= 1 && !talkTitle.isNullOrEmpty() && talkTitle.length <= TalkSlot.MAX_TALK_TITLE_LENGTH
}

// Mitglied-ID, Familienname und Vorname gehören zusammen (alle gesetzt oder alle leer).
private fun hasValidMember(memberId: String?, lastName: String?, firstName: String?): Boolean = if (memberId == null) {
    lastName == null && firstName == null
} else {
    memberId.isNotBlank() &&
        !lastName.isNullOrEmpty() &&
        firstName != null &&
        maxOf(lastName.length, firstName.length) <= TalkSlot.MAX_PERSON_NAME_LENGTH
}

/**
 * Verschiebt einen Termin auf ein anderes Datum. Da das Datum der Schlüssel ist, wird der neue Termin zuerst
 * gespeichert und danach der alte gelöscht: Schlägt das Löschen fehl, bleibt ein Duplikat statt eines Verlusts.
 * Der Aufrufer muss sicherstellen, dass am Zieltag kein anderer Termin liegt, denn Speichern überschreibt.
 */
@Inject
class MoveTalkSlotUseCase(private val save: SaveTalkSlotUseCase, private val delete: DeleteTalkSlotUseCase) {
    suspend operator fun invoke(user: AppUser, from: LocalDate, slot: TalkSlot): Result<TalkSlot> =
        save(user, slot).mapCatching { saved ->
            if (from != saved.date) delete(user, from).getOrThrow()
            saved
        }
}

@Inject
class DeleteTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, date: LocalDate): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return repository.deleteSlot(congregation, date)
    }
}
