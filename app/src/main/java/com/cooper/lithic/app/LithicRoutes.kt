package com.cooper.lithic.app

import androidx.navigation3.runtime.NavKey
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.feature.project.ProjectEditorMode
import com.cooper.lithic.feature.project.ProjectUiState
import kotlinx.serialization.Serializable

@Serializable
internal data object Home : NavKey

@Serializable
internal data object Review : NavKey

@Serializable
internal data object LongTermReview : NavKey

@Serializable
internal data object TotalCostTrend : NavKey

@Serializable
internal data object AnnualReview : NavKey

@Serializable
internal data class CategoryAnalysis(val categoryId: String? = null, val subscription: Boolean = false) : NavKey

@Serializable
internal data object Settings : NavKey

@Serializable
internal data object NotificationSettings : NavKey

@Serializable
internal data object CalculationExplanation : NavKey

@Serializable
internal data object AboutLithic : NavKey

@Serializable
internal data class MilestoneSources(val generationId: String, val publicationOn: String,
    val digest: String, val count: Int) : NavKey {
    fun reference(): com.cooper.lithic.data.repository.MilestoneNoticeRef {
        require(validMilestoneReference(this)) { "Invalid milestone notification reference" }
        return com.cooper.lithic.data.repository.MilestoneNoticeRef(generationId,
            java.time.LocalDate.parse(publicationOn), digest, count)
    }
}

@Serializable
internal data object InvalidMilestoneNotice : NavKey

/** Shared by direct navigation and the already-open discard/pending dialogs. */
internal fun navigationAvailable(phase: com.cooper.lithic.feature.settings.RestoreUiPhase): Boolean =
    phase != com.cooper.lithic.feature.settings.RestoreUiPhase.RESTORING

internal fun milestoneAwareBackStack(current: List<NavKey>, target: List<NavKey>): List<NavKey> {
    val notice = current.getOrNull(1) as? MilestoneSources ?: return target
    if (!validMilestoneReference(notice) || target.getOrNull(1) !in listOf(ProjectList, SubscriptionList) ||
        target.size < 3 || target != canonicalBackStack(target.last())) return target
    return listOf(Home, notice) + target.drop(2)
}

private fun validMilestoneReference(route: MilestoneSources): Boolean = runCatching {
    StoredValueValidation.isUuid(route.generationId) && route.digest.matches(Regex("[0-9a-f]{64}")) &&
        route.count in 1..1_000_000 && java.time.LocalDate.parse(route.publicationOn).toString() == route.publicationOn
}.getOrDefault(false)

@Serializable
internal data object NameSearch : NavKey

@Serializable
internal data object CategoryManagement : NavKey

@Serializable
internal data object ProjectList : NavKey

@Serializable
internal data object ProjectHistory : NavKey

@Serializable
internal enum class ProjectOrigin { CURRENT, HISTORY, REVIEW, TREND, CATEGORY, ANNUAL, HOME, REVIEW_HOME }

@Serializable
internal data object CreateProject : NavKey

@Serializable
internal data class ProjectDetail(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectCostHistory(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class SubscriptionPriceHistory(val id: String, val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectEdit(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectMoneyEntry(val id: String, val direction: MoneyDirection = MoneyDirection.OUTLAY, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectEnd(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectUpdateEnd(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectMoneyCorrection(val id: String, val eventId: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectDelete(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ReplacementComparison(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class TradeInProject(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data object SubscriptionList : NavKey

@Serializable
internal data object CreateSubscription : NavKey

@Serializable
internal enum class SubscriptionOrigin { CURRENT, HISTORY, REVIEW, TREND, CATEGORY, ANNUAL, HOME, REVIEW_HOME }

@Serializable
internal enum class SubscriptionAction { PAUSE, RESUME, REOPEN, STOP }

@Serializable
internal data object SubscriptionHistory : NavKey

@Serializable
internal data class SubscriptionDetail(val id: String, val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class ConfirmSubscriptionRenewal(val id: String, val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class ChangeSubscriptionState(val id: String, val action: SubscriptionAction,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class CorrectSubscriptionState(val id: String, val stateId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class CancelSubscriptionStop(val id: String, val stateId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class CorrectSubscriptionRenewal(val id: String, val renewalId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class DeleteSubscriptionRenewal(val id: String, val renewalId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

internal fun canonicalBackStack(destination: NavKey): List<NavKey> = when (destination) {
    Home -> listOf(Home)
    Review, Settings, ProjectList, SubscriptionList -> listOf(Home, destination)
    LongTermReview -> listOf(Home, Review, LongTermReview)
    TotalCostTrend -> listOf(Home, Review, TotalCostTrend)
    AnnualReview -> listOf(Home, Review, AnnualReview)
    is CategoryAnalysis -> if (destination.categoryId == null || StoredValueValidation.isUuid(destination.categoryId))
        listOf(Home, Review, destination) else listOf(Home)
    NameSearch -> listOf(Home, NameSearch)
    CategoryManagement -> listOf(Home, Settings, CategoryManagement)
    NotificationSettings -> listOf(Home, Settings, NotificationSettings)
    CalculationExplanation, AboutLithic -> listOf(Home, Settings, destination)
    is MilestoneSources -> if (validMilestoneReference(destination)) listOf(Home, destination) else listOf(Home)
    InvalidMilestoneNotice -> listOf(Home, InvalidMilestoneNotice)
    ProjectHistory -> listOf(Home, Review, ProjectHistory)
    CreateProject -> listOf(Home, CreateProject)
    CreateSubscription -> listOf(Home, CreateSubscription)
    SubscriptionHistory -> listOf(Home, Review, SubscriptionHistory)
    is SubscriptionDetail -> if (StoredValueValidation.isUuid(destination.id)) subscriptionParent(destination.origin) + destination else listOf(Home)
    is ConfirmSubscriptionRenewal -> subscriptionEditorStack(destination.id, destination.origin, destination)
    is ChangeSubscriptionState -> subscriptionEditorStack(destination.id, destination.origin, destination)
    is CorrectSubscriptionState -> if (StoredValueValidation.isUuid(destination.stateId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is CancelSubscriptionStop -> if (StoredValueValidation.isUuid(destination.stateId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is CorrectSubscriptionRenewal -> if (StoredValueValidation.isUuid(destination.renewalId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is DeleteSubscriptionRenewal -> if (StoredValueValidation.isUuid(destination.renewalId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is ProjectDetail -> projectDetailStack(destination.id, destination.origin, destination.comparisonId)
    is ReplacementComparison -> if (StoredValueValidation.isUuid(destination.id)) projectDetailStack(destination.id, destination.origin, null) + destination else listOf(Home)
    is TradeInProject -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectCostHistory -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is SubscriptionPriceHistory -> subscriptionEditorStack(destination.id, destination.origin, destination)
    is ProjectEdit -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectMoneyEntry -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectEnd -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectUpdateEnd -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectMoneyCorrection -> if (StoredValueValidation.isUuid(destination.eventId)) editorStack(destination.id, destination.origin, destination, destination.comparisonId) else listOf(Home)
    is ProjectDelete -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    else -> listOf(Home)
}

internal fun subscriptionOrigin(destination: NavKey): SubscriptionOrigin = when (destination) {
    SubscriptionHistory -> SubscriptionOrigin.HISTORY
    is SubscriptionDetail -> destination.origin
    is SubscriptionPriceHistory -> destination.origin
    is ConfirmSubscriptionRenewal -> destination.origin
    is ChangeSubscriptionState -> destination.origin
    is CorrectSubscriptionState -> destination.origin
    is CancelSubscriptionStop -> destination.origin
    is CorrectSubscriptionRenewal -> destination.origin
    is DeleteSubscriptionRenewal -> destination.origin
    else -> SubscriptionOrigin.CURRENT
}

private fun subscriptionParent(origin: SubscriptionOrigin): List<NavKey> = when (origin) {
    SubscriptionOrigin.HOME -> listOf(Home)
    SubscriptionOrigin.REVIEW_HOME -> listOf(Home, Review)
    SubscriptionOrigin.CURRENT -> listOf(Home, SubscriptionList)
    SubscriptionOrigin.HISTORY -> listOf(Home, Review, SubscriptionHistory)
    SubscriptionOrigin.REVIEW -> listOf(Home, Review, LongTermReview)
    SubscriptionOrigin.TREND -> listOf(Home, Review, TotalCostTrend)
    SubscriptionOrigin.CATEGORY -> listOf(Home, Review, CategoryAnalysis())
    SubscriptionOrigin.ANNUAL -> listOf(Home, Review, AnnualReview)
}

private fun subscriptionEditorStack(id: String, origin: SubscriptionOrigin, destination: NavKey): List<NavKey> =
    if (StoredValueValidation.isUuid(id)) subscriptionParent(origin) + listOf(SubscriptionDetail(id, origin), destination) else listOf(Home)

internal fun projectOrigin(destination: NavKey): ProjectOrigin = when (destination) {
    is ProjectDetail -> destination.origin
    is ProjectCostHistory -> destination.origin
    is ReplacementComparison -> destination.origin
    is TradeInProject -> destination.origin
    is ProjectEdit -> destination.origin
    is ProjectMoneyEntry -> destination.origin
    is ProjectEnd -> destination.origin
    is ProjectUpdateEnd -> destination.origin
    is ProjectMoneyCorrection -> destination.origin
    is ProjectDelete -> destination.origin
    ProjectHistory -> ProjectOrigin.HISTORY
    else -> ProjectOrigin.CURRENT
}

private fun projectParent(origin: ProjectOrigin): List<NavKey> = when (origin) {
    ProjectOrigin.HOME -> listOf(Home)
    ProjectOrigin.REVIEW_HOME -> listOf(Home, Review)
    ProjectOrigin.CURRENT -> listOf(Home, ProjectList)
    ProjectOrigin.HISTORY -> listOf(Home, Review, ProjectHistory)
    ProjectOrigin.REVIEW -> listOf(Home, Review, LongTermReview)
    ProjectOrigin.TREND -> listOf(Home, Review, TotalCostTrend)
    ProjectOrigin.CATEGORY -> listOf(Home, Review, CategoryAnalysis())
    ProjectOrigin.ANNUAL -> listOf(Home, Review, AnnualReview)
}

private fun projectDetailStack(id: String, origin: ProjectOrigin, comparisonId: String?): List<NavKey> {
    if (!StoredValueValidation.isUuid(id)) return listOf(Home)
    if (comparisonId == null) return projectParent(origin) + ProjectDetail(id, origin)
    if (!StoredValueValidation.isUuid(comparisonId) || comparisonId == id) return listOf(Home)
    return projectParent(origin) + listOf(ProjectDetail(comparisonId, origin), ReplacementComparison(comparisonId, origin), ProjectDetail(id, origin, comparisonId))
}

private fun editorStack(id: String, origin: ProjectOrigin, destination: NavKey, comparisonId: String? = null): List<NavKey> {
    val detail = projectDetailStack(id, origin, comparisonId)
    return if (detail == listOf(Home)) detail else detail + destination
}

internal fun projectComparisonId(destination: NavKey): String? = when (destination) {
    is ProjectDetail -> destination.comparisonId
    is ProjectCostHistory -> destination.comparisonId
    is ProjectEdit -> destination.comparisonId
    is ProjectMoneyEntry -> destination.comparisonId
    is ProjectEnd -> destination.comparisonId
    is ProjectUpdateEnd -> destination.comparisonId
    is ProjectMoneyCorrection -> destination.comparisonId
    is ProjectDelete -> destination.comparisonId
    is TradeInProject -> destination.comparisonId
    else -> null
}

/** The pending dialog uses this route without discarding or reconstructing the fixed command. */
internal fun projectRecoveryRoute(state: ProjectUiState, origin: ProjectOrigin, comparisonId: String? = null): NavKey? = when {
    state.editor != null -> if (state.editor.mode == ProjectEditorMode.CREATE) CreateProject else ProjectEdit(state.editor.projectId, origin, comparisonId)
    state.moneyEditor != null -> ProjectMoneyEntry(state.moneyEditor.projectId, state.moneyEditor.direction, origin, comparisonId)
    state.endEditor != null -> if (state.endEditor.correcting) ProjectUpdateEnd(state.endEditor.projectId, origin, comparisonId) else ProjectEnd(state.endEditor.projectId, origin, comparisonId)
    state.moneyCorrection != null -> ProjectMoneyCorrection(state.moneyCorrection.projectId, state.moneyCorrection.eventId, origin, comparisonId)
    state.deleteConfirmation != null -> ProjectDelete(state.deleteConfirmation.projectId, origin, comparisonId)
    else -> null
}

/** Preserves only browsing context around a canonical, already-validated record path.
 * No new origin, fact, command or authority is added. Guards still own the actual navigation. */
internal fun browsingAwareBackStack(current: List<NavKey>, target: List<NavKey>): List<NavKey> {
    val destination = target.lastOrNull() ?: return target
    val canonical = canonicalBackStack(destination)
    if (target != canonical) return target
    val tail = searchRecordTail(canonical)
    if (current.take(2) == listOf(Home, NameSearch) && tail != null) return listOf(Home, NameSearch) + tail
    val category = current.getOrNull(2) as? CategoryAnalysis
    if (current.take(2) == listOf(Home, Review) && category != null && canonical.size > 3 &&
        canonical.getOrNull(2) is CategoryAnalysis && categoryMatchesRecord(category, canonical) && canonicalBackStack(category) == listOf(Home, Review, category)) {
        return canonical.toMutableList().apply { set(2, category) }
    }
    return target
}

private fun categoryMatchesRecord(category: CategoryAnalysis, canonical: List<NavKey>): Boolean {
    // The original empty/default parent is a neutral anchor, also used by subscription routes.
    if (category.categoryId == null && !category.subscription) return true
    return when (canonical.getOrNull(3)) {
        is ProjectDetail -> !category.subscription
        is SubscriptionDetail -> category.subscription
        else -> false
    }
}

/** Return context only; the caller has already matched the deletion and passed existing guards. */
internal fun projectBrowseReturnAfterDelete(current: List<NavKey>): NavKey {
    if (current.getOrNull(1) == NameSearch) return NameSearch
    return when (projectOrigin(current.lastOrNull() ?: Home)) {
        ProjectOrigin.HOME -> Home
        ProjectOrigin.REVIEW_HOME -> Review
        ProjectOrigin.CURRENT -> ProjectList
        ProjectOrigin.HISTORY -> ProjectHistory
        ProjectOrigin.REVIEW -> LongTermReview
        ProjectOrigin.TREND -> TotalCostTrend
        ProjectOrigin.CATEGORY -> current.getOrNull(2) as? CategoryAnalysis ?: CategoryAnalysis()
        ProjectOrigin.ANNUAL -> AnnualReview
    }
}

private fun searchRecordTail(canonical: List<NavKey>): List<NavKey>? = when {
    canonical.getOrNull(1) in listOf(ProjectList, SubscriptionList) && canonical.size > 2 -> canonical.drop(2)
    canonical.take(2) == listOf(Home, Review) &&
        canonical.getOrNull(2) in listOf(ProjectHistory, SubscriptionHistory) && canonical.size > 3 -> canonical.drop(3)
    else -> null
}

/** Only decoded, known routes can be presented; old Skeleton and invalid stacks return home. */
internal fun normalizeBackStack(restored: List<NavKey>): List<NavKey> {
    if (restored == listOf(Home, CalculationExplanation)) return restored.toList()
    if (restored == listOf(Home, ProjectList, CreateProject)) return restored.toList()
    if (restored == listOf(Home, SubscriptionList, CreateSubscription)) return restored.toList()
    val destination = restored.lastOrNull() ?: return listOf(Home)
    val notice = restored.getOrNull(1) as? MilestoneSources
    if (restored.firstOrNull() == Home && notice != null && validMilestoneReference(notice) && restored.size > 2) {
        val canonical = canonicalBackStack(destination)
        if (canonical.getOrNull(1) in listOf(ProjectList, SubscriptionList) && canonical.size > 2 &&
            restored.drop(2) == canonical.drop(2)) return restored.toList()
    }
    val canonical = canonicalBackStack(destination)
    if (restored.take(2) == listOf(Home, NameSearch)) {
        val tail = searchRecordTail(canonical)
        if (tail != null && restored.drop(2) == tail) return restored.toList()
    }
    val category = restored.getOrNull(2) as? CategoryAnalysis
    if (restored.take(2) == listOf(Home, Review) && category != null && canonical.size > 3 &&
        canonical.getOrNull(2) is CategoryAnalysis && categoryMatchesRecord(category, canonical) && canonicalBackStack(category) == listOf(Home, Review, category) &&
        restored.drop(3) == canonical.drop(3)) return restored.toList()
    if (restored == canonical) return restored.toList()
    return listOf(Home)
}

/** Detail and editing layers keep a single explicit return path. */
internal fun c2ShowPrimaryNavigation(route: NavKey): Boolean = route == Home || route == Review || route == Settings
