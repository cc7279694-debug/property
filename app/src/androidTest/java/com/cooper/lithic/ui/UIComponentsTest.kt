package com.cooper.lithic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.DateField
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.NameField
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UIComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missing_amount_and_real_zero_have_distinct_visible_and_accessible_values() {
        compose.setContent {
            LithicTheme {
                Column {
                    AmountText(null, Modifier.testTag("missing"))
                    AmountText(BigDecimal.ZERO, Modifier.testTag("zero"))
                }
            }
        }
        compose.onNodeWithTag("missing").assertContentDescriptionEquals("暂无数据").assert(hasText("—"))
        compose.onNodeWithTag("zero").assertContentDescriptionEquals("0.00元/天").assert(hasText("¥0.00"))
    }

    @Test fun amount_units_keep_ordinary_rounding_separate_from_daily_subcent_values() {
        compose.setContent {
            LithicTheme {
                Column {
                    AmountText(BigDecimal("0.005"), Modifier.testTag("ordinary"), AmountUnit.YUAN)
                    AmountText(BigDecimal("0.005"), Modifier.testTag("daily"))
                    AmountText(BigDecimal("-0.005"), Modifier.testTag("negative"))
                }
            }
        }
        compose.onNodeWithTag("ordinary").assert(hasText("¥0.01")).assertContentDescriptionEquals("0.01元")
        compose.onNodeWithTag("daily").assert(hasText("<¥0.01")).assertContentDescriptionEquals("小于0.01元/天")
        compose.onNodeWithTag("negative").assert(hasText("−<¥0.01"))
            .assertContentDescriptionEquals("负值，绝对值小于0.01元/天")
        compose.onNodeWithText("元", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun theme_has_the_frozen_type_hierarchy_and_readable_light_and_dark_surfaces() {
        var dark by mutableStateOf(false)
        var capturedType: Typography? = null
        var capturedColors: ColorScheme? = null
        compose.setContent {
            LithicTheme(darkTheme = dark) {
                val typography = MaterialTheme.typography
                val colors = MaterialTheme.colorScheme
                SideEffect { capturedType = typography; capturedColors = colors }
                Surface { Text("浅深色可读性") }
            }
        }
        compose.runOnIdle {
            val typography = requireNotNull(capturedType)
            assertEquals(38.sp, typography.displayLarge.fontSize)
            assertEquals(24.sp, typography.headlineLarge.fontSize)
            assertEquals(24.sp, typography.titleLarge.fontSize)
            assertEquals(16.sp, typography.bodyLarge.fontSize)
            assertEquals(14.sp, typography.bodyMedium.fontSize)
        }
        val light = requireNotNull(capturedColors)
        assertTrue(contrast(light) >= 4.5f)
        compose.runOnIdle { dark = true }
        compose.waitForIdle()
        val night = requireNotNull(capturedColors)
        assertTrue(night.background.luminance() < light.background.luminance())
        assertTrue(contrast(night) >= 4.5f)
    }

    @Test fun cards_keep_each_callers_status_and_time_semantics_and_only_real_actions() {
        val clicks = AtomicInteger()
        compose.setContent {
            LithicTheme {
                Column {
                    EntryCard("真实长期项目", "使用中", "已持有365天", BigDecimal("-0.005"), Modifier.testTag("project"))
                    EntryCard("合成订阅组件", "已到期", "折算付费时长365天", BigDecimal.ZERO,
                        Modifier.testTag("subscription"), onClick = { clicks.incrementAndGet() }, cover = { Text("测试封面") })
                }
            }
        }
        compose.onNodeWithText("使用中", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("已持有365天", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("折算付费时长365天", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("已到期", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("测试封面", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("project").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose.onNodeWithTag("subscription").assertHasClickAction().performClick()
        assertEquals(1, clicks.get())
    }

    @Test fun controlled_fields_keep_input_show_associated_errors_and_deliver_ime_callbacks() {
        val nameIme = AtomicInteger()
        val amountIme = AtomicInteger()
        compose.setContent {
            LithicTheme {
                var name by remember { mutableStateOf("") }
                var amount by remember { mutableStateOf("12.30") }
                Column {
                    NameField(name, { name = it }, Modifier.testTag("name"), error = "名称不能为空",
                        imeAction = ImeAction.Next, onImeAction = { nameIme.incrementAndGet() })
                    AmountField(amount, { amount = it }, Modifier.testTag("amount"), error = "请核对金额",
                        imeAction = ImeAction.Done, onImeAction = { amountIme.incrementAndGet() })
                }
            }
        }
        compose.onNodeWithText("名称", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("名称不能为空", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("name").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "名称不能为空"))
            .performTextInput("保留的输入")
        compose.onNodeWithTag("name").assertTextContains("保留的输入").performImeAction()
        compose.onNodeWithTag("amount").assertTextContains("12.30")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "请核对金额")).performImeAction()
        assertEquals(1, nameIme.get())
        assertEquals(1, amountIme.get())
    }

    @Test fun date_field_is_a_labeled_controlled_selection_with_a_disabled_state() {
        val selections = AtomicInteger()
        compose.setContent {
            LithicTheme {
                var date by remember { mutableStateOf(LocalDate.of(2026, 10, 12)) }
                Column {
                    DateField(date, { selections.incrementAndGet(); date = LocalDate.of(2026, 10, 13) },
                        Modifier.testTag("date"), error = "请核对日期")
                    DateField(null, { selections.incrementAndGet() }, Modifier.testTag("disabled"), label = "结束日期", enabled = false)
                }
            }
        }
        compose.onNodeWithText("日期", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("请核对日期", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("date").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("2026-10-13", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("disabled").assertIsNotEnabled()
        assertEquals(1, selections.get())
    }

    @Test fun large_text_synthetic_form_keeps_errors_save_and_return_reachable_after_input() {
        val saves = AtomicInteger()
        val backs = AtomicInteger()
        var savedName = ""
        compose.setContent {
            LargeTextHost {
                SyntheticFormHost(onSave = { name, amount, date ->
                    savedName = name
                    assertEquals("12.30", amount)
                    assertEquals(LocalDate.of(2026, 10, 12), date)
                    saves.incrementAndGet()
                }, onBack = { backs.incrementAndGet() })
            }
        }
        compose.onNodeWithTag("save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(0, saves.get())
        compose.onNodeWithText(FORM_ERROR, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTextFits(FORM_ERROR)
        saveFormScreenshot("form-error.png")
        compose.onNodeWithTag("form-name").performScrollTo().performClick().performTextInput("测试项目")
        compose.waitForIdle()
        compose.onNodeWithTag("save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        saveFormScreenshot("form-keyboard.png")
        compose.onNodeWithTag("save").performClick()
        assertEquals(1, saves.get())
        assertEquals("测试项目", savedName)
        compose.onNodeWithTag("form-back").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs.get())
    }

    @Test fun large_text_cards_and_long_amounts_wrap_without_losing_the_numeric_value() {
        val name = "用于验证窄屏换行的很长项目名称及真实状态信息"
        val amount = "¥92233720368547758.07"
        compose.setContent {
            LargeTextHost {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    EntryCard(name, "已结束", "结果截至2026-10-12", BigDecimal("92233720368547758.07"))
                }
            }
        }
        compose.onNodeWithText(name, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTextFits(name)
        compose.onNodeWithText(amount, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTextFits(amount)
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Text layout must be observable", layouts.isNotEmpty())
        assertFalse("Text must not be clipped or ellipsized", layouts.single().hasVisualOverflow)
    }

    private fun saveFormScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull("The synthetic form screenshot must be available", bitmap)
        val screenshot = requireNotNull(bitmap)
        try {
            val directory = File(instrumentation.targetContext.cacheDir, "m05-ui-evidence")
            assertTrue(directory.isDirectory || directory.mkdirs())
            val file = File(directory, name)
            FileOutputStream(file).use { output ->
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            assertTrue("The screenshot must contain PNG bytes", file.length() > 0)
        } finally {
            screenshot.recycle()
        }
    }

    private fun contrast(colors: ColorScheme): Float {
        val foreground = colors.onSurface.luminance()
        val background = colors.surface.luminance()
        return (maxOf(foreground, background) + 0.05f) / (minOf(foreground, background) + 0.05f)
    }

    private companion object {
        const val FORM_ERROR = "名称不能为空。请填写名称后保存，其他已输入的金额和日期会保留。"
    }
}

@Composable
private fun LargeTextHost(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        LithicTheme {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.width(360.dp).fillMaxHeight()) { content() }
            }
        }
    }
}

/** Test only: this host exercises callbacks and does not open or save a business database. */
@Composable
private fun SyntheticFormHost(onSave: (String, String, LocalDate) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("12.30") }
    var showError by remember { mutableStateOf(false) }
    val date = LocalDate.of(2026, 10, 12)
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onBack, Modifier.testTag("form-back").heightIn(min = 48.dp)) { Text("返回") }
        Text("组件验证表单（仅测试）", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        NameField(name, { name = it }, Modifier.fillMaxWidth().testTag("form-name"),
            error = if (showError && name.isBlank()) "名称不能为空。请填写名称后保存，其他已输入的金额和日期会保留。" else null)
        Spacer(Modifier.height(16.dp))
        AmountField(amount, { amount = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        DateField(date, {}, Modifier.fillMaxWidth())
        repeat(3) {
            Text("以下内容只验证表单滚动与大字体，不写入任何业务数据。", Modifier.padding(vertical = 16.dp))
        }
        Button(onClick = { showError = true; if (name.isNotBlank()) onSave(name, amount, date) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save")) { Text("保存") }
        Spacer(Modifier.height(24.dp))
    }
}
