package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.ParticipantDao
import com.speleo.start.data.local.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParticipantRepository @Inject constructor(
    private val participantDao: ParticipantDao
) {

    fun getActiveParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getActiveParticipantsByTeam(teamId)

    fun getAllParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getAllParticipantsByTeam(teamId)

    fun getFreeAgents(competitionId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getFreeAgents(competitionId)

    suspend fun getParticipantById(id: Long): ParticipantEntity? =
        participantDao.getParticipantById(id)

    suspend fun addParticipant(
        teamId: Long,
        personId: Long,
        role: String = "member",
        mentorId: Long? = null
    ): Long {
        val participant = ParticipantEntity(
            teamId = teamId,
            personId = personId,
            role = role,
            mentorId = mentorId,
            statusMember = "active"
        )
        return participantDao.insert(participant)
    }

    suspend fun updateParticipant(participant: ParticipantEntity) =
        participantDao.update(participant)

    suspend fun updateParticipantStatus(id: Long, status: String) =
        participantDao.updateStatus(id, status)

    suspend fun updateMentor(id: Long, mentorId: Long?, confirmed: Boolean) =
        participantDao.updateMentor(id, mentorId, confirmed)

    suspend fun deleteParticipant(id: Long) =
        participantDao.delete(id)
}