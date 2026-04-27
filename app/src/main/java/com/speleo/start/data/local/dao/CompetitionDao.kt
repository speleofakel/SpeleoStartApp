package com.speleo.start.data.local.dao

import androidx.room.*
import com.speleo.start.data.local.entity.CompetitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetitionDao {

    @Query("SELECT * FROM competitions WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE id = :id")
    suspend fun getCompetitionById(id: Long): CompetitionEntity?

    @Insert
    suspend fun insert(competition: CompetitionEntity): Long

    @Update
    suspend fun update(competition: CompetitionEntity)

    @Query("UPDATE competitions SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM competitions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM competitions")
    suspend fun deleteAll()
}