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
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["teamId"]), Index(value = ["personId"])]
)
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: Long,
    val personId: Long,
    val mentorId: Long? = null,
    val role: String = "member",
    val statusMember: String = "active",
    val judgeApproved: Boolean = false,
    val mentorConfirmed: Boolean = false,
    val replacedAt: Long? = null,
    val replacedBy: String? = null
)