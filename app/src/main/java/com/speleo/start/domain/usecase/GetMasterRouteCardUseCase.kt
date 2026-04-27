package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.MasterRouteCardEntity
import com.speleo.start.data.repository.MasterRouteCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMasterRouteCardUseCase @Inject constructor(
    private val repository: MasterRouteCardRepository
) {
    operator fun invoke(competitionId: Long): Flow<List<MasterRouteCardEntity>> =
        repository.getRouteCardByCompetition(competitionId)
}