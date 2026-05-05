package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
    ],
    indices = [Index(value = ["competitionId"])]
)
data class MasterRouteCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val competitionId: Long,
    val displayNumber: Int,
    val weight: Int = 1,
    val type: String = "normal",  // "normal" или "technical"
    val sortOrder: Int,
    val normativeSeconds: Int = 0,
    val forClass2: Boolean = true,
    val forClass3: Boolean = true,
    val trackWaitTime: Boolean = false,      // ✅ Поле для миграции
    val trackExecutionTime: Boolean = false, // ✅ Поле для миграции
    val bonusPoints: Int = 0                 // ✅ Поле для миграции
)