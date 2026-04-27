package com.speleo.start.domain.usecase

import com.speleo.start.data.repository.TeamRepository
import javax.inject.Inject

class SetTeamFinishTimestampUseCase @Inject constructor(
    private val repository: TeamRepository
) {
    suspend operator fun invoke(teamId: Long, timestamp: Long) =
        repository.setFinishTimestamp(teamId, timestamp)
}