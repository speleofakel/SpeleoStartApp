package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_route_card")
data class TeamRouteCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: Long,
    val checkpointId: Long,
    val taken: Boolean = false,
    val takenWithError: Boolean = false,
    val offsetTime: Long? = null,
    val penalty: Int = 0,
    val judgeConfirmed: Boolean = false,
    val secretaryConfirmed: Boolean = false,
    val confirmedAt: Long? = null
)