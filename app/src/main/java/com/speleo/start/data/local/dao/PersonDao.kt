package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.speleo.start.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY lastName, firstName")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("""
        SELECT * FROM persons 
        WHERE (REPLACE(REPLACE(lastName, 'Ё', 'Е'), 'ё', 'е') LIKE REPLACE(REPLACE(:query, 'Ё', 'Е'), 'ё', 'е') || '%')
        AND (:gender IS NULL OR gender = :gender)
        AND blacklisted = 0
        ORDER BY lastName, firstName 
        LIMIT 10
    """)
    fun searchPersons(query: String, gender: String?): Flow<List<PersonEntity>>

    @Query("DELETE FROM persons")
    suspend fun deleteAll()

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("""
        SELECT * FROM persons 
        WHERE lastName = :lastName 
        AND firstName = :firstName 
        AND (birthDate = :birthDate OR birthDate IS NULL)
    """)
    suspend fun findPerson(lastName: String, firstName: String, birthDate: String?): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Query("UPDATE persons SET blacklisted = 1, blacklistReason = :reason WHERE id = :id")
    suspend fun blacklist(id: Long, reason: String)

    @Query("UPDATE persons SET blacklisted = 0, blacklistReason = NULL WHERE id = :id")
    suspend fun unblacklist(id: Long)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun delete(id: Long)
}