package com.speleo.start.presentation.screen.teamcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.local.dao.AppSettingsDao
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.local.entity.TeamRouteCardEntity
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.MasterRouteCardRepository
import com.speleo.start.data.repository.MentorRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.data.repository.TeamRouteCardRepository
import com.speleo.start.presentation.TimerManager
import com.speleo.start.util.AgeCalculator
import com.speleo.start.util.normalizeName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamCardMember(
    val participantId: Long,
    val personId: Long,
    val firstName: String,
    val lastName: String,
    val nickname: String?,
    val birthDate: String?,
    val phone: String?,
    val gender: String?,
    val age: Int?,
    val role: String,
    val mentorName: String?,
    val mentorConfirmed: Boolean,
    val judgeApproved: Boolean
)

data class TeamCardInfo(
    val teamId: Long,
    val number: Int,
    val className: String,
    val status: String,
    val colorMark: String,
    val startTimestamp: Long?,
    val finishTimestamp: Long?,
    val members: List<TeamCardMember>,
    val replacedCount: Int,
    val replacedMembers: List<String> = emptyList(),
    val checkpointsEntered: Boolean = false
)

data class CheckpointGridItem(
    val checkpointId: Long,
    val displayNumber: Int,
    val taken: Boolean,
    val takenWithError: Boolean
)

data class RouteCardSaveEntry(
    val checkpointId: Long,
    val taken: Boolean,
    val takenWithError: Boolean,
    val offsetTime: Long?,
    val penalty: Int
)

sealed class TeamCardEvent {
    data class ShowMessage(val message: String) : TeamCardEvent()
    data class ShowPasswordDialog(val action: String, val reason: String? = null) : TeamCardEvent()
    data class ShowReplaceDialog(val participantId: Long, val currentPersonName: String) : TeamCardEvent()
    data class TeamUpdated(val teamId: Long) : TeamCardEvent()
}

@HiltViewModel
class TeamCardVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    private val mentorRepo: MentorRepository,
    private val masterRouteCardRepo: MasterRouteCardRepository,
    private val teamRouteCardRepo: TeamRouteCardRepository,
    private val competitionRepo: CompetitionRepository,
    private val prefs: PreferencesManager,
    private val appSettingsDao: AppSettingsDao,
    private val timerManager: TimerManager
) : ViewModel() {

    private val _teamCard = MutableStateFlow<TeamCardInfo?>(null)
    val teamCard: StateFlow<TeamCardInfo?> = _teamCard.asStateFlow()

    private val _checkpoints = MutableStateFlow<List<CheckpointGridItem>>(emptyList())
    val checkpoints: StateFlow<List<CheckpointGridItem>> = _checkpoints.asStateFlow()

    private val _startTimeRelative = MutableStateFlow("—:—:—")
    val startTimeRelative: StateFlow<String> = _startTimeRelative.asStateFlow()

    private val _finishTimeRelative = MutableStateFlow("—:—:—")
    val finishTimeRelative: StateFlow<String> = _finishTimeRelative.asStateFlow()

    private val _takenCount = MutableStateFlow(0)
    val takenCount: StateFlow<Int> = _takenCount.asStateFlow()

    private val _totalCheckpoints = MutableStateFlow(0)
    val totalCheckpointsCount: StateFlow<Int> = _totalCheckpoints.asStateFlow()

    private val _isMasterMode = MutableStateFlow(false)
    val isMasterMode: StateFlow<Boolean> = _isMasterMode.asStateFlow()

    private val _event = MutableSharedFlow<TeamCardEvent>()
    val event: SharedFlow<TeamCardEvent> = _event.asSharedFlow()

    private var competitionStartTimestamp: Long = 0L

    private suspend fun loadCompetitionStartTimestamp() {
        competitionStartTimestamp = appSettingsDao.get(TimerManager.KEY_START_TIMESTAMP)?.toLongOrNull() ?: 0L
    }

    fun loadTeam(teamId: Long) {
        viewModelScope.launch {
            loadCompetitionStartTimestamp()

            val team = teamRepo.getTeamById(teamId) ?: return@launch

            updateRelativeTimes(team.startTimestamp, team.finishTimestamp)

            val participants = participantRepo.getAllParticipantsByTeam(teamId).first()

            val activeParticipants = participants.filter { participant -> participant.status == "active" }
            val replacedParticipants = participants.filter { participant -> participant.status == "replaced" }

            val members = mutableListOf<TeamCardMember>()
            for (participant in activeParticipants) {
                val person = personRepo.getPersonById(participant.personId) ?: continue
                val age = AgeCalculator.calculateAge(person.birthDate)

                var mentorName: String? = null
                var mentorConfirmed = participant.mentorConfirmed

                if (participant.mentorId != null) {
                    val mentor = mentorRepo.getMentorByParticipantId(participant.id)
                    if (mentor != null) {
                        val mentorPerson = personRepo.getPersonById(mentor.personId)
                        mentorName = mentorPerson?.let { "${it.lastName} ${it.firstName}" }
                    }
                }

                members.add(
                    TeamCardMember(
                        participantId = participant.id,
                        personId = person.id,
                        firstName = person.firstName,
                        lastName = person.lastName,
                        nickname = person.nickname,
                        birthDate = person.birthDate,
                        phone = person.phone,
                        gender = person.gender,
                        age = age,
                        role = participant.role,
                        mentorName = mentorName,
                        mentorConfirmed = mentorConfirmed,
                        judgeApproved = participant.judgeApproved
                    )
                )
            }

            val replacedMembers = mutableListOf<String>()
            for (replaced in replacedParticipants) {
                val person = personRepo.getPersonById(replaced.personId)
                if (person != null) {
                    replacedMembers.add("${person.lastName} ${person.firstName}")
                }
            }

            _teamCard.value = TeamCardInfo(
                teamId = team.id,
                number = team.teamNumber,
                className = team.className,
                status = team.status,
                colorMark = team.colorMark,
                startTimestamp = team.startTimestamp,
                finishTimestamp = team.finishTimestamp,
                members = members,
                replacedCount = replacedParticipants.size,
                replacedMembers = replacedMembers,
                checkpointsEntered = team.checkpointsEntered
            )
        }
    }

    fun loadCheckpoints(teamId: Long) {
        viewModelScope.launch {
            val team = teamRepo.getTeamById(teamId) ?: return@launch

            masterRouteCardRepo.getRouteCardByCompetition(team.competitionId)
                .combine(teamRouteCardRepo.getRouteCardByTeam(teamId)) { master, teamRoute ->
                    _totalCheckpoints.value = master.size
                    _takenCount.value = teamRoute.count { it.taken }

                    master.map { cp ->
                        val te = teamRoute.find { it.checkpointId == cp.id }
                        CheckpointGridItem(
                            checkpointId = cp.id,
                            displayNumber = cp.displayNumber,
                            taken = te?.taken ?: false,
                            takenWithError = te?.takenWithError ?: false
                        )
                    }
                }
                .collect { _checkpoints.value = it }
        }
    }

    private fun updateRelativeTimes(startTimestamp: Long?, finishTimestamp: Long?) {
        if (competitionStartTimestamp == 0L) {
            _startTimeRelative.value = "—:—:—"
            _finishTimeRelative.value = "—:—:—"
            return
        }

        _startTimeRelative.value = startTimestamp?.let {
            val relativeSeconds = (it - competitionStartTimestamp) / 1000
            if (relativeSeconds < 0) "--:--:--" else formatRelativeTime(relativeSeconds.toInt())
        } ?: "—:—:—"

        _finishTimeRelative.value = finishTimestamp?.let {
            val relativeSeconds = (it - competitionStartTimestamp) / 1000
            if (relativeSeconds < 0) "--:--:--" else formatRelativeTime(relativeSeconds.toInt())
        } ?: "—:—:—"
    }

    private fun formatRelativeTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, secs)
        else "%02d:%02d".format(minutes, secs)
    }

    fun adjustFinishTimeRelative(relativeSeconds: Int) {
        viewModelScope.launch {
            val team = _teamCard.value ?: return@launch
            if (competitionStartTimestamp == 0L) return@launch

            val absoluteTimestamp = competitionStartTimestamp + (relativeSeconds * 1000L)
            teamRepo.setFinishTimestamp(team.teamId, absoluteTimestamp)

            _finishTimeRelative.value = formatRelativeTime(relativeSeconds)
            _event.emit(TeamCardEvent.ShowMessage("Время финиша обновлено"))
            loadTeam(team.teamId)
        }
    }

    fun masterUnlockRouteCard(teamId: Long, password: String): Boolean {
        if (password == "devdebug") {
            viewModelScope.launch {
                val team = teamRepo.getTeamById(teamId)
                if (team != null) {
                    teamRepo.updateTeam(team.copy(checkpointsEntered = false))
                    _event.emit(TeamCardEvent.ShowMessage("🔓 Путевой лист разблокирован для мастер-правки"))
                }
            }
            return true
        }
        return false
    }

    fun masterSaveRouteCard(teamId: Long, entries: List<RouteCardSaveEntry>) {
        viewModelScope.launch {
            val team = teamRepo.getTeamById(teamId) ?: return@launch

            for (entry in entries) {
                // ✅ ИСПРАВЛЕНО: getEntry возвращает TeamRouteCardEntity?, не Flow
                val existing = teamRouteCardRepo.getEntry(teamId, entry.checkpointId)
                val teamRouteCard = TeamRouteCardEntity(
                    teamId = teamId,
                    checkpointId = entry.checkpointId,
                    taken = entry.taken,
                    takenWithError = entry.takenWithError,
                    offsetTime = entry.offsetTime,
                    penalty = entry.penalty,
                    judgeConfirmed = existing?.judgeConfirmed ?: true,
                    secretaryConfirmed = existing?.secretaryConfirmed ?: true
                )
                teamRouteCardRepo.saveEntry(teamRouteCard)
            }

            teamRepo.updateTeam(team.copy(checkpointsEntered = true))
            _isMasterMode.value = false
            _event.emit(TeamCardEvent.ShowMessage("✅ Путевой лист сохранён. Подтверждения не требуются."))
            loadTeam(teamId)
            loadCheckpoints(teamId)
        }
    }

    fun unlockRouteCardForEdit(teamId: Long) {
        viewModelScope.launch {
            val team = teamRepo.getTeamById(teamId) ?: return@launch
            teamRepo.updateTeam(team.copy(checkpointsEntered = false))
            _isMasterMode.value = false
            loadTeam(teamId)
            loadCheckpoints(teamId)
        }
    }

    fun getColorMarkText(): String {
        val card = _teamCard.value ?: return ""
        val hasMinor = card.members.any { member -> member.age != null && member.age in 14..17 && !member.mentorConfirmed }
        val hasChild = card.members.any { member -> member.age != null && member.age < 14 && !member.judgeApproved }
        return when {
            card.status == "disqualified" -> "СНЯТЫ"
            hasChild -> "<14 !!!"
            hasMinor -> "<17 !!!"
            else -> {
                if (card.members.any { member -> member.age != null && member.age in 14..17 }) "14+"
                else if (card.members.any { member -> member.age != null && member.age < 14 }) "<14ок"
                else "18+"
            }
        }
    }

    fun getFabAction(): String? {
        val card = _teamCard.value ?: return null
        return when (card.status) {
            "started" -> "finish"
            "finished" -> if (!card.checkpointsEntered) "route" else null
            else -> null
        }
    }

    fun canEdit(): Boolean {
        val card = _teamCard.value ?: return false
        return card.status == "registered" || card.status == "started"
    }

    fun replaceMember(participantId: Long, newPersonId: Long) {
        viewModelScope.launch {
            try {
                val oldParticipant = participantRepo.getParticipantById(participantId)
                if (oldParticipant == null) {
                    _event.emit(TeamCardEvent.ShowMessage("Участник не найден"))
                    return@launch
                }

                val team = teamRepo.getTeamById(oldParticipant.teamId)
                if (team == null) {
                    _event.emit(TeamCardEvent.ShowMessage("Команда не найдена"))
                    return@launch
                }

                val activeParticipants = participantRepo.getActiveParticipantsByTeam(team.id).first()
                var existingInTeam = false
                for (participant in activeParticipants) {
                    if (participant.personId == newPersonId) {
                        existingInTeam = true
                        break
                    }
                }
                if (existingInTeam) {
                    _event.emit(TeamCardEvent.ShowMessage("Эта персона уже в команде"))
                    return@launch
                }

                val existingInComp = participantRepo.findActiveByPersonAndComp(newPersonId, team.competitionId)
                if (existingInComp != null) {
                    val otherTeam = teamRepo.getTeamById(existingInComp.teamId)
                    _event.emit(TeamCardEvent.ShowMessage(
                        "Персона уже в команде №${otherTeam?.teamNumber} (${otherTeam?.className}-й класс)"
                    ))
                    return@launch
                }

                participantRepo.updateParticipantStatus(oldParticipant.id, "replaced")

                val newParticipantId = participantRepo.addParticipant(
                    teamId = team.id,
                    personId = newPersonId,
                    role = oldParticipant.role,
                    mentorId = oldParticipant.mentorId
                )

                if (newParticipantId != -1L) {
                    _event.emit(TeamCardEvent.ShowMessage("Участник заменён"))
                    loadTeam(team.id)
                } else {
                    _event.emit(TeamCardEvent.ShowMessage("Ошибка при добавлении участника"))
                }
            } catch (e: Exception) {
                _event.emit(TeamCardEvent.ShowMessage("Ошибка: ${e.message}"))
            }
        }
    }

    fun createQuickPerson(fullName: String, onResult: (PersonEntity) -> Unit) {
        viewModelScope.launch {
            val parts = fullName.split(" ")
            val rawLastName = parts[0]
            val rawFirstName = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

            val lastName = rawLastName.normalizeName()
            val firstName = rawFirstName.normalizeName()

            val personId = personRepo.createPerson(
                lastName = lastName,
                firstName = firstName,
                middleName = null,
                birthDate = null,
                phone = null,
                gender = null
            )

            if (personId != -1L) {
                val person = personRepo.getPersonById(personId)
                if (person != null) {
                    _event.emit(TeamCardEvent.ShowMessage("Персона создана"))
                    onResult(person)
                } else {
                    _event.emit(TeamCardEvent.ShowMessage("Ошибка: персона не найдена после создания"))
                }
            } else {
                _event.emit(TeamCardEvent.ShowMessage("Ошибка при создании персоны"))
            }
        }
    }

    fun assignMentor(participantId: Long, mentorPersonId: Long) {
        viewModelScope.launch {
            try {
                val participant = participantRepo.getParticipantById(participantId)
                if (participant == null) {
                    _event.emit(TeamCardEvent.ShowMessage("Участник не найден"))
                    return@launch
                }

                val mentorPerson = personRepo.getPersonById(mentorPersonId)
                if (mentorPerson == null) {
                    _event.emit(TeamCardEvent.ShowMessage("Ментор не найден"))
                    return@launch
                }

                val mentorAge = AgeCalculator.calculateAge(mentorPerson.birthDate)
                if (mentorAge == null || mentorAge < 18) {
                    _event.emit(TeamCardEvent.ShowMessage("Выбранный человек не может быть ментором (возраст < 18)"))
                    return@launch
                }

                var mentorEntity = mentorRepo.getMentorByPersonId(mentorPersonId)
                if (mentorEntity == null) {
                    val newMentorId = mentorRepo.createMentor(mentorPersonId)
                    if (newMentorId == -1L) {
                        _event.emit(TeamCardEvent.ShowMessage("Не удалось создать запись ментора"))
                        return@launch
                    }
                    mentorEntity = mentorRepo.getMentorByPersonId(mentorPersonId)
                }

                participantRepo.updateMentorAndFlags(
                    id = participant.id,
                    mentorId = mentorEntity?.id,
                    mentorConfirmed = true,
                    judgeApproved = participant.judgeApproved
                )
                _event.emit(TeamCardEvent.ShowMessage("Ментор ${mentorPerson.lastName} ${mentorPerson.firstName} назначен"))
                loadTeam(participant.teamId)
            } catch (e: Exception) {
                _event.emit(TeamCardEvent.ShowMessage("Ошибка при назначении ментора: ${e.message}"))
            }
        }
    }

    fun removeMember(participantId: Long) {
        viewModelScope.launch {
            try {
                val participant = participantRepo.getParticipantById(participantId)
                if (participant == null) {
                    _event.emit(TeamCardEvent.ShowMessage("Участник не найден"))
                    return@launch
                }

                val team = teamRepo.getTeamById(participant.teamId)
                if (team == null) {
                    _event.emit(TeamCardEvent.ShowMessage("Команда не найдена"))
                    return@launch
                }

                val activeMembers = participantRepo.getActiveParticipantsByTeam(team.id).first()
                if (activeMembers.size <= 1) {
                    _event.emit(TeamCardEvent.ShowMessage("Нельзя удалить последнего участника. Используйте «Расформировать»"))
                    return@launch
                }

                participantRepo.updateParticipantStatus(participant.id, "free_agent")
                _event.emit(TeamCardEvent.ShowMessage("Участник исключён из команды"))
                loadTeam(team.id)
            } catch (e: Exception) {
                _event.emit(TeamCardEvent.ShowMessage("Ошибка: ${e.message}"))
            }
        }
    }

    fun disbandTeam(password: String, reason: String) {
        viewModelScope.launch {
            val card = _teamCard.value
            if (card == null) {
                _event.emit(TeamCardEvent.ShowMessage("Команда не найдена"))
                return@launch
            }

            when (card.status) {
                "finished", "lost", "disqualified" -> {
                    _event.emit(TeamCardEvent.ShowMessage("Нельзя расформировать ${card.status} команду"))
                    return@launch
                }
                "started" -> {
                    if (password != "1234" && password != "devdebug") {
                        _event.emit(TeamCardEvent.ShowMessage("Неверный пароль"))
                        return@launch
                    }
                }
                "registered" -> { }
            }

            try {
                val activeParticipants = participantRepo.getActiveParticipantsByTeam(card.teamId).first()
                for (participant in activeParticipants) {
                    participantRepo.updateParticipantStatus(participant.id, "free_agent")
                }

                teamRepo.updateTeamStatus(card.teamId, "disqualified")

                val reasonText = if (reason.isNotBlank()) " Причина: $reason" else ""
                _event.emit(TeamCardEvent.ShowMessage("Команда №${card.number} расформирована.$reasonText"))
                loadTeam(card.teamId)
            } catch (e: Exception) {
                _event.emit(TeamCardEvent.ShowMessage("Ошибка: ${e.message}"))
            }
        }
    }

    fun changeClass(newClassName: String, password: String? = null) {
        viewModelScope.launch {
            _event.emit(TeamCardEvent.ShowMessage("Смена класса временно недоступна"))
        }
    }
}