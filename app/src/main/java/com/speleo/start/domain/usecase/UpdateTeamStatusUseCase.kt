package com.speleo.start.domain.usecase

import com.speleo.start.data.repository.TeamRepository
import javax.inject.Inject

class UpdateTeamStatusUseCase @Inject constructor(
    private val repository: TeamRepository
) {
    suspend operator fun invoke(teamId: Long, status: String) =
        repository.updateTeamStatus(teamId, status)
}