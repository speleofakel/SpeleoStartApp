package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.CompetitionEntity
import com.speleo.start.data.repository.CompetitionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveCompetitionsUseCase @Inject constructor(
    private val repository: CompetitionRepository
) {
    operator fun invoke(): Flow<List<CompetitionEntity>> =
        repository.getActiveCompetitions()
}