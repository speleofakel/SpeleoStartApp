package com.speleo.start.domain.usecase

import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchPersonsUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    operator fun invoke(query: String, gender: String? = null): Flow<List<PersonEntity>> =
        repository.searchPersons(query, gender)
}