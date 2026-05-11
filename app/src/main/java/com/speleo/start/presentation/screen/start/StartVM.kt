package com.speleo.start.presentation.screen.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.presentation.SharedState
import com.speleo.start.presentation.TimerManager
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

data class StartMemberInfo(
    val personId: Long,
    val lastName: String,
    val firstName: String,
    val role: String,
    val age: Int,
    val mentorName: String? = null,
    val mentorConfirmed: Boolean = false,
    val judgeApproved: Boolean = false
)

data class StartTeamInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val members: List<StartMemberInfo> = emptyList(),
    val memberCount: Int = 0,
    val hasMentorIssues: Boolean = false,
    val hasInactiveMembers: Boolean = false,
    val hasChildWithoutPermission: Boolean = false,
    val colorMark: String = "18+",
    val isReady: Boolean = false
)

data class StartValidationError(
    val message: String,
    val canProceed: Boolean = false
)

sealed class StartEvent {
    data object CountdownFinished : StartEvent()
}

@HiltViewModel
class StartVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    private val competitionRepo: CompetitionRepository,
    private val prefs: PreferencesManager,
    val timer: TimerManager,
    private val sharedState: SharedState
) : ViewModel() {

    private val _queue = MutableStateFlow<List<StartTeamInfo>>(emptyList())
    val queue: StateFlow<List<StartTeamInfo>> = _queue.asStateFlow()

    private val _hasActiveCompetition = MutableStateFlow(false)
    val hasActiveCompetition: StateFlow<Boolean> = _hasActiveCompetition.asStateFlow()

    private val _validationError = MutableStateFlow<StartValidationError?>(null)
    val validationError: StateFlow<StartValidationError?> = _validationError.asStateFlow()

    private val _startInterval = MutableStateFlow(TimerManager.DEFAULT_START_INTERVAL)
    val startInterval: StateFlow<Int> = _startInterval.asStateFlow()

    private val _navigateToTeamCard = MutableStateFlow<Long?>(null)
    val navigateToTeamCard: StateFlow<Long?> = _navigateToTeamCard.asStateFlow()

    private val _event = MutableSharedFlow<StartEvent>()
    val event: SharedFlow<StartEvent> = _event.asSharedFlow()

    init {
        timer.restoreFromSavedState()
        checkActiveCompetition()
        loadStartInterval()
        setupCountdownListener()

        viewModelScope.launch {
            sharedState.shouldRefreshStartQueue.collect {
                if (it > 0) {
                    loadQueue()
                }
            }
        }
    }

    private fun setupCountdownListener() {
        viewModelScope.launch {
            var lastCountdown = -1
            timer.countdown.collect { current ->
                if (lastCountdown > 0 && current == 0) {
                    _event.emit(StartEvent.CountdownFinished)
                }
                lastCountdown = current
            }
        }
    }

    fun onTeamCardNavigated() {
        _navigateToTeamCard.value = null
    }

    private fun checkActiveCompetition() {
        val cid = prefs.activeCompetitionId
        _hasActiveCompetition.value = cid != -1L
    }

    private fun loadStartInterval() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            val comp = competitionRepo.getCompetitionById(cid) ?: return@launch
            val interval = parseIntervalFromSettings(comp.settingsJson)
            _startInterval.value = interval
        }
    }

    private fun parseIntervalFromSettings(settingsJson: String?): Int {
        if (settingsJson.isNullOrBlank()) return TimerManager.DEFAULT_START_INTERVAL
        return try {
            val regex = """"start_interval"\s*:\s*(\d+)""".toRegex()
            regex.find(settingsJson)?.groupValues?.get(1)?.toInt() ?: TimerManager.DEFAULT_START_INTERVAL
        } catch (e: Exception) {
            TimerManager.DEFAULT_START_INTERVAL
        }
    }

    private fun parseMinTeamSizeFromSettings(settingsJson: String?): Int {
        if (settingsJson.isNullOrBlank()) return 2
        return try {
            val regex = """"min_team_size"\s*:\s*(\d+)""".toRegex()
            regex.find(settingsJson)?.groupValues?.get(1)?.toInt() ?: 2
        } catch (e: Exception) {
            2
        }
    }

    fun loadQueue() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) {
                _queue.value = emptyList()
                return@launch
            }

            teamRepo.getRegisteredTeamsByClass(cid, "2")
                .combine(teamRepo.getRegisteredTeamsByClass(cid, "3")) { c2, c3 ->
                    val maxSize = maxOf(c2.size, c3.size)
                    val result = mutableListOf<StartTeamInfo>()
                    for (i in 0 until maxSize) {
                        if (i < c2.size) result.add(enrichTeamInfo(c2[i]))
                        if (i < c3.size) result.add(enrichTeamInfo(c3[i]))
                    }
                    if (result.isNotEmpty()) {
                        result[0] = result[0].copy(isReady = true)
                    }
                    result
                }
                .collect { _queue.value = it }
        }
    }

    private suspend fun enrichTeamInfo(team: com.speleo.start.data.local.entity.TeamEntity): StartTeamInfo {
        val participants = participantRepo.getParticipantsByTeam(team.id).first()

        val memberInfos = mutableListOf<StartMemberInfo>()
        for (participant in participants) {
            val person = personRepo.getPersonById(participant.personId)
            val age = person?.let { calculateAge(it.birthDate) } ?: 0

            val mentor = participant.mentorId?.let { mid ->
                personRepo.getPersonById(mid)
            }

            val mentorName = mentor?.let { m ->
                "${m.lastName} ${m.firstName.firstOrNull()?.plus(".") ?: ""}"
            }

            memberInfos.add(
                StartMemberInfo(
                    personId = participant.personId,
                    lastName = person?.lastName ?: "???",
                    firstName = person?.firstName ?: "???",
                    role = if (participant.role == "captain") "капитан" else "участник",
                    age = age,
                    mentorName = mentorName,
                    mentorConfirmed = participant.mentorConfirmed,
                    judgeApproved = participant.judgeApproved
                )
            )
        }

        val hasMentorIssues = memberInfos.any { member ->
            if (member.age < 18) {
                member.mentorName == null || !member.mentorConfirmed
            } else {
                false
            }
        }

        val hasChildWithoutPermission = memberInfos.any { member ->
            member.age < 14 && !member.judgeApproved
        }

        val hasInactive = participants.any { participant ->
            participant.status != "active"
        }

        val colorMark = when {
            hasInactive -> "СНЯТЫ"
            hasChildWithoutPermission -> "<14 !!!"
            memberInfos.any { it.age < 14 } -> "<14ок"
            memberInfos.any { it.age in 14..17 && !it.mentorConfirmed } -> "<17 !!!"
            memberInfos.any { it.age in 14..17 } -> "14+"
            else -> "18+"
        }

        return StartTeamInfo(
            id = team.id,
            number = team.teamNumber,
            className = team.className,
            members = memberInfos,
            memberCount = participants.size,
            hasMentorIssues = hasMentorIssues,
            hasInactiveMembers = hasInactive,
            hasChildWithoutPermission = hasChildWithoutPermission,
            colorMark = colorMark,
            isReady = false
        )
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
            if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            0
        }
    }

    fun skipCurrentTeam() {
        viewModelScope.launch {
            val currentQueue = _queue.value
            if (currentQueue.isEmpty()) return@launch

            val first = currentQueue.first()

            val cid = prefs.activeCompetitionId
            val allTeams = teamRepo.getTeamsByCompetition(cid).first()
            val startedCount = allTeams.count { it.status == "started" }

            if (startedCount == 0) {
                _validationError.value = StartValidationError(
                    message = "Нельзя пропустить первую команду",
                    canProceed = false
                )
                return@launch
            }

            teamRepo.incrementSkipCount(first.id)
            timer.resetCountdown(_startInterval.value)
            loadQueue()
        }
    }

    fun startCurrentTeam() {
        viewModelScope.launch {
            val currentQueue = _queue.value
            if (currentQueue.isEmpty()) return@launch

            val first = currentQueue.first()

            val cid = prefs.activeCompetitionId
            val comp = competitionRepo.getCompetitionById(cid) ?: return@launch
            val minSize = parseMinTeamSizeFromSettings(comp.settingsJson)

            if (first.memberCount < minSize) {
                _validationError.value = StartValidationError(
                    message = "Недостаточно участников: ${first.memberCount} из $minSize",
                    canProceed = false
                )
                return@launch
            }

            if (first.hasInactiveMembers) {
                _validationError.value = StartValidationError(
                    message = "Есть неактивные участники",
                    canProceed = false
                )
                return@launch
            }

            if (first.hasChildWithoutPermission) {
                _validationError.value = StartValidationError(
                    message = "Нет разрешения судьи для участников до 14 лет",
                    canProceed = false
                )
                return@launch
            }

            if (first.hasMentorIssues) {
                _validationError.value = StartValidationError(
                    message = "Не все менторы подтверждены для участников <18",
                    canProceed = false
                )
                return@launch
            }

            teamRepo.updateTeamStatus(first.id, "started")
            teamRepo.setStartTimestamp(first.id, System.currentTimeMillis())

            val isFirst = !timer.isFirstStart.value
            timer.resetCountdown(_startInterval.value, isFirst = isFirst)

            loadQueue()
            _validationError.value = null
        }
    }

    fun disqualifyCurrentTeam() {
        viewModelScope.launch {
            val currentQueue = _queue.value
            if (currentQueue.isEmpty()) return@launch
            val first = currentQueue.first()
            teamRepo.updateTeamStatus(first.id, "disqualified")
            timer.resetCountdown(_startInterval.value)
            loadQueue()
        }
    }

    fun onTeamClick(teamId: Long) {
        _navigateToTeamCard.value = teamId
    }

    fun startCompetition() {
        timer.startCompetition(TimerManager.FIRST_START_COUNTDOWN)
    }

    fun clearValidationError() {
        _validationError.value = null
    }
}