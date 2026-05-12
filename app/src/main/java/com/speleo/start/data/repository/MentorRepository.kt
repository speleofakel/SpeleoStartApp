package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.MentorDao
import com.speleo.start.data.local.entity.MentorEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MentorRepository @Inject constructor(
    private val mentorDao: MentorDao
) {

    suspend fun getMentorByPersonId(personId: Long): MentorEntity? =
        mentorDao.getMentorByPersonId(personId)

    suspend fun getMentorByParticipantId(participantId: Long): MentorEntity? =
        mentorDao.getMentorByParticipantId(participantId)

    suspend fun getMentorById(id: Long): MentorEntity? = mentorDao.getMentorById(id)


    suspend fun createMentor(personId: Long): Long {
        val mentor = MentorEntity(personId = personId)
        return mentorDao.insert(mentor)
    }

    suspend fun deleteMentor(id: Long) =
        mentorDao.delete(id)
}