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
    private var detailId: String? = null
    private var detailVersion = 0L
    private var loadedPages = 1
    private var cursor: ProjectCursor? = null
    private val reads = Mutex()
    private val mutableState = MutableStateFlow(ProjectUiState())
    val state: StateFlow<ProjectUiState> = mutableState.asStateFlow()

    init {
        form?.let { active ->
            if (!validIdentity(active)) form = null
            else if (active.getString("phase") == SubmissionPhase.IN_FLIGHT.name) active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
        }
        if (phase() == SubmissionPhase.COMMITTED) mutableState.value = state.value.copy(committedProjectId = form?.getString("projectId"))
        publishForm()
        if (form != null) loadCategories()
        form?.takeIf(::needsInitialEditRead)?.let(::loadInitialEdit)
        viewModelScope.launch {
            try { repository.changes.collectLatest { refreshReads() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.value = state.value.copy(
                    list = state.value.list.copy(loading = false, loadingMore = false, error = "本地记录暂时无法刷新，请重试读取。"),
                    detail = state.value.detail.copy(loading = false, error = "本地记录暂时无法刷新，请重试读取。"))
            }
        }
    }

    fun loadList() { listRequested = true; refresh() }
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
        loadInitialEdit(requireNotNull(form))
    }

    /** Restore only an unsubmitted initial edit; pending submitted baselines remain fixed. */
    private fun loadInitialEdit(requested: Bundle) {
        if (loadingEditor || !needsInitialEditRead(requested)) return
        val projectId = text(requested, "projectId")
        val commandId = text(requested, "commandId")
        fun activeRequest(): Boolean = form?.getString("projectId") == projectId &&
            form?.getString("commandId") == commandId && form?.let(::needsInitialEditRead) == true
        loadingEditor = true
        publishForm()
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { repository.loadProjectDetail(projectId) }
                if (!activeRequest()) return@launch
                val active = form ?: return@launch
                if (detail == null) message = "项目不存在，请返回列表。"
                else {
                    active.putBundle("baseline", encodeProject(detail.project))
                    if (!active.containsKey("name")) active.putString("name", detail.project.name)
                    if (!active.containsKey("startedOn")) active.putString("startedOn", detail.project.startedOn.toString())
                    if (!active.containsKey("categoryId")) active.putString("categoryId", detail.project.categoryId)
                    if (!active.containsKey("note")) active.putString("note", detail.project.note.orEmpty())
                    message = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (activeRequest()) message = "项目资料暂时无法读取，输入不会被替换。" }
            finally {
                if (form?.getString("projectId") == projectId && form?.getString("commandId") == commandId) {
                    loadingEditor = false
                    publishForm()
                }
            }
        }
    }

    private fun needsInitialEditRead(active: Bundle): Boolean = active.getString("kind") == "edit" &&
        active.getBundle("baseline") == null && active.getBundle("submitted") == null && active.getString("phase") == SubmissionPhase.IDLE.name
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
    fun changeDirection(value: MoneyDirection) = change("direction", value.name)
    fun changeAmount(value: String) = change("amount", value)
    fun changeOccurredOn(value: String) = change("occurredOn", value)
    fun changeDescription(value: String) = change("description", value)

    fun save() {
        val active = form ?: return
        if (phase() == SubmissionPhase.IN_FLIGHT || phase() == SubmissionPhase.COMMITTED || loadingEditor) return
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
                when (result) {
                    is MutationResult.Success -> committed(result.projectId)
                    is MutationResult.ProjectUpdated -> committed(result.projectId)
                    is MutationResult.ValidationFailure -> {
                        active.remove("submitted")
                        active.putString("phase", SubmissionPhase.IDLE.name)
                        message = "记录未保存，请核对金额、日期及所属类别。"
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
                active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                publishForm()
                throw cancelled
            } catch (_: Exception) {
                active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
                message = "暂时无法确认保存结果，请核对并重试原提交。"
                publishForm()
            }
        }
    }
    /** Lifecycle/read invalidation refresh never acknowledges or replaces a pending command. */
    fun refresh() {
        publishForm()
        form?.takeIf(::needsInitialEditRead)?.let(::loadInitialEdit)
        viewModelScope.launch { refreshReads() }
    }

    /** The editor's explicit conflict-resolution button is the only rebase entry. */
    fun retryRead() {
        publishForm() // Refresh business-today labels while keeping all raw input and the envelope.
        val active = form
        if (active != null && needsInitialEditRead(active)) { loadInitialEdit(active); return }
        if (active?.getBoolean("conflict") == true && active.getString("kind") == "edit") {
            if (loadingEditor) return
            loadingEditor = true
            publishForm()
            viewModelScope.launch {
                try {
                    val current = withContext(Dispatchers.IO) { repository.loadProjectDetail(text(active, "projectId")) }
                    if (current == null) message = "当前项目不存在，无法重新编辑。"
                    else {
                        active.putBundle("baseline", encodeProject(current.project))
                        active.putString("name", current.project.name)
                        active.putString("startedOn", current.project.startedOn.toString())
                        active.putString("categoryId", current.project.categoryId)
                        active.putString("note", current.project.note.orEmpty())
                        active.remove("submitted")
                        active.putString("commandId", UUID.randomUUID().toString())
                        active.putString("phase", SubmissionPhase.IDLE.name)
                        active.putBoolean("conflict", false)
                        active.putBoolean("dirty", false)
                        errors = emptyMap()
                        message = "已读取当前资料，请重新确认修改后保存。"
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { message = "当前记录暂时无法读取，原始提交与输入仍然保留。" }
                finally { loadingEditor = false; publishForm() }
            }
            return
        }
        refresh()
    }
    fun leaveDecision(): LeaveDecision = when (phase()) {
        SubmissionPhase.IN_FLIGHT, SubmissionPhase.UNCONFIRMED -> LeaveDecision.BLOCK_PENDING
        SubmissionPhase.COMMITTED -> LeaveDecision.ALLOW
        SubmissionPhase.IDLE -> if (form?.getBoolean("dirty") == true) LeaveDecision.CONFIRM_DISCARD else LeaveDecision.ALLOW
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
            else -> MutationResult.ValidationFailure("Unknown form")
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
    private suspend fun refreshReads() = reads.withLock {
        if (listRequested) {
            try {
                val page = withContext(Dispatchers.IO) { repository.loadProjectWindow(loadedPages, clock.today()) }
                cursor = page.nextCursor
                mutableState.value = state.value.copy(list = ProjectListUiState(page.items, page.totalCount, loading = false, hasMore = page.nextCursor != null))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(list = state.value.list.copy(loading = false, loadingMore = false, error = "长期项目暂时无法读取，当前记录不会被当作空账本。")) }
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
    private fun canChange(): Boolean = form != null && phase() == SubmissionPhase.IDLE && !loadingEditor
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
        if (active == null) { mutableState.value = state.value.copy(editor = null, moneyEditor = null); return }
        if (active.getString("kind") == "money") {
            mutableState.value = state.value.copy(editor = null, moneyEditor = MoneyEventEditorUiState(text(active, "projectId"), text(active, "projectName"),
                MoneyDirection.entries.firstOrNull { it.name == text(active, "direction") } ?: MoneyDirection.OUTLAY, text(active, "amount"), text(active, "occurredOn"),
                text(active, "description"), clock.today(), phase(), active.getBoolean("dirty"), errors, message))
        } else {
            mutableState.value = state.value.copy(moneyEditor = null, editor = ProjectEditorUiState(
                if (active.getString("kind") == "edit") ProjectEditorMode.EDIT else ProjectEditorMode.CREATE,
                text(active, "projectId"), text(active, "name"), text(active, "startedOn"), text(active, "initialAmount"), text(active, "initialPaidOn"),
                text(active, "categoryId"), text(active, "note"), categories, clock.today(), phase(), active.getBoolean("dirty"), loadingEditor, errors, message,
                active.getBoolean("conflict"), needsInitialEditRead(active)))
        }
    }
    private fun validate(active: Bundle): Map<String, String> = buildMap {
        val kind = active.getString("kind")
        if (kind != "money") {
            if (text(active, "name").isBlank()) put("name", "请输入名称。")
            if (date(active, "startedOn") == null) put("startedOn", "请输入有效日期，例如 2026-01-01。")
            if (!StoredValueValidation.isUuid(text(active, "categoryId"))) put("categoryId", "请选择长期项目类别。")
            if (kind == "edit" && active.getBundle("baseline") == null) put("name", "请先成功读取项目资料。")
        }
        if (kind != "edit") {
            val amountKey = if (kind == "money") "amount" else "initialAmount"
            val result = MoneyParser.parseCny(text(active, amountKey))
            if (result is MoneyParseResult.Failure) put(amountKey, when (result.reason) {
                MoneyParseError.EXCESS_PRECISION -> "金额最多保留两位小数。"
                MoneyParseError.OUT_OF_RANGE -> "单笔金额超过允许范围。"
                MoneyParseError.INVALID_FORMAT -> "请输入人民币金额绝对值，可填写 0。"
            })
            val dateKey = if (kind == "money") "occurredOn" else "initialPaidOn"
            val payment = date(active, dateKey)
            if (payment == null) put(dateKey, "请输入有效的真实付款日期。")
            else if (payment.isAfter(clock.today())) put(dateKey, "实际付款日期不能晚于记录时区的今天。")
        }
    }
    private fun validIdentity(active: Bundle): Boolean = active.getString("kind") in listOf("create", "edit", "money") &&
        listOf("projectId", "commandId", "eventId").all { StoredValueValidation.isUuid(text(active, it)) }
    private fun date(active: Bundle, key: String): LocalDate? = try { LocalDate.parse(text(active, key).trim()) } catch (_: Exception) { null }
    private fun text(active: Bundle, key: String): String = active.getString(key).orEmpty()
    private fun nullableText(active: Bundle, key: String): String? = text(active, key).trim().ifEmpty { null }
    private fun money(active: Bundle, key: String): Long = (MoneyParser.parseCny(text(active, key)) as MoneyParseResult.Success).money.cents
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
