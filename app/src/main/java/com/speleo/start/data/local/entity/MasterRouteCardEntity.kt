package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_route_card",
    foreignKeys = [
        ForeignKey(
            entity = CompetitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["competitionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MasterRouteCardEntity(
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