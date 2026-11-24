package de.geosphere.congregationplaner.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScopedViewModelTest {
    private class PollingViewModel : ViewModel() {
        val polling = viewModelScope.launch { awaitCancellation() }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearing the store stops the work of the view model`() {
        val store = ViewModelStore()
        val viewModel = scopedViewModel(store, PollingViewModel::class) { PollingViewModel() }
        assertTrue(viewModel.polling.isActive)

        store.clear()

        assertFalse(viewModel.polling.isActive)
    }

    @Test
    fun `the same store keeps the same view model`() {
        val store = ViewModelStore()
        val first = scopedViewModel(store, PollingViewModel::class) { PollingViewModel() }

        assertSame(first, scopedViewModel(store, PollingViewModel::class) { PollingViewModel() })
    }
}
