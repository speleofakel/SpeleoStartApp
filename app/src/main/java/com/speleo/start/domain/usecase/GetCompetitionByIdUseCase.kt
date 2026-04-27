package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.CompetitionEntity
import com.speleo.start.data.repository.CompetitionRepository
import javax.inject.Inject

class GetCompetitionByIdUseCase @Inject constructor(
    private val repository: CompetitionRepository
) {
    suspend operator fun invoke(id: Long): CompetitionEntity? =
        repository.getCompetitionById(id)
}