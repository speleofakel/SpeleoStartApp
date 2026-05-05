package com.speleo.start.presentation.screen.teamlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.presentation.SharedState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamListInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val status: String,
    val statusText: String,
    val colorMark: String = "",
    val memberNames: String = "",
    val memberFullInfo: String = "",
    val timeInfo: String = ""
)

@HiltViewModel
class TeamListVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    private val prefs: PreferencesManager,
    private val sharedState: SharedState
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _teamsRaw = MutableStateFlow<List<TeamListInfo>>(emptyList())
    val teams: StateFlow<List<TeamListInfo>> = combine(_teamsRaw, _searchQuery) { raw, query ->
        if (query.isBlank()) {
            raw
        } else {
            val lowerQuery = query.lowercase()
            raw.filter { team ->
                team.number.toString().contains(query) ||
                        team.memberFullInfo.contains(lowerQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadTeams() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch

            teamRepo.getTeamsByCompetition(cid).collect { teams ->
                val result = mutableListOf<TeamListInfo>()
                for (team in teams) {
                    val participants = participantRepo.getParticipantsByTeam(team.id).first()

                    val persons = mutableListOf<com.speleo.start.data.local.entity.PersonEntity>()
                    for (participant in participants) {
                        val person = personRepo.getPersonById(participant.personId)
                        if (person != null) {
                            persons.add(person)
                        }
                    }

                    val memberFullInfoBuilder = StringBuilder()
                    for (person in persons) {
                        if (memberFullInfoBuilder.isNotEmpty()) {
                            memberFullInfoBuilder.append(" ")
                        }
                        memberFullInfoBuilder.append(person.lastName)
                        memberFullInfoBuilder.append(" ")
                        memberFullInfoBuilder.append(person.firstName)
                        if (!person.nickname.isNullOrBlank()) {
                            memberFullInfoBuilder.append(" ")
                            memberFullInfoBuilder.append(person.nickname)
                        }
                    }
                    val memberFullInfo = memberFullInfoBuilder.toString().lowercase()

                    val memberNamesBuilder = StringBuilder()
                    for ((index, person) in persons.withIndex()) {
                        if (index > 0) {
                            memberNamesBuilder.append(", ")
                        }
                        val initial = if (person.firstName.isNotEmpty()) "${person.firstName.first()}." else ""
                        val nicknamePart = if (!person.nickname.isNullOrBlank()) " «${person.nickname}»" else ""
                        memberNamesBuilder.append(person.lastName)
                        memberNamesBuilder.append(" ")
                        memberNamesBuilder.append(initial)
                        memberNamesBuilder.append(nicknamePart)
                    }
                    val memberNames = memberNamesBuilder.toString()

                    var hasMinor = false
                    var hasChild = false
                    for (person in persons) {
                        val age = person.birthDate?.let { birth ->
                            try {
                                val day = birth.substring(0, 2).toInt()
                                val month = birth.substring(3, 5).toInt()
                                val year = birth.substring(6, 10).toInt()

                                val today = java.util.Calendar.getInstance()
                                val birthCal = java.util.Calendar.getInstance().apply { set(year, month - 1, day) }

                                var age = today.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR)
                                if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                                    age--
                                }
                                age
                            } catch (e: Exception) {
                                0
                            }
                        } ?: 0

                        if (age in 14..17) hasMinor = true
                        if (age < 14) hasChild = true
                    }

                    val colorMark = when {
                        team.status == "disqualified" -> "СНЯТЫ"
                        team.status == "lost" -> "Lost"
                        hasChild -> "<14 !!!"
                        hasMinor -> "<17 !!!"
                        else -> "18+"
                    }

                    val statusText = when (team.status) {
                        "registered" -> "Зарег."
                        "started" -> "В пути"
                        "finished" -> "Финиш"
                        "lost" -> "Lost"
                        "disqualified" -> "Сняты"
                        else -> team.status
                    }

                    result.add(
                        TeamListInfo(
                            id = team.id,
                            number = team.teamNumber,
                            className = team.className,
                            status = team.status,
                            statusText = statusText,
                            colorMark = colorMark,
                            memberNames = memberNames,
                            memberFullInfo = memberFullInfo,
                            timeInfo = when (team.status) {
                                "started" -> "В пути"
                                "finished" -> "Финиш"
                                "registered" -> "Ожидает"
                                "lost" -> "Потеряна"
                                "disqualified" -> "Снята"
                                else -> team.status
                            }
                        )
                    )
                }
                _teamsRaw.value = result
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectTeam(teamId: Long) {
        sharedState.selectTeam(teamId)
    }

    /**
     * Корректировка времени финиша для команды
     */
    fun adjustFinishTime(teamId: Long, timestamp: Long) {
        viewModelScope.launch {
            try {
                teamRepo.setFinishTimestamp(teamId, timestamp)
                teamRepo.updateTeamStatus(teamId, "finished")
                loadTeams() // Перезагружаем список
            } catch (e: Exception) {
                // Обработка ошибки через snackbar в UI
            }
        }
    }
}