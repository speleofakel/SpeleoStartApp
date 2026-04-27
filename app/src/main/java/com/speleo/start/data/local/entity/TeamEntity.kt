package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val competitionId: Long,
    val teamNumber: Int,
    val className: String,
    val status: String = "registered",
    val colorMark: String = "green",
    val startTimestamp: Long? = null,
    val finishTimestamp: Long? = null,
    val skipCount: Int = 0,
    val psrResolved: Boolean = false,
    val checkpointsEntered: Boolean = false,
    val finalPlace: Int? = null,
    val placeMerged: Boolean = false,
    val mergedWith: String? = null
)