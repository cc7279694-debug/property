package com.cooper.lithic.feature.subscription

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import org.junit.Rule
import org.junit.Test

/** This test is compiled only; UI-C2 B has no Android runtime authorization. */
class C2B1SubscriptionListTest {
    @get:Rule val compose = createComposeRule()
    @Test fun supplemental_state_explanation_is_discoverable_and_dismissible() {
        compose.setContent { LithicTheme { SubscriptionListScreen(SubscriptionListUiState(), {}, {}, {}, {}, {}) } }
        compose.onNodeWithTag("subscription-list-information").performClick()
        compose.onNodeWithTag("subscription-list-history-explanation").assertExists()
        compose.onNodeWithTag("subscription-list-expired-explanation").assertExists()
        compose.onNodeWithTag("subscription-list-close-information").performClick()
        compose.onNodeWithTag("subscription-list-history-explanation").assertDoesNotExist()
    }
}
