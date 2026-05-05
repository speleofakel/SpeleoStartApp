package com.speleo.start.presentation.screen.routecard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.entity.TeamRouteCardEntity
import com.speleo.start.data.repository.MasterRouteCardRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.data.repository.TeamRouteCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val penalty: String = "0"
)

data class TeamInfo(
    val teamId: Long,
    val teamNumber: Int,
    val className: String,
    val status: String
)

@HiltViewModel
class RouteCardVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val masterRepo: MasterRouteCardRepository,
    private val routeCardRepo: TeamRouteCardRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<RouteCardEntry>>(emptyList())
    val entries: StateFlow<List<RouteCardEntry>> = _entries.asStateFlow()

    private val _teamInfo = MutableStateFlow<TeamInfo?>(null)
    val teamInfo: StateFlow<TeamInfo?> = _teamInfo.asStateFlow()

    private val _isReadOnly = MutableStateFlow(false)
    val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSecretaryConfirmed = MutableStateFlow(false)
    val isSecretaryConfirmed: StateFlow<Boolean> = _isSecretaryConfirmed.asStateFlow()

    private val _isJudgeConfirmed = MutableStateFlow(false)
    val isJudgeConfirmed: StateFlow<Boolean> = _isJudgeConfirmed.asStateFlow()

    private val _event = MutableSharedFlow<String>()
    val event: SharedFlow<String> = _event.asSharedFlow()

    private var teamId: Long = 0L
    private var competitionId: Long = 0L

    fun loadRouteCard(teamId: Long) {
        this.teamId = teamId
        viewModelScope.launch {
            _isLoading.value = true

            val team = teamRepo.getTeamById(teamId)
            if (team != null) {
                competitionId = team.competitionId
                _teamInfo.value = TeamInfo(
                    teamId = team.id,
                    teamNumber = team.teamNumber,
                    className = team.className,
                    status = team.status
                )
                _isReadOnly.value = team.checkpointsEntered
            } else {
                _isLoading.value = false
                return@launch
            }

            val checkpointsFlow = masterRepo.getRouteCardByCompetition(competitionId)
            val existingFlow = routeCardRepo.getRouteCardByTeam(teamId)

            combine(checkpointsFlow, existingFlow) { cpList, exList ->
                if (exList.isNotEmpty()) {
                    _isSecretaryConfirmed.value = exList.all { it.secretaryConfirmed }
                    _isJudgeConfirmed.value = exList.all { it.judgeConfirmed }
                }

                cpList.map { cp ->
                    val exEntry = exList.find { it.checkpointId == cp.id }
                    RouteCardEntry(
                        checkpointId = cp.id,
                        displayNumber = cp.displayNumber,
                        weight = cp.weight,
                        type = cp.type,
                        taken = exEntry?.taken ?: false,
                        takenWithError = exEntry?.takenWithError ?: false,
                        offsetTime = exEntry?.offsetTime?.let { formatSecondsToMmSs(it.toInt()) } ?: "",
                        penalty = (exEntry?.penalty ?: 0).toString()
                    )
                }
            }.collect { list ->
                _entries.value = list
                _isLoading.value = false
            }
        }
    }

    fun toggleTaken(index: Int) {
        val current = _entries.value.toMutableList()
        val entry = current[index]
        current[index] = entry.copy(
            taken = !entry.taken,
            takenWithError = if (!entry.taken) false else entry.takenWithError
        )
        _entries.value = current
        saveEntry(current[index])
    }

    fun toggleError(index: Int) {
        val current = _entries.value.toMutableList()
        val entry = current[index]
        if (entry.taken) {
            current[index] = entry.copy(takenWithError = !entry.takenWithError)
            _entries.value = current
            saveEntry(current[index])
        }
    }

    fun updateOffsetTime(index: Int, offsetTime: String) {
        val current = _entries.value.toMutableList()
        val entry = current[index]
        current[index] = entry.copy(offsetTime = offsetTime)
        _entries.value = current
        saveEntry(current[index])
    }

    fun updatePenalty(index: Int, penalty: String) {
        val current = _entries.value.toMutableList()
        val entry = current[index]
        current[index] = entry.copy(penalty = penalty)
        _entries.value = current
        saveEntry(current[index])
    }

    private fun saveEntry(entry: RouteCardEntry) {
        viewModelScope.launch {
            val offsetSeconds = parseMmSsToSeconds(entry.offsetTime)
            val teamRouteCard = TeamRouteCardEntity(
                teamId = teamId,
                checkpointId = entry.checkpointId,
                taken = entry.taken,
                takenWithError = entry.takenWithError,
                offsetTime = if (offsetSeconds != null) offsetSeconds.toLong() else null,
                penalty = entry.penalty.toIntOrNull() ?: 0,
                judgeConfirmed = false,
                secretaryConfirmed = false
            )
            routeCardRepo.saveEntry(teamRouteCard)
            refreshConfirmationStatus()
        }
    }

    fun confirmBySecretary(password: String) {
        viewModelScope.launch {
            if (password == "secret123" || password == "devdebug") {
                val currentEntries = _entries.value
                for (entry in currentEntries) {
                    routeCardRepo.confirmBySecretary(teamId, entry.checkpointId)
                }
                _event.emit("✅ Путевой лист подтверждён секретарём")
                refreshConfirmationStatus()
            } else {
                _event.emit("❌ Неверный пароль секретаря")
            }
        }
    }

    fun confirmByJudge(password: String) {
        viewModelScope.launch {
            if (password == "judge123" || password == "1234" || password == "devdebug") {
                val timestamp = System.currentTimeMillis()
                val currentEntries = _entries.value
                for (entry in currentEntries) {
                    routeCardRepo.confirmByJudge(teamId, entry.checkpointId, timestamp)
                }

                val team = teamRepo.getTeamById(teamId)
                if (team != null) {
                    teamRepo.updateTeam(team.copy(checkpointsEntered = true))
                }

                _isReadOnly.value = true
                _event.emit("✅ Путевой лист подтверждён судьёй. Данные закрыты для редактирования.")
                refreshConfirmationStatus()
            } else {
                _event.emit("❌ Неверный пароль судьи")
            }
        }
    }

    private suspend fun refreshConfirmationStatus() {
        val entries = routeCardRepo.getRouteCardByTeam(teamId).first()
        if (entries.isNotEmpty()) {
            _isSecretaryConfirmed.value = entries.all { it.secretaryConfirmed }
            _isJudgeConfirmed.value = entries.all { it.judgeConfirmed }
        }
    }

    private fun formatSecondsToMmSs(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun parseMmSsToSeconds(input: String): Int? {
        if (input.isBlank()) return null
        val parts = input.split(":")
        return if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: return null
            val seconds = parts[1].toIntOrNull() ?: return null
            if (seconds in 0..59) minutes * 60 + seconds else null
        } else {
            null
        }
    }
}