package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compile only. LocalDensity does not prove system fonts, IME or real navigation. */
class C2BPageLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun large_header_keeps_back_action_and_complete_title_separate() {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                LithicTheme { Box(Modifier.width(360.dp)) {
                    C2PageHeader("长期项目历史档案", {}, "b-back", actions = { Button({}) { Text("新增") } })
                } }
            }
        }
        compose.onNodeWithTag("b-back").assertHeightIsAtLeast(48.dp)
        val back = compose.onNodeWithTag("b-back").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("长期项目历史档案").fetchSemanticsNode().boundsInRoot
        assertTrue(title.top >= back.bottom)
    }
    @Test fun measured_footer_does_not_cover_the_last_scrollable_field() {
        compose.setContent { LithicTheme { Box(Modifier.size(360.dp, 640.dp)) {
            C2FormScaffold("记录一笔", {}, "b-back", footer = {
                Button({}, Modifier.fillMaxWidth().testTag("b-save")) { Text("保存") }
            }) {
                repeat(20) { Text("字段 $it", Modifier.heightIn(min = 48.dp)) }
                Text("最后的错误说明", Modifier.testTag("b-last"))
            }
        } } }
        compose.onNodeWithTag("b-last").performScrollTo().assertIsDisplayed()
        val last = compose.onNodeWithTag("b-last").fetchSemanticsNode().boundsInRoot
        val save = compose.onNodeWithTag("b-save").fetchSemanticsNode().boundsInRoot
        assertTrue(last.bottom <= save.top)
    }
    @Test fun a_read_only_setting_row_does_not_offer_a_fake_action() {
        compose.setContent { LithicTheme { C2SettingRow("本地保存", C2Glyph.Project, Modifier.testTag("b-read-only"), subtitle = "无自动云同步") } }
        compose.onNodeWithTag("b-read-only").assert(hasClickAction().not())
    }
    @Test fun status_chip_preserves_tagged_text_for_existing_state_assertions() {
        compose.setContent { LithicTheme { C2StatusChip("暂停", Modifier.testTag("b-status")) } }
        compose.onNodeWithTag("b-status").assertTextEquals("暂停")
    }

}
