package com.cooper.lithic.feature.project

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.model.MoneyParseResult
import com.cooper.lithic.core.model.MoneyParser
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ReplacementEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.repository.LinkReplacementCommand
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.ReplacementMutationResult
import com.cooper.lithic.data.repository.ReplacementRepository
import com.cooper.lithic.data.repository.TradeInCommand
import com.cooper.lithic.data.repository.TradeInIdentities
import com.cooper.lithic.data.repository.UnlinkReplacementCommand
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A generation-scoped editor. Fixed submitted facts survive recreation and uncertain results. */
internal class ReplacementViewModel(
    private val repository: ReplacementRepository,
    private val projects: ProjectRepository,
    private val saved: SavedStateHandle,
    private val clock: BusinessClock,
    private val instant: () -> Instant = Instant::now,
) : ViewModel() {
    private var form = saved.get<Bundle>(FORM_KEY)?.let(::Bundle)
    private var gate = restoredGate(form)
    private var categories: List<CategoryEntity> = emptyList()
    private var readTicket = 0L
    private var choiceTicket = 0L
    private var choicePages = 1
    private val mutableState = MutableStateFlow(ReplacementUiState())
    val state = mutableState.asStateFlow()

    init {
        form?.let {
            it.putBoolean("loading", false)
            if (!validFormIdentity(it)) {
                gate.unknown()
                it.putString("message", "保存的替换交易身份不完整，不能执行新提交。请保留当前数据。")
                it.putBoolean("invalidIdentity", true)
            }
            publish()
            if (!it.containsKey("baseline") && !it.containsKey("submitted")) hydrate()
            if (gate.phase == SubmissionPhase.COMMITTED && text(it, "kind") == ReplacementFormKind.TRADE_IN.name) {
                mutableState.value = state.value.copy(committedProjectId = TradeInIdentities.newProjectId(text(it, "commandId")))
            }
        }
        viewModelScope.launch {
            try {
                categories = withContext(Dispatchers.IO) { projects.loadProjectCategories() }
                publish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { form?.putString("message", "类别暂时无法读取，请返回核对。") ; publish() }
        }
        viewModelScope.launch {
            try { repository.changes.collectLatest { refresh() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(loading = false, error = READ_ERROR) }
        }
    }

    fun openComparison(projectId: String) {
        if (!StoredValueValidation.isUuid(projectId)) return
        mutableState.value = state.value.copy(anchorId = projectId)
        refresh()
    }

    fun refresh() {
        val id = state.value.anchorId
        if (id.isBlank()) return
        val ticket = ++readTicket
        mutableState.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val chain = withContext(Dispatchers.IO) { repository.loadChain(id) }
                if (ticket != readTicket || state.value.anchorId != id) return@launch
                mutableState.value = state.value.copy(chain = chain, loading = false,
                    error = if (chain == null) "项目已不存在，不能显示替换链。" else null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (ticket == readTicket) mutableState.value = state.value.copy(loading = false, error = READ_ERROR) }
        }
    }

    fun startTradeIn(projectId: String) = start(ReplacementFormKind.TRADE_IN, projectId)
    fun startLink(projectId: String) {
        start(ReplacementFormKind.LINK, projectId)
        loadChoices()
    }
    fun startUnlink(projectId: String, link: ReplacementEntity) {
        start(ReplacementFormKind.UNLINK, projectId, link)
    }
    private fun start(kind: ReplacementFormKind, projectId: String, selected: ReplacementEntity? = null) {
        if (form != null || !StoredValueValidation.isUuid(projectId)) return
        gate = ReplacementSubmissionGate()
        form = Bundle().apply {
            putString("kind", kind.name); putString("projectId", projectId); putString("commandId", UUID.randomUUID().toString())
            putString("writtenAt", instant().toString()); putString("date", clock.today().toString())
            putString("categoryId", LithicDatabase.PROJECT_FALLBACK_ID)
            putBoolean("linkReplacement", true); putBoolean("asSuccessor", true)
            selected?.let { putBundle("selectedLink", ReplacementSavedFacts.link(it)) }
        }
        publish()
        hydrate()
    }

    private fun hydrate() {
        val active = form ?: return
        if (active.getBundle("baseline") != null || active.getBundle("submitted") != null || active.getBoolean("loading")) return
        active.putBoolean("loading", true); publish()
        viewModelScope.launch {
            try {
                val (chain, options) = withContext(Dispatchers.IO) {
                    repository.loadChain(text(active, "projectId")) to projects.loadProjectCategories()
                }
                if (form !== active) return@launch
                categories = options
                val detail = chain?.items?.singleOrNull { it.project.id == text(active, "projectId") }
                if (detail == null) active.putString("message", "当前项目不存在，不能提交。")
                else {
                    // A fixed command needs a strictly later revision even for same-tick writes or
                    // a clock rollback. This initial hydration never runs for a submitted envelope.
                    if (text(active, "kind") == ReplacementFormKind.TRADE_IN.name) {
                        val originallyCaptured = Instant.parse(text(active, "writtenAt"))
                        val minimumRevision = detail.project.updatedAt.plusNanos(1)
                        active.putString("writtenAt", maxOf(originallyCaptured, minimumRevision).toString())
                    }
                    active.putBundle("baseline", ReplacementSavedFacts.project(detail.project))
                    active.putString("projectName", detail.project.name)
                    active.putParcelableArrayList("events", ArrayList(detail.events.map(ReplacementSavedFacts::event)))
                    active.putParcelableArrayList("links", ArrayList(checkNotNull(chain).allLinks.map(ReplacementSavedFacts::link)))
                    active.getBundle("selectedLink")?.let(ReplacementSavedFacts::link)?.let { selected ->
                        chain.items.singleOrNull { it.project.id == selected.predecessorId }?.let {
                            active.putBundle("unlinkPredecessor", ReplacementSavedFacts.project(it.project))
                        }
                        chain.items.singleOrNull { it.project.id == selected.successorId }?.let {
                            active.putBundle("unlinkSuccessor", ReplacementSavedFacts.project(it.project))
                        }
                    }
                    active.putString("message", null)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (form === active) active.putString("message", "项目及关系暂时无法读取，输入已保留。") }
            finally { if (form === active) { active.putBoolean("loading", false); publish() } }
        }
    }

    fun retryRead() {
        if (form?.getBundle("baseline") == null) hydrate() else refresh()
        if (form?.getString("kind") == ReplacementFormKind.LINK.name) loadChoices()
    }
    fun loadMoreChoices() {
        if (!state.value.hasMoreChoices || state.value.loadingChoices) return
        choicePages++
        loadChoices()
    }
    private fun loadChoices() {
        val active = form ?: return
        if (text(active, "kind") != ReplacementFormKind.LINK.name) return
        val ticket = ++choiceTicket
        mutableState.value = state.value.copy(loadingChoices = true)
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.IO) { projects.loadProjectWindow(choicePages) }
                if (form !== active || ticket != choiceTicket) return@launch
                mutableState.value = state.value.copy(choices = page.items.filter { it.project.id != text(active, "projectId") },
                    hasMoreChoices = page.nextCursor != null, loadingChoices = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (form === active && ticket == choiceTicket) {
                    active.putString("message", "可选项目暂时无法读取，请重新读取。")
                    mutableState.value = state.value.copy(loadingChoices = false); publish()
                }
            }
        }
    }
    fun selectTarget(id: String) {
        val detail = state.value.choices.singleOrNull { it.project.id == id } ?: return
        change("targetId", id)
        form?.takeIf { gate.phase == SubmissionPhase.IDLE }?.let {
            it.putString("targetName", detail.project.name)
            it.putBundle("targetBaseline", ReplacementSavedFacts.project(detail.project)); publish()
        }
    }
    fun changeAsSuccessor(value: Boolean) = changeBoolean("asSuccessor", value)
    fun changeName(value: String) = change("name", value)
    fun changeDate(value: String) = change("date", value)
    fun changeCash(value: String) = change("cash", value)
    fun changeCredit(value: String) = change("credit", value)
    fun changeCategory(value: String) = change("categoryId", value)
    fun changeNote(value: String) = change("note", value)
    fun changeLink(value: Boolean) = changeBoolean("linkReplacement", value)
    private fun change(key: String, value: String) {
        val active = form ?: return
        if (gate.phase != SubmissionPhase.IDLE || active.getBoolean("loading")) return
        active.putString(key, value); active.remove("message"); active.remove("submitted")
        gate.changed(); publish()
    }
    private fun changeBoolean(key: String, value: Boolean) {
        val active = form ?: return
        if (gate.phase != SubmissionPhase.IDLE || active.getBoolean("loading")) return
        active.putBoolean(key, value); active.remove("message"); active.remove("submitted")
        gate.changed(); publish()
    }

    fun preview() {
        val active = form ?: return
        if (gate.phase != SubmissionPhase.IDLE || active.getBoolean("loading")) return
        val errors = validate(active)
        active.putBundle("errors", Bundle().apply { errors.forEach { (key, value) -> putString(key, value) } })
        if (errors.isEmpty()) gate.confirm()
        publish()
    }
    fun editConfirmation() {
        if (gate.phase != SubmissionPhase.IDLE) return
        gate.changed(); publish()
    }

    fun save() {
        val active = form ?: return
        if (active.getBoolean("invalidIdentity") || active.getBoolean("loading")) return
        if (gate.phase == SubmissionPhase.IDLE && validate(active).isNotEmpty()) { preview(); return }
        val wasUnknown = gate.phase == SubmissionPhase.UNCONFIRMED
        if (!gate.begin(text(active, "commandId"))) return
        val envelope = active.getBundle("submitted") ?: Bundle(active).also { active.putBundle("submitted", it) }
        publish()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { apply(envelope) }
                if (form !== active) return@launch
                when (result) {
                    is ReplacementMutationResult.TradeInRecorded -> {
                        check(result.oldProjectId == text(envelope, "projectId") &&
                            result.newProjectId == TradeInIdentities.newProjectId(text(envelope, "commandId")) &&
                            result.groupId == text(envelope, "commandId"))
                        gate.committed()
                        mutableState.value = state.value.copy(committedProjectId = result.newProjectId)
                    }
                    is ReplacementMutationResult.Linked -> {
                        check(text(envelope, "kind") == ReplacementFormKind.LINK.name && result.linkId == TradeInIdentities.linkId(text(envelope, "commandId")))
                        gate.committed(); discardForm(); refresh(); return@launch
                    }
                    is ReplacementMutationResult.Unlinked -> {
                        check(text(envelope, "kind") == ReplacementFormKind.UNLINK.name && result.linkId == text(requireNotNull(envelope.getBundle("selectedLink")), "id"))
                        gate.committed(); discardForm(); refresh(); return@launch
                    }
                    is ReplacementMutationResult.ValidationFailure -> {
                        if (wasUnknown) gate.unknown() else { gate.rejected(); active.remove("submitted") }
                        active.putString("message", "未保存：${result.reason}。请核对当前项目与输入。")
                    }
                    is ReplacementMutationResult.Conflict -> {
                        if (wasUnknown) gate.unknown() else { gate.rejected(); active.remove("submitted") }
                        active.putString("message", "当前项目或替换关系已变化，应用没有覆盖它。请返回核对后重新发起；待核对提交请重试原身份。")
                    }
                    is ReplacementMutationResult.StorageFailure -> {
                        gate.unknown(); active.putString("message", "暂时无法确认结果，原输入和提交身份已保留。请核对并重试同一提交。")
                    }
                }
            } catch (cancelled: CancellationException) {
                if (form === active) { gate.unknown(); publish() }; throw cancelled
            } catch (_: Exception) {
                if (form === active) {
                    gate.unknown(); active.putString("message", "结果尚未确认，不能生成另一组交易身份。请重试原提交。")
                }
            } finally { if (form === active) publish() }
        }
    }

    private suspend fun apply(value: Bundle): ReplacementMutationResult {
        val commandId = text(value, "commandId")
        val old = ReplacementSavedFacts.project(requireNotNull(value.getBundle("baseline")))
        val links = ReplacementSavedFacts.links(value)
        return when (ReplacementFormKind.valueOf(text(value, "kind"))) {
            ReplacementFormKind.LINK -> {
                val target = ReplacementSavedFacts.project(requireNotNull(value.getBundle("targetBaseline")))
                val predecessor = if (value.getBoolean("asSuccessor", true)) old else target
                val successor = if (value.getBoolean("asSuccessor", true)) target else old
                repository.link(LinkReplacementCommand(commandId, predecessor, successor, links, Instant.parse(text(value, "writtenAt"))))
            }
            ReplacementFormKind.UNLINK -> {
                val selected = ReplacementSavedFacts.link(requireNotNull(value.getBundle("selectedLink")))
                val predecessor = value.getBundle("unlinkPredecessor")?.let(ReplacementSavedFacts::project)
                    ?: return ReplacementMutationResult.Conflict("Missing fixed predecessor")
                val successor = value.getBundle("unlinkSuccessor")?.let(ReplacementSavedFacts::project)
                    ?: return ReplacementMutationResult.Conflict("Missing fixed successor")
                repository.unlink(UnlinkReplacementCommand(commandId, selected, predecessor, successor, links))
            }
            ReplacementFormKind.TRADE_IN -> {
                val time = Instant.parse(text(value, "writtenAt"))
                val date = LocalDate.parse(text(value, "date").trim())
                val newProject = LongTermProjectEntity(TradeInIdentities.newProjectId(commandId), text(value, "name").trim(),
                    text(value, "categoryId"), date, note = text(value, "note").trim().ifEmpty { null }, createdAt = time, updatedAt = time)
                repository.recordTradeIn(TradeInCommand(commandId, old, ReplacementSavedFacts.events(value), links, newProject,
                    date, cents(value, "cash"), cents(value, "credit"), value.getBoolean("linkReplacement", true), time))
            }
        }
    }

    fun leaveDecision(): LeaveDecision = gate.leaveDecision()
    fun discardForm() {
        if (gate.leaveDecision() == LeaveDecision.BLOCK_PENDING) return
        form = null; gate = ReplacementSubmissionGate(); choicePages = 1; choiceTicket++
        saved.remove<Bundle>(FORM_KEY)
        mutableState.value = state.value.copy(form = null, choices = emptyList(), hasMoreChoices = false, loadingChoices = false, committedProjectId = null)
    }
    fun consumeCommitted() {
        if (gate.phase != SubmissionPhase.COMMITTED) return
        discardForm(); mutableState.value = state.value.copy(committedProjectId = null)
    }

    private fun publish() {
        val active = form
        if (active == null) { mutableState.value = state.value.copy(form = null); return }
        active.putString("phase", gate.phase.name); active.putBoolean("dirty", gate.dirty); active.putBoolean("confirmed", gate.confirmed)
        saved[FORM_KEY] = Bundle(active)
        val errors = active.getBundle("errors")?.let { b -> b.keySet().associateWith { b.getString(it).orEmpty() } }.orEmpty()
        mutableState.value = state.value.copy(form = ReplacementFormUiState(
            kind = ReplacementFormKind.entries.firstOrNull { it.name == text(active, "kind") } ?: ReplacementFormKind.TRADE_IN,
            projectId = text(active, "projectId"), projectName = text(active, "projectName"), targetId = text(active, "targetId"),
            targetName = text(active, "targetName"), selectedLink = active.getBundle("selectedLink")?.let(ReplacementSavedFacts::link),
            asSuccessor = active.getBoolean("asSuccessor", true), name = text(active, "name"), date = text(active, "date"), cash = text(active, "cash"),
            credit = text(active, "credit"), categoryId = text(active, "categoryId"), note = text(active, "note"),
            linkReplacement = active.getBoolean("linkReplacement", true), categories = categories, today = clock.today(), phase = gate.phase,
            loading = active.getBoolean("loading"), confirmed = gate.confirmed, needsInitialRead = active.getBundle("baseline") == null,
            message = active.getString("message"), errors = errors))
    }
    private fun validate(value: Bundle): Map<String, String> = buildMap {
        if (!validFormIdentity(value) || value.getBundle("baseline") == null) put("identity", "项目身份尚未完整读取")
        when (text(value, "kind")) {
            ReplacementFormKind.LINK.name -> if (value.getBundle("targetBaseline") == null) put("target", "请选择明确的另一项目")
            ReplacementFormKind.UNLINK.name -> if (value.getBundle("selectedLink") == null || value.getBundle("unlinkPredecessor") == null ||
                value.getBundle("unlinkSuccessor") == null) put("target", "关系及两端身份尚未完整读取")
            ReplacementFormKind.TRADE_IN.name -> {
                if (text(value, "name").trim().isEmpty() || text(value, "name").trim().length > 120) put("name", "请填写1～120字名称")
                val date = runCatching { LocalDate.parse(text(value, "date").trim()) }.getOrNull()
                val old = value.getBundle("baseline")?.let { runCatching { ReplacementSavedFacts.project(it) }.getOrNull() }
                if (date == null || date.isAfter(clock.today()) || (old != null && date.isBefore(old.startedOn))) put("date", "请填写已发生且不早于旧项目取得的日期")
                if (old?.endedOn != null) put("identity", "旧项目已有结束记录，不能重复以旧换新")
                listOf("cash", "credit").forEach { key -> if (MoneyParser.parseCny(text(value, key)) !is MoneyParseResult.Success) put(key, "请填写精确到分的人民币金额") }
                if (categories.none { it.id == text(value, "categoryId") }) put("categoryId", "请选择当前项目类别")
                if (text(value, "note").length > 2000) put("note", "备注最多2000字")
            }
        }
    }
    private fun validFormIdentity(value: Bundle): Boolean = try {
        fun facts(v: Bundle): Boolean {
            if (!StoredValueValidation.isUuid(text(v, "projectId")) || !StoredValueValidation.isUuid(text(v, "commandId")) ||
                ReplacementFormKind.entries.none { it.name == text(v, "kind") }) return false
            val old = v.getBundle("baseline")?.let(ReplacementSavedFacts::project)
            if (old != null && old.id != text(v, "projectId")) return false
            if (ReplacementSavedFacts.events(v).any { it.projectId != text(v, "projectId") }) return false
            if (v.getBundle("targetBaseline")?.let(ReplacementSavedFacts::project)?.id.let { it != null && it != text(v, "targetId") }) return false
            val links = ReplacementSavedFacts.links(v)
            if (links.map { it.id }.distinct().size != links.size) return false
            return true
        }
        if (!facts(value)) false else value.getBundle("submitted")?.let { submitted ->
            facts(submitted) && listOf("kind", "projectId", "commandId", "writtenAt", "targetId", "name", "date", "cash", "credit", "categoryId", "note").all {
                text(value, it) == text(submitted, it)
            } && listOf("asSuccessor", "linkReplacement").all { value.getBoolean(it, true) == submitted.getBoolean(it, true) } &&
                value.getBundle("baseline")?.let(ReplacementSavedFacts::project) == submitted.getBundle("baseline")?.let(ReplacementSavedFacts::project) &&
                ReplacementSavedFacts.events(value) == ReplacementSavedFacts.events(submitted) &&
                ReplacementSavedFacts.links(value) == ReplacementSavedFacts.links(submitted) &&
                value.getBundle("selectedLink")?.let(ReplacementSavedFacts::link) == submitted.getBundle("selectedLink")?.let(ReplacementSavedFacts::link) &&
                listOf("targetBaseline", "unlinkPredecessor", "unlinkSuccessor").all { key ->
                    value.getBundle(key)?.let(ReplacementSavedFacts::project) == submitted.getBundle(key)?.let(ReplacementSavedFacts::project)
                }
        } ?: true
    } catch (_: Exception) { false }
    private fun cents(value: Bundle, key: String) = (MoneyParser.parseCny(text(value, key)) as MoneyParseResult.Success).money.cents
    private fun text(value: Bundle, key: String) = value.getString(key).orEmpty()
    private fun restoredGate(value: Bundle?): ReplacementSubmissionGate {
        val phase = SubmissionPhase.entries.firstOrNull { it.name == value?.getString("phase") } ?: SubmissionPhase.IDLE
        return ReplacementSubmissionGate(phase, value?.getBoolean("dirty") == true, value?.getBoolean("confirmed") == true,
            if (phase == SubmissionPhase.IN_FLIGHT || phase == SubmissionPhase.UNCONFIRMED) value?.getString("commandId") else null)
    }
    companion object {
        private const val FORM_KEY = "lithic.replacement.activeForm"
        private const val READ_ERROR = "替换关系暂时无法刷新，保留上次结果；请重新读取。"
    }
}
