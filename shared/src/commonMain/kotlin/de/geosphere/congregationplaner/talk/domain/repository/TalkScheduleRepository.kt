package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TalkScheduleRepository {
    /** Alle Termine des Kalenderjahres, aufsteigend nach Datum. */
    fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>>

    suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot>

    suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit>
}
