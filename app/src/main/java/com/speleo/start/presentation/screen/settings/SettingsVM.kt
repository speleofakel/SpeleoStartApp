package com.speleo.start.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsVM @Inject constructor(
    private val competitionRepo: CompetitionRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _startInterval = MutableStateFlow("120")
    val startInterval: StateFlow<String> = _startInterval.asStateFlow()

    private val _controlTime2 = MutableStateFlow("90")
    val controlTime2: StateFlow<String> = _controlTime2.asStateFlow()

    private val _controlTime3 = MutableStateFlow("60")
    val controlTime3: StateFlow<String> = _controlTime3.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            val comp = competitionRepo.getCompetitionById(cid) ?: return@launch
            // Парсим settingsJson (пока упрощённо — загружаем дефолты)
        }
    }

    fun updateInterval(value: String) { _startInterval.value = value }
    fun updateControlTime2(value: String) { _controlTime2.value = value }
    fun updateControlTime3(value: String) { _controlTime3.value = value }

    fun saveSettings() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            val comp = competitionRepo.getCompetitionById(cid) ?: return@launch
            val json = """
                {
                    "start_interval": ${_startInterval.value},
                    "control_time_2": ${_controlTime2.value},
                    "control_time_3": ${_controlTime3.value}
                }
            """.trimIndent()
            competitionRepo.updateCompetition(comp.copy(settingsJson = json))
        }
    }
}