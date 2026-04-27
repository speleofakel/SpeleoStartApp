package com.speleo.start.domain.usecase

import com.speleo.start.data.repository.TeamRepository
import javax.inject.Inject

class CreateTeamUseCase @Inject constructor(
    private val repository: TeamRepository
) {
    suspend operator fun invoke(
        competitionId: Long,
        teamNumber: Int,
        className: String,
        colorMark: String = "green"
    ): Long {
        return repository.createTeam(
            competitionId = competitionId,
            teamNumber = teamNumber,
            className = className,
            colorMark = colorMark
        )
    }
}
