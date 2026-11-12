package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate

/** In-Memory-[TalkScheduleRepository]; Fehler lassen sich über die `*Result`-Felder einstellen. */
internal class FakeTalkScheduleRepository(slots: List<TalkSlot> = emptyList()) : TalkScheduleRepository {
    val slots = MutableStateFlow(slots)

    var saveResult: Result<Unit> = Result.success(Unit)
    var createMissingResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var confirmResult: Result<Unit> = Result.success(Unit)

    /** Lässt [observeYear] mit diesem Fehler enden. */
    var observeError: Throwable? = null

    /** Hält [saveSlot] an, bis das Signal gesetzt ist; so lässt sich der Zustand während des Speicherns prüfen. */
    var saveGate: CompletableDeferred<Unit>? = null

    var requestedCongregation: CongregationRef? = null
        private set
    var requestedYear: Int? = null
        private set
    val savedSlots = mutableListOf<TalkSlot>()
    val createdSlots = mutableListOf<TalkSlot>()
    val deletedDates = mutableListOf<LocalDate>()
    val confirmedUpdates = mutableListOf<Pair<LocalDate, Boolean>>()

    override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> {
        requestedCongregation = congregation
        requestedYear = year
        return observeError?.let { error -> flow { throw error } } ?: slots
    }

    override fun observeAll(congregation: CongregationRef): Flow<List<TalkSlot>> {
        requestedCongregation = congregation
        return observeError?.let { error -> flow { throw error } } ?: slots
    }

    override suspend fun saveSlots(congregation: CongregationRef, slots: List<TalkSlot>): Result<Unit> {
        requestedCongregation = congregation
        return saveResult.map {
            savedSlots += slots
            this.slots.value = (this.slots.value.filterNot { slot -> slots.any { it.date == slot.date } } + slots)
                .sortedBy { it.date }
        }
    }

    override suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot> {
        requestedCongregation = congregation
        saveGate?.await()
        return saveResult.map {
            savedSlots += slot
            slot
        }
    }

    override suspend fun createMissingSlots(congregation: CongregationRef, slots: List<TalkSlot>): Result<Int> {
        requestedCongregation = congregation
        return createMissingResult.map {
            val existing = this.slots.value.map { slot -> slot.date }.toSet()
            val missing = slots.filter { slot -> slot.date !in existing }
            createdSlots += missing
            this.slots.value = (this.slots.value + missing).sortedBy { slot -> slot.date }
            missing.size
        }
    }

    override suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit> {
        requestedCongregation = congregation
        return deleteResult.onSuccess { deletedDates += date }
    }

    override suspend fun setConfirmed(
        congregation: CongregationRef,
        date: LocalDate,
        confirmed: Boolean,
    ): Result<Unit> {
        requestedCongregation = congregation
        return confirmResult.onSuccess {
            confirmedUpdates += date to confirmed
            slots.value = slots.value.map { slot -> if (slot.date == date) slot.copy(confirmed = confirmed) else slot }
        }
    }
}
