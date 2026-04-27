package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.PersonDao
import com.speleo.start.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val personDao: PersonDao
) {

    fun getAllPersons(): Flow<List<PersonEntity>> =
        personDao.getAllPersons()

    fun searchPersons(query: String, gender: String? = null): Flow<List<PersonEntity>> =
        personDao.searchPersons(query, gender)

    suspend fun getPersonById(id: Long): PersonEntity? =
        personDao.getPersonById(id)

    suspend fun findPerson(lastName: String, firstName: String, birthDate: String?): PersonEntity? =
        personDao.findPerson(lastName, firstName, birthDate)

    suspend fun createPerson(
        lastName: String,
        firstName: String,
        middleName: String? = null,
        birthDate: String? = null,
        phone: String? = null,
        email: String? = null,
        gender: String? = null,
        note: String? = null
    ): Long {
        val person = PersonEntity(
            lastName = lastName,
            firstName = firstName,
            middleName = middleName,
            birthDate = birthDate,
            phone = phone,
            email = email,
            gender = gender,
            note = note
        )
        return personDao.insert(person)
    }

    suspend fun updatePerson(person: PersonEntity) =
        personDao.update(person)

    suspend fun blacklistPerson(id: Long, reason: String) =
        personDao.blacklist(id, reason)

    suspend fun unblacklistPerson(id: Long) =
        personDao.unblacklist(id)

    suspend fun deletePerson(id: Long) =
        personDao.delete(id)
}