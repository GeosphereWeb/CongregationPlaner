package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TalkScheduleRepository {
    /** Alle Termine des Kalenderjahres, aufsteigend nach Datum. */
    fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>>

    /** Alle Termine der Versammlung über alle Jahre, aufsteigend nach Datum (Grundlage der Vortragshistorie). */
    fun observeAll(congregation: CongregationRef): Flow<List<TalkSlot>>

    suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot>

    /** Speichert alle [slots] in einem Schreibvorgang: entweder alle oder keiner (z. B. beim Tauschen). */
    suspend fun saveSlots(congregation: CongregationRef, slots: List<TalkSlot>): Result<Unit>

    /**
     * Legt nur die Termine aus [slots] an, deren Datum noch keinen Termin hat; bestehende Termine bleiben
     * unverändert. Liefert die Zahl der neu angelegten Termine.
     */
    suspend fun createMissingSlots(congregation: CongregationRef, slots: List<TalkSlot>): Result<Int>

    suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit>

    /**
     * Setzt nur das Feld `confirmed` des Termins vom [date]; alle anderen Felder bleiben unverändert. Ein Termin, den
     * es nicht gibt, wird nicht angelegt, sondern meldet einen Fehler.
     */
    suspend fun setConfirmed(congregation: CongregationRef, date: LocalDate, confirmed: Boolean): Result<Unit>
}
