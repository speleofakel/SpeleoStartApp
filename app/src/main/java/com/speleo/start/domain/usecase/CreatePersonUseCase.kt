package com.speleo.start.domain.usecase

import com.speleo.start.data.repository.PersonRepository
import javax.inject.Inject

class CreatePersonUseCase @Inject constructor(
    private val repository: PersonRepository
) {
    suspend operator fun invoke(
        lastName: String,
        firstName: String,
        middleName: String? = null,
        birthDate: String? = null,
        phone: String? = null,
        email: String? = null,
        gender: String? = null,
        note: String? = null
    ): Long {
        return repository.createPerson(
            lastName = lastName,
            firstName = firstName,
            middleName = middleName,
            birthDate = birthDate,
            phone = phone,
            email = email,
            gender = gender,
            note = note
        )
    }
}