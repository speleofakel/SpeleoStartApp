package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.MasterRouteCardDao
import com.speleo.start.data.local.entity.MasterRouteCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterRouteCardRepository @Inject constructor(
    private val masterRouteCardDao: MasterRouteCardDao
) {

    fun getRouteCardByCompetition(competitionId: Long): Flow<List<MasterRouteCardEntity>> =
        masterRouteCardDao.getRouteCardByCompetition(competitionId)



    suspend fun getCheckpointById(id: Long): MasterRouteCardEntity? =
        masterRouteCardDao.getCheckpointById(id)


    suspend fun addCheckpoint(
        competitionId: Long,
        displayNumber: Int,
        weight: Int = 1,
        type: String = "normal",
        sortOrder: Int,
        normativeSeconds: Int = 0,
        forClass2: Boolean = true,
        forClass3: Boolean = true,
        trackWaitTime: Boolean = false,
        trackExecutionTime: Boolean = false,
        bonusPoints: Int = 0
    ): Long {
        val checkpoint = MasterRouteCardEntity(
            competitionId = competitionId,
            displayNumber = displayNumber,
            weight = weight,
            type = type,
            sortOrder = sortOrder,
            normativeSeconds = normativeSeconds,
            forClass2 = forClass2,
            forClass3 = forClass3,
            trackWaitTime = trackWaitTime,
            trackExecutionTime = trackExecutionTime,
            bonusPoints = bonusPoints
        )
        return masterRouteCardDao.insert(checkpoint)
    }

    suspend fun updateCheckpoint(checkpoint: MasterRouteCardEntity) =
        masterRouteCardDao.update(checkpoint)

    suspend fun deleteCheckpoint(id: Long) =
        masterRouteCardDao.delete(id)

    suspend fun deleteAllForCompetition(competitionId: Long) =
        masterRouteCardDao.deleteAllForCompetition(competitionId)


    suspend fun getRouteCardByCompetitionFirst(competitionId: Long): List<MasterRouteCardEntity> =
        masterRouteCardDao.getRouteCardByCompetition(competitionId).first()
}