package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.speleo.start.data.local.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {

    @Query("SELECT * FROM participants WHERE teamId = :teamId")
    fun getParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>>

    // Все участники команды (включая replaced, free_agent)
    @Query("SELECT * FROM participants WHERE teamId = :teamId")
    fun getAllParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>>

    // Только активные участники команды
    @Query("SELECT * FROM participants WHERE teamId = :teamId AND status = 'active'")
    fun getActiveParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE personId = :personId")
    fun getParticipantsByPerson(personId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE id = :id")
    suspend fun getParticipantById(id: Long): ParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(participant: ParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<ParticipantEntity>): List<Long>

    @Update
    suspend fun update(participant: ParticipantEntity)

    @Query("UPDATE participants SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE participants SET mentorId = :mentorId, mentorConfirmed = :confirmed WHERE id = :id")
    suspend fun updateMentor(id: Long, mentorId: Long?, confirmed: Boolean)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM participants WHERE teamId = :teamId")
    suspend fun deleteByTeam(teamId: Long)

    @Query("DELETE FROM participants")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM participants WHERE teamId = :teamId AND status = 'active'")
    suspend fun countActiveByTeam(teamId: Long): Int

    @Query("""
        SELECT p.* FROM participants p
        INNER JOIN teams t ON p.teamId = t.id
        WHERE p.personId = :personId 
        AND t.competitionId = :competitionId
        AND p.status = 'active'
        LIMIT 1
    """)
    suspend fun findActiveByPersonAndComp(personId: Long, competitionId: Long): ParticipantEntity?
}