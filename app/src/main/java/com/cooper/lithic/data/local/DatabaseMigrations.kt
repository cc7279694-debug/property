package com.cooper.lithic.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Each upgrade is explicit and leaves existing business facts untouched. */
object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE `subscriptions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `coverAssetId` TEXT, `note` TEXT, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`coverAssetId`) REFERENCES `local_media_assets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            db.execSQL("CREATE INDEX `index_subscriptions_categoryId` ON `subscriptions` (`categoryId`)")
            db.execSQL("CREATE INDEX `index_subscriptions_coverAssetId` ON `subscriptions` (`coverAssetId`)")
            db.execSQL("""CREATE TABLE `subscription_renewals` (`id` TEXT NOT NULL, `subscriptionId` TEXT NOT NULL, `paidOn` INTEGER NOT NULL, `effectiveFrom` INTEGER NOT NULL, `paidUntilExclusive` INTEGER NOT NULL, `reminderDueOn` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `cycle` TEXT NOT NULL, `standardDays` INTEGER NOT NULL, `operationId` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subscriptionId`) REFERENCES `subscriptions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            db.execSQL("CREATE UNIQUE INDEX `index_subscription_renewals_operationId` ON `subscription_renewals` (`operationId`)")
            db.execSQL("CREATE INDEX `index_subscription_renewals_subscriptionId_paidOn_id` ON `subscription_renewals` (`subscriptionId`, `paidOn`, `id`)")
            db.execSQL("CREATE INDEX `index_subscription_renewals_subscriptionId_effectiveFrom_id` ON `subscription_renewals` (`subscriptionId`, `effectiveFrom`, `id`)")
            db.execSQL("""CREATE TABLE `subscription_state_changes` (`id` TEXT NOT NULL, `subscriptionId` TEXT NOT NULL, `effectiveOn` INTEGER NOT NULL, `state` TEXT NOT NULL, `reason` TEXT, `note` TEXT, `operationId` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subscriptionId`) REFERENCES `subscriptions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            db.execSQL("CREATE UNIQUE INDEX `index_subscription_state_changes_operationId` ON `subscription_state_changes` (`operationId`)")
            db.execSQL("CREATE UNIQUE INDEX `index_subscription_state_changes_subscriptionId_effectiveOn` ON `subscription_state_changes` (`subscriptionId`, `effectiveOn`)")
            db.execSQL("CREATE INDEX `index_subscription_state_changes_subscriptionId_effectiveOn_id` ON `subscription_state_changes` (`subscriptionId`, `effectiveOn`, `id`)")
        }
    }
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
