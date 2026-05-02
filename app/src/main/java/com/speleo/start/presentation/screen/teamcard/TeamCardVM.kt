package com.speleo.start.presentation.screen.teamcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.repository.MentorRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.util.AgeCalculator
import com.speleo.start.util.normalizeName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _teamCard = MutableStateFlow<TeamCardInfo?>(null)
    val teamCard: StateFlow<TeamCardInfo?> = _teamCard.asStateFlow()

    private val _event = MutableSharedFlow<TeamCardEvent>()
    val event: SharedFlow<TeamCardEvent> = _event.asSharedFlow()

    fun loadTeam(teamId: Long) {
        viewModelScope.launch {
            val team = teamRepo.getTeamById(teamId) ?: return@launch
            val participants = participantRepo.getAllParticipantsByTeam(teamId).first()
            val activeParticipants = participants.filter { it.statusMember == "active" }
            val replacedParticipants = participants.filter { it.statusMember == "replaced" }

            val members = mutableListOf<TeamCardMember>()
            for (p in activeParticipants) {
                val person = personRepo.getPersonById(p.personId) ?: continue
                val age = AgeCalculator.calculateAge(person.birthDate)

                var mentorName: String? = null
                var mentorConfirmed = p.mentorConfirmed

                if (p.mentorId != null) {
                    val mentor = mentorRepo.getMentorByParticipantId(p.id)
                    if (mentor != null) {
                        val mentorPerson = personRepo.getPersonById(mentor.personId)
                        mentorName = mentorPerson?.let { "${it.lastName} ${it.firstName}" }
                    }
                }

                members.add(
                    TeamCardMember(
                        participantId = p.id,
                        personId = person.id,
                        firstName = person.firstName,
                        lastName = person.lastName,
                        nickname = person.nickname,
                        birthDate = person.birthDate,
                        phone = person.phone,
                        gender = person.gender,
                        age = age,
                        role = p.role,
                        mentorName = mentorName,
                        mentorConfirmed = mentorConfirmed,
                        judgeApproved = p.judgeApproved
                    )
                )
            }

            val replacedMembers = replacedParticipants.mapNotNull { replaced ->
                val person = personRepo.getPersonById(replaced.personId)
                person?.let { "${it.lastName} ${it.firstName}" }
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

    fun getColorMarkText(): String {
        val card = _teamCard.value ?: return ""
        val hasMinor = card.members.any { it.age != null && it.age in 14..17 && !it.mentorConfirmed }
        val hasChild = card.members.any { it.age != null && it.age < 14 && !it.judgeApproved }
        return when {
            card.status == "disqualified" -> "СНЯТЫ"
            hasChild -> "<14 !!!"
            hasMinor -> "<17 !!!"
            else -> {
                if (card.members.any { it.age != null && it.age in 14..17 }) "14+"
                else if (card.members.any { it.age != null && it.age < 14 }) "<14ок"
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

                val existingInTeam = participantRepo.getActiveParticipantsByTeam(team.id).first()
                    .any { it.personId == newPersonId }
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

            // FIX: Применяем нормализацию к введенным данным
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

                // Проверяем возраст ментора (должен быть >= 18)
                val mentorAge = AgeCalculator.calculateAge(mentorPerson.birthDate)
                if (mentorAge == null || mentorAge < 18) {
                    _event.emit(TeamCardEvent.ShowMessage("Выбранный человек не может быть ментором (возраст < 18)"))
                    return@launch
                }

                // Находим или создаем запись в таблице mentors
                var mentorEntity = mentorRepo.getMentorByPersonId(mentorPersonId)
                if (mentorEntity == null) {
                    val newMentorId = mentorRepo.createMentor(mentorPersonId)
                    if (newMentorId == -1L) {
                        _event.emit(TeamCardEvent.ShowMessage("Не удалось создать запись ментора"))
                        return@launch
                    }
                    mentorEntity = mentorRepo.getMentorByPersonId(mentorPersonId)
                }

                // Обновляем участника: привязываем ментора и подтверждаем
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
                val participants = participantRepo.getActiveParticipantsByTeam(card.teamId).first()
                for (participant in participants) {
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