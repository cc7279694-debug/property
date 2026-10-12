package com.cooper.lithic.data.repository

import androidx.room.withTransaction
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity

data class LedgerSnapshot(val categories: List<CategoryEntity>, val projects: List<LongTermProjectEntity>, val moneyEvents: List<ProjectMoneyEventEntity>, val mediaAssets: List<LocalMediaAssetEntity>)

class LedgerReadRepository(private val db: LithicDatabase, private val coordinator: LocalWriteCoordinator) {
    suspend fun snapshot(): LedgerSnapshot = coordinator.withSnapshot {
        db.withTransaction {
            LedgerSnapshot(
                categories = db.categoryDao().list(),
                projects = db.projectDao().listProjects(),
                moneyEvents = db.projectDao().listEvents(),
                mediaAssets = db.mediaDao().list(),
            )
        }
    }
}
