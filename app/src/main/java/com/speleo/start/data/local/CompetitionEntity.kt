package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "competitions")
data class CompetitionEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val name: String,
        val shortName: String = "",
        val date: String,
        val place: String,
        val discipline: String = "underground",
        val system: String? = null,
        val settingsJson: String = "{}",
        val isArchived: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
)