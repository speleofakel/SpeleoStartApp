package com.speleo.start.data.local.dao

import androidx.room.*
import com.speleo.start.data.local.entity.MasterRouteCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterRouteCardDao {

    @Query("SELECT * FROM master_route_card WHERE competitionId = :competitionId ORDER BY sortOrder")
    fun getRouteCardByCompetition(competitionId: Long): Flow<List<MasterRouteCardEntity>>

    @Query("SELECT * FROM master_route_card WHERE id = :id")
    suspend fun getCheckpointById(id: Long): MasterRouteCardEntity?

    @Insert
    suspend fun insert(checkpoint: MasterRouteCardEntity): Long

    @Update
    suspend fun update(checkpoint: MasterRouteCardEntity)

    @Query("DELETE FROM master_route_card WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM master_route_card WHERE competitionId = :competitionId")
    suspend fun deleteAllForCompetition(competitionId: Long)

    @Query("DELETE FROM master_route_card")
    suspend fun deleteAll()
}