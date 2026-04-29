package com.speleo.start.presentation.screen.persons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonDetailVM @Inject constructor(
    private val personRepo: PersonRepository
) : ViewModel() {

    private val _person = MutableStateFlow<PersonEntity?>(null)
    val person: StateFlow<PersonEntity?> = _person.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPerson(personId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _person.value = personRepo.getPersonById(personId)
            _isLoading.value = false
        }
    }

    fun savePerson(
        lastName: String,
        firstName: String,
        middleName: String?,
        nickname: String?,
        birthDate: String?,
        phone: String?,
        email: String?,
        gender: String?,
        note: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val current = _person.value ?: return@launch
            val updated = current.copy(
                lastName = lastName,
                firstName = firstName,
                middleName = middleName,
                nickname = nickname,
                birthDate = birthDate,
                phone = phone,
                email = email,
                gender = gender,
                note = note,
                updatedAt = System.currentTimeMillis()
            )
            personRepo.updatePerson(updated)
            _person.value = updated
            onSuccess()
        }
    }
}