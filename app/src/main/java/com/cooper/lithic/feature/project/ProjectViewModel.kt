package com.cooper.lithic.feature.project

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.MoneyParseError
import com.cooper.lithic.core.model.MoneyParseResult
import com.cooper.lithic.core.model.MoneyParser
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.repository.AddProjectMoneyEventCommand
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectCursor
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.StorageFailureKind
import com.cooper.lithic.data.repository.UpdateProjectCommand
import com.cooper.lithic.data.repository.EndProjectCommand
import com.cooper.lithic.data.repository.UpdateEndCommand
import com.cooper.lithic.data.repository.CorrectMoneyEventCommand
import com.cooper.lithic.data.repository.DeleteProjectCommand
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One task's form and derived reads; persistent storage belongs to AppContainer. */
class ProjectViewModel(
    private val repository: ProjectRepository,
    private val savedState: SavedStateHandle,
    private val clock: BusinessClock,
    private val instant: () -> Instant = Instant::now,
) : ViewModel() {
    private var form = savedState.get<Bundle>(FORM_KEY)?.let(::Bundle)
    private var categories: List<CategoryEntity> = emptyList()
    private var errors: Map<String, String> = emptyMap()
    private var message: String? = null
    private var loadingEditor = false
    private var listRequested = false
    private var historyRequested = false
    private var detailId: String? = null
    private var detailVersion = 0L
    private var loadedPages = 1
    private var cursor: ProjectCursor? = null
    private var loadedHistoryPages = 1
    private var historyCursor: ProjectCursor? = null
    private val reads = Mutex()
    private val mutableState = MutableStateFlow(ProjectUiState())
    val state: StateFlow<ProjectUiState> = mutableState.asStateFlow()

    init {
        form?.let { active ->
            if (!validIdentity(active)) form = null
            else if (active.getString("phase") == SubmissionPhase.IN_FLIGHT.name) active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
        }
        if (phase() == SubmissionPhase.COMMITTED) {
            mutableState.value = if (form?.getString("kind") == "delete") state.value.copy(deletedProjectId = form?.getString("projectId"))
            else state.value.copy(committedProjectId = form?.getString("projectId"))
        }
        publishForm()
        if (form?.getString("kind") in listOf("create", "edit")) loadCategories()
        form?.takeIf(::needsInitialRead)?.let(::loadInitialForm)
        viewModelScope.launch {
            try { repository.changes.collectLatest { refreshReads() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.value = state.value.copy(
                    list = state.value.list.copy(loading = false, loadingMore = false, error = "本地记录暂时无法刷新，请重试读取。"),
                    history = state.value.history.copy(loading = false, loadingMore = false, error = "历史档案暂时无法刷新，请重试读取。"),
                    detail = state.value.detail.copy(loading = false, error = "本地记录暂时无法刷新，请重试读取。"))
            }
        }
    }

    fun loadList() { listRequested = true; refresh() }
    fun loadHistory() { historyRequested = true; refresh() }
    fun loadMoreHistory() {
        if (!historyRequested || state.value.history.loading || state.value.history.loadingMore || historyCursor == null) return
        mutableState.value = state.value.copy(history = state.value.history.copy(loadingMore = true))
        loadedHistoryPages += 1
        refresh()
    }
    fun startEnd(projectId: String) = startLifecycle("end", projectId)
    fun startUpdateEnd(projectId: String) = startLifecycle("updateEnd", projectId)
    fun startCorrectMoney(projectId: String, eventId: String) {
        if (!StoredValueValidation.isUuid(eventId)) return
        startLifecycle("correctMoney", projectId, eventId)
    }
    fun startDelete(projectId: String) = startLifecycle("delete", projectId)
    private fun startLifecycle(kind: String, projectId: String, eventId: String? = null) {
        if (form != null || !StoredValueValidation.isUuid(projectId)) return
        form = newForm(kind, projectId).apply { eventId?.let { putString("eventId", it) } }
        loadInitialForm(requireNotNull(form))
    }
    fun changeEndedOn(value: String) {
        if (!canChange()) return
        if (form?.getString("kind") == "end" && form?.getBoolean("recoveryDateExplicit") != true) form?.putString("recoveryOn", value)
        change("endedOn", value)
    }
    fun changeEndReason(value: String) = change("endReason", value)
    fun changeRecordRecovery(value: Boolean) {
        val active = form ?: return
        if (!canChange() || active.getString("kind") != "end" || active.getBoolean("recordRecovery") == value) return
        active.putBoolean("recordRecovery", value)
        active.putBoolean("dirty", true)
        errors = errors - listOf("recoveryAmount", "recoveryOn")
        message = null
        publishForm()
    }
    fun changeRecoveryAmount(value: String) {
        if (form?.getString("kind") == "updateEnd" && form?.getBundle("settlementBaseline") == null) return
        change("recoveryAmount", value)
    }
    fun changeRecoveryOn(value: String) {
        if (!canChange() || (form?.getString("kind") == "updateEnd" && form?.getBundle("settlementBaseline") == null)) return
        form?.putBoolean("recoveryDateExplicit", true)
        change("recoveryOn", value)
    }
    fun changeRecoveryDescription(value: String) {
        if (form?.getString("kind") == "updateEnd" && form?.getBundle("settlementBaseline") == null) return
        change("recoveryDescription", value)
    }
    fun loadMore() {
        if (!listRequested || state.value.list.loading || state.value.list.loadingMore || cursor == null) return
        mutableState.value = state.value.copy(list = state.value.list.copy(loadingMore = true))
        loadedPages += 1
        refresh()
    }
    fun openDetail(projectId: String) {
        detailVersion += 1
        if (!StoredValueValidation.isUuid(projectId)) {
            detailId = null
            mutableState.value = state.value.copy(detail = ProjectDetailUiState(loading = false, error = "项目身份无效，请返回列表。"))
            return
        }
        detailId = projectId
        mutableState.value = state.value.copy(detail = ProjectDetailUiState())
        refresh()
    }
    fun startCreate() {
        if (form != null) return
        form = newForm("create", UUID.randomUUID().toString()).apply {
            putString("startedOn", clock.today().toString())
            putString("initialPaidOn", clock.today().toString())
            putString("categoryId", LithicDatabase.PROJECT_FALLBACK_ID)
        }
        publishForm()
        loadCategories()
    }
    fun startEdit(projectId: String) {
        if (form != null || !StoredValueValidation.isUuid(projectId)) return
        form = newForm("edit", projectId)
        loadCategories()
        loadInitialForm(requireNotNull(form))
    }

    /** Restore only an unsubmitted initial read; submitted baselines remain fixed. */
    private fun loadInitialForm(requested: Bundle) {
        if (loadingEditor || !needsInitialRead(requested)) return
        val projectId = text(requested, "projectId")
        val commandId = text(requested, "commandId")
        val kind = text(requested, "kind")
        val eventId = text(requested, "eventId")
        fun activeRequest(): Boolean = form?.getString("projectId") == projectId &&
            form?.getString("commandId") == commandId && form?.getString("kind") == kind &&
            form?.getString("eventId") == eventId && form?.let(::needsInitialRead) == true
        loadingEditor = true
        publishForm()
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { repository.loadProjectDetail(projectId) }
                if (!activeRequest()) return@launch
                val active = form ?: return@launch
                if (detail == null) message = "项目不存在，请返回列表。"
                else hydrate(active, detail, preserveRaw = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (activeRequest()) message = "项目资料暂时无法读取，输入不会被替换。" }
            finally {
                if (form?.getString("projectId") == projectId && form?.getString("commandId") == commandId && form?.getString("kind") == kind && form?.getString("eventId") == eventId) {
                    loadingEditor = false
                    publishForm()
                }
            }
        }
    }

    private fun needsInitialRead(active: Bundle): Boolean = active.getString("kind") in listOf("edit", "end", "updateEnd", "correctMoney", "delete") &&
        active.getBundle("baseline") == null && active.getBundle("submitted") == null && active.getString("phase") == SubmissionPhase.IDLE.name

    private fun hydrate(active: Bundle, detail: ProjectDetail, preserveRaw: Boolean) {
        check(detail.project.id == text(active, "projectId")) { "The read identity changed" }
        fun raw(key: String, value: String) { if (!preserveRaw || !active.containsKey(key)) active.putString(key, value) }
        active.putBundle("baseline", encodeProject(detail.project))
        active.putString("projectName", detail.project.name)
        active.remove("invalidBaseline")
        message = null
        when (active.getString("kind")) {
            "edit" -> {
                raw("name", detail.project.name); raw("startedOn", detail.project.startedOn.toString())
                raw("categoryId", detail.project.categoryId); raw("note", detail.project.note.orEmpty())
            }
            "end", "updateEnd" -> {
                val correcting = active.getString("kind") == "updateEnd"
                val settlements = detail.events.filter { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
                val invalid = when {
                    settlements.size > 1 -> "关联结束收回存在冲突，请核对记录，应用不会猜选一条。"
                    !correcting && detail.project.endedOn != null -> "项目已经有结束记录，请使用修正结束。"
                    correcting && detail.project.endedOn == null -> "项目尚无结束记录，无法修正结束。"
                    else -> null
                }
                active.remove("settlementBaseline")
                settlements.singleOrNull()?.let { active.putBundle("settlementBaseline", encodeEvent(it)) }
                raw("endedOn", if (correcting) detail.project.endedOn.toString() else clock.today().toString())
                raw("endReason", if (correcting) detail.project.endReason.orEmpty() else "")
                val settlement = settlements.singleOrNull()
                if (!preserveRaw || !active.containsKey("recordRecovery")) active.putBoolean("recordRecovery", correcting && settlement != null)
                raw("recoveryAmount", settlement?.amountCents?.let(::amountText).orEmpty())
                raw("recoveryOn", settlement?.occurredOn?.toString() ?: text(active, "endedOn"))
                raw("recoveryDescription", settlement?.description.orEmpty())
                invalid?.let { active.putString("invalidBaseline", it); message = it }
            }
            "correctMoney" -> {
                val event = detail.events.singleOrNull { it.id == text(active, "eventId") }
                active.remove("eventBaseline")
                if (event == null) {
                    message = "金额记录不存在，无法纠错。"
                    active.putString("invalidBaseline", message)
                } else if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) {
                    message = "关联结束收回请通过修正结束管理。"
                    active.putString("invalidBaseline", message)
                } else {
                    active.putBundle("eventBaseline", encodeEvent(event))
                    active.putString("direction", event.direction.name)
                    active.putString("role", event.role.name)
                    raw("amount", amountText(event.amountCents)); raw("occurredOn", event.occurredOn.toString())
                    raw("description", event.description.orEmpty())
                }
            }
            "delete" -> active.putParcelableArrayList("eventsBaseline", ArrayList(detail.events.map(::encodeEvent)))
        }
    }

    fun startMoney(projectId: String, direction: MoneyDirection) {
        if (form != null || !StoredValueValidation.isUuid(projectId)) return
        form = newForm("money", projectId).apply {
            putString("direction", direction.name)
            putString("occurredOn", clock.today().toString())
            putString("projectName", state.value.detail.detail?.takeIf { it.project.id == projectId }?.project?.name.orEmpty())
        }
        publishForm()
    }
    fun changeName(value: String) = change("name", value)
    fun changeStartedOn(value: String) {
        if (!canChange()) return
        if (form?.getString("kind") == "create" && form?.getBoolean("paidExplicit") != true) form?.putString("initialPaidOn", value)
        change("startedOn", value)
    }
    fun changeInitialAmount(value: String) = change("initialAmount", value)
    fun changeInitialPaidOn(value: String) {
        if (!canChange()) return
        form?.putBoolean("paidExplicit", true)
        change("initialPaidOn", value)
    }
    fun changeCategory(value: String) = change("categoryId", value)
    fun changeNote(value: String) = change("note", value)
    fun changeDirection(value: MoneyDirection) {
        if (form?.getString("kind") == "money") change("direction", value.name)
    }
    fun changeAmount(value: String) = change("amount", value)
    fun changeOccurredOn(value: String) = change("occurredOn", value)
    fun changeDescription(value: String) = change("description", value)

    fun save() {
        val active = form ?: return
        if (phase() == SubmissionPhase.IN_FLIGHT || phase() == SubmissionPhase.COMMITTED || loadingEditor) return
        if (active.getBoolean("verifiedMissing")) return
        if (active.getBoolean("conflict")) return
        if (active.getBundle("submitted") == null) {
            errors = validate(active)
            if (errors.isNotEmpty()) { publishForm(); return }
            try {
                val submitted = Bundle(active)
                val baseline = submitted.getBundle("baseline")?.let(::decodeProject)
                val now = instant()
                val targetTime = if (baseline != null && !now.isAfter(baseline.updatedAt)) baseline.updatedAt.plusNanos(1) else now
                submitted.putString("writtenAt", targetTime.toString())
                active.putBundle("submitted", submitted)
            } catch (_: Exception) { message = "项目版本时间无法安全更新，请核对当前记录。"; publishForm(); return }
        }
        active.putString("phase", SubmissionPhase.IN_FLIGHT.name)
        message = null
        publishForm() // Freeze and save the exact envelope before starting the transaction.
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { applyEnvelope(requireNotNull(active.getBundle("submitted"))) }
                if (form !== active) return@launch
                if (!matchesEnvelope(result, requireNotNull(active.getBundle("submitted")))) {
                    active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                    message = "返回的提交身份无法核对，原始输入及提交已保留，请重试同一提交。"
                    publishForm()
                    return@launch
                }
                when (result) {
                    is MutationResult.Success -> committed(result.projectId)
                    is MutationResult.ProjectUpdated -> committed(result.projectId)
                    is MutationResult.LifecycleApplied -> committed(result.projectId)
                    is MutationResult.MoneyCorrected -> committed(result.projectId)
                    is MutationResult.ProjectDeleted -> deleted(result.projectId, absent = false)
                    is MutationResult.ProjectAbsent -> deleted(result.projectId, absent = true)
                    is MutationResult.ValidationFailure -> {
                        active.remove("submitted")
                        active.putString("phase", SubmissionPhase.IDLE.name)
                        message = "记录未保存，请核对当前资料、金额、日期及所属类别。"
                        publishForm()
                    }
                    is MutationResult.Conflict -> {
                        active.putBoolean("conflict", true)
                        active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                        message = "记录版本或提交内容发生冲突，请读取当前记录核对。应用不会覆盖已有修改。"
                        publishForm()
                    }
                    is MutationResult.StorageFailure -> {
                        active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                        message = when (result.kind) {
                            StorageFailureKind.DISK_FULL -> "设备空间不足，输入已保留。腾出空间后核对并重试原提交。"
                            StorageFailureKind.CORRUPT -> "本地数据异常，输入已保留。请不要清除应用数据。"
                            StorageFailureKind.DATABASE -> "暂时无法确认保存结果，输入已保留。请核对并重试原提交。"
                        }
                        publishForm()
                    }
                }
            } catch (cancelled: CancellationException) {
                if (active.getString("phase") != SubmissionPhase.COMMITTED.name) {
                    active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                    publishForm()
                }
                throw cancelled
            } catch (_: Exception) {
                if (active.getString("phase") != SubmissionPhase.COMMITTED.name) {
                    active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                    message = "暂时无法确认保存结果，请核对并重试原提交。"
                    publishForm()
                }
            }
        }
    }
    /** Lifecycle/read invalidation refresh never acknowledges or replaces a pending command. */
    fun refresh() {
        publishForm()
        form?.takeIf(::needsInitialRead)?.let(::loadInitialForm)
        viewModelScope.launch { refreshReads() }
    }

    /** The editor's explicit conflict-resolution button is the only rebase entry. */
    fun retryRead() {
        publishForm() // Refresh business-today labels while keeping all raw input and the envelope.
        val active = form
        if (active != null && needsInitialRead(active)) { loadInitialForm(active); return }
        if (active?.getBoolean("conflict") == true && active.getString("kind") in listOf("edit", "end", "updateEnd", "correctMoney", "delete")) {
            if (loadingEditor) return
            val projectId = text(active, "projectId")
            val commandId = text(active, "commandId")
            val kind = text(active, "kind")
            val eventId = text(active, "eventId")
            fun sameRequest(): Boolean = form === active && text(active, "projectId") == projectId && text(active, "commandId") == commandId &&
                text(active, "kind") == kind && text(active, "eventId") == eventId
            loadingEditor = true
            publishForm()
            viewModelScope.launch {
                try {
                    val current = withContext(Dispatchers.IO) { repository.loadProjectDetail(projectId) }
                    if (!sameRequest()) return@launch
                    if (current == null) {
                        if (kind == "delete") {
                            // A detail read proves no project, but only the writer also checks orphaned events.
                            val envelope = requireNotNull(active.getBundle("submitted"))
                            val result = withContext(Dispatchers.IO) { applyEnvelope(envelope) }
                            if (!sameRequest()) return@launch
                            when {
                                result is MutationResult.ProjectAbsent && matchesEnvelope(result, envelope) -> deleted(projectId, absent = true)
                                result is MutationResult.ProjectDeleted && matchesEnvelope(result, envelope) -> deleted(projectId, absent = false)
                                else -> message = "当前删除结果仍无法完整核对，原始确认与提交已保留，请重试读取。"
                            }
                        } else {
                            active.putBoolean("verifiedMissing", true)
                            message = "已核对当前项目不存在，无法继续保存。原始输入已保留，可以返回当前记录列表。"
                            if (detailId == projectId) {
                                detailVersion += 1
                                mutableState.value = state.value.copy(detail = ProjectDetailUiState(loading = false, missing = true))
                            }
                        }
                    }
                    else {
                        hydrate(active, current, preserveRaw = false)
                        if (active.getString("invalidBaseline") != null) return@launch
                        active.remove("submitted")
                        active.putString("commandId", UUID.randomUUID().toString())
                        active.putString("phase", SubmissionPhase.IDLE.name)
                        active.putBoolean("conflict", false)
                        active.putBoolean("dirty", false)
                        active.remove("verifiedMissing")
                        errors = emptyMap()
                        message = if (kind == "delete") "已读取当前项目及全部金额记录，请重新确认是否删除。" else "已读取当前资料，请重新确认修改后保存。"
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { if (sameRequest()) message = "当前记录暂时无法读取，原始提交与输入仍然保留。" }
                finally { if (form === active) { loadingEditor = false; publishForm() } }
            }
            return
        }
        refresh()
    }
    fun leaveDecision(): LeaveDecision {
        if (form?.getBoolean("verifiedMissing") == true) return LeaveDecision.ALLOW
        return when (phase()) {
            SubmissionPhase.IN_FLIGHT, SubmissionPhase.UNCONFIRMED -> LeaveDecision.BLOCK_PENDING
            SubmissionPhase.COMMITTED -> LeaveDecision.ALLOW
            SubmissionPhase.IDLE -> if (form?.getBoolean("dirty") == true) LeaveDecision.CONFIRM_DISCARD else LeaveDecision.ALLOW
        }
    }
    fun discardForm() {
        if (leaveDecision() == LeaveDecision.BLOCK_PENDING) return
        form = null
        loadingEditor = false
        errors = emptyMap()
        message = null
        publishForm()
    }
    fun consumeCommitted() {
        if (phase() != SubmissionPhase.COMMITTED) return
        form = null
        errors = emptyMap()
        message = null
        mutableState.value = state.value.copy(committedProjectId = null)
        publishForm()
    }
    fun consumeDeleted() {
        if (phase() != SubmissionPhase.COMMITTED || form?.getString("kind") != "delete") return
        form = null
        loadingEditor = false
        errors = emptyMap()
        message = null
        mutableState.value = state.value.copy(deletedProjectId = null)
        publishForm()
    }
    private suspend fun applyEnvelope(envelope: Bundle): MutationResult {
        val id = text(envelope, "projectId")
        val commandId = text(envelope, "commandId")
        val writtenAt = Instant.parse(text(envelope, "writtenAt"))
        return when (envelope.getString("kind")) {
            "create" -> {
                val project = LongTermProjectEntity(id, text(envelope, "name").trim(), text(envelope, "categoryId"), LocalDate.parse(text(envelope, "startedOn").trim()),
                    note = nullableText(envelope, "note"), createdAt = writtenAt, updatedAt = writtenAt)
                val event = ProjectMoneyEventEntity(text(envelope, "eventId"), id, MoneyDirection.OUTLAY, money(envelope, "initialAmount"),
                    LocalDate.parse(text(envelope, "initialPaidOn").trim()), role = ProjectMoneyEventRole.INITIAL, operationId = commandId)
                repository.createProject(CreateProjectCommand(commandId, project, event))
            }
            "edit" -> repository.updateProject(UpdateProjectCommand(commandId, decodeProject(requireNotNull(envelope.getBundle("baseline"))),
                text(envelope, "name").trim(), LocalDate.parse(text(envelope, "startedOn").trim()), text(envelope, "categoryId"), nullableText(envelope, "note"), writtenAt))
            "money" -> repository.appendMoneyEvent(AddProjectMoneyEventCommand(commandId,
                ProjectMoneyEventEntity(text(envelope, "eventId"), id, MoneyDirection.valueOf(text(envelope, "direction")), money(envelope, "amount"),
                    LocalDate.parse(text(envelope, "occurredOn").trim()), nullableText(envelope, "description"), ProjectMoneyEventRole.REGULAR, commandId)))
            "end" -> repository.endProject(EndProjectCommand(commandId, decodeProject(requireNotNull(envelope.getBundle("baseline"))),
                LocalDate.parse(text(envelope, "endedOn").trim()), text(envelope, "endReason").trim(),
                if (envelope.getBoolean("recordRecovery")) ProjectMoneyEventEntity(text(envelope, "eventId"), id, MoneyDirection.RECOVERY,
                    money(envelope, "recoveryAmount"), LocalDate.parse(text(envelope, "recoveryOn").trim()), nullableText(envelope, "recoveryDescription"),
                    ProjectMoneyEventRole.END_SETTLEMENT, commandId) else null, writtenAt))
            "updateEnd" -> {
                val settlement = envelope.getBundle("settlementBaseline")?.let(::decodeEvent)
                repository.updateEnd(UpdateEndCommand(commandId, decodeProject(requireNotNull(envelope.getBundle("baseline"))), settlement,
                    LocalDate.parse(text(envelope, "endedOn").trim()), text(envelope, "endReason").trim(),
                    if (settlement != null) money(envelope, "recoveryAmount") else null,
                    if (settlement != null) LocalDate.parse(text(envelope, "recoveryOn").trim()) else null,
                    if (settlement != null) nullableText(envelope, "recoveryDescription") else null, writtenAt))
            }
            "correctMoney" -> repository.correctMoneyEvent(CorrectMoneyEventCommand(commandId, decodeProject(requireNotNull(envelope.getBundle("baseline"))),
                decodeEvent(requireNotNull(envelope.getBundle("eventBaseline"))), money(envelope, "amount"),
                LocalDate.parse(text(envelope, "occurredOn").trim()), nullableText(envelope, "description"), writtenAt))
            "delete" -> repository.deleteProject(DeleteProjectCommand(commandId, decodeProject(requireNotNull(envelope.getBundle("baseline"))), decodeEvents(envelope)))
            else -> MutationResult.ValidationFailure("Unknown form")
        }
    }
    private fun matchesEnvelope(result: MutationResult, envelope: Bundle): Boolean {
        val id = text(envelope, "projectId")
        val kind = text(envelope, "kind")
        return when (result) {
            is MutationResult.Success -> kind in listOf("create", "money") && result.projectId == id && result.eventId == text(envelope, "eventId")
            is MutationResult.ProjectUpdated -> kind == "edit" && result.projectId == id
            is MutationResult.LifecycleApplied -> kind in listOf("end", "updateEnd") && result.projectId == id && result.settlementEventId ==
                if (kind == "updateEnd") envelope.getBundle("settlementBaseline")?.getString("id")
                else if (envelope.getBoolean("recordRecovery")) text(envelope, "eventId") else null
            is MutationResult.MoneyCorrected -> kind == "correctMoney" && result.projectId == id && result.eventId == text(envelope, "eventId")
            is MutationResult.ProjectDeleted -> kind == "delete" && result.projectId == id
            is MutationResult.ProjectAbsent -> kind == "delete" && result.projectId == id
            is MutationResult.ValidationFailure, is MutationResult.Conflict, is MutationResult.StorageFailure -> true
        }
    }
    private suspend fun committed(projectId: String) {
        savedState["lithic.project.lastConfirmedId"] = projectId
        form?.putString("phase", SubmissionPhase.COMMITTED.name)
        message = null
        mutableState.value = state.value.copy(committedProjectId = projectId)
        publishForm()
        refreshReads()
    }
    private suspend fun deleted(projectId: String, absent: Boolean) {
        form?.putString("phase", SubmissionPhase.COMMITTED.name)
        form?.putBoolean("deleteAbsent", absent)
        message = if (absent) "已核对项目及关联金额记录当前不存在。没有持久删除日志，无法证明是哪次操作删除。" else "项目及其金额记录已删除。"
        if (detailId == projectId) {
            detailVersion += 1
            mutableState.value = state.value.copy(detail = ProjectDetailUiState(loading = false, missing = true))
        }
        mutableState.value = state.value.copy(deletedProjectId = projectId, committedProjectId = null)
        publishForm()
        refreshReads()
    }
    private suspend fun refreshReads() = reads.withLock {
        if (listRequested) {
            try {
                val page = withContext(Dispatchers.IO) { repository.loadCurrentProjectWindow(loadedPages, clock.today()) }
                cursor = page.nextCursor
                mutableState.value = state.value.copy(list = ProjectListUiState(page.items, page.totalCount, loading = false, hasMore = page.nextCursor != null))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(list = state.value.list.copy(loading = false, loadingMore = false, error = "长期项目暂时无法读取，当前记录不会被当作空账本。")) }
        }
        if (historyRequested) {
            try {
                val page = withContext(Dispatchers.IO) { repository.loadProjectHistoryWindow(loadedHistoryPages, clock.today()) }
                historyCursor = page.nextCursor
                mutableState.value = state.value.copy(history = ProjectListUiState(page.items, page.totalCount, loading = false, hasMore = page.nextCursor != null))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(history = state.value.history.copy(loading = false, loadingMore = false, error = "历史档案暂时无法读取，当前记录不会被当作空档案。")) }
        }
        detailId?.let { id ->
            val version = detailVersion
            try {
                val detail = withContext(Dispatchers.IO) { repository.loadProjectDetail(id, clock.today()) }
                if (detailId == id && detailVersion == version) {
                    mutableState.value = state.value.copy(detail = ProjectDetailUiState(detail, loading = false, missing = detail == null))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                val error = if (savedState.get<String>("lithic.project.lastConfirmedId") == id) "已保存，暂时无法读取。请重试读取，不必重新录入。" else "项目暂时无法读取，请重试读取。"
                if (detailId == id && detailVersion == version) {
                    mutableState.value = state.value.copy(detail = state.value.detail.copy(loading = false, error = error, missing = false))
                }
            }
        }
    }
    private fun loadCategories() = viewModelScope.launch {
        try { categories = withContext(Dispatchers.IO) { repository.loadProjectCategories() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message = "类别暂时无法读取，输入已保留。" }
        publishForm()
    }
    private fun newForm(kind: String, id: String): Bundle = Bundle().apply {
        putString("kind", kind); putString("projectId", id)
        putString("commandId", UUID.randomUUID().toString()); putString("eventId", UUID.randomUUID().toString())
        putString("phase", SubmissionPhase.IDLE.name)
    }
    private fun phase(): SubmissionPhase = form?.getString("phase")?.let { value -> SubmissionPhase.entries.firstOrNull { it.name == value } } ?: SubmissionPhase.IDLE
    private fun canChange(): Boolean = form != null && phase() == SubmissionPhase.IDLE && !loadingEditor && form?.getBoolean("verifiedMissing") != true
    private fun change(key: String, value: String) {
        if (!canChange() || form?.getString(key).orEmpty() == value) return
        form?.putString(key, value)
        form?.putBoolean("dirty", true)
        errors = errors - key
        message = null
        publishForm()
    }
    private fun publishForm() {
        savedState[FORM_KEY] = form?.let(::Bundle)
        val active = form
        val base = state.value.copy(editor = null, moneyEditor = null, endEditor = null, moneyCorrection = null, deleteConfirmation = null)
        if (active == null) { mutableState.value = base; return }
        if (message == null && active.getBoolean("verifiedMissing")) message = "已核对当前项目不存在，无法继续保存。原始输入已保留，可以返回当前记录列表。"
        var next = base
        if (active.getString("kind") == "money") {
            next = base.copy(moneyEditor = MoneyEventEditorUiState(text(active, "projectId"), text(active, "projectName"),
                MoneyDirection.entries.firstOrNull { it.name == text(active, "direction") } ?: MoneyDirection.OUTLAY, text(active, "amount"), text(active, "occurredOn"),
                text(active, "description"), clock.today(), phase(), active.getBoolean("dirty"), errors, message))
        } else if (active.getString("kind") in listOf("create", "edit")) {
            next = base.copy(editor = ProjectEditorUiState(
                if (active.getString("kind") == "edit") ProjectEditorMode.EDIT else ProjectEditorMode.CREATE,
                text(active, "projectId"), text(active, "name"), text(active, "startedOn"), text(active, "initialAmount"), text(active, "initialPaidOn"),
                text(active, "categoryId"), text(active, "note"), categories, clock.today(), phase(), active.getBoolean("dirty"), loadingEditor, errors, message,
                active.getBoolean("conflict"), needsInitialRead(active)))
        } else if (active.getString("kind") in listOf("end", "updateEnd")) {
            next = base.copy(endEditor = EndProjectUiState(
                projectId = text(active, "projectId"), projectName = text(active, "projectName"), correcting = active.getString("kind") == "updateEnd",
                endedOn = text(active, "endedOn"), endReason = text(active, "endReason"), recordRecovery = active.getBoolean("recordRecovery"),
                recoveryAmount = text(active, "recoveryAmount"), recoveryOn = text(active, "recoveryOn"), recoveryDescription = text(active, "recoveryDescription"),
                hasSettlement = active.getBundle("settlementBaseline") != null, today = clock.today(), phase = phase(), dirty = active.getBoolean("dirty"),
                loading = loadingEditor, errors = errors, message = message, conflict = active.getBoolean("conflict"), needsInitialRead = needsInitialRead(active)))
        } else if (active.getString("kind") == "correctMoney") {
            next = base.copy(moneyCorrection = MoneyCorrectionUiState(
                projectId = text(active, "projectId"), eventId = text(active, "eventId"), projectName = text(active, "projectName"),
                direction = MoneyDirection.entries.firstOrNull { it.name == text(active, "direction") } ?: MoneyDirection.OUTLAY,
                role = ProjectMoneyEventRole.entries.firstOrNull { it.name == text(active, "role") } ?: ProjectMoneyEventRole.REGULAR,
                amount = text(active, "amount"), occurredOn = text(active, "occurredOn"), description = text(active, "description"), today = clock.today(),
                phase = phase(), dirty = active.getBoolean("dirty"), loading = loadingEditor, errors = errors, message = message,
                conflict = active.getBoolean("conflict"), needsInitialRead = needsInitialRead(active)))
        } else if (active.getString("kind") == "delete") {
            val deleteMessage = message ?: if (phase() == SubmissionPhase.COMMITTED && active.getBoolean("deleteAbsent")) "已核对项目及关联金额记录当前不存在。" else null
            next = base.copy(deleteConfirmation = DeleteProjectUiState(
                text(active, "projectId"), text(active, "projectName"), phase(), loadingEditor, deleteMessage, active.getBoolean("conflict"), needsInitialRead(active)))
        }
        mutableState.value = next
    }
    private fun validate(active: Bundle): Map<String, String> = buildMap {
        val kind = active.getString("kind")
        fun validateMoney(key: String) {
            val result = MoneyParser.parseCny(text(active, key))
            if (result is MoneyParseResult.Failure) put(key, when (result.reason) {
                MoneyParseError.EXCESS_PRECISION -> "金额最多保留两位小数。"
                MoneyParseError.OUT_OF_RANGE -> "单笔金额超过允许范围。"
                MoneyParseError.INVALID_FORMAT -> "请输入人民币金额绝对值，可填写 0。"
            })
        }
        fun validatePayment(key: String) {
            val payment = date(active, key)
            if (payment == null) put(key, "请输入有效的真实金额日期。")
            else if (payment.isAfter(clock.today())) put(key, "实际金额日期不能晚于记录时区的今天。")
        }
        if (kind in listOf("create", "edit")) {
            if (text(active, "name").isBlank()) put("name", "请输入名称。")
            if (date(active, "startedOn") == null) put("startedOn", "请输入有效日期，例如 2026-01-01。")
            if (!StoredValueValidation.isUuid(text(active, "categoryId"))) put("categoryId", "请选择长期项目类别。")
            if (kind == "edit" && active.getBundle("baseline") == null) put("name", "请先成功读取项目资料。")
        }
        if (kind in listOf("create", "money", "correctMoney")) {
            val amountKey = if (kind == "money") "amount" else "initialAmount"
            validateMoney(if (kind == "correctMoney") "amount" else amountKey)
            validatePayment(if (kind == "create") "initialPaidOn" else "occurredOn")
        }
        if (kind in listOf("end", "updateEnd")) {
            val ended = date(active, "endedOn")
            val baseline = active.getBundle("baseline")?.let(::decodeProject)
            if (ended == null) put("endedOn", "请输入有效结束日期，例如 2026-01-01。")
            else if (baseline != null && ended.isBefore(baseline.startedOn)) put("endedOn", "结束日期不能早于项目开始日期。")
            if (text(active, "endReason").isBlank()) put("endReason", "请输入真实结束原因。")
            if ((kind == "end" && active.getBoolean("recordRecovery")) || (kind == "updateEnd" && active.getBundle("settlementBaseline") != null)) {
                validateMoney("recoveryAmount"); validatePayment("recoveryOn")
            }
        }
        if (kind in listOf("end", "updateEnd", "correctMoney", "delete")) {
            if (active.getBundle("baseline") == null) put("form", "请先成功读取当前项目及金额记录。")
            active.getString("invalidBaseline")?.let { put("form", it) }
            if (kind == "correctMoney" && active.getBundle("eventBaseline") == null) put("form", "请先成功读取当前金额记录。")
            if (kind == "delete" && !active.containsKey("eventsBaseline")) put("form", "请先成功读取项目及全部金额记录。")
            if (containsKey("form")) message = get("form")
        }
    }
    private fun validIdentity(active: Bundle): Boolean = try {
        fun identifiers(envelope: Bundle): Boolean = envelope.getString("kind") in listOf("create", "edit", "money", "end", "updateEnd", "correctMoney", "delete") &&
            listOf("projectId", "commandId", "eventId").all { StoredValueValidation.isUuid(text(envelope, it)) }
        if (!identifiers(active)) false
        else {
            val id = text(active, "projectId")
            val baseline = active.getBundle("baseline")?.let(::decodeProject)
            val event = active.getBundle("eventBaseline")?.let(::decodeEvent)
            val settlement = active.getBundle("settlementBaseline")?.let(::decodeEvent)
            val submitted = active.getBundle("submitted")
            (baseline == null || baseline.id == id) && (event == null || (event.projectId == id && event.id == text(active, "eventId"))) &&
                (settlement == null || settlement.projectId == id) &&
                (!active.containsKey("eventsBaseline") || decodeEvents(active).all { it.projectId == id }) &&
                (submitted == null || (identifiers(submitted) && listOf("kind", "projectId", "commandId", "eventId").all { text(submitted, it) == text(active, it) } &&
                    submitted.getBundle("baseline")?.let(::decodeProject)?.id.let { it == null || it == id } &&
                    submitted.getBundle("eventBaseline")?.let(::decodeEvent).let { it == null || (it.projectId == id && it.id == text(active, "eventId")) } &&
                    submitted.getBundle("settlementBaseline")?.let(::decodeEvent).let { it == null || it.projectId == id } &&
                    (!submitted.containsKey("eventsBaseline") || decodeEvents(submitted).all { it.projectId == id })))
        }
    } catch (_: Exception) { false }
    private fun date(active: Bundle, key: String): LocalDate? = try { LocalDate.parse(text(active, key).trim()) } catch (_: Exception) { null }
    private fun text(active: Bundle, key: String): String = active.getString(key).orEmpty()
    private fun nullableText(active: Bundle, key: String): String? = text(active, key).trim().ifEmpty { null }
    private fun money(active: Bundle, key: String): Long = (MoneyParser.parseCny(text(active, key)) as MoneyParseResult.Success).money.cents
    private fun amountText(cents: Long): String = BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()
    private fun encodeEvent(event: ProjectMoneyEventEntity): Bundle = Bundle().apply {
        putString("id", event.id); putString("projectId", event.projectId); putString("direction", event.direction.name)
        putLong("amountCents", event.amountCents); putString("occurredOn", event.occurredOn.toString()); putString("description", event.description)
        putString("role", event.role.name); putString("operationId", event.operationId)
    }
    private fun decodeEvent(bundle: Bundle): ProjectMoneyEventEntity = ProjectMoneyEventEntity(
        text(bundle, "id"), text(bundle, "projectId"), MoneyDirection.valueOf(text(bundle, "direction")), bundle.getLong("amountCents"),
        LocalDate.parse(text(bundle, "occurredOn")), bundle.getString("description"), ProjectMoneyEventRole.valueOf(text(bundle, "role")), text(bundle, "operationId"))
    private fun decodeEvents(bundle: Bundle): List<ProjectMoneyEventEntity> = bundle.getParcelableArrayList<Bundle>("eventsBaseline").orEmpty().map(::decodeEvent)
    private fun encodeProject(project: LongTermProjectEntity): Bundle = Bundle().apply {
        putString("id", project.id); putString("name", project.name); putString("categoryId", project.categoryId)
        putString("startedOn", project.startedOn.toString()); putString("endedOn", project.endedOn?.toString()); putString("endReason", project.endReason)
        putString("coverAssetId", project.coverAssetId); putString("note", project.note); putString("createdAt", project.createdAt.toString()); putString("updatedAt", project.updatedAt.toString())
    }
    private fun decodeProject(bundle: Bundle): LongTermProjectEntity = LongTermProjectEntity(text(bundle, "id"), text(bundle, "name"), text(bundle, "categoryId"),
        LocalDate.parse(text(bundle, "startedOn")), bundle.getString("endedOn")?.let(LocalDate::parse), bundle.getString("endReason"), bundle.getString("coverAssetId"),
        bundle.getString("note"), Instant.parse(text(bundle, "createdAt")), Instant.parse(text(bundle, "updatedAt")))
    companion object { private const val FORM_KEY = "lithic.project.activeForm" }
}
