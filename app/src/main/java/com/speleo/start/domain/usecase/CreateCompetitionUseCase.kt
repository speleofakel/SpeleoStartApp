package com.speleo.start.domain.usecase

import com.speleo.start.data.repository.CompetitionRepository
import javax.inject.Inject

class CreateCompetitionUseCase @Inject constructor(
    private val repository: CompetitionRepository
) {
    suspend operator fun invoke(
        name: String,
        shortName: String = "",
        date: String,
        place: String,
        discipline: String = "underground",
        system: String? = null
    ): Long {
        return repository.createCompetition(name, shortName, date, place, discipline, system)
    }
}