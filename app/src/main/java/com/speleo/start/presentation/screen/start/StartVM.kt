package com.speleo.start.presentation.screen.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.presentation.TimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartTeamInfo(
    val id: Long,
    val number: Int,
    val className: String
)

@HiltViewModel
class StartVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val prefs: PreferencesManager,
    val timer: TimerManager
) : ViewModel() {

    private val _queue = MutableStateFlow<List<StartTeamInfo>>(emptyList())
    val queue: StateFlow<List<StartTeamInfo>> = _queue.asStateFlow()

    init {
        timer.restoreFromSavedState()
    }

    fun loadQueue() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            teamRepo.getRegisteredTeamsByClass(cid, "2")
                .combine(teamRepo.getRegisteredTeamsByClass(cid, "3")) { c2, c3 ->
                    c2.map { StartTeamInfo(it.id, it.teamNumber, "2") } +
                            c3.map { StartTeamInfo(it.id, it.teamNumber, "3") }
                }
                .collect { _queue.value = it }
        }
    }
}