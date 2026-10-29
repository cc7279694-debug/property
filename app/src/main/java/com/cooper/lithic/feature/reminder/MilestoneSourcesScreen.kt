package com.cooper.lithic.feature.reminder

import com.cooper.lithic.core.design.*
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import com.cooper.lithic.domain.reminder.MilestoneCandidate
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ephemeral notification sources only. Every read and row click revalidates the
 * bounded reference against the current generation and actual publication receipts. */
@Composable
internal fun MilestoneSourcesScreen(
    reference: MilestoneNoticeRef,
    changes: Flow<Unit>,
    resolve: suspend (MilestoneNoticeRef) -> List<MilestoneCandidate>,
    openSingle: Boolean,
    onSingleHandled: () -> Unit,
    onOpen: (MilestoneCandidate) -> Unit,
    onBack: () -> Unit,
) {
    var records by remember(reference) { mutableStateOf<List<MilestoneCandidate>>(emptyList()) }
    var error by remember(reference) { mutableStateOf<String?>(null) }
    var loading by remember(reference) { mutableStateOf(true) }
    var window by remember(reference) { mutableIntStateOf(50) }
    var revision by remember(reference) { mutableIntStateOf(0) }
    var autoConsumed by remember(reference) { mutableStateOf(false) }
    val currentResolve by rememberUpdatedState(resolve)
    val currentOpen by rememberUpdatedState(onOpen)
    val currentSingle by rememberUpdatedState(openSingle)
    val singleHandled by rememberUpdatedState(onSingleHandled)
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun failed() {
        records = emptyList()
        error = "这条通知已失效或来源暂时无法核验。记录可能已删除、纠错，或已切换数据集。请返回当前账本查看。"
    }
    suspend fun read() {
        loading = true
        error = null
        try {
            val fresh = withContext(Dispatchers.IO) { currentResolve(reference) }
            check(fresh.isNotEmpty() && fresh.size == reference.count)
            records = fresh
            window = 50
            if (currentSingle && !autoConsumed && fresh.size == 1) {
                autoConsumed = true
                singleHandled()
                currentOpen(fresh.single())
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed(); singleHandled() }
        finally { loading = false }
    }
    LaunchedEffect(reference, revision) { read() }
    LaunchedEffect(reference, changes) { changes.collectLatest { revision++ } }
    DisposableEffect(lifecycle, reference) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("milestone-sources"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                C2PageHeader("本次里程碑来源", onBack, "milestone-sources-back", backLabel = "返回现在")
                Text("发布日 ${reference.publicationOn} · ${reference.count}项", style = MaterialTheme.typography.bodyLarge)
                Text("仅显示这次摘要；节点日期保留各自真实记录。打开前会重新核对当前记录。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("milestone-source-reference-check"))
                if (loading) Text("正在核对当前记录", Modifier.testTag("milestone-sources-loading")
                    .semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
                error?.let {
                    UiFeedbackText(it, UiMessageTone.ERROR, "milestone-sources-error")
                    TextButton({ revision++ }, Modifier.heightIn(min = 48.dp).testTag("milestone-sources-retry"), enabled = !loading) { Text("重新核对") }
                }
            }
        }
        items(records.take(window), key = { it.logicalKey }) { item ->
            Card(onClick = {
                if (!loading) scope.launch {
                    loading = true
                    try {
                        val fresh = withContext(Dispatchers.IO) { currentResolve(reference) }
                        val selected = fresh.singleOrNull { it.logicalKey == item.logicalKey &&
                            it.ownerType == item.ownerType && it.ownerId == item.ownerId && it.reachedOn == item.reachedOn }
                        checkNotNull(selected) { "Milestone source changed" }
                        currentOpen(selected)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { failed() }
                    finally { loading = false }
                }
            }, enabled = !loading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("milestone-source-${item.logicalKey}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        C2RecordIdentity(item.title, "", if (item.ownerType == MilestoneOwnerType.PROJECT) C2Glyph.Project else C2Glyph.Subscription)
                        Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    }
                    C2StatusChip(if (item.ownerType == MilestoneOwnerType.PROJECT) "长期项目" else "订阅服务")
                    Text(item.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (records.size > window) item {
            Button({ window = minOf(records.size, window + 50) }, Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
                enabled = !loading, content = { Text("继续查看") })
        }
    }
}

@Composable
internal fun InvalidMilestoneNoticeScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        C2PageHeader("无法打开这条里程碑通知", onBack, "milestone-invalid-back", backLabel = "返回现在")
        UiFeedbackText("通知来源无效，未根据这条通知打开或修改记录。请从当前账本查看。",
            UiMessageTone.ERROR, "milestone-invalid-notice")
    }
}
