package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.TeamRouteCardDao
import com.speleo.start.data.local.entity.TeamRouteCardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRouteCardRepository @Inject constructor(
    private val teamRouteCardDao: TeamRouteCardDao
) {

    fun getRouteCardByTeam(teamId: Long): Flow<List<TeamRouteCardEntity>> =
        teamRouteCardDao.getRouteCardByTeam(teamId)

    suspend fun getEntry(teamId: Long, checkpointId: Long): TeamRouteCardEntity? =
        teamRouteCardDao.getEntry(teamId, checkpointId)

    suspend fun getPendingCount(teamId: Long): Int =
        teamRouteCardDao.getPendingCount(teamId)

    suspend fun saveEntry(entry: TeamRouteCardEntity) {
        val existing = teamRouteCardDao.getEntry(entry.teamId, entry.checkpointId)
        if (existing != null) {
            teamRouteCardDao.update(entry)
        } else {
            teamRouteCardDao.insert(entry)
        }
    }

    suspend fun updateEntry(
        teamId: Long,
        checkpointId: Long,
        taken: Boolean,
        takenWithError: Boolean,
        offsetTime: Long?,
        penalty: Int
    ) = teamRouteCardDao.updateEntry(teamId, checkpointId, taken, takenWithError, offsetTime, penalty)

    suspend fun confirmBySecretary(teamId: Long, checkpointId: Long) =
        teamRouteCardDao.confirmBySecretary(teamId, checkpointId)

    suspend fun confirmByJudge(teamId: Long, checkpointId: Long, timestamp: Long) =
        teamRouteCardDao.confirmByJudge(teamId, checkpointId, timestamp)

    suspend fun deleteAllForTeam(teamId: Long) =
        teamRouteCardDao.deleteAllForTeam(teamId)
}