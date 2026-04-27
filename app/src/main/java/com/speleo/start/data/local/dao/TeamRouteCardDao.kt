package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.speleo.start.data.local.entity.TeamRouteCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamRouteCardDao {

    @Query("""
        SELECT trc.* FROM team_route_card trc
        JOIN master_route_card mrc ON trc.checkpointId = mrc.id
        WHERE trc.teamId = :teamId
        ORDER BY mrc.sortOrder
    """)
    fun getRouteCardByTeam(teamId: Long): Flow<List<TeamRouteCardEntity>>

    @Query("SELECT * FROM team_route_card WHERE teamId = :teamId AND checkpointId = :checkpointId")
    suspend fun getEntry(teamId: Long, checkpointId: Long): TeamRouteCardEntity?

    @Query("""
        SELECT COUNT(*) FROM team_route_card 
        WHERE teamId = :teamId 
        AND (judgeConfirmed = 0 OR secretaryConfirmed = 0)
    """)
    suspend fun getPendingCount(teamId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TeamRouteCardEntity): Long

    @Update
    suspend fun update(entry: TeamRouteCardEntity)

    @Query("""
        UPDATE team_route_card 
        SET taken = :taken, takenWithError = :takenWithError, 
            offsetTime = :offsetTime, penalty = :penalty
        WHERE teamId = :teamId AND checkpointId = :checkpointId
    """)
    suspend fun updateEntry(
        teamId: Long,
        checkpointId: Long,
        taken: Boolean,
        takenWithError: Boolean,
        offsetTime: Long?,
        penalty: Int
    )

    @Query("""
        UPDATE team_route_card 
        SET secretaryConfirmed = 1 
        WHERE teamId = :teamId AND checkpointId = :checkpointId
    """)
    suspend fun confirmBySecretary(teamId: Long, checkpointId: Long)

    @Query("""
        UPDATE team_route_card 
        SET judgeConfirmed = 1, confirmedAt = :timestamp
        WHERE teamId = :teamId AND checkpointId = :checkpointId
    """)
    suspend fun confirmByJudge(teamId: Long, checkpointId: Long, timestamp: Long)

    @Query("DELETE FROM team_route_card WHERE teamId = :teamId")
    suspend fun deleteAllForTeam(teamId: Long)
}