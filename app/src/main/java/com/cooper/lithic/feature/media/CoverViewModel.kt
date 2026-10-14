package com.cooper.lithic.feature.media

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.media.CoverBaseline
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.CoverTarget
import com.cooper.lithic.data.media.CoverTargetType
import com.cooper.lithic.data.media.MediaFailureKind
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CoverEditorUiState(
    val target: CoverTarget,
    val origin: String,
    val name: String = "",
    val oldAssetId: String? = null,
    val preparedAsset: LocalMediaAssetEntity? = null,
    val remove: Boolean = false,
    val loading: Boolean = false,
    val preparing: Boolean = false,
    val phase: SubmissionPhase = SubmissionPhase.IDLE,
    val dirty: Boolean = false,
    val message: String? = null,
    val conflict: Boolean = false,
    val missing: Boolean = false,
)

data class CoverUiState(
    val editor: CoverEditorUiState? = null,
    val committedTarget: CoverTarget? = null,
    val committedOrigin: String = "CURRENT",
)

/** One task envelope; Room and immutable media stay owned by the current generation. */
class CoverViewModel(
    private val repository: MediaRepository,
    private val savedState: SavedStateHandle,
    private val instant: () -> Instant = Instant::now,
) : ViewModel() {
    private var form = savedState.get<Bundle>(FORM_KEY)?.let(::Bundle)
    private val mutableState = MutableStateFlow(CoverUiState())
    val state: StateFlow<CoverUiState> = mutableState.asStateFlow()
    private var readVersion = 0L
    private var message: String? = null
    private var loading = false
    private var preparing = false
    private var conflict = false
    private var missing = false

    init {
        form?.let { active ->
            if (!validIdentity(active)) { form = null; savedState[FORM_KEY] = null }
            else {
                if (phase() == SubmissionPhase.IN_FLIGHT) active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                if (active.getBoolean("preparing")) active.putBoolean("preparing", false)
                publish()
                when {
                    phase() == SubmissionPhase.COMMITTED -> readCommitted()
                    active.getBundle("baseline") == null -> loadBaseline()
                    phase() == SubmissionPhase.IDLE && (active.getBundle("asset") != null || active.getString("pendingAssetId") != null) -> restorePrepared()
                }
            }
        }
    }

    override fun onCleared() {
        val preparedIds = listOfNotNull(form?.getBundle("asset")?.getString("id"), form?.getString("pendingAssetId")).distinct()
        if (preparedIds.isNotEmpty()) {
            // A delivered preparation has no running child left to cancel. Release only its
            // memory lease; saved task recovery and explicit safe collection retain the bytes.
            viewModelScope.launch(NonCancellable + Dispatchers.IO) {
                preparedIds.forEach { repository.releasePreparedLease(it) }
            }
        }
        super.onCleared()
    }

    fun start(target: CoverTarget, origin: String = "CURRENT") {
        if (form != null || !StoredValueValidation.isUuid(target.id) || origin !in listOf("CURRENT", "HISTORY")) return
        form = Bundle().apply {
            putString("targetType", target.type.name); putString("targetId", target.id); putString("origin", origin)
            putString("operationId", UUID.randomUUID().toString()); putString("phase", SubmissionPhase.IDLE.name)
        }
        publish(); loadBaseline()
    }

    /** Cancellation has no staging, database, dirty-state or persistent URI side effect. */
    fun chooseImage(uri: Uri?) {
        val active = form ?: return
        if (uri == null || !canChange() || baseline() == null) return
        val operation = active.getString("operationId")!!
        val previousId = active.getBundle("asset")?.getString("id")
        val assetId = UUID.randomUUID().toString()
        active.putString("pendingAssetId", assetId); active.putBoolean("preparing", true)
        preparing = true; message = null; publish()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.prepareImport(uri, assetId) }
                if (!isSame(operation)) return@launch
                preparing = false; active.putBoolean("preparing", false); active.remove("pendingAssetId")
                when (result) {
                    is MediaResult.Success -> {
                        if (result.value.asset.id != assetId) { message = "图片准备结果不一致，请重新选择。" }
                        else {
                            active.putBundle("asset", CoverSavedState.encodeAsset(result.value.asset))
                            active.putBoolean("remove", false); active.putBoolean("dirty", true)
                            message = "新封面已准备，确认保存后才会替换当前封面。"
                            if (previousId != null && previousId != assetId) release(previousId)
                        }
                    }
                    is MediaResult.Failure -> { message = imageError(result.kind); release(assetId) }
                }
                publish()
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) { repository.releasePreparedLease(assetId) }
                throw cancelled
            }
            catch (_: Exception) {
                if (isSame(operation)) {
                    preparing = false; active.putBoolean("preparing", false); active.remove("pendingAssetId")
                    message = "图片暂时无法导入，当前封面没有改变，请重新选择。"; publish(); release(assetId)
                }
            }
        }
    }

    fun removeCover() {
        val active = form ?: return
        if (!canChange() || baseline()?.coverAssetId == null) return
        active.getBundle("asset")?.getString("id")?.let(::release)
        active.remove("asset"); active.putBoolean("remove", true); active.putBoolean("dirty", true)
        message = "确认保存后仅移除此记录的封面；其他记录使用的图片会保留。"; publish()
    }

    fun save() {
        val active = form ?: return
        if (loading || preparing || conflict || missing) return
        if (phase() == SubmissionPhase.COMMITTED) { readCommitted(); return }
        if (phase() == SubmissionPhase.IN_FLIGHT) return
        if (phase() == SubmissionPhase.IDLE && !active.getBoolean("dirty")) return
        val expected = baseline() ?: return
        if (active.getString("writtenAt") == null) {
            val now = instant()
            active.putString("writtenAt", (if (now.isAfter(expected.updatedAt)) now else expected.updatedAt.plusNanos(1)).toString())
        }
        val command = try { command() } catch (_: Exception) {
            message = "本次封面操作无法核对，请重新读取当前记录后再选择。"; conflict = true; publish(); return
        }
        active.putString("phase", SubmissionPhase.IN_FLIGHT.name); message = null; publish()
        val operation = command.operationId
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.commit(command) }
                if (!isSame(operation)) return@launch
                when (result) {
                    is MediaResult.Success -> {
                        active.putString("phase", SubmissionPhase.COMMITTED.name); publish(); readCommitted(); cleanupAfterWrite()
                    }
                    is MediaResult.Failure -> {
                        active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                        conflict = result.kind in setOf(MediaFailureKind.CONFLICT, MediaFailureKind.NOT_FOUND,
                            MediaFailureKind.PATH, MediaFailureKind.INVALID_IMAGE, MediaFailureKind.UNSUPPORTED, MediaFailureKind.TOO_LARGE)
                        message = if (conflict) "本次图片或记录已无法按原选择安全保存。请重新读取当前记录并选择；已有封面信息仍保留。"
                            else "封面保存结果尚未确认，当前输入已保留，请核对并重试同一保存。"
                        publish()
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (isSame(operation)) {
                    active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                    message = "封面保存结果尚未确认，请核对并重试同一保存，不必重新选择图片。"; publish()
                }
            }
        }
    }

    fun retryRead() {
        when {
            phase() == SubmissionPhase.COMMITTED -> readCommitted()
            conflict -> resetToCurrentBaseline()
            baseline() == null -> loadBaseline()
            phase() == SubmissionPhase.IDLE -> resetToCurrentBaseline()
        }
    }

    fun consumeCommitted() {
        if (phase() != SubmissionPhase.COMMITTED || state.value.committedTarget == null) return
        clearForm()
    }
    fun discardForm() {
        if (leaveDecision() == LeaveDecision.BLOCK_PENDING) return
        clearForm()
    }
    fun leaveDecision(): LeaveDecision = when {
        preparing || phase() in listOf(SubmissionPhase.IN_FLIGHT, SubmissionPhase.UNCONFIRMED) -> LeaveDecision.BLOCK_PENDING
        form?.getBoolean("dirty") == true && phase() != SubmissionPhase.COMMITTED -> LeaveDecision.CONFIRM_DISCARD
        else -> LeaveDecision.ALLOW
    }

    private fun clearForm() {
        form?.getBundle("asset")?.getString("id")?.let(::release)
        form = null; readVersion += 1; loading = false; preparing = false; conflict = false; missing = false; message = null
        savedState[FORM_KEY] = null; mutableState.value = CoverUiState()
    }

    private fun resetToCurrentBaseline() {
        val active = form ?: return
        active.getBundle("asset")?.getString("id")?.let(::release)
        active.remove("asset"); active.remove("pendingAssetId"); active.remove("baseline"); active.remove("writtenAt")
        active.putString("operationId", UUID.randomUUID().toString()); active.putString("phase", SubmissionPhase.IDLE.name)
        active.putBoolean("dirty", false); active.putBoolean("remove", false); conflict = false; missing = false
        mutableState.value = state.value.copy(committedTarget = null); loadBaseline()
    }

    private fun loadBaseline() {
        val active = form ?: return
        val operation = active.getString("operationId")!!; val target = target(active)
        val version = ++readVersion; loading = true; message = null; publish()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.loadTarget(target) }
                if (!isSame(operation) || version != readVersion) return@launch
                loading = false
                when (result) {
                    is MediaResult.Success -> {
                        if (result.value.target != target) { message = "读取结果与当前记录不一致，请重试读取。" }
                        else { active.putBundle("baseline", CoverSavedState.encodeBaseline(result.value)); missing = false }
                    }
                    is MediaResult.Failure -> { missing = result.kind == MediaFailureKind.NOT_FOUND; message = "当前记录暂时无法读取，请重新读取。" }
                }
                publish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (isSame(operation) && version == readVersion) { loading = false; message = "当前记录暂时无法读取，请重新读取。"; publish() }
            }
        }
    }

    private fun restorePrepared() {
        val active = form ?: return
        val operation = active.getString("operationId")!!
        val assetId = active.getBundle("asset")?.getString("id") ?: active.getString("pendingAssetId") ?: return
        preparing = true; publish()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.restorePrepared(assetId) }
                if (!isSame(operation)) return@launch
                preparing = false; active.remove("pendingAssetId")
                when (result) {
                    is MediaResult.Success -> {
                        active.putBundle("asset", CoverSavedState.encodeAsset(result.value.asset)); active.putBoolean("dirty", true)
                        message = "已恢复本地准备的封面，请核对后保存。"
                    }
                    is MediaResult.Failure -> {
                        active.remove("asset"); active.putBoolean("dirty", active.getBoolean("remove"))
                        message = "此前选择的图片未能恢复，请重新选择；当前封面没有改变。"
                    }
                }
                publish()
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) { repository.releasePreparedLease(assetId) }
                throw cancelled
            }
            catch (_: Exception) {
                if (isSame(operation)) { preparing = false; active.remove("asset"); active.remove("pendingAssetId")
                    active.putBoolean("dirty", active.getBoolean("remove")); message = "此前选择的图片未能恢复，请重新选择。"; publish() }
            }
        }
    }

    private fun readCommitted() {
        val active = form ?: return
        val operation = active.getString("operationId")!!; val version = ++readVersion
        loading = true; publish()
        viewModelScope.launch {
            try {
                val command = command()
                val result = withContext(Dispatchers.IO) { repository.verifyCommitted(command) }
                if (!isSame(operation) || version != readVersion) return@launch
                loading = false
                when (result) {
                    is MediaResult.Success -> {
                        message = null; publish()
                        mutableState.value = state.value.copy(committedTarget = result.value.target, committedOrigin = active.getString("origin") ?: "CURRENT")
                    }
                    is MediaResult.Failure -> { message = if (result.kind == MediaFailureKind.CONFLICT)
                        "封面已保存，之后记录又有变化。请返回详情核对当前记录。"
                        else "封面已保存，暂时无法读取。请重试读取，不必重新导入。"; publish() }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (isSame(operation) && version == readVersion) { loading = false; message = "封面已保存，暂时无法读取。请重试读取，不必重新导入。"; publish() }
            }
        }
    }

    private fun command(): CoverCommand {
        val active = requireNotNull(form)
        return CoverCommand(requireNotNull(active.getString("operationId")), requireNotNull(baseline()),
            if (active.getBoolean("remove")) null else CoverSavedState.decodeAsset(requireNotNull(active.getBundle("asset"))),
            Instant.parse(requireNotNull(active.getString("writtenAt"))))
    }

    private fun cleanupAfterWrite() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.removeUnreferenced() }
                if (form != null && (result is MediaResult.Failure || result is MediaResult.Success && result.value.failures.isNotEmpty())) {
                    message = "封面已保存；部分未使用资源暂未清理，可在之后安全重试。"; publish()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (form != null) { message = "封面已保存；未使用资源暂未清理，可稍后重试。"; publish() } }
        }
    }
    private fun release(assetId: String) {
        viewModelScope.launch {
            try {
                val result = withContext(NonCancellable + Dispatchers.IO) { repository.discardPrepared(assetId) }
                if (result is MediaResult.Failure && form != null) { message = "临时图片暂未清理，已保存封面仍保留，可稍后重试。"; publish() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (form != null) { message = "临时图片暂未清理，已保存封面仍保留，可稍后重试。"; publish() } }
        }
    }
    private fun baseline(): CoverBaseline? = form?.getBundle("baseline")?.let {
        try { CoverSavedState.decodeBaseline(it) } catch (_: Exception) { null }
    }
    private fun canChange() = phase() == SubmissionPhase.IDLE && !loading && !preparing && !conflict && !missing
    private fun phase(): SubmissionPhase = form?.getString("phase")?.let { value ->
        SubmissionPhase.entries.firstOrNull { it.name == value }
    } ?: SubmissionPhase.IDLE
    private fun isSame(operation: String) = form?.getString("operationId") == operation
    private fun target(active: Bundle) = CoverTarget(CoverTargetType.valueOf(requireNotNull(active.getString("targetType"))), requireNotNull(active.getString("targetId")))
    private fun validIdentity(active: Bundle) = try {
        StoredValueValidation.isUuid(target(active).id) && StoredValueValidation.isUuid(active.getString("operationId") ?: "") &&
            active.getString("origin") in listOf("CURRENT", "HISTORY") && SubmissionPhase.entries.any { it.name == active.getString("phase") }
    } catch (_: Exception) { false }
    private fun publish() {
        val active = form ?: return
        val expected = baseline()
        val asset = active.getBundle("asset")?.let { try { CoverSavedState.decodeAsset(it) } catch (_: Exception) { null } }
        savedState[FORM_KEY] = Bundle(active)
        mutableState.value = state.value.copy(editor = CoverEditorUiState(target(active), active.getString("origin") ?: "CURRENT",
            expected?.name ?: "", expected?.coverAssetId, asset, active.getBoolean("remove"), loading, preparing,
            phase(), active.getBoolean("dirty"), message, conflict, missing))
    }
    private fun imageError(kind: MediaFailureKind) = when (kind) {
        MediaFailureKind.TOO_LARGE -> "图片超过安全大小限制，请选择较小的图片；当前封面没有改变。"
        MediaFailureKind.INVALID_IMAGE, MediaFailureKind.UNSUPPORTED -> "图片损坏或格式不受支持，请选择静态JPEG或PNG；当前封面没有改变。"
        else -> "图片暂时无法导入，可能已无法读取，请重新选择；当前封面没有改变。"
    }
    private companion object { const val FORM_KEY = "lithic.media.activeForm" }
}
