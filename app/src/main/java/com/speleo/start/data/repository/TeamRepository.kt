package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.TeamDao
import com.speleo.start.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val teamDao: TeamDao
) {
    suspend fun getMaxTeamNumber(competitionId: Long, className: String): Int? =
        teamDao.getMaxTeamNumber(competitionId, className)

    fun getTeamsByCompetition(competitionId: Long): Flow<List<TeamEntity>> =
        teamDao.getTeamsByCompetition(competitionId)

    fun getRegisteredTeamsByClass(competitionId: Long, className: String): Flow<List<TeamEntity>> =
        teamDao.getRegisteredTeamsByClass(competitionId, className)

    fun getStartedTeams(competitionId: Long): Flow<List<TeamEntity>> =
        teamDao.getStartedTeams(competitionId)

    fun getFinishedTeams(competitionId: Long): Flow<List<TeamEntity>> =
        teamDao.getFinishedTeams(competitionId)

    suspend fun getTeamById(id: Long): TeamEntity? =
        teamDao.getTeamById(id)

    suspend fun getTeamCountByClass(competitionId: Long, className: String): Int =
        teamDao.getTeamCountByClass(competitionId, className)

    suspend fun createTeam(
        competitionId: Long,
        teamNumber: Int,
        className: String,
        colorMark: String = "green"
    ): Long {
        val team = TeamEntity(
            competitionId = competitionId,
            teamNumber = teamNumber,
            className = className,
            status = "registered",
            colorMark = colorMark
        )
        return teamDao.insert(team)
    }

    suspend fun updateTeam(team: TeamEntity) =
        teamDao.update(team)

    suspend fun updateTeamStatus(id: Long, status: String) =
        teamDao.updateStatus(id, status)

    suspend fun setStartTimestamp(id: Long, timestamp: Long) =
        teamDao.setStartTimestamp(id, timestamp)

    suspend fun setFinishTimestamp(id: Long, timestamp: Long) =
        teamDao.setFinishTimestamp(id, timestamp)

    suspend fun incrementSkipCount(id: Long) =
        teamDao.incrementSkipCount(id)

    suspend fun deleteTeam(id: Long) =
        teamDao.delete(id)
}