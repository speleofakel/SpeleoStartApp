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
                        id = entity.id, displayNumber = index + 1, weight = entity.weight,
                        type = entity.type, sortOrder = entity.sortOrder,
                        normativeSeconds = if (entity.type == "technical") entity.normativeSeconds?.toString() else null,
                        bonusPoints = if (entity.type == "technical") entity.bonusPoints else null,
                        trackWaitTime = entity.trackWaitTime,
                        trackExecutionTime = entity.trackExecutionTime,
                        forClass2 = entity.forClass2, forClass3 = entity.forClass3
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
                competitionId = competitionId, displayNumber = nextSort,
                weight = weight, type = "normal", sortOrder = nextSort,
                normativeSeconds = normativeSeconds, forClass2 = forClass2, forClass3 = forClass3,
                trackWaitTime = false, trackExecutionTime = false, bonusPoints = penalty
            )
            if (id != -1L) _event.emit(UiEvent.ShowMessage("КП добавлен"))
            else _event.emit(UiEvent.ShowMessage("Ошибка добавления"))
        }
    }

    fun updateWeight(id: Long, newWeight: Int) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(weight = newWeight))
        }
    }

    fun updateNormative(id: Long, normative: String) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            val seconds = parseNormativeSeconds(normative)
            repository.updateCheckpoint(entity.copy(normativeSeconds = seconds))
        }
    }

    fun updatePenalty(id: Long, penalty: Int) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(bonusPoints = penalty))
        }
    }

    fun toggleCheckpointType(id: Long) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            val newType = if (entity.type == "normal") "technical" else "normal"
            repository.updateCheckpoint(entity.copy(
                type = newType,
                trackWaitTime = newType == "technical",
                trackExecutionTime = newType == "technical",
                normativeSeconds = if (newType == "technical") 60 else 0,
                bonusPoints = if (newType == "technical") 0 else 0
            ))
            _event.emit(UiEvent.ShowMessage("Тип КП изменён"))
        }
    }

    fun toggleClass2(id: Long, currentValue: Boolean) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(forClass2 = !currentValue))
        }
    }

    fun toggleClass3(id: Long, currentValue: Boolean) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(forClass3 = !currentValue))
        }
    }

    fun toggleTrackWaitTime(id: Long, currentValue: Boolean) {
        viewModelScope.launch {
            val entity = repository.getCheckpointById(id) ?: return@launch
            repository.updateCheckpoint(entity.copy(trackWaitTime = !currentValue))
        }
    }

    fun deleteCheckpoint(id: Long) {
        viewModelScope.launch {
            repository.deleteCheckpoint(id)
            val remaining = _checkpoints.value.filter { it.id != id }
            reorderAfterDelete(remaining)
            _event.emit(UiEvent.ShowMessage("КП удалён"))
        }
    }

    private suspend fun reorderAfterDelete(remaining: List<CheckpointItem>) {
        remaining.forEachIndexed { index, item ->
            val entity = repository.getCheckpointById(item.id) ?: return@forEachIndexed
            val newOrder = index + 1
            if (entity.sortOrder != newOrder || entity.displayNumber != newOrder) {
                repository.updateCheckpoint(entity.copy(sortOrder = newOrder, displayNumber = newOrder))
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
                val newSort = index + 1
                if (entity.sortOrder != newSort || entity.displayNumber != newSort) {
                    repository.updateCheckpoint(entity.copy(sortOrder = newSort, displayNumber = newSort))
                }
            }
            _event.emit(UiEvent.ShowMessage("Порядок обновлён"))
        }
    }

    fun reorderCheckpoints() {
        viewModelScope.launch {
            _checkpoints.value.forEachIndexed { index, cp ->
                val entity = repository.getCheckpointById(cp.id) ?: return@forEachIndexed
                val newSort = index + 1
                if (entity.sortOrder != newSort || entity.displayNumber != newSort) {
                    repository.updateCheckpoint(entity.copy(sortOrder = newSort, displayNumber = newSort))
                }
            }
            _event.emit(UiEvent.ShowMessage("Порядок сохранён"))
        }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    private fun parseNormativeSeconds(input: String): Int {
        val parts = input.split(":")
        return if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else input.toIntOrNull() ?: 0
    }
}

// ✅ ДАННЫЕ КЛАССЫ ТОЛЬКО ЗДЕСЬ!
data class CheckpointItem(
    val id: Long, val displayNumber: Int, val weight: Int, val type: String, val sortOrder: Int,
    val normativeSeconds: String? = null, val bonusPoints: Int? = null,
    val trackWaitTime: Boolean = false, val trackExecutionTime: Boolean = false,
    val forClass2: Boolean = true, val forClass3: Boolean = true
)