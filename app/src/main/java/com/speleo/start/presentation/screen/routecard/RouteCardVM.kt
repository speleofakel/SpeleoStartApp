package com.speleo.start.presentation.screen.routecard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.repository.MasterRouteCardRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.data.repository.TeamRouteCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteCardEntry(
    val checkpointId: Long,
    val displayNumber: Int,
    val weight: Int,
    val type: String,
    val taken: Boolean = false,
    val takenWithError: Boolean = false,
    val offsetTime: String = "",
    val penalty: Int = 0
)

@HiltViewModel
class RouteCardVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val masterRepo: MasterRouteCardRepository,
    private val routeCardRepo: TeamRouteCardRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<RouteCardEntry>>(emptyList())
    val entries: StateFlow<List<RouteCardEntry>> = _entries.asStateFlow()

    private val _teamId = MutableStateFlow(0L)
    val teamId: StateFlow<Long> = _teamId.asStateFlow()

    fun loadRouteCard(teamId: Long) {
        _teamId.value = teamId
        viewModelScope.launch {
            val team = teamRepo.getTeamById(teamId) ?: return@launch
            val checkpoints = masterRepo.getRouteCardByCompetition(team.competitionId)
            val existing = routeCardRepo.getRouteCardByTeam(teamId)

            checkpoints.combine(existing) { cp, ex ->
                cp.map { cp ->
                    val exEntry = ex.find { it.checkpointId == cp.id }
                    RouteCardEntry(
                        checkpointId = cp.id,
                        displayNumber = cp.displayNumber,
                        weight = cp.weight,
                        type = cp.type,
                        taken = exEntry?.taken ?: false,
                        takenWithError = exEntry?.takenWithError ?: false,
                        offsetTime = exEntry?.offsetTime?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "",
                        penalty = exEntry?.penalty ?: 0
                    )
                }
            }.collect { _entries.value = it }
        }
    }

    fun toggleTaken(index: Int) {
        _entries.value = _entries.value.toMutableList().also {
            val e = it[index]
            it[index] = e.copy(taken = !e.taken, takenWithError = false)
        }
    }

    fun toggleError(index: Int) {
        _entries.value = _entries.value.toMutableList().also {
            val e = it[index]
            if (e.taken) it[index] = e.copy(takenWithError = !e.takenWithError)
        }
    }

    fun save() {
        // TODO: сохранить через TeamRouteCardRepository
    }
}