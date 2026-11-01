package com.cooper.lithic.feature.media

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.media.photolite.PhotoLiteCaptureResult
import com.cooper.lithic.data.media.photolite.PhotoLiteCommitResult
import com.cooper.lithic.data.media.photolite.PhotoLiteCoverWriter
import com.cooper.lithic.data.media.photolite.PhotoLiteCoverChange
import com.cooper.lithic.data.media.photolite.PhotoLiteFailure
import com.cooper.lithic.data.media.photolite.PhotoLitePng
import com.cooper.lithic.data.media.photolite.PhotoLiteTarget
import com.cooper.lithic.feature.project.LeaveDecision
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class PhotoLiteCoverPhase { IDLE, PREPARING, CHOOSING, LOADING, PREVIEW, REMOVE_CONFIRM, SAVING, SUCCESS, REJECTED, UNKNOWN }

/** Presentation only. External URI, image bytes and bitmaps are never written into saved state. */
internal data class PhotoLiteCoverUiState(
    val phase: PhotoLiteCoverPhase = PhotoLiteCoverPhase.IDLE,
    val projectId: String? = null,
    val pickerTicket: String? = null,
    val preview: PhotoLitePreview? = null,
    val message: String? = null,
    val change: PhotoLiteCoverChange = PhotoLiteCoverChange.ADD,
) {
    val blocksLeaving: Boolean get() = phase == PhotoLiteCoverPhase.SAVING || phase == PhotoLiteCoverPhase.UNKNOWN
}

/** One generation's transient photo choice. This never owns or opens business storage. */
internal class PhotoLiteCoverController(
    private val writer: PhotoLiteCoverWriter,
    private val savedState: SavedStateHandle,
    private val available: () -> Boolean,
    private val loadPreview: suspend (Uri) -> PhotoLitePreview,
    private val encodePreview: suspend (PhotoLitePreview) -> PhotoLitePng,
    private val loadTimeoutMillis: Long = 15_000,
    private val saveTimeoutMillis: Long = 30_000,
) : ViewModel() {
    constructor(writer: PhotoLiteCoverWriter, pipeline: PhotoLiteThumbnailPipeline, savedState: SavedStateHandle) :
        this(writer, savedState, pipeline::isAvailable, pipeline::load, pipeline::encode)

    val sessionKey: String = UUID.randomUUID().toString()
    private val mutableState = MutableStateFlow(if (savedState.get<Bundle>(SAVED_KEY)?.getBoolean(INCOMPLETE) == true)
        PhotoLiteCoverUiState(phase = PhotoLiteCoverPhase.UNKNOWN, message = UNKNOWN_MESSAGE) else PhotoLiteCoverUiState())
    val state: StateFlow<PhotoLiteCoverUiState> = mutableState.asStateFlow()
    private var target: PhotoLiteTarget? = null
    private var pendingChooser: String? = null
    private var activeTicket: String? = null
    private var currentRouteProject: String? = null
    private var work: Job? = null
    private var loadWatchdog: Job? = null
    private var saveWatchdog: Job? = null
    private var heldPreview: PhotoLitePreview? = null
    private var retired = false

    fun isAvailable(): Boolean = try { available() } catch (_: Exception) { false }

    /** Route changes revoke transient results; an unresolved commit is never silently discarded. */
    fun bindProject(projectId: String?) {
        if (currentRouteProject == projectId) return
        currentRouteProject = projectId
        if (state.value.blocksLeaving) return
        if (state.value.phase != PhotoLiteCoverPhase.IDLE) discard()
    }

    fun begin(projectId: String, change: PhotoLiteCoverChange = PhotoLiteCoverChange.ADD) {
        if (retired || state.value.phase != PhotoLiteCoverPhase.IDLE || currentRouteProject != projectId) return
        if (!isAvailable()) { reject("当前系统暂不支持这项照片试验。尚未更改物品。", projectId); return }
        if (!ownerCurrent()) { reject("当前账本已变化，已停止选择照片。", projectId); return }
        val ticket = UUID.randomUUID().toString()
        activeTicket = ticket
        mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.PREPARING, projectId, change = change)
        work = viewModelScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { writer.capture(projectId, change) }
                if (!sameRequest(ticket, projectId) || state.value.phase != PhotoLiteCoverPhase.PREPARING) return@launch
                if (!ownerCurrent()) { reject("当前账本已变化，已停止选择照片。", projectId); return@launch }
                when (captured) {
                    is PhotoLiteCaptureResult.Ready -> {
                        val candidate = captured.target
                        if (candidate.projectId != projectId || candidate.generationId != writer.generationId ||
                            candidate.ownerKey !== writer.ownerKey || candidate.change != change || !ownerCurrent()) {
                            reject("物品或账本已变化，已停止选择照片。", projectId)
                            return@launch
                        }
                        val admissionFailure = change.admissionFailure(candidate.originalCoverAssetId)
                        if (admissionFailure != null) {
                            reject(failureMessage(admissionFailure), projectId)
                            return@launch
                        }
                        target = candidate
                        if (change == PhotoLiteCoverChange.REMOVE) {
                            // No URI, decoder or image certification is needed to stop displaying a cover.
                            pendingChooser = null
                            mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.REMOVE_CONFIRM, projectId, change = change)
                        } else {
                            pendingChooser = ticket
                            mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.CHOOSING, projectId,
                                pickerTicket = ticket, change = change)
                        }
                    }
                    is PhotoLiteCaptureResult.Rejected -> reject(failureMessage(captured.reason), projectId)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (matches(ticket, projectId)) reject("暂时无法核对物品，尚未保存照片。", projectId) }
        }
    }

    fun pickerWillLaunch(ticket: String): Boolean {
        if (pendingChooser != ticket || state.value.phase != PhotoLiteCoverPhase.CHOOSING ||
            !matches(ticket, state.value.projectId)) return false
        mutableState.value = state.value.copy(pickerTicket = null)
        return true
    }

    fun pickerLaunchFailed(ticket: String, previousChoicePending: Boolean = false) {
        if (pendingChooser != ticket) return
        pendingChooser = null
        reject(if (previousChoicePending) "上一项照片选择尚未结束，请先在系统选择器中完成或取消。尚未保存照片。"
            else "无法打开系统照片选择器。尚未保存照片。", state.value.projectId)
    }

    fun pickerResult(ticket: String?, uri: Uri?) {
        val projectId = state.value.projectId
        val change = state.value.change
        if (ticket == null || pendingChooser != ticket || state.value.phase != PhotoLiteCoverPhase.CHOOSING ||
            !sameRequest(ticket, projectId)) return
        if (!ownerCurrent()) { reject("当前账本已变化，照片选择已作废。", projectId); return }
        pendingChooser = null
        if (uri == null) { discard(); return }
        if (uri.scheme != "content") { reject("照片来源不可用，尚未保存照片。", projectId); return }
        mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.LOADING, projectId, change = change)
        val watchdog = viewModelScope.launch {
            delay(loadTimeoutMillis)
            if (sameRequest(ticket, projectId) && state.value.phase == PhotoLiteCoverPhase.LOADING) {
                // Revoke before cancellation; native thumbnail work may still be running.
                unknown(projectId)
                work?.cancel()
            }
        }
        loadWatchdog = watchdog
        work = viewModelScope.launch {
            // The holder remains outside withContext so cancellation cannot lose ownership of a native result.
            var loaded: PhotoLitePreview? = null
            try {
                withContext(Dispatchers.IO) { loaded = loadPreview(uri) }
                if (!sameRequest(ticket, projectId) || state.value.phase != PhotoLiteCoverPhase.LOADING) return@launch
                if (!ownerCurrent()) {
                    if (sameRequest(ticket, projectId)) reject("当前账本已变化，照片预览已作废。", projectId)
                    return@launch
                }
                val preview = requireNotNull(loaded)
                heldPreview = preview
                mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.PREVIEW, projectId, preview = preview, change = change)
                loaded = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: PhotoLiteImageException) {
                if (matches(ticket, projectId)) {
                    if (failure.unknown) unknown(projectId) else reject("无法取得安全的照片预览。物品封面未改变。", projectId)
                }
            } catch (_: OutOfMemoryError) { if (matches(ticket, projectId)) unknown(projectId) }
            catch (_: Exception) { if (matches(ticket, projectId)) reject("照片读取失败，尚未保存照片。", projectId) }
            finally {
                watchdog.cancel()
                if (loadWatchdog === watchdog) loadWatchdog = null
                loaded?.let { closeUnpublished(it, projectId) }
            }
        }
    }

    fun confirm() {
        val before = state.value
        val request = target ?: return
        val removing = request.change == PhotoLiteCoverChange.REMOVE
        val preview = heldPreview
        val ticket = activeTicket ?: return
        val expectedPhase = if (removing) PhotoLiteCoverPhase.REMOVE_CONFIRM else PhotoLiteCoverPhase.PREVIEW
        if (before.phase != expectedPhase || before.change != request.change ||
            (!removing && preview == null) || !sameRequest(ticket, request.projectId)) return
        if (!ownerCurrent()) { reject("当前账本已变化，照片预览已作废。", request.projectId); return }
        // Synchronous state transition rejects a second tap before the first coroutine starts.
        mutableState.value = before.copy(phase = PhotoLiteCoverPhase.SAVING, pickerTicket = null)
        val watchdog = viewModelScope.launch {
            delay(saveTimeoutMillis)
            if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) {
                unknown(request.projectId)
                work?.cancel()
            }
        }
        saveWatchdog = watchdog
        work = viewModelScope.launch {
            var commitStarted = false
            try {
                val png = if (removing) null else withContext(Dispatchers.IO) { encodePreview(requireNotNull(preview)) }
                if (!sameRequest(ticket, request.projectId) || state.value.phase != PhotoLiteCoverPhase.SAVING) return@launch
                if (!ownerCurrent()) {
                    reject("当前物品或账本已变化，本次没有提交封面。", request.projectId)
                    return@launch
                }
                markIncomplete(request.nonce)
                commitStarted = true
                val result = withContext(Dispatchers.IO) {
                    if (removing) writer.remove(request) else writer.commit(request, requireNotNull(png))
                }
                when (result) {
                    is PhotoLiteCommitResult.Success -> {
                        if (!removing && result.assetId == request.nonce) finishSuccess(ticket, request, if (request.change == PhotoLiteCoverChange.REPLACE)
                            "封面已更换。旧照片未清理，仍会随完整备份保留。" else "封面已保存。")
                        else if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
                    }
                    PhotoLiteCommitResult.Cleared -> {
                        if (removing) finishSuccess(ticket, request,
                            "已取消封面显示。旧照片未清理，已有文件可能继续占用空间，并会随完整备份保留。")
                        else if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
                    }
                    is PhotoLiteCommitResult.Rejected -> {
                        if (acceptCommitReply(ticket, request.projectId)) {
                            clearIncomplete()
                            reject(failureMessage(result.reason), request.projectId)
                        } else if (!retired && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
                    }
                    PhotoLiteCommitResult.Unknown -> if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
                }
            } catch (cancelled: CancellationException) {
                if (commitStarted && sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
                throw cancelled
            } catch (failure: PhotoLiteImageException) {
                if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) {
                    if (commitStarted || failure.unknown) unknown(request.projectId)
                    else reject("照片未通过保存检查，原封面未改变。", request.projectId)
                }
            } catch (_: OutOfMemoryError) {
                if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
            }
            catch (_: Exception) {
                if (sameRequest(ticket, request.projectId) && state.value.phase == PhotoLiteCoverPhase.SAVING) {
                    if (commitStarted) unknown(request.projectId) else reject("照片处理失败，本次没有提交封面。", request.projectId)
                }
            }
            finally {
                watchdog.cancel()
                if (saveWatchdog === watchdog) saveWatchdog = null
            }
        }
    }

    fun leaveDecision(): LeaveDecision = when (state.value.phase) {
        PhotoLiteCoverPhase.SAVING, PhotoLiteCoverPhase.UNKNOWN -> LeaveDecision.BLOCK_PENDING
        PhotoLiteCoverPhase.PREPARING, PhotoLiteCoverPhase.CHOOSING, PhotoLiteCoverPhase.LOADING,
        PhotoLiteCoverPhase.PREVIEW, PhotoLiteCoverPhase.REMOVE_CONFIRM -> LeaveDecision.CONFIRM_DISCARD
        else -> LeaveDecision.ALLOW
    }

    /** Explicit cancel only. A potentially committed operation cannot be dismissed or submitted again. */
    fun discard() {
        if (state.value.blocksLeaving) return
        val projectId = state.value.projectId
        activeTicket = null
        pendingChooser = null
        target = null
        work?.cancel()
        loadWatchdog?.cancel()
        loadWatchdog = null
        mutableState.value = PhotoLiteCoverUiState()
        releasePreview(projectId)
    }

    private fun sameRequest(ticket: String, projectId: String?): Boolean = !retired && activeTicket == ticket &&
        projectId != null && currentRouteProject == projectId

    private fun matches(ticket: String, projectId: String?): Boolean = sameRequest(ticket, projectId) && ownerCurrent()

    private fun acceptCommitReply(ticket: String, projectId: String): Boolean =
        state.value.phase == PhotoLiteCoverPhase.SAVING && matches(ticket, projectId)

    private fun finishSuccess(ticket: String, request: PhotoLiteTarget, message: String) {
        if (acceptCommitReply(ticket, request.projectId)) {
            clearIncomplete()
            mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.SUCCESS, request.projectId,
                message = message, change = request.change)
            releasePreview(request.projectId)
        } else if (!retired && state.value.phase == PhotoLiteCoverPhase.SAVING) unknown(request.projectId)
    }

    private fun ownerCurrent(): Boolean = try { writer.isCurrent() } catch (_: Exception) { false }

    private fun reject(message: String, projectId: String?) {
        activeTicket = null
        pendingChooser = null
        target = null
        mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.REJECTED, projectId, message = message, change = state.value.change)
        releasePreview(projectId)
    }

    private fun unknown(projectId: String?) {
        markIncomplete(target?.nonce ?: savedState.get<Bundle>(SAVED_KEY)?.getString(NONCE))
        activeTicket = null
        pendingChooser = null
        target = null
        mutableState.value = PhotoLiteCoverUiState(PhotoLiteCoverPhase.UNKNOWN, projectId, message = UNKNOWN_MESSAGE, change = state.value.change)
        releasePreview(projectId)
    }

    private fun releasePreview(projectId: String?) {
        val preview = heldPreview
        heldPreview = null
        if (preview != null) closeUnpublished(preview, projectId)
    }

    private fun closeUnpublished(preview: PhotoLitePreview, projectId: String?) {
        try { preview.close() }
        catch (_: Exception) {
            if (!retired) unknown(projectId)
            else markIncomplete(savedState.get<Bundle>(SAVED_KEY)?.getString(NONCE))
        }
    }

    private fun markIncomplete(nonce: String?) {
        savedState[SAVED_KEY] = Bundle().apply { putBoolean(INCOMPLETE, true); putString(NONCE, nonce) }
    }

    private fun clearIncomplete() { savedState.remove<Bundle>(SAVED_KEY) }

    override fun onCleared() {
        retired = true
        activeTicket = null
        pendingChooser = null
        work?.cancel()
        loadWatchdog?.cancel()
        loadWatchdog = null
        saveWatchdog?.cancel()
        saveWatchdog = null
        releasePreview(state.value.projectId)
    }

    companion object {
        internal const val SAVED_KEY = "lithic.photoLite.incomplete"
        internal const val INCOMPLETE = "unfinishedCommit"
        internal const val NONCE = "nonce"
        internal const val UNKNOWN_MESSAGE = "封面操作或资源状态尚未确认，本轮已停止。请保留当前现场，不要重复选择、保存、取消显示或清理文件。"
        internal fun failureMessage(reason: PhotoLiteFailure): String = when (reason) {
            PhotoLiteFailure.OWNER_UNAVAILABLE -> "当前账本已变化，已停止保存封面。"
            PhotoLiteFailure.STALE_PROJECT -> "物品已变化，本次没有更改封面。请查看物品的当前状态。"
            PhotoLiteFailure.ALREADY_HAS_COVER -> "物品已有封面，请核对当前状态后使用更换照片或取消封面显示。"
            PhotoLiteFailure.NO_COVER -> "物品当前没有封面，本次没有更改任何记录。"
            PhotoLiteFailure.INVALID_OPERATION -> "封面操作与当前物品状态不符，本次没有提交更改。"
            PhotoLiteFailure.INVALID_IMAGE -> "图片未通过检查，本次没有更改封面。"
            PhotoLiteFailure.FILE_FAILED, PhotoLiteFailure.STORAGE_FAILED -> "封面未能保存，请保留现有记录；不会自动再次提交。"
            PhotoLiteFailure.DUPLICATE -> "本次照片提交已处理，不会重复登记。"
        }
    }
}
