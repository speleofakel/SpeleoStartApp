package com.speleo.start.data.repository

import com.speleo.start.data.local.dao.CompetitionDao
import com.speleo.start.data.local.entity.CompetitionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompetitionRepository @Inject constructor(
    private val competitionDao: CompetitionDao
) {

    fun getActiveCompetitions(): Flow<List<CompetitionEntity>> =
        competitionDao.getActiveCompetitions()

    fun getArchivedCompetitions(): Flow<List<CompetitionEntity>> =
        competitionDao.getArchivedCompetitions()

    suspend fun getCompetitionById(id: Long): CompetitionEntity? =
        competitionDao.getCompetitionById(id)

    suspend fun createCompetition(
        name: String, shortName: String, date: String, place: String,
        discipline: String = "underground", system: String? = null
    ): Long {
        val competition = CompetitionEntity(
            name = name, shortName = shortName, date = date, place = place,
            discipline = discipline, system = system
        )
        return competitionDao.insert(competition)
    }

    suspend fun updateCompetition(competition: CompetitionEntity) =
        competitionDao.update(competition)

    suspend fun archiveCompetition(id: Long) =
        competitionDao.archive(id)

    suspend fun deleteCompetition(id: Long) =
        competitionDao.delete(id)
}