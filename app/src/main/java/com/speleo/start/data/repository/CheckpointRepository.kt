package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.CheckpointDao
import com.speleo.start.data.local.entity.CheckpointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckpointRepository @Inject constructor(
    private val dao: CheckpointDao
) {
    fun getByCompetition(id: Long): Flow<List<CheckpointEntity>> = dao.getByCompetition(id)

    suspend fun addCheckpoint(
        competitionId: Long, displayNumber: Int, weight: Int,
        type: String, sortOrder: Int, forClass2: Boolean, forClass3: Boolean
    ) = dao.insert(CheckpointEntity(
        competitionId = competitionId, displayNumber = displayNumber, weight = weight,
        type = type, sortOrder = sortOrder, forClass2 = forClass2, forClass3 = forClass3
    ))

    suspend fun deleteAllForCompetition(id: Long) = dao.deleteAllForCompetition(id)
}