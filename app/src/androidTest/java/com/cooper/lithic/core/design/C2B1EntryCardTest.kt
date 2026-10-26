package com.cooper.lithic.core.design

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Source compilation only until a separately authorized Android run. */
class C2B1EntryCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun negative_cost_and_record_identity_keep_the_original_open_callback() {
        var opened = 0
        compose.setContent { LithicTheme { EntryCard("相机", "持有中", "已持有300天", BigDecimal("-1.23"),
            onClick = { opened++ }, category = "电子设备", metricLabel = "当前日均成本") } }
        compose.onNodeWithText("相机").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithText("−¥1.23", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("当前日均成本", useUnmergedTree = true).assertExists()
    }
    @Test fun unavailable_cover_reference_is_not_presented_as_no_cover() {
        compose.setContent { LithicTheme { EntryCard("服务", "已暂停", "折算付费时长365天", BigDecimal.ZERO,
            category = "其他", kind = C2Glyph.Subscription, hasCoverReference = true, metricLabel = "历史平均日成本") } }
        compose.onNodeWithContentDescription("封面暂不可用", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("¥0.00", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("已暂停", useUnmergedTree = true).assertExists()
    }
}
