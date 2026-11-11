package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.usecase.ChangeYearSlotTimesUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.CreateYearSlotsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

/** Die UseCases, die alle Termine eines Jahres betreffen. */
@Inject
class YearSetupUseCases(val createYearSlots: CreateYearSlotsUseCase, val changeYearTimes: ChangeYearSlotTimesUseCase)

/** Legt die Termine eines Jahres an und ändert ihre Uhrzeit, jeweils nach einer Rückfrage. */
class YearSetupHolder(private val session: TalkScheduleSession, private val useCases: YearSetupUseCases) {
    private val store get() = session.store

    /** Öffnet die Rückfrage; angelegt wird erst nach [create]. */
    fun askCreate() {
        if (store.value.isSaving || session.allowedUser() == null) return
        store.update { it.copy(confirmCreateYear = true, errorMessage = null, infoMessage = null) }
    }

    fun dismissCreate() = store.update { it.copy(confirmCreateYear = false) }

    /** Legt alle fehlenden Termine am Wochenende des gewählten Jahres an; vorhandene bleiben unverändert. */
    fun create() {
        if (store.value.isSaving) return
        val user = session.allowedUser() ?: return
        val year = store.value.year
        store.update { it.copy(confirmCreateYear = false, isSaving = true, errorMessage = null) }
        session.scope.launch {
            useCases.createYearSlots(user, year)
                .onSuccess { result ->
                    store.update { it.copy(isSaving = false, infoMessage = yearSlotsMessage(year, result)) }
                }.onFailure { session.fail(it, "Termine konnten nicht angelegt werden.") }
        }
    }

    /** Öffnet den Dialog mit der häufigsten Uhrzeit des Jahres; geändert wird erst nach [changeTime]. */
    fun askChangeTime() {
        if (store.value.isSaving || session.allowedUser() == null) return
        val initial = commonSlotTime(store.value.slots) ?: DEFAULT_YEAR_TIME
        store.update { it.copy(yearTimeDialog = initial, errorMessage = null, infoMessage = null) }
    }

    /** Ändert die Uhrzeit im offenen Dialog; ohne Dialog bleibt alles, wie es ist. */
    fun selectTime(time: LocalTime) = store.update { state ->
        state.yearTimeDialog?.let { state.copy(yearTimeDialog = time) } ?: state
    }

    fun dismissTime() = store.update { it.copy(yearTimeDialog = null) }

    /** Setzt die gewählte Uhrzeit bei allen Terminen des gewählten Jahres. */
    fun changeTime() {
        val time = store.value.yearTimeDialog ?: return
        if (store.value.isSaving) return
        val user = session.allowedUser() ?: return
        val year = store.value.year
        store.update { it.copy(yearTimeDialog = null, isSaving = true, errorMessage = null) }
        session.scope.launch {
            useCases.changeYearTimes(user, year, time)
                .onSuccess { changed ->
                    store.update { it.copy(isSaving = false, infoMessage = yearTimeMessage(year, changed, time)) }
                }.onFailure { session.fail(it, "Uhrzeit konnte nicht geändert werden.") }
        }
    }

    private companion object {
        val DEFAULT_YEAR_TIME = LocalTime(10, 0)
    }
}
