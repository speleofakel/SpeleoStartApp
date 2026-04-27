package com.speleo.start.presentation.screen.finish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinishTeamInfo(
    val id: Long,
    val number: Int,
    val className: String
)

@HiltViewModel
class FinishVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _startedTeams = MutableStateFlow<List<FinishTeamInfo>>(emptyList())
    val startedTeams: StateFlow<List<FinishTeamInfo>> = _startedTeams.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun loadStartedTeams() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            teamRepo.getStartedTeams(cid)
                .map { list -> list.map { FinishTeamInfo(it.id, it.teamNumber, it.className) } }
                .collect { _startedTeams.value = it }
        }
    }

    fun toggleSelection(teamId: Long) {
        _selectedIds.value = _selectedIds.value.let {
            if (teamId in it) it - teamId else it + teamId
        }
    }

    fun confirmFinish() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            _selectedIds.value.forEach { id ->
                teamRepo.setFinishTimestamp(id, timestamp)
            }
            _selectedIds.value = emptySet()
            loadStartedTeams()
        }
    }
}