package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.model.datesOfWeekdayIn
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
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
 * der Mitglieder erfordert Adminrechte oder eine Planerrolle (ADR-0012), sonst lehnen die Rules ab.
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

/** Wie viele Termine [CreateYearSlotsUseCase] angelegt hat und wie viele es schon gab. */
data class YearSlotsResult(val created: Int, val alreadyExisting: Int)

/**
 * Legt für ein Jahr alle Termine am Wochenende auf einmal an: den Wochentag und die Uhrzeit der Zusammenkunft am
 * Wochenende, die für dieses Jahr in den Versammlungsdaten stehen (Jahreseintrag oder Standard). Ein Tag, für den
 * es schon einen Termin gibt, bleibt unverändert; nichts wird überschrieben.
 */
@Inject
class CreateYearSlotsUseCase(
    private val repository: TalkScheduleRepository,
    private val congregations: CongregationMetadataRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, year: Int): Result<YearSlotsResult> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val all = try {
            congregations.observeAll().first()
        } catch (exception: CancellationException) {
            throw exception
        } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
            return Result.failure(exception)
        }
        val times = all.firstOrNull { it.id == congregation.congregationId }?.meetingTimesFor(year)
            ?: return Result.failure(TalkScheduleException.CongregationNotFound())
        val slots = datesOfWeekdayIn(year, times.weekendWeekday).map { TalkSlot(date = it, time = times.weekendTime) }
        return repository.createMissingSlots(congregation, slots)
            .map { created -> YearSlotsResult(created = created, alreadyExisting = slots.size - created) }
    }
}

private fun TalkSlot.trimmed(): TalkSlot = copy(
    weekEventName = weekEventName?.trim(),
    talkTitle = talkTitle?.trim(),
    speakerLastName = speakerLastName?.trim(),
    speakerFirstName = speakerFirstName?.trim(),
    chairmanLastName = chairmanLastName?.trim(),
    chairmanFirstName = chairmanFirstName?.trim(),
    speakerCongregation = speakerCongregation?.trim(),
    note = note?.trim()?.ifEmpty { null },
)

private fun TalkSlot.validationError(): TalkScheduleException? = when {
    !hasValidWeekEvent() -> TalkScheduleException.InvalidWeekEvent()
    !hasValidTalk() -> TalkScheduleException.InvalidTalk()
    !hasValidSpeaker() -> TalkScheduleException.InvalidSpeaker()
    (note?.length ?: 0) > TalkSlot.MAX_NOTE_LENGTH -> TalkScheduleException.InvalidNote()
    !hasValidMember(chairmanMemberId, chairmanLastName, chairmanFirstName) -> TalkScheduleException.InvalidChairman()
    speakerMemberId != null && speakerMemberId == chairmanMemberId -> TalkScheduleException.SpeakerIsChairman()
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

// Ein Mitglied oder ein externer Redner mit Versammlung, nie beides.
private fun TalkSlot.hasValidSpeaker(): Boolean = when {
    speakerMemberId != null && externalSpeakerId != null -> false
    externalSpeakerId != null ->
        hasValidMember(externalSpeakerId, speakerLastName, speakerFirstName) &&
            !speakerCongregation.isNullOrEmpty() &&
            speakerCongregation.length <= TalkSlot.MAX_PERSON_NAME_LENGTH
    else -> speakerCongregation == null && hasValidMember(speakerMemberId, speakerLastName, speakerFirstName)
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
