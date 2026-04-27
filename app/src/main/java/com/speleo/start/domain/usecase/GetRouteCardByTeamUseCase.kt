package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.TeamRouteCardEntity
import com.speleo.start.data.repository.TeamRouteCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRouteCardByTeamUseCase @Inject constructor(
    private val repository: TeamRouteCardRepository
) {
    operator fun invoke(teamId: Long): Flow<List<TeamRouteCardEntity>> =
        repository.getRouteCardByTeam(teamId)
}
