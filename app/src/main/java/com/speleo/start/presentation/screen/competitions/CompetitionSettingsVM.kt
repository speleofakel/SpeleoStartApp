package com.speleo.start.presentation.screen.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.entity.MasterRouteCardEntity
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.MasterRouteCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class CompetitionSettingsVM @Inject constructor(
    private val competitionRepo: CompetitionRepository,
    private val masterRouteCardRepo: MasterRouteCardRepository
) : ViewModel() {

    private var competitionId: Long = -1L

    // ─── Название соревнования ───
    private val _competitionName = MutableStateFlow("")
    val competitionName: StateFlow<String> = _competitionName.asStateFlow()

    // ─── Параметры соревнования ───
    private val _startInterval = MutableStateFlow("60")
    val startInterval: StateFlow<String> = _startInterval.asStateFlow()

    private val _controlTime2 = MutableStateFlow("90")
    val controlTime2: StateFlow<String> = _controlTime2.asStateFlow()

    private val _controlTime3 = MutableStateFlow("60")
    val controlTime3: StateFlow<String> = _controlTime3.asStateFlow()

    private val _minTeamSize = MutableStateFlow("2")
    val minTeamSize: StateFlow<String> = _minTeamSize.asStateFlow()

    // ─── КП ───
    private val _checkpoints = MutableStateFlow<List<MasterRouteCardEntity>>(emptyList())
    val checkpoints: StateFlow<List<MasterRouteCardEntity>> = _checkpoints.asStateFlow()

    // ─── Состояния ───
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _editingCheckpoint = MutableStateFlow<MasterRouteCardEntity?>(null)
    val editingCheckpoint: StateFlow<MasterRouteCardEntity?> = _editingCheckpoint.asStateFlow()

    // ─── События ───
    private val _event = MutableSharedFlow<SettingsUiEvent>()
    val event: SharedFlow<SettingsUiEvent> = _event.asSharedFlow()

    sealed class SettingsUiEvent {
        data class ShowSnackbar(val message: String) : SettingsUiEvent()
        object NavigateBack : SettingsUiEvent()
    }

    // ═══════════════════════════════════════
    // ЗАГРУЗКА
    // ═══════════════════════════════════════

    fun loadCompetition(id: Long) {
        competitionId = id
        viewModelScope.launch {
            _isLoading.value = true

            // 1. Загружаем соревнование
            val competition = competitionRepo.getCompetitionById(id)
            if (competition == null) {
                _event.emit(SettingsUiEvent.ShowSnackbar("Соревнование не найдено"))
                _event.emit(SettingsUiEvent.NavigateBack)
                _isLoading.value = false
                return@launch
            }

            // 2. Устанавливаем название (shortName приоритетнее)
            _competitionName.value = competition.shortName.takeIf { it.isNotBlank() }
                ?: competition.name

            // 3. Парсим settingsJson
            parseSettingsJson(competition.settingsJson)

            // 4. Подписываемся на КП
            masterRouteCardRepo.getRouteCardByCompetition(id)
                .collect { list ->
                    _checkpoints.value = list.sortedBy { it.displayNumber }
                    _isLoading.value = false
                }
        }
    }

    private fun parseSettingsJson(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            _startInterval.value = json.optInt("start_interval", 60).toString()
            _controlTime2.value = json.optInt("control_time_2", 90).toString()
            _controlTime3.value = json.optInt("control_time_3", 60).toString()
            _minTeamSize.value = json.optInt("min_team_size", 2).toString()
        } catch (e: Exception) {
            // Ошибка парсинга — используем дефолты
            _startInterval.value = "60"
            _controlTime2.value = "90"
            _controlTime3.value = "60"
            _minTeamSize.value = "2"
        }
    }

    // ═══════════════════════════════════════
    // ОБНОВЛЕНИЕ ПОЛЕЙ
    // ═══════════════════════════════════════

    fun updateInterval(value: String) {
        _startInterval.value = value.filter { it.isDigit() }
    }

    fun updateControlTime2(value: String) {
        _controlTime2.value = value.filter { it.isDigit() }
    }

    fun updateControlTime3(value: String) {
        _controlTime3.value = value.filter { it.isDigit() }
    }

    fun updateMinTeamSize(value: String) {
        _minTeamSize.value = value.filter { it.isDigit() }
    }

    // ═══════════════════════════════════════
    // КП — ДИАЛОГ РЕДАКТИРОВАНИЯ
    // ═══════════════════════════════════════

    fun editCheckpoint(cp: MasterRouteCardEntity) {
        _editingCheckpoint.value = cp
    }

    fun closeEditDialog() {
        _editingCheckpoint.value = null
    }

    fun updateCheckpointType(id: Long, newType: String) {
        viewModelScope.launch {
            val entity = masterRouteCardRepo.getCheckpointById(id) ?: return@launch
            val updated = entity.copy(type = newType)
            masterRouteCardRepo.updateCheckpoint(updated)
            // Flow автоматически обновит список
            _editingCheckpoint.value = updated
        }
    }

    // ═══════════════════════════════════════
    // СОХРАНЕНИЕ
    // ═══════════════════════════════════════

    fun saveSettings() {
        viewModelScope.launch {
            // Валидация
            val interval = _startInterval.value.toIntOrNull() ?: 0
            val ct2 = _controlTime2.value.toIntOrNull() ?: 0
            val ct3 = _controlTime3.value.toIntOrNull() ?: 0
            val minSize = _minTeamSize.value.toIntOrNull() ?: 0

            when {
                interval <= 0 -> {
                    _event.emit(SettingsUiEvent.ShowSnackbar("Интервал старта должен быть положительным числом"))
                    return@launch
                }
                ct2 <= 0 -> {
                    _event.emit(SettingsUiEvent.ShowSnackbar("КВ для 2-го класса должно быть положительным числом"))
                    return@launch
                }
                ct3 <= 0 -> {
                    _event.emit(SettingsUiEvent.ShowSnackbar("КВ для 3-го класса должно быть положительным числом"))
                    return@launch
                }
                minSize < 1 -> {
                    _event.emit(SettingsUiEvent.ShowSnackbar("Минимальный состав команды должен быть не менее 1"))
                    return@launch
                }
            }

            val competition = competitionRepo.getCompetitionById(competitionId) ?: return@launch

            val json = JSONObject().apply {
                put("start_interval", interval)
                put("control_time_2", ct2)
                put("control_time_3", ct3)
                put("min_team_size", minSize)
            }.toString()

            competitionRepo.updateCompetition(competition.copy(settingsJson = json))
            _event.emit(SettingsUiEvent.ShowSnackbar("Настройки сохранены"))
        }
    }
}