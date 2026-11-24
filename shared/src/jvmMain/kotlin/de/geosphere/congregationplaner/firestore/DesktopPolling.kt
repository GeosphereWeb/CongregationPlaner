package de.geosphere.congregationplaner.firestore

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Alle wie viele Millisekunden die Desktop-App eine Abfrage wiederholt (CP-189: vorher 15 s). */
internal const val FIRESTORE_POLL_INTERVAL_MILLIS = 60_000L

/**
 * Wann die Desktop-App abfragt: nur bei aktivem Fenster ([active]); jede eigene Schreibänderung ([writes] zählt hoch)
 * löst sofort eine neue Abfrage aus, damit eigene Änderungen nicht bis zur nächsten Runde fehlen.
 */
class PollingSignals(val active: MutableStateFlow<Boolean>, val writes: MutableStateFlow<Long>)

/** Steuerung der Abfragen der Desktop-App; das Fenster meldet über [setWindowActive], ob es im Vordergrund ist. */
object DesktopPolling {
    val signals = PollingSignals(MutableStateFlow(true), MutableStateFlow(0L))

    /** Ohne Fensterfokus ruht die Abfrage; kommt das Fenster zurück, wird sofort neu abgefragt. */
    fun setWindowActive(active: Boolean) {
        signals.active.value = active
    }

    internal fun onWrite() = signals.writes.update { it + 1 }
}

/**
 * Die REST-API kennt keine Listener; Änderungen werden daher regelmäßig abgefragt: sofort, danach alle
 * [intervalMillis] oder früher nach einem eigenen Schreibzugriff, und nur solange das Fenster aktiv ist.
 */
internal fun <T> pollingFlow(
    intervalMillis: Long = FIRESTORE_POLL_INTERVAL_MILLIS,
    signals: PollingSignals = DesktopPolling.signals,
    io: CoroutineDispatcher = Dispatchers.IO,
    fetch: () -> T,
): Flow<T> = flow {
    while (true) {
        signals.active.first { it }
        val seenWrites = signals.writes.value
        emit(withContext(io) { fetch() })
        withTimeoutOrNull(intervalMillis) { signals.writes.first { it != seenWrites } }
    }
}
