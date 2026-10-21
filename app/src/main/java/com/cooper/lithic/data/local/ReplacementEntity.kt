package com.cooper.lithic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "replacements", foreignKeys = [
    ForeignKey(entity = LongTermProjectEntity::class, parentColumns = ["id"], childColumns = ["predecessorId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = LongTermProjectEntity::class, parentColumns = ["id"], childColumns = ["successorId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index(value = ["predecessorId"], unique = true), Index(value = ["successorId"], unique = true)])
data class ReplacementEntity(@PrimaryKey val id: String, val predecessorId: String, val successorId: String, val createdAt: Instant)
