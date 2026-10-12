package com.cooper.lithic.feature.project

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import java.time.LocalDate
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDateInputTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invalid_raw_date_and_associated_error_are_retained_without_correction() {
        var raw by mutableStateOf("2026-99-40")
        val error = "请输入准确日期，格式为年-月-日"
        compose.setContent {
            LithicTheme {
                Surface {
                    ProjectDateInput(raw, { raw = it }, LocalDate.of(2026, 10, 12),
                        "开始拥有日期", "test-date", error = error)
                }
            }
        }
        compose.onNodeWithTag("test-date-input").assertTextContains("2026-99-40")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, error))
            .performTextReplacement("2026-02-30")
        compose.onNodeWithTag("test-date-input").assertTextContains("2026-02-30")
        assertEquals("2026-02-30", raw)
    }

    @Test fun local_date_outside_calendar_range_remains_available_as_raw_input() {
        var raw by mutableStateOf("2026-10-12")
        compose.setContent {
            LithicTheme {
                Surface {
                    ProjectDateInput(raw, { raw = it }, LocalDate.of(2026, 10, 12),
                        "开始拥有日期", "test-date")
                }
            }
        }
        compose.onNodeWithTag("test-date-input").performTextReplacement("+12000-01-02")
        compose.onNodeWithTag("test-date-picker").performClick()
        compose.onNodeWithTag("test-date-input").assertTextContains("+12000-01-02")
        compose.onNodeWithText("该日期超出日历范围，请直接修改日期文字").assertIsDisplayed()
        compose.onNodeWithTag("date-picker").assertDoesNotExist()
        assertEquals("+12000-01-02", raw)
    }

    @Test fun real_calendar_confirmation_preserves_the_selected_day_across_device_time_zones() {
        val original = TimeZone.getDefault()
        val changes = AtomicInteger()
        var raw by mutableStateOf("2026-01-01")
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            compose.setContent {
                LithicTheme {
                    Surface {
                        ProjectDateInput(raw, { raw = it; changes.incrementAndGet() },
                            LocalDate.of(2026, 10, 12), "实际付款日期", "test-date")
                    }
                }
            }
            compose.onNodeWithTag("test-date-picker").performClick()
            compose.onNodeWithTag("date-picker").assertIsDisplayed()
            compose.onNodeWithTag("date-picker-confirm").performClick()
            compose.onNodeWithTag("test-date-input").assertTextContains("2026-01-01")
            assertEquals("2026-01-01", raw)
            assertEquals(1, changes.get())
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun canceling_the_calendar_does_not_change_the_controlled_date() {
        val changes = AtomicInteger()
        compose.setContent {
            LithicTheme {
                Surface {
                    ProjectDateInput("2026-10-12", { changes.incrementAndGet() },
                        LocalDate.of(2026, 10, 12), "真实发生日期", "test-date")
                }
            }
        }
        compose.onNodeWithTag("test-date-picker").performClick()
        compose.onNodeWithTag("date-picker-cancel").performClick()
        compose.onNodeWithTag("test-date-input").assertTextContains("2026-10-12")
        compose.onNodeWithTag("date-picker").assertDoesNotExist()
        assertEquals(0, changes.get())
    }

    @Test fun busy_date_input_and_calendar_action_are_disabled_and_touch_target_is_large_enough() {
        compose.setContent {
            LithicTheme {
                Surface {
                    ProjectDateInput("2026-10-12", {}, LocalDate.of(2026, 10, 12),
                        "真实发生日期", "test-date", enabled = false)
                }
            }
        }
        compose.onNodeWithTag("test-date-input").assertIsNotEnabled()
        compose.onNodeWithTag("test-date-picker").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
    }
}
