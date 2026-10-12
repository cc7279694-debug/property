package com.cooper.lithic.feature.project

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.*
import com.cooper.lithic.core.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectRoutesTest {
    private val id = "a0a0a0a0-1111-4444-8888-aaaaaaaaaaaa"
    @Test fun project_routes_keep_stable_identity_and_bounded_parents() {
        assertEquals(listOf(Home, ProjectList, ProjectDetail(id)), canonicalBackStack(ProjectDetail(id)))
        assertEquals(listOf(Home, ProjectList, ProjectDetail(id), ProjectEdit(id)), canonicalBackStack(ProjectEdit(id)))
        assertEquals(listOf(Home, ProjectList, ProjectDetail(id), ProjectMoneyEntry(id, MoneyDirection.RECOVERY)), canonicalBackStack(ProjectMoneyEntry(id, MoneyDirection.RECOVERY)))
    }
    @Test fun creation_restores_its_actual_home_or_list_parent() {
        assertEquals(listOf(Home, CreateProject), normalizeBackStack(listOf(Home, CreateProject)))
        assertEquals(listOf(Home, ProjectList, CreateProject), normalizeBackStack(listOf(Home, ProjectList, CreateProject)))
    }
    @Test fun mismatched_or_invalid_project_routes_restore_safely_home() {
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail("not-a-uuid")))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ProjectList, ProjectDetail(id), ProjectEdit("b0b0b0b0-1111-4444-8888-bbbbbbbbbbbb"))))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ProjectList, ProjectDetail(id), ProjectEdit(id), ProjectEdit(id))))
    }
}
