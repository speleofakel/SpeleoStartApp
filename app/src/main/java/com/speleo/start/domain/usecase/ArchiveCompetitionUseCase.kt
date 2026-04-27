package com.speleo.start.domain.usecase

import com.speleo.start.data.repository.CompetitionRepository
import javax.inject.Inject

class ArchiveCompetitionUseCase @Inject constructor(
    private val repository: CompetitionRepository
) {
    suspend operator fun invoke(id: Long) =
        repository.archiveCompetition(id)
}