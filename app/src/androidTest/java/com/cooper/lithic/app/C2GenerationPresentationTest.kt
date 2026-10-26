package com.cooper.lithic.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class C2GenerationPresentationTest {
    @Test fun late_old_generation_read_cannot_replace_the_model_presented_by_the_current_scope() {
        val scope = GenerationUiScopeViewModel(SavedStateHandle())
        val oldSession = scope.bind("synthetic-old", Any())
        fun visible() = ViewModelProvider(scope, viewModelFactory { initializer { PreviewProbe() } })[PreviewProbe::class.java]
        val old = visible()
        old.rows.value = listOf("old-row")
        val newSession = scope.bind("synthetic-new", Any())
        val current = visible()
        current.rows.value = listOf("current-row")
        old.rows.value = listOf("late-old-row")
        assertNotEquals(oldSession, newSession)
        assertNotSame(old, current)
        assertTrue(old.cleared)
        assertSame(current, visible())
        assertEquals(listOf("current-row"), visible().rows.value)
    }
    private class PreviewProbe : ViewModel() {
        val rows = MutableStateFlow<List<String>>(emptyList())
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
