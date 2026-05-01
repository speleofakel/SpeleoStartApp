package com.speleo.start.presentation.screen.persons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.util.DateValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

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
        gender: String?,
        note: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val current = _person.value
            if (current == null) {
                _error.emit("Персона не найдена")
                return@launch
            }

            // ЖЁСТКАЯ ВАЛИДАЦИЯ: блокируем сохранение некорректной даты
            if (!birthDate.isNullOrBlank()) {
                if (birthDate.length != 10) {
                    _error.emit("Дата рождения неполная")
                    return@launch
                }
                if (!DateValidator.isRealDate(birthDate)) {
                    _error.emit("Некорректная дата рождения: $birthDate")
                    return@launch
                }
            }

            try {
                val updated = current.copy(
                    lastName = lastName,
                    firstName = firstName,
                    middleName = middleName,
                    nickname = nickname,
                    birthDate = birthDate,
                    phone = phone,
                    gender = gender,
                    note = note,
                    updatedAt = System.currentTimeMillis()
                )
                personRepo.updatePerson(updated)
                _person.value = updated
                _error.emit("Сохранено успешно")
                onSuccess()
            } catch (e: Exception) {
                _error.emit("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun blacklistPerson(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val current = _person.value ?: return@launch
            try {
                if (current.blacklisted) {
                    personRepo.unblacklistPerson(current.id)
                    _error.emit("Удалён из чёрного списка")
                } else {
                    personRepo.blacklistPerson(current.id, "Добавлено вручную")
                    _error.emit("Добавлен в чёрный список")
                }
                loadPerson(current.id)
                onSuccess()
            } catch (e: Exception) {
                _error.emit("Ошибка: ${e.message}")
            }
        }
    }
}