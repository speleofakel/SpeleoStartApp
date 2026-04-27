package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.TeamEntity
import com.speleo.start.data.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStartedTeamsUseCase @Inject constructor(
    private val repository: TeamRepository
) {
    operator fun invoke(competitionId: Long): Flow<List<TeamEntity>> =
        repository.getStartedTeams(competitionId)
}
