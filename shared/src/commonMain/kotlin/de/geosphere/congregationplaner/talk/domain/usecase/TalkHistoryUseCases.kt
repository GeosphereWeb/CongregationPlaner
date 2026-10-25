package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Die Vortragshistorie der Versammlung des Nutzers, nach Vortragsnummer sortiert. */
@Inject
class ObserveTalkHistoryUseCase(private val repository: TalkHistoryRepository) {
    operator fun invoke(user: AppUser): Flow<List<TalkHistoryEntry>> = user.congregation?.let { congregation ->
        repository.observeAll(congregation).map { entries -> entries.sortedBy { it.talkNumber } }
    } ?: emptyFlow()
}

/** Speichert, an welchen Tagen ein Vortrag gehalten wurde; die Tage werden sortiert und von Duplikaten befreit. */
@Inject
class SaveTalkHistoryEntryUseCase(
    private val repository: TalkHistoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, entry: TalkHistoryEntry): Result<TalkHistoryEntry> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val planned = entry.plannedOn.distinct().sorted()
        val cleaned = entry.copy(heldOn = (entry.heldOn + planned).distinct().sorted(), plannedOn = planned)
        val valid = cleaned.talkNumber in 1..Talk.MAX_NUMBER &&
            cleaned.heldOn.size in 1..TalkHistoryEntry.MAX_DATES
        if (!valid) return Result.failure(TalkScheduleException.InvalidTalkHistory())
        return repository.save(congregation, cleaned)
    }
}

/** Entfernt die Historie eines Vortrags; er gilt danach wieder als noch nie gehalten. */
@Inject
class DeleteTalkHistoryEntryUseCase(
    private val repository: TalkHistoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, talkNumber: Int): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        if (talkNumber !in 1..Talk.MAX_NUMBER) return Result.failure(TalkScheduleException.InvalidTalkHistory())
        return repository.delete(congregation, talkNumber)
    }
}

/**
 * Hält die Vortragshistorie zu einem gespeicherten oder gelöschten Termin aktuell: Der Tag des Termins wird beim
 * Vortrag des Termins als geplanter Tag eingetragen und beim Vortrag des bisherigen Termins ([previous]) entfernt, wenn sich Vortrag
 * oder Tag geändert haben, der Termin gelöscht oder der Vortrag abgesagt wurde. Ein Vortrag ohne Tage verliert
 * seinen Eintrag. [current] ist `null`, wenn der Termin gelöscht wurde, [previous] `null` bei einem neuen Termin.
 */
@Inject
class SyncTalkHistoryWithSlotUseCase(
    private val repository: TalkHistoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, previous: TalkSlot?, current: TalkSlot?): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val old = previous?.heldTalk()
        val new = current?.heldTalk()
        if (old == new) return Result.success(Unit)

        val stored = repository.observeAll(congregation).first().associateBy { it.talkNumber }
        val entries = stored.toMutableMap()
        old?.let { (number, day) -> entries[number] = entries.entry(number).without(day) }
        new?.let { (number, day) -> entries[number] = entries.entry(number).with(day) }
        for (number in listOfNotNull(old?.first, new?.first).distinct()) {
            val updated = entries.entry(number)
            if (updated == stored.entry(number)) continue
            val result = if (updated.heldOn.isEmpty()) {
                repository.delete(congregation, number)
            } else {
                repository.save(congregation, updated).map { }
            }
            result.onFailure { return Result.failure(it) }
        }
        return Result.success(Unit)
    }

    private fun Map<Int, TalkHistoryEntry>.entry(number: Int): TalkHistoryEntry =
        this[number] ?: TalkHistoryEntry(number, emptyList())

    private fun TalkHistoryEntry.without(day: LocalDate) = copy(heldOn = heldOn - day, plannedOn = plannedOn - day)

    private fun TalkHistoryEntry.with(day: LocalDate) = copy(
        heldOn = (heldOn + day).distinct().sorted().takeLast(TalkHistoryEntry.MAX_DATES),
        plannedOn = (plannedOn + day).distinct().sorted().takeLast(TalkHistoryEntry.MAX_DATES),
    )

    // Nur ein Termin mit Vortrag, der nicht abgesagt ist, zählt als gehalten.
    private fun TalkSlot.heldTalk(): Pair<Int, LocalDate>? =
        talkNumber?.takeUnless { talkCancelled }?.let { it to date }
}
