package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = CompetitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["competitionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["competitionId"])]
)
data class CheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val competitionId: Long,
    val displayNumber: Int,
    val weight: Int = 1,
    val type: String = "normal",
    val sortOrder: Int,
    val normativeSeconds: Int = 0,
    val forClass2: Boolean = true,
    val forClass3: Boolean = true,
    val trackWaitTime: Boolean = false,
    val trackExecutionTime: Boolean = false,
    val bonusPoints: Int = 0
)