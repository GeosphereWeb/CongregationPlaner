package de.geosphere.congregationplaner.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Der Zustand eines Bildschirms, den ViewModel und Zustandshalter gemeinsam ändern (ADR-0025). [derive] leitet nach
 * jeder Änderung abhängige Werte neu ab, z. B. die Rechte aus Nutzer und Liste.
 */
class UiStateStore<S>(initial: S, private val derive: (S) -> S = { it }) {
    private val mutableState = MutableStateFlow(derive(initial))
    val state: StateFlow<S> = mutableState.asStateFlow()
    val value: S get() = mutableState.value

    fun update(transform: (S) -> S) = mutableState.update { derive(transform(it)) }
}
