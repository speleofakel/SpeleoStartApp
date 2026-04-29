package com.speleo.start.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lastName: String,
    val firstName: String,
    val middleName: String? = null,
    val nickname: String? = null,
    val birthDate: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val note: String? = null,
    val blacklisted: Boolean = false,
    val blacklistReason: String? = null,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)