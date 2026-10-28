package com.cooper.lithic.app

import org.junit.Assert.assertEquals
import org.junit.Test

class C7JourneyNavigationTest {
    private val id = "c7000000-0000-0000-0000-000000000001"
    private val comparisonId = "c7000000-0000-0000-0000-000000000002"

    @Test fun project_created_from_home_returns_to_home_after_saved_detail() {
        val destination = projectCommittedDetail(listOf(Home, CreateProject), id)
        assertEquals(listOf(Home, ProjectDetail(id, ProjectOrigin.HOME)), canonicalBackStack(destination))
    }

    @Test fun project_created_from_list_keeps_the_list_parent() {
        val destination = projectCommittedDetail(listOf(Home, ProjectList, CreateProject), id)
        assertEquals(listOf(Home, ProjectList, ProjectDetail(id)), canonicalBackStack(destination))
    }

    @Test fun subscription_created_from_home_returns_to_home_after_saved_detail() {
        val destination = subscriptionCommittedDetail(listOf(Home, CreateSubscription), id, SubscriptionOrigin.CURRENT.name)
        assertEquals(listOf(Home, SubscriptionDetail(id, SubscriptionOrigin.HOME)), canonicalBackStack(destination))
    }

    @Test fun subscription_created_from_list_keeps_the_list_parent() {
        val destination = subscriptionCommittedDetail(listOf(Home, SubscriptionList, CreateSubscription), id, SubscriptionOrigin.CURRENT.name)
        assertEquals(listOf(Home, SubscriptionList, SubscriptionDetail(id)), canonicalBackStack(destination))
    }

    @Test fun saved_project_operations_keep_their_existing_origin_and_comparison() {
        ProjectOrigin.entries.forEach { origin ->
            val source = ProjectMoneyEntry(id, com.cooper.lithic.core.model.MoneyDirection.OUTLAY, origin, comparisonId)
            assertEquals(ProjectDetail(id, origin, comparisonId), projectCommittedDetail(canonicalBackStack(source), id))
        }
    }

    @Test fun subscription_operation_result_keeps_the_confirmed_current_or_history_origin() {
        SubscriptionOrigin.entries.forEach { origin ->
            val current = canonicalBackStack(ConfirmSubscriptionRenewal(id, origin))
            assertEquals(SubscriptionDetail(id, origin), subscriptionCommittedDetail(current, id, origin.name))
        }
        val current = canonicalBackStack(ChangeSubscriptionState(id,
            SubscriptionAction.STOP, SubscriptionOrigin.HOME))
        assertEquals(SubscriptionDetail(id, SubscriptionOrigin.HISTORY),
            subscriptionCommittedDetail(current, id, SubscriptionOrigin.HISTORY.name))
    }

    @Test fun saving_from_search_keeps_the_existing_browsing_adapter() {
        val projectSource = listOf(Home, NameSearch, ProjectDetail(id), ProjectEdit(id))
        val projectTarget = projectCommittedDetail(projectSource, id)
        assertEquals(listOf(Home, NameSearch, ProjectDetail(id)),
            browsingAwareBackStack(projectSource, canonicalBackStack(projectTarget)))
        val subscriptionSource = listOf(Home, NameSearch, SubscriptionDetail(id), ConfirmSubscriptionRenewal(id))
        val subscriptionTarget = subscriptionCommittedDetail(subscriptionSource, id, SubscriptionOrigin.CURRENT.name)
        assertEquals(listOf(Home, NameSearch, SubscriptionDetail(id)),
            browsingAwareBackStack(subscriptionSource, canonicalBackStack(subscriptionTarget)))
    }

    @Test fun saving_from_a_selected_category_keeps_the_exact_category_parent() {
        val category = CategoryAnalysis(comparisonId)
        val current = listOf(Home, Review, category, ProjectDetail(id, ProjectOrigin.CATEGORY),
            ProjectEdit(id, ProjectOrigin.CATEGORY))
        assertEquals(listOf(Home, Review, category, ProjectDetail(id, ProjectOrigin.CATEGORY)),
            browsingAwareBackStack(current, canonicalBackStack(projectCommittedDetail(current, id))))
    }

    @Test fun stopping_a_subscription_from_search_keeps_search_and_the_confirmed_history_origin() {
        val current = listOf(Home, NameSearch, SubscriptionDetail(id), ChangeSubscriptionState(id, SubscriptionAction.STOP))
        val destination = subscriptionCommittedDetail(current, id, SubscriptionOrigin.HISTORY.name)
        assertEquals(listOf(Home, NameSearch, SubscriptionDetail(id, SubscriptionOrigin.HISTORY)),
            browsingAwareBackStack(current, canonicalBackStack(destination)))
    }

    @Test fun committed_destinations_still_use_the_existing_identity_validation() {
        assertEquals(listOf(Home), canonicalBackStack(projectCommittedDetail(listOf(Home, CreateProject), "invalid")))
        assertEquals(listOf(Home), canonicalBackStack(subscriptionCommittedDetail(listOf(Home, CreateSubscription), "invalid", null)))
        val invalidComparison = ProjectEdit(id, comparisonId = "invalid")
        assertEquals(listOf(Home), canonicalBackStack(projectCommittedDetail(listOf(Home, invalidComparison), id)))
    }
}
