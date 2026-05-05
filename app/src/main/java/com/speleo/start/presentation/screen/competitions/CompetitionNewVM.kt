package com.speleo.start.presentation.screen.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompetitionNewVM @Inject constructor(
    private val repository: CompetitionRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _event = MutableSharedFlow<UiEvent>()
    val event: SharedFlow<UiEvent> = _event.asSharedFlow()

    sealed class UiEvent {
        data class CompetitionCreated(val id: Long, val name: String) : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }

    fun saveCompetition(
        name: String,
        shortName: String,
        date: String,
        place: String,
        discipline: String,
        system: String?,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createCompetition(name, shortName, date, place, discipline, system)
            if (id != -1L) {
                // Автоматически делаем созданное соревнование активным
                prefs.activeCompetitionId = id
                _event.emit(UiEvent.CompetitionCreated(id, shortName.ifBlank { name }))
                onSuccess(id)
            } else {
                _event.emit(UiEvent.ShowError("Ошибка при создании соревнования"))
            }
        }
    }
}