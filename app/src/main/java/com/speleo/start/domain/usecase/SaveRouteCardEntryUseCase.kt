package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.TeamRouteCardEntity
import com.speleo.start.data.repository.TeamRouteCardRepository
import javax.inject.Inject

class SaveRouteCardEntryUseCase @Inject constructor(
    private val repository: TeamRouteCardRepository
) {
    suspend operator fun invoke(entry: TeamRouteCardEntity) =
        repository.saveEntry(entry)
}
