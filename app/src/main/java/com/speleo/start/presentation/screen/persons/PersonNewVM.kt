package com.speleo.start.presentation.screen.persons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonNewVM @Inject constructor(
    private val repository: PersonRepository
) : ViewModel() {

    fun savePerson(
        lastName: String,
        firstName: String,
        middleName: String?,
        birthDate: String?,
        phone: String?,
        gender: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createPerson(
                lastName = lastName,
                firstName = firstName,
                middleName = middleName,
                birthDate = birthDate,
                phone = phone,
                gender = gender
            )
            onSuccess()
        }

    }
    fun savePerson(
        lastName: String,
        firstName: String,
        middleName: String?,
        birthDate: String?,
        phone: String?,
        gender: String?,
        canBeMentor: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createPerson(
                lastName = lastName,
                firstName = firstName,
                middleName = middleName,
                birthDate = birthDate,
                phone = phone,
                gender = gender
            )
            onSuccess()
        }
    }
}