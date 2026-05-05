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

    fun getParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getParticipantsByTeam(teamId)

    fun getAllParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getAllParticipantsByTeam(teamId)

    fun getActiveParticipantsByTeam(teamId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getActiveParticipantsByTeam(teamId)

    fun getParticipantsByPerson(personId: Long): Flow<List<ParticipantEntity>> =
        participantDao.getParticipantsByPerson(personId)

    suspend fun getParticipantById(id: Long): ParticipantEntity? =
        participantDao.getParticipantById(id)

    suspend fun createParticipant(
        teamId: Long,
        personId: Long,
        role: String = "member",
        status: String = "active",
        mentorId: Long? = null,
        mentorConfirmed: Boolean = false,
        judgeApproved: Boolean = false
    ): Long {
        val participant = ParticipantEntity(
            teamId = teamId,
            personId = personId,
            role = role,
            status = status,
            mentorId = mentorId,
            mentorConfirmed = mentorConfirmed,
            judgeApproved = judgeApproved
        )
        return participantDao.insert(participant)
    }

    suspend fun addParticipant(
        teamId: Long,
        personId: Long,
        role: String = "member",
        mentorId: Long? = null
    ): Long {
        return createParticipant(
            teamId = teamId,
            personId = personId,
            role = role,
            mentorId = mentorId
        )
    }

    suspend fun updateParticipant(participant: ParticipantEntity) =
        participantDao.update(participant)

    suspend fun updateStatus(id: Long, status: String) =
        participantDao.updateStatus(id, status)

    // Алиас для updateStatus — используется в TeamCardVM
    suspend fun updateParticipantStatus(id: Long, status: String) =
        participantDao.updateStatus(id, status)

    suspend fun updateMentor(id: Long, mentorId: Long?, confirmed: Boolean) =
        participantDao.updateMentor(id, mentorId, confirmed)

    suspend fun updateMentorAndFlags(
        id: Long,
        mentorId: Long?,
        mentorConfirmed: Boolean,
        judgeApproved: Boolean
    ) {
        val participant = participantDao.getParticipantById(id) ?: return
        val updated = participant.copy(
            mentorId = mentorId,
            mentorConfirmed = mentorConfirmed,
            judgeApproved = judgeApproved
        )
        participantDao.update(updated)
    }

    suspend fun deleteParticipant(id: Long) =
        participantDao.delete(id)

    suspend fun deleteByTeam(teamId: Long) =
        participantDao.deleteByTeam(teamId)

    suspend fun countActiveByTeam(teamId: Long): Int =
        participantDao.countActiveByTeam(teamId)

    suspend fun insertAll(participants: List<ParticipantEntity>): List<Long> =
        participantDao.insertAll(participants)

    suspend fun findActiveByPersonAndComp(personId: Long, competitionId: Long): ParticipantEntity? =
        participantDao.findActiveByPersonAndComp(personId, competitionId)
}