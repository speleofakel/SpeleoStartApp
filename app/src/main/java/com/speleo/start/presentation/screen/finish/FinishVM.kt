package com.speleo.start.presentation.screen.finish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class FinishTeamInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val members: String,
    val colorMark: String,
    val hasFinished: Boolean = false
)

@HiltViewModel
class FinishVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _allTeams = MutableStateFlow<List<FinishTeamInfo>>(emptyList())
    val allTeams: StateFlow<List<FinishTeamInfo>> = _allTeams.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredTeams = MutableStateFlow<List<FinishTeamInfo>>(emptyList())
    val filteredTeams: StateFlow<List<FinishTeamInfo>> = _filteredTeams.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private var fixedFinishTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            combine(_allTeams, _searchQuery) { teams, query ->
                if (query.isBlank()) {
                    teams
                } else {
                    val lowerQuery = query.lowercase()
                    teams.filter { team ->
                        team.number.toString().contains(query) ||
                                team.members.lowercase().contains(lowerQuery)
                    }
                }
            }.collect { _filteredTeams.value = it }
        }
    }

    fun loadStartedTeams() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch

            teamRepo.getStartedTeams(cid)
                .collect { teams ->
                    val result = mutableListOf<FinishTeamInfo>()
                    for (team in teams) {
                        val participants = participantRepo.getParticipantsByTeam(team.id).first()
                        val membersList = mutableListOf<String>()

                        for (participant in participants) {
                            val person = personRepo.getPersonById(participant.personId)
                            if (person != null) {
                                val initial = if (person.firstName.isNotEmpty()) "${person.firstName.first()}." else ""
                                membersList.add("${person.lastName} $initial")
                            }
                        }

                        val membersText = membersList.joinToString(", ")
                        val colorMark = calculateColorMark(participants)

                        result.add(
                            FinishTeamInfo(
                                id = team.id,
                                number = team.teamNumber,
                                className = team.className,
                                members = membersText,
                                colorMark = colorMark,
                                hasFinished = false
                            )
                        )
                    }
                    _allTeams.value = result
                }
        }
    }

    private suspend fun calculateColorMark(participants: List<com.speleo.start.data.local.entity.ParticipantEntity>): String {
        var hasChild = false
        var hasMinorWithoutMentor = false

        for (participant in participants) {
            val person = personRepo.getPersonById(participant.personId)
            val age = person?.let { calculateAge(it.birthDate) } ?: 0

            if (age < 14) hasChild = true
            if (age in 14..17 && !participant.mentorConfirmed) hasMinorWithoutMentor = true
        }

        return when {
            hasChild -> "<14 !!!"
            hasMinorWithoutMentor -> "<17 !!!"
            else -> "18+"
        }
    }

    private fun calculateAge(birthDate: String?): Int {
        if (birthDate.isNullOrBlank() || birthDate.length != 10) return 0
        return try {
            val day = birthDate.substring(0, 2).toInt()
            val month = birthDate.substring(3, 5).toInt()
            val year = birthDate.substring(6, 10).toInt()
            val now = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply { set(year, month - 1, day) }
            var age = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) age--
            age
        } catch (e: Exception) { 0 }
    }

    fun captureFinishTime() {
        fixedFinishTimestamp = System.currentTimeMillis()
        Log.d("FINISH_DEBUG", "Finish time captured: $fixedFinishTimestamp")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(teamId: Long) {
        _selectedIds.value = _selectedIds.value.let {
            if (teamId in it) {
                Log.d("FINISH_DEBUG", "Removed team $teamId")
                it - teamId
            } else {
                Log.d("FINISH_DEBUG", "Added team $teamId")
                it + teamId
            }
        }
    }

    fun confirmFinish() {
        viewModelScope.launch {
            Log.d("FINISH_DEBUG", "Confirm finish called, selected: ${_selectedIds.value}")
            _selectedIds.value.forEach { id ->
                Log.d("FINISH_DEBUG", "Finishing team $id")
                teamRepo.setFinishTimestamp(id, fixedFinishTimestamp)
                teamRepo.updateTeamStatus(id, "finished")
            }
            _selectedIds.value = emptySet()
            loadStartedTeams()
        }
    }

    suspend fun adjustFinishTime(teamId: Long, newTimestamp: Long) {
        teamRepo.setFinishTimestamp(teamId, newTimestamp)
        teamRepo.updateTeamStatus(teamId, "finished")
        loadStartedTeams()
    }
}