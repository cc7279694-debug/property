package com.cooper.lithic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode

/** Follow the visible sample UI; these drivers never call a business command directly. */
fun ComposeContentTestRule.openHomeCreate(tag: String) {
    require(tag == "home-create-project" || tag == "home-create-subscription")
    if (onAllNodes(hasTestTag("home-add-record")).fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("home-add-record").assertIsDisplayed().performClick()
    } else {
        onNodeWithTag("home-first-record").performScrollTo().assertIsDisplayed().performClick()
    }
    onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performClick()
}

fun ComposeContentTestRule.openProjectMore() {
    onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-more"))
    onNodeWithTag("project-more").assertIsDisplayed().performClick()
}

fun ComposeContentTestRule.projectDetailAction(tag: String) {
    if (tag in setOf("project-edit", "project-end", "project-update-end", "project-delete", "project-cover-edit")) {
        openProjectMore()
    } else {
        onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag(tag))
    }
    onNodeWithTag(tag).performClick()
}
