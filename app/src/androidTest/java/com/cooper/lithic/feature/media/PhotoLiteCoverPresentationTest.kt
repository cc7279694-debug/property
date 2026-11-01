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
import com.cooper.lithic.data.media.photolite.PhotoLiteCoverChange
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

    @Test fun existing_cover_uses_expanded_slot_and_exposes_explicit_replace_and_cancel_display() {
        val covered = detail.copy(project = detail.project.copy(coverAssetId = "95000000-0000-0000-0000-000000000001"))
        var replaced = 0
        var removed = 0
        show(covered, onAdd = { error("Adding over an existing cover must remain unavailable") }, expand = true,
            onReplace = { replaced++ }, onRemove = { removed++ })
        compose.onNodeWithTag("project-detail-cover-expanded").assertIsDisplayed()
        compose.onNodeWithTag("project-add-photo").assertDoesNotExist()
        compose.onNodeWithTag("project-replace-photo").performClick()
        compose.onNodeWithTag("project-remove-photo").performClick()
        assertEquals(1, replaced)
        assertEquals(1, removed)
        compose.onNodeWithTag("project-detail-name").assertTextContains(detail.project.name)
        compose.onNodeWithTag("project-record-money").assertIsDisplayed()
    }

    @Test fun existing_cover_on_unsupported_device_keeps_management_actions_disabled() {
        val covered = detail.copy(project = detail.project.copy(coverAssetId = "95000000-0000-0000-0000-000000000001"))
        show(covered, enabled = false, onReplace = { error("Disabled may not replace") },
            onRemove = { error("Disabled may not clear") }, unavailable = "此设备暂不支持照片试验。")
        compose.onNodeWithTag("project-replace-photo").assertIsNotEnabled()
        compose.onNodeWithTag("project-remove-photo").assertIsNotEnabled()
        compose.onNodeWithTag("project-photo-unavailable").assertTextContains("不支持")
    }

    @Test fun cancel_display_requires_confirmation_and_cancel_keeps_the_cover() {
        val state = mutableStateOf(PhotoLiteCoverUiState(PhotoLiteCoverPhase.REMOVE_CONFIRM, change = PhotoLiteCoverChange.REMOVE))
        var confirmed = 0
        var cancelled = 0
        compose.setContent { LithicTheme { PhotoLiteCoverDialog(state.value, { confirmed++ },
            { cancelled++; state.value = PhotoLiteCoverUiState() }) } }
        compose.onNodeWithTag("photo-lite-removal-warning").assertTextContains("不会彻底删除旧照片")
        compose.onNodeWithTag("photo-lite-removal-warning").assertTextContains("完整备份仍会包含并校验旧照片")
        compose.onNodeWithTag("photo-lite-remove-confirm").assertIsDisplayed()
        compose.onNodeWithTag("photo-lite-preview").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-cancel").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, cancelled)
        compose.onNodeWithTag("photo-lite-dialog").assertDoesNotExist()
    }

    @Test fun cancelled_display_success_does_not_claim_the_file_was_deleted() {
        compose.setContent { LithicTheme { PhotoLiteCoverDialog(
            PhotoLiteCoverUiState(PhotoLiteCoverPhase.SUCCESS,
                message = "已取消封面显示。旧照片未清理，已有文件可能继续占用空间，并会随完整备份保留。",
                change = PhotoLiteCoverChange.REMOVE), { error("Success may not submit") }, {}) } }
        compose.onNodeWithTag("photo-lite-feedback").assertTextContains("旧照片未清理")
        compose.onNodeWithTag("photo-lite-feedback").assertTextContains("随完整备份保留")
        compose.onNodeWithTag("photo-lite-confirm").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-remove-confirm").assertDoesNotExist()
        compose.onNodeWithTag("photo-lite-dismiss").assertIsDisplayed()
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
        unavailable: String? = null, expand: Boolean = false, onReplace: (() -> Unit)? = null,
        onRemove: (() -> Unit)? = null) {
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(400.dp, 840.dp)) {
            ProjectDetailScreen(ProjectDetailUiState(value, loading = false), {}, {}, {}, {},
                onAddPhoto = onAdd, photoActionEnabled = enabled, photoUnavailableMessage = unavailable,
                expandPhotoCover = expand, coverContent = { Box(Modifier.testTag("synthetic-cover-slot")) },
                onReplacePhoto = onReplace, onRemovePhoto = onRemove)
        } } }
    }
}
