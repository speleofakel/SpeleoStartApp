package com.speleo.start.presentation.screen.results

import androidx.lifecycle.ViewModel
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


data class TeamResult(
    val number: Int,
    val className: String,
    val members: String = "",
    val score: Int = 0,
    val time: String = "",
    val status: String = ""
)

@HiltViewModel
class ResultsVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _results2 = MutableStateFlow<List<TeamResult>>(emptyList())
    val results2: StateFlow<List<TeamResult>> = _results2.asStateFlow()

    private val _results3 = MutableStateFlow<List<TeamResult>>(emptyList())
    val results3: StateFlow<List<TeamResult>> = _results3.asStateFlow()

    fun loadResults() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            teamRepo.getFinishedTeams(cid)
                .map { teams ->
                    teams.map { t ->
                        TeamResult(
                            number = t.teamNumber,
                            className = t.className,
                            status = t.status,
                            time = t.finishTimestamp?.let { "${(it - (t.startTimestamp ?: 0)) / 60000} мин" } ?: "-"
                        )
                    }.sortedByDescending { it.score }
                }
                .collect { all ->
                    _results2.value = all.filter { it.className == "2" }
                    _results3.value = all.filter { it.className == "3" }
                }
        }
    }
}