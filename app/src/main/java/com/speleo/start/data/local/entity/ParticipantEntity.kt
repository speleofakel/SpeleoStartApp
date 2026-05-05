package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["personId"]),
        Index(value = ["mentorId"])
    ]
)
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: Long,
    val personId: Long,
    val role: String = "member",      // captain | member
    val status: String = "active",   // active | replaced | free_agent
    val mentorId: Long? = null,
    val mentorConfirmed: Boolean = false,
    val judgeApproved: Boolean = false
)