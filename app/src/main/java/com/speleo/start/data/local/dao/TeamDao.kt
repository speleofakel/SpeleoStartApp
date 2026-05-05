package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.speleo.start.data.local.entity.ParticipantEntity
import com.speleo.start.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams WHERE competitionId = :competitionId ORDER BY className, teamNumber")
    fun getTeamsByCompetition(competitionId: Long): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE competitionId = :competitionId AND status = 'registered' AND className = :className ORDER BY teamNumber")
    fun getRegisteredTeamsByClass(competitionId: Long, className: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE competitionId = :competitionId AND status = 'started'")
    fun getStartedTeams(competitionId: Long): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE competitionId = :competitionId AND status = 'finished'")
    fun getFinishedTeams(competitionId: Long): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: Long): TeamEntity?

    @Insert
    suspend fun insert(team: TeamEntity): Long

    @Update
    suspend fun update(team: TeamEntity)

    @Query("UPDATE teams SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE teams SET startTimestamp = :timestamp WHERE id = :id")
    suspend fun setStartTimestamp(id: Long, timestamp: Long)

    // ❌ Убрано автоматическое изменение статуса — статус меняется отдельно после проверки КВ
    @Query("UPDATE teams SET finishTimestamp = :timestamp WHERE id = :id")
    suspend fun setFinishTimestamp(id: Long, timestamp: Long)

    @Query("UPDATE teams SET skipCount = skipCount + 1 WHERE id = :id")
    suspend fun incrementSkipCount(id: Long)

    @Query("SELECT COUNT(*) FROM teams WHERE competitionId = :competitionId AND className = :className")
    suspend fun getTeamCountByClass(competitionId: Long, className: String): Int

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM teams")
    suspend fun deleteAll()

    @Query("SELECT MAX(teamNumber) FROM teams WHERE competitionId = :competitionId AND className = :className")
    suspend fun getMaxTeamNumber(competitionId: Long, className: String): Int?

    @Query("SELECT * FROM participants WHERE teamId = :teamId")
    fun getParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>>
}