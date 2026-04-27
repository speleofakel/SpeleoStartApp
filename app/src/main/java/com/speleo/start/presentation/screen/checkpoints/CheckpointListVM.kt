package com.speleo.start.presentation.screen.checkpoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.repository.MasterRouteCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckpointListVM @Inject constructor(
    private val repository: MasterRouteCardRepository
) : ViewModel() {

    private var competitionId: Long = -1L

    private val _checkpoints = MutableStateFlow<List<CheckpointItem>>(emptyList())
    val checkpoints: StateFlow<List<CheckpointItem>> = _checkpoints.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialogState: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _event = MutableSharedFlow<UiEvent>()
    val event: SharedFlow<UiEvent> = _event.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
    }

    fun load(id: Long) {
        competitionId = id
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRouteCardByCompetition(id).collect { entities ->
                _checkpoints.value = entities.mapIndexed { index, entity ->
                    CheckpointItem(
                        id = entity.id,
                        displayNumber = index + 1,
                        weight = entity.weight,
                        type = entity.type,
                        sortOrder = entity.sortOrder,
                        normativeSeconds = entity.normativeSeconds,
                        bonusPoints = entity.bonusPoints,
                        trackWaitTime = entity.trackWaitTime,
                        trackExecutionTime = entity.trackExecutionTime,
                        forClass2 = entity.forClass2,
                        forClass3 = entity.forClass3
                    )
                }
                _isLoading.value = false
            }
        }
    }

    fun addCheckpoint(weight: Int, normativeSeconds: Int, penalty: Int, forClass2: Boolean, forClass3: Boolean) {
        viewModelScope.launch {
            val nextSort = (_checkpoints.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val id = repository.addCheckpoint(
                competitionId = competitionId,
                displayNumber = nextSort,
                weight = weight,
                type = "normal",
                sortOrder = nextSort,
                normativeSeconds = normativeSeconds,
                forClass2 = forClass2,
                forClass3 = forClass3,
                trackWaitTime = false,
                trackExecutionTime = false,
                bonusPoints = penalty
            )
            if (id != -1L) {
                _event.emit(UiEvent.ShowMessage("КП добавлен"))
                load(competitionId) // Перезагрузка для обновления порядка
            } else {
                _event.emit(UiEvent.ShowMessage("Ошибка при добавлении КП"))
            }
        }
    }

    fun updateWeight(id: Long, newWeight: Int) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            if (entity.weight != newWeight) {
                repository.updateCheckpoint(entity.copy(weight = newWeight))
                _event.emit(UiEvent.ShowMessage("Вес обновлён"))
            }
        }
    }

    fun updateNormative(id: Long, seconds: Int) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            if (entity.normativeSeconds != seconds) {
                repository.updateCheckpoint(entity.copy(normativeSeconds = seconds))
                _event.emit(UiEvent.ShowMessage("Норматив обновлён"))
            }
        }
    }

    fun updatePenalty(id: Long, penalty: Int) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            if (entity.bonusPoints != penalty) {
                repository.updateCheckpoint(entity.copy(bonusPoints = penalty))
                _event.emit(UiEvent.ShowMessage("Штраф обновлён"))
            }
        }
    }

    fun toggleCheckpointType(id: Long) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            val newType = if (entity.type == "normal") "technical" else "normal"
            val updated = entity.copy(
                type = newType,
                trackWaitTime = newType == "technical",
                normativeSeconds = if (newType == "technical") 60 else 0,
                bonusPoints = if (newType == "technical") 0 else 0
            )
            repository.updateCheckpoint(updated)
            _event.emit(UiEvent.ShowMessage(if (newType == "technical") "КП стал техническим" else "КП стал обычным"))
        }
    }

    fun toggleClass2(id: Long) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(forClass2 = !entity.forClass2))
        }
    }

    fun toggleClass3(id: Long) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(forClass3 = !entity.forClass3))
        }
    }

    fun toggleTrackWaitTime(id: Long) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(trackWaitTime = !entity.trackWaitTime))
            _event.emit(UiEvent.ShowMessage("Отсечка ${if (!entity.trackWaitTime) "включена" else "выключена"}"))
        }
    }

    fun deleteCheckpoint(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCheckpoint(id)
                _event.emit(UiEvent.ShowMessage("КП удалён"))
                load(competitionId)
            } catch (e: Exception) {
                _event.emit(UiEvent.ShowMessage("Ошибка при удалении: ${e.message}"))
            }
        }
    }

    fun moveCheckpoint(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val current = _checkpoints.value.toMutableList()
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)

        viewModelScope.launch {
            current.forEachIndexed { index, cp ->
                val entity = repository.getCheckpointById(cp.id) ?: return@forEachIndexed
                val newSortOrder = index + 1
                if (entity.sortOrder != newSortOrder) {
                    repository.updateCheckpoint(entity.copy(sortOrder = newSortOrder, displayNumber = newSortOrder))
                }
            }
            _event.emit(UiEvent.ShowMessage("Порядок обновлён"))
            load(competitionId)
        }
    }

    fun reorderCheckpoints() {
        viewModelScope.launch {
            _checkpoints.value.forEachIndexed { index, cp ->
                val entity = repository.getCheckpointById(cp.id) ?: return@forEachIndexed
                val newSortOrder = index + 1
                if (entity.sortOrder != newSortOrder) {
                    repository.updateCheckpoint(entity.copy(sortOrder = newSortOrder, displayNumber = newSortOrder))
                }
            }
            _event.emit(UiEvent.ShowMessage("Порядок сохранён"))
            load(competitionId)
        }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }
}

data class CheckpointItem(
    val id: Long,
    val displayNumber: Int,
    val weight: Int,
    val type: String,
    val sortOrder: Int,
    val normativeSeconds: Int? = null,
    val bonusPoints: Int? = null,
    val trackWaitTime: Boolean = false,
    val trackExecutionTime: Boolean = false,
    val forClass2: Boolean = true,
    val forClass3: Boolean = true
)