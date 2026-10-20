package com.cooper.lithic.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenerationUiScopeTest {
    @Test fun generation_switch_cancels_old_models_and_drops_old_saved_form_and_navigation_session() {
        val saved = SavedStateHandle()
        val scope = GenerationUiScopeViewModel(saved)
        val previousSession = scope.bind("first-generation", Any())
        val oldForms = scope.savedState
        oldForms["synthetic-form"] = "old pending command"
        val previousModel = ClearedProbe()
        scope.viewModelStore.put("synthetic-model", previousModel)
        val nextSession = scope.bind("second-generation", Any())
        assertTrue(previousModel.cleared)
        // A late cancelled job can update only the detached old handle.
        oldForms["synthetic-form"] = "late old command"
        assertNull(scope.savedState.get<String>("synthetic-form"))
        assertNotEquals(previousSession, nextSession)
    }

    @Test fun same_generation_rotation_keeps_models_and_saved_form_but_reopened_owner_cannot_reuse_them() {
        val saved = SavedStateHandle()
        val scope = GenerationUiScopeViewModel(saved)
        val ownerIdentity = Any()
        val session = scope.bind("same-generation", ownerIdentity)
        scope.savedState["synthetic-form"] = "current draft"
        val model = ClearedProbe()
        scope.viewModelStore.put("synthetic-model", model)
        assertEquals(session, scope.bind("same-generation", ownerIdentity))
        assertFalse(model.cleared)
        assertEquals("current draft", scope.savedState.get<String>("synthetic-form"))
        assertNotEquals(session, scope.bind("same-generation", Any()))
        assertTrue(model.cleared)
        assertNull(scope.savedState.get<String>("synthetic-form"))
    }

    @Test fun process_recreation_with_same_generation_retains_saved_draft_without_old_model_reference() {
        val saved = SavedStateHandle()
        val previous = GenerationUiScopeViewModel(saved)
        val session = previous.bind("current-generation", Any())
        previous.savedState["synthetic-form"] = "restore this raw draft"
        val recreated = GenerationUiScopeViewModel(SavedStateHandle(mapOf(
            GenerationUiScopeViewModel.GENERATION_KEY to "current-generation",
            GenerationUiScopeViewModel.EPOCH_KEY to session,
            GenerationUiScopeViewModel.FORMS_KEY to previous.saveForms(),
        )))
        assertEquals(session, recreated.bind("current-generation", Any()))
        assertEquals("restore this raw draft", recreated.savedState.get<String>("synthetic-form"))
    }

    private class ClearedProbe : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }
}
