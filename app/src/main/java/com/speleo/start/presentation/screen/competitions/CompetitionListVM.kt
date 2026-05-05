package com.speleo.start.presentation.screen.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CompetitionListVM @Inject constructor(
    private val competitionRepository: CompetitionRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    val activeCompetitions = competitionRepository
        .getActiveCompetitions()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCompetitionId = MutableStateFlow(prefs.activeCompetitionId)
    val activeCompetitionId: StateFlow<Long> = _activeCompetitionId.asStateFlow()

    private val _event = MutableSharedFlow<String>()
    val event: SharedFlow<String> = _event.asSharedFlow()

    fun selectCompetition(id: Long) {
        viewModelScope.launch {
            val competition = competitionRepository.getCompetitionById(id)
            if (competition != null) {
                prefs.activeCompetitionId = id
                _activeCompetitionId.value = id
                _event.emit("✅ Соревнование «${competition.shortName.ifBlank { competition.name }}» активировано")
            } else {
                _event.emit("❌ Соревнование не найдено")
            }
        }
    }
}