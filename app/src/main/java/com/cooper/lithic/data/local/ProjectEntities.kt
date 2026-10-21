package com.cooper.lithic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cooper.lithic.core.model.MoneyDirection
import java.time.Instant
import java.time.LocalDate

enum class CategoryType { PROJECT, SUBSCRIPTION }
enum class ProjectMoneyEventRole { INITIAL, REGULAR, END_SETTLEMENT }
enum class MoneyBasis { CASH, TRADE_IN_CREDIT }

@Entity(tableName = "categories", indices = [Index(value = ["type", "normalizedName"], unique = true)])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val type: CategoryType,
    val name: String,
    val normalizedName: String,
    val isFallback: Boolean,
) {
    init {
        StoredValueValidation.requireUuid(id)
        require(name.isNotBlank()) { "Category names cannot be blank" }
        require(normalizedName.isNotBlank() && normalizedName == CategoryNames.normalized(name)) {
            "The comparison name must match the normalized display name"
        }
    }
}

@Entity(tableName = "local_media_assets", indices = [Index(value = ["relativePath"], unique = true)])
data class LocalMediaAssetEntity(
    @PrimaryKey val id: String,
    val relativePath: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val byteLength: Long,
    val sha256: String,
) {
    init {
        StoredValueValidation.requireUuid(id)
        require(StoredValueValidation.isMediaPath(relativePath)) { "Media paths must stay inside the generation's media directory" }
        require(mime.isNotBlank() && mime.none { it.code < 32 || it.code == 127 }) { "Media MIME metadata is invalid" }
        require(width > 0 && height > 0) { "Media dimensions must be positive" }
        require(byteLength >= 0) { "Media byte length cannot be negative" }
        require(Regex("[0-9a-fA-F]{64}").matches(sha256)) { "SHA-256 must contain exactly 64 hexadecimal characters" }
    }
}

@Entity(
    tableName = "long_term_projects",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = LocalMediaAssetEntity::class, parentColumns = ["id"], childColumns = ["coverAssetId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("categoryId"), Index("coverAssetId"), Index(value = ["startedOn", "id"])],
)
data class LongTermProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val startedOn: LocalDate,
    val endedOn: LocalDate? = null,
    val endReason: String? = null,
    val coverAssetId: String? = null,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "project_money_events",
    foreignKeys = [ForeignKey(entity = LongTermProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["projectId", "occurredOn", "id"]), Index(value = ["operationId"], unique = true)],
)
data class ProjectMoneyEventEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val direction: MoneyDirection,
    val amountCents: Long,
    val occurredOn: LocalDate,
    val description: String? = null,
    val role: ProjectMoneyEventRole,
    val operationId: String,
    @androidx.room.ColumnInfo(defaultValue = "'CASH'") val basis: MoneyBasis = MoneyBasis.CASH,
    val exchangeGroupId: String? = null,
)
