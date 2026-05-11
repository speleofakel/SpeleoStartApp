package com.speleo.start.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.TestDataGenerator
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.presentation.TimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompetitionStats(
    val onCourse: Int = 0,
    val finished: Int = 0,
    val disqualified: Int = 0
)

data class TeamListInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val status: String,
    val statusText: String,
    val colorMark: String,
    val memberNames: String,
    val memberFullInfo: String,
    val timeInfo: String
)

// НОВЫЙ data class для диалога
data class PendingTeamInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val membersShort: String // первые две фамилии
)

@HiltViewModel
class HomeVM @Inject constructor(
    private val generator: TestDataGenerator,
    private val prefs: PreferencesManager,
    private val competitionRepo: CompetitionRepository,
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    val timerManager: TimerManager
) : ViewModel() {

    private val _competitionName = MutableStateFlow("")
    val competitionName: StateFlow<String> = _competitionName.asStateFlow()

    private val _hasActiveCompetition = MutableStateFlow(false)
    val hasActiveCompetition: StateFlow<Boolean> = _hasActiveCompetition.asStateFlow()

    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived.asStateFlow()

    private val _stats = MutableStateFlow(CompetitionStats())
    val stats: StateFlow<CompetitionStats> = _stats.asStateFlow()

    private val _finishedTeams = MutableStateFlow<List<TeamListInfo>>(emptyList())
    val finishedTeams: StateFlow<List<TeamListInfo>> = _finishedTeams.asStateFlow()

    private val _pendingRouteCards = MutableStateFlow(Pair(0, 0)) // (pending, total)
    val pendingRouteCards: StateFlow<Pair<Int, Int>> = _pendingRouteCards.asStateFlow()

    // НОВЫЙ flow для диалога
    private val _pendingTeams = MutableStateFlow<List<PendingTeamInfo>>(emptyList())
    val pendingTeams: StateFlow<List<PendingTeamInfo>> = _pendingTeams.asStateFlow()

    val mainTimer = timerManager.mainTimer

    init {
        viewModelScope.launch {
            prefs.activeCompetitionFlow.collect { cid ->
                loadActiveCompetition(cid)
            }
        }
    }

    suspend fun loadActiveCompetition(cid: Long) {
        if (cid == -1L) {
            _hasActiveCompetition.value = false
            _isArchived.value = false
            _competitionName.value = ""
            _stats.value = CompetitionStats()
            _pendingRouteCards.value = Pair(0, 0)
            _pendingTeams.value = emptyList()
            timerManager.stop()
            return
        }

        val comp = competitionRepo.getCompetitionById(cid)

        if (comp == null) {
            prefs.activeCompetitionId = -1L
            _hasActiveCompetition.value = false
            _isArchived.value = false
            _competitionName.value = ""
            _stats.value = CompetitionStats()
            _pendingRouteCards.value = Pair(0, 0)
            _pendingTeams.value = emptyList()
            timerManager.stop()
            return
        }

        _hasActiveCompetition.value = true
        _isArchived.value = comp.isArchived
        _competitionName.value = comp.shortName.ifBlank { comp.name }

        if (!comp.isArchived) {
            timerManager.restoreFromSavedState()
            loadFinishedTeams()
            loadPendingTeamsWithMembers()
        } else {
            timerManager.stop()
        }

        try {
            val teams = teamRepo.getTeamsByCompetition(cid).first()
            _stats.value = CompetitionStats(
                onCourse = teams.count { it.status == "started" },
                finished = teams.count { it.status == "finished" },
                disqualified = teams.count { it.status == "disqualified" || it.status == "lost" }
            )
        } catch (e: Exception) {
            _stats.value = CompetitionStats()
        }
    }

    fun loadFinishedTeams() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch

            teamRepo.getTeamsByCompetition(cid).collect { teams ->
                val finished = teams.filter { it.status == "finished" }
                val total = finished.size
                val pending = finished.count { !it.checkpointsEntered }
                _pendingRouteCards.value = Pair(pending, total)

                _finishedTeams.value = finished
                    .filter { !it.checkpointsEntered }
                    .map { team ->
                        TeamListInfo(
                            id = team.id,
                            number = team.teamNumber,
                            className = team.className,
                            status = team.status,
                            statusText = "Финиш",
                            colorMark = "",
                            memberNames = "",
                            memberFullInfo = "",
                            timeInfo = ""
                        )
                    }
            }
        }
    }

    // НОВЫЙ метод: загружает команды с незаполненным ПЛ и первыми двумя участниками
    fun loadPendingTeamsWithMembers() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch

            val teams = teamRepo.getTeamsByCompetition(cid).first()
            val pendingTeamsList = teams.filter { it.status == "finished" && !it.checkpointsEntered }

            val result = mutableListOf<PendingTeamInfo>()
            for (team in pendingTeamsList) {
                val participants = participantRepo.getActiveParticipantsByTeam(team.id).first()
                val firstTwoNames = mutableListOf<String>()

                for (i in 0 until minOf(2, participants.size)) {
                    val person = personRepo.getPersonById(participants[i].personId)
                    if (person != null) {
                        val initial = if (person.firstName.isNotEmpty()) "${person.firstName.first()}." else ""
                        firstTwoNames.add("${person.lastName} $initial")
                    }
                }

                val membersShort = if (firstTwoNames.isNotEmpty()) {
                    firstTwoNames.joinToString(", ")
                } else {
                    "Нет участников"
                }

                result.add(
                    PendingTeamInfo(
                        id = team.id,
                        number = team.teamNumber,
                        className = team.className,
                        membersShort = membersShort
                    )
                )
            }

            _pendingTeams.value = result
        }
    }

    fun restoreTimer() {
        timerManager.restoreFromSavedState()
    }

    fun stopTimer() {
        timerManager.stop()
    }

    fun stopMainTimer(password: String): Boolean {
        return timerManager.stopMainTimer(password)
    }

    fun generateTestData() {
        viewModelScope.launch {
            generator.generate()
            val cid = prefs.activeCompetitionId
            if (cid != -1L) {
                loadActiveCompetition(cid)
            }
        }
    }

    fun clearTestData() {
        viewModelScope.launch {
            generator.clearAll()
            prefs.activeCompetitionId = -1L
            _hasActiveCompetition.value = false
            _isArchived.value = false
            _competitionName.value = ""
            _stats.value = CompetitionStats()
            _pendingRouteCards.value = Pair(0, 0)
            _pendingTeams.value = emptyList()
            timerManager.stop()
        }
    }

    fun exportData() {
        viewModelScope.launch {
            // TODO: реализовать экспорт
        }
    }

    fun importData() {
        viewModelScope.launch {
            // TODO: реализовать импорт
        }
    }
}