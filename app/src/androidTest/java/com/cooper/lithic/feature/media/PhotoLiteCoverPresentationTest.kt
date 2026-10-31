package com.cooper.lithic.feature.media

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.ui.C2VisualFixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.Before

/** Production composables with androidTest-only facts; Android execution remains NOT_RUN. */
class PhotoLiteCoverPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val detail = C2VisualFixtures(true).projects.items.single()

    @Before fun disposable_plain_host_only() { assertPhotoLitePlainHost() }

    @Test fun ordinary_detail_has_no_experimental_photo_action() {
        show()
        compose.onNodeWithTag("project-add-photo").assertDoesNotExist()
        compose.onNodeWithTag("project-detail-name").assertTextContains(detail.project.name)
    }

    @Test fun empty_cover_exposes_one_add_action_without_replacing_money_action() {
        var selected = 0
        show(onAdd = { selected++ })
        compose.onNodeWithTag("project-add-photo").performClick()
        assertEquals(1, selected)
        compose.onNodeWithTag("project-record-money").assertIsDisplayed()
    }

    @Test fun unsupported_device_has_explicit_disabled_entry() {
        show(onAdd = { error("Disabled action must not run") }, enabled = false,
            unavailable = "此设备暂不支持照片试验，物品记录不受影响。")
        compose.onNodeWithTag("project-add-photo").assertIsNotEnabled()
        compose.onNodeWithTag("project-photo-unavailable").assertTextContains("不支持")
    }

    @Test fun existing_cover_uses_expanded_slot_and_has_no_replace_or_remove_action() {
        val covered = detail.copy(project = detail.project.copy(coverAssetId = "95000000-0000-0000-0000-000000000001"))
        show(covered, onAdd = { error("Existing cover must not be replaced") }, expand = true)
        compose.onNodeWithTag("project-detail-cover-expanded").assertIsDisplayed()
        compose.onNodeWithTag("project-add-photo").assertDoesNotExist()
        compose.onNodeWithTag("project-detail-name").assertTextContains(detail.project.name)
    }

    @Test fun unknown_result_is_persistent_without_confirm_cancel_or_retry() {
        compose.setContent { LithicTheme { PhotoLiteCoverDialog(
            PhotoLiteCoverUiState(PhotoLiteCoverPhase.UNKNOWN, message = PhotoLiteCoverController.UNKNOWN_MESSAGE),
            { error("Unknown may not submit") }, { error("Unknown may not discard") }) } }
        compose.onNodeWithTag("photo-lite-feedback").assertTextContains("不要重复")
        compose.onNodeWithTag("photo-lite-confirm").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-cancel").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-dismiss").assertDoesNotExist()
    }

    @Test fun saving_disables_dismissal_and_second_confirmation() {
        compose.setContent { LithicTheme { PhotoLiteCoverDialog(PhotoLiteCoverUiState(PhotoLiteCoverPhase.SAVING),
            { error("Saving may not submit again") }, { error("Saving may not discard") }) } }
        compose.onNodeWithTag("photo-lite-progress").assertIsDisplayed()
        compose.onNodeWithTag("photo-lite-confirm").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-cancel").assertDoesNotExist()
    }

    @Test fun preview_renders_a_real_synthetic_raster_and_cancel_does_not_confirm() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val preview = PhotoLitePreview(bitmap, requireNotNull(CoverPixelBudget(4096).reserve(bitmap.allocationByteCount.toLong())))
        val state = mutableStateOf(PhotoLiteCoverUiState(PhotoLiteCoverPhase.PREVIEW, preview = preview))
        var confirmed = 0
        var cancelled = 0
        try {
            compose.setContent { LithicTheme { PhotoLiteCoverDialog(state.value, { confirmed++ },
                { cancelled++; state.value = PhotoLiteCoverUiState(); preview.close() }) } }
            compose.onNodeWithTag("photo-lite-preview").assertIsDisplayed()
            compose.onNodeWithTag("photo-lite-confirm").assertIsDisplayed()
            compose.onNodeWithTag("photo-lite-cancel").performClick()
            compose.onNodeWithTag("photo-lite-dialog").assertDoesNotExist()
            assertEquals(0, confirmed)
            assertEquals(1, cancelled)
        } finally { preview.close() }
    }

    @Test fun rejected_result_has_acknowledgement_without_false_success() {
        var closed = 0
        compose.setContent { LithicTheme { PhotoLiteCoverDialog(
            PhotoLiteCoverUiState(PhotoLiteCoverPhase.REJECTED, message = "图片未通过检查，物品封面未改变。"),
            { error("Rejected may not commit") }, { closed++ }) } }
        compose.onNodeWithTag("photo-lite-feedback").assertTextContains("未改变")
        compose.onNodeWithTag("photo-lite-confirm").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-dismiss").performClick()
        assertEquals(1, closed)
    }

    private fun show(value: ProjectDetail = detail, onAdd: (() -> Unit)? = null, enabled: Boolean = true,
        unavailable: String? = null, expand: Boolean = false) {
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(400.dp, 840.dp)) {
            ProjectDetailScreen(ProjectDetailUiState(value, loading = false), {}, {}, {}, {},
                onAddPhoto = onAdd, photoActionEnabled = enabled, photoUnavailableMessage = unavailable,
                expandPhotoCover = expand, coverContent = { Box(Modifier.testTag("synthetic-cover-slot")) })
        } } }
    }
}
