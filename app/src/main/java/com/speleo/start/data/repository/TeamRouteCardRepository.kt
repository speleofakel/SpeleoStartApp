package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.TeamRouteCardDao
import com.speleo.start.data.local.entity.TeamRouteCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
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
        Timber.d("saveEntry: teamId=${entry.teamId}, checkpointId=${entry.checkpointId}, taken=${entry.taken}")
        val existing = teamRouteCardDao.getEntry(entry.teamId, entry.checkpointId)
        Timber.d("existing: $existing")

        if (existing != null) {
            Timber.d("Updating existing entry")
            teamRouteCardDao.update(entry)
        } else {
            Timber.d("Inserting new entry")
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

    suspend fun getRouteCardByTeamFirst(teamId: Long): List<TeamRouteCardEntity> =
        teamRouteCardDao.getRouteCardByTeam(teamId).first()
}