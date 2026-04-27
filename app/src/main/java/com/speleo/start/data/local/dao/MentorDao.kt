package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.speleo.start.data.local.entity.MentorEntity

@Dao
interface MentorDao {

    @Query("SELECT * FROM mentors WHERE personId = :personId")
    suspend fun getMentorByPersonId(personId: Long): MentorEntity?

    @Query("""
        SELECT m.* FROM mentors m
        JOIN participants p ON m.id = p.mentorId
        WHERE p.id = :participantId
    """)
    suspend fun getMentorByParticipantId(participantId: Long): MentorEntity?
    @Query("DELETE FROM mentors")
    suspend fun deleteAll()
    @Insert
    suspend fun insert(mentor: MentorEntity): Long

    @Query("DELETE FROM mentors WHERE id = :id")
    suspend fun delete(id: Long)
}
