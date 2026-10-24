package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/** In-Memory-[TalkScheduleRepository]; Fehler lassen sich über die `*Result`-Felder einstellen. */
internal class FakeTalkScheduleRepository(slots: List<TalkSlot> = emptyList()) : TalkScheduleRepository {
    val slots = MutableStateFlow(slots)

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    var requestedCongregation: CongregationRef? = null
        private set
    var requestedYear: Int? = null
        private set
    val savedSlots = mutableListOf<TalkSlot>()
    val deletedDates = mutableListOf<LocalDate>()

    override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> {
        requestedCongregation = congregation
        requestedYear = year
        return slots
    }

    override suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot> {
        requestedCongregation = congregation
        return saveResult.map {
            savedSlots += slot
            slot
        }
    }

    override suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit> {
        requestedCongregation = congregation
        return deleteResult.onSuccess { deletedDates += date }
    }
}
