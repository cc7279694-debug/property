package com.cooper.lithic.app

import org.junit.Assert.assertEquals
import org.junit.Test

class P02BrowseNavigationTest {
    private val id = "72000000-0000-0000-0000-000000000001"
    private val eventId = "72000000-0000-0000-0000-000000000002"
    private val categoryId = "72000000-0000-0000-0000-000000000003"

    @Test fun search_project_details_and_editors_keep_the_search_parent() {
        listOf(ProjectOrigin.CURRENT, ProjectOrigin.HISTORY).forEach { origin ->
            val detail = ProjectDetail(id, origin)
            listOf(listOf(Home, NameSearch, detail),
                listOf(Home, NameSearch, detail, ProjectEdit(id, origin)),
                listOf(Home, NameSearch, detail, ProjectDelete(id, origin)),
                listOf(Home, NameSearch, detail, ProjectMoneyCorrection(id, eventId, origin))).forEach {
                assertEquals(it, normalizeBackStack(it))
            }
        }
    }
    @Test fun search_subscription_details_and_followup_forms_keep_the_search_parent() {
        listOf(SubscriptionOrigin.CURRENT, SubscriptionOrigin.HISTORY).forEach { origin ->
            val detail = SubscriptionDetail(id, origin)
            listOf(listOf(Home, NameSearch, detail),
                listOf(Home, NameSearch, detail, ConfirmSubscriptionRenewal(id, origin)),
                listOf(Home, NameSearch, detail, CancelSubscriptionStop(id, eventId, origin))).forEach {
                assertEquals(it, normalizeBackStack(it))
            }
        }
    }
    @Test fun selected_category_survives_a_matching_detail_and_editor_return() {
        val parent = CategoryAnalysis(categoryId)
        val detail = ProjectDetail(id, ProjectOrigin.CATEGORY)
        listOf(listOf(Home, Review, parent, detail),
            listOf(Home, Review, parent, detail, ProjectEdit(id, ProjectOrigin.CATEGORY))).forEach {
            assertEquals(it, normalizeBackStack(it))
        }
    }
    @Test fun selected_subscription_category_retains_its_kind_and_identity() {
        val parent = CategoryAnalysis(categoryId, subscription = true)
        val stack = listOf(Home, Review, parent, SubscriptionDetail(id, SubscriptionOrigin.CATEGORY))
        assertEquals(stack, normalizeBackStack(stack))
    }
    @Test fun malformed_search_or_cross_identity_stacks_still_fail_closed() {
        listOf(listOf(NameSearch, ProjectDetail(id)), listOf(Home, NameSearch, Settings),
            listOf(Home, NameSearch, ProjectDetail("not-an-id")),
            listOf(Home, NameSearch, ProjectDetail(id), ProjectEdit(eventId)),
            listOf(Home, NameSearch, SubscriptionDetail(id), ProjectEdit(id)),
            listOf(Home, NameSearch, ProjectDetail(id, ProjectOrigin.REVIEW))).forEach {
            assertEquals(listOf(Home), normalizeBackStack(it))
        }
    }
    @Test fun malformed_category_parent_does_not_gain_a_valid_record_stack() {
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review,
            CategoryAnalysis("not-an-id"), ProjectDetail(id, ProjectOrigin.CATEGORY))))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review,
            CategoryAnalysis(categoryId), ProjectDetail(id, ProjectOrigin.CURRENT))))
    }
    @Test fun adapter_keeps_search_for_known_current_and_history_record_targets() {
        val current = listOf(Home, NameSearch)
        listOf(ProjectOrigin.CURRENT, ProjectOrigin.HISTORY).forEach { origin ->
            val detail = ProjectDetail(id, origin)
            val expected = listOf(Home, NameSearch, detail, ProjectEdit(id, origin))
            assertEquals(expected, browsingAwareBackStack(current, canonicalBackStack(ProjectEdit(id, origin))))
            assertEquals(expected, normalizeBackStack(expected))
        }
        val sub = SubscriptionDetail(id, SubscriptionOrigin.HISTORY)
        assertEquals(listOf(Home, NameSearch, sub), browsingAwareBackStack(current, canonicalBackStack(sub)))
    }
    @Test fun adapter_keeps_a_selected_category_without_changing_record_identity() {
        val source = CategoryAnalysis(categoryId, subscription = true)
        val current = listOf(Home, Review, source)
        val detail = SubscriptionDetail(id, SubscriptionOrigin.CATEGORY)
        assertEquals(listOf(Home, Review, source, detail),
            browsingAwareBackStack(current, canonicalBackStack(detail)))
    }
    @Test fun adapter_does_not_capture_explicit_primary_switch_or_different_analysis_context() {
        val current = listOf(Home, NameSearch, ProjectDetail(id))
        listOf(canonicalBackStack(Home), canonicalBackStack(Review), canonicalBackStack(Settings),
            canonicalBackStack(ProjectDetail(id, ProjectOrigin.REVIEW))).forEach {
            assertEquals(it, browsingAwareBackStack(current, it))
        }
    }
    @Test fun adapter_does_not_rewrite_an_already_contextual_or_invalid_target() {
        val current = listOf(Home, NameSearch)
        val contextual = listOf(Home, NameSearch, ProjectDetail(id))
        val invalid = listOf(Home, NameSearch, ProjectDetail("invalid"))
        assertEquals(contextual, browsingAwareBackStack(current, contextual))
        assertEquals(invalid, browsingAwareBackStack(current, invalid))
        assertEquals(listOf(Home), normalizeBackStack(invalid))
    }

    @Test fun custom_category_parent_rejects_the_wrong_record_type_but_keeps_old_neutral_parent() {
        val project = ProjectDetail(id, ProjectOrigin.CATEGORY)
        val subscription = SubscriptionDetail(id, SubscriptionOrigin.CATEGORY)
        listOf(listOf(Home, Review, CategoryAnalysis(categoryId, true), project),
            listOf(Home, Review, CategoryAnalysis(categoryId, false), subscription),
            listOf(Home, Review, CategoryAnalysis(null, true), project)).forEach {
            assertEquals(listOf(Home), normalizeBackStack(it))
        }
        val canonical = canonicalBackStack(subscription)
        assertEquals(canonical, normalizeBackStack(canonical))
        assertEquals(canonicalBackStack(project), browsingAwareBackStack(
            listOf(Home, Review, CategoryAnalysis(categoryId, true)), canonicalBackStack(project)))
    }
    @Test fun project_delete_returns_to_the_existing_browse_source_only() {
        val search = listOf(Home, NameSearch, ProjectDetail(id), ProjectDelete(id))
        assertEquals(NameSearch, projectBrowseReturnAfterDelete(search))
        val category = CategoryAnalysis(categoryId)
        val selected = listOf(Home, Review, category, ProjectDetail(id, ProjectOrigin.CATEGORY),
            ProjectDelete(id, ProjectOrigin.CATEGORY))
        assertEquals(category, projectBrowseReturnAfterDelete(selected))
        assertEquals(ProjectHistory, projectBrowseReturnAfterDelete(canonicalBackStack(ProjectDelete(id, ProjectOrigin.HISTORY))))
        assertEquals(ProjectList, projectBrowseReturnAfterDelete(canonicalBackStack(ProjectDelete(id))))
    }

}
