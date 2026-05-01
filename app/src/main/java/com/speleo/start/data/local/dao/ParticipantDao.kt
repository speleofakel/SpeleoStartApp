package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.speleo.start.data.local.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants WHERE teamId = :teamId AND statusMember = 'active'")
    fun getActiveParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE teamId = :teamId")
    fun getAllParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>>

    @Query("""
        SELECT * FROM participants
        WHERE statusMember = 'free_agent'
        AND teamId IN (SELECT id FROM teams WHERE competitionId = :competitionId)
    """)
    fun getFreeAgents(competitionId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE id = :id")
    suspend fun getParticipantById(id: Long): ParticipantEntity?

    @Query("""
        SELECT p.* FROM participants p
        JOIN teams t ON p.teamId = t.id
        WHERE p.personId = :personId 
        AND t.competitionId = :competitionId 
        AND p.statusMember = 'active'
        LIMIT 1
    """)
    suspend fun findActiveByPersonAndComp(personId: Long, competitionId: Long): ParticipantEntity?

    @Insert
    suspend fun insert(participant: ParticipantEntity): Long

    @Update
    suspend fun update(participant: ParticipantEntity)

    @Query("UPDATE participants SET statusMember = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE participants SET mentorId = :mentorId, mentorConfirmed = :confirmed WHERE id = :id")
    suspend fun updateMentor(id: Long, mentorId: Long?, confirmed: Boolean)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM participants")
    suspend fun deleteAll()

    @Query("""
        UPDATE participants
        SET mentorId = :mentorId,
            mentorConfirmed = :mentorConfirmed,
            judgeApproved = :judgeApproved
        WHERE id = :id
    """)
    suspend fun updateMentorAndFlags(
        id: Long,
        mentorId: Long?,
        mentorConfirmed: Boolean,
        judgeApproved: Boolean
    )
}