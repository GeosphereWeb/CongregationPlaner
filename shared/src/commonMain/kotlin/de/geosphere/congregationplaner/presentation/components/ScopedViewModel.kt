package de.geosphere.congregationplaner.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.reflect.KClass

/**
 * Ein ViewModel, das so lange lebt wie der Aufrufer in der Komposition: Verlässt der Nutzer den Bereich, wird es
 * beendet und seine Abfragen (z. B. das Polling der Desktop-App) hören auf. Ein bloßes `remember { … }` beendet das
 * ViewModel nie, sodass jeder verlassene Bereich weiter Firestore liest (CP-189).
 */
@Composable
inline fun <reified VM : ViewModel> rememberScopedViewModel(noinline create: () -> VM): VM {
    val store = remember { ViewModelStore() }
    DisposableEffect(store) { onDispose { store.clear() } }
    return remember(store) { scopedViewModel(store, VM::class, create) }
}

/** Das ViewModel vom Typ [type] aus [store]; fehlt es, erzeugt [create] es. `store.clear()` beendet es. */
fun <VM : ViewModel> scopedViewModel(store: ViewModelStore, type: KClass<VM>, create: () -> VM): VM =
    ViewModelProvider.create(store, viewModelFactory { addInitializer(type) { create() } })[type]
