package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.speleo.start.data.local.entity.CheckpointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckpointDao {

    @Query("SELECT * FROM checkpoints WHERE competitionId = :competitionId ORDER BY sortOrder")
    fun getByCompetition(competitionId: Long): Flow<List<CheckpointEntity>>

    @Insert
    suspend fun insert(checkpoint: CheckpointEntity): Long

    @Query("DELETE FROM checkpoints WHERE competitionId = :competitionId")
    suspend fun deleteAllForCompetition(competitionId: Long)
}