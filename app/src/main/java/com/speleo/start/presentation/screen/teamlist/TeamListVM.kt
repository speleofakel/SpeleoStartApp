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
                    val participants = participantRepo.getActiveParticipantsByTeam(team.id).first()
                    val persons = participants.mapNotNull { personRepo.getPersonById(it.personId) }

                    // Полное имя + ник для поиска
                    val memberFullInfo = persons.joinToString(" ") { person ->
                        buildString {
                            append("${person.lastName} ${person.firstName}")
                            if (!person.nickname.isNullOrBlank()) {
                                append(" ${person.nickname}")
                            }
                        }.lowercase()
                    }

                    // Для отображения: фамилия + первая буква имени + ник
                    val memberNames = persons.joinToString(", ") { person ->
                        val initial = if (person.firstName.isNotEmpty()) "${person.firstName.first()}." else ""
                        val nicknamePart = if (!person.nickname.isNullOrBlank()) " «${person.nickname}»" else ""
                        "${person.lastName} $initial$nicknamePart"
                    }

                    val hasMinor = persons.any { person ->
                        person.birthDate?.let { birth ->
                            try {
                                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                val bd = sdf.parse(birth)
                                val today = java.util.Calendar.getInstance()
                                val birthCal = java.util.Calendar.getInstance().apply { time = bd!! }
                                var age = today.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR)
                                if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCal.get(java.util.Calendar.DAY_OF_YEAR)) age--
                                age in 14..17
                            } catch (e: Exception) { false }
                        } ?: false
                    }

                    val hasChild = persons.any { person ->
                        person.birthDate?.let { birth ->
                            try {
                                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                val bd = sdf.parse(birth)
                                val today = java.util.Calendar.getInstance()
                                val birthCal = java.util.Calendar.getInstance().apply { time = bd!! }
                                var age = today.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR)
                                if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCal.get(java.util.Calendar.DAY_OF_YEAR)) age--
                                age < 14
                            } catch (e: Exception) { false }
                        } ?: false
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
}