package com.speleo.start.presentation.screen.teamcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.entity.*
import com.speleo.start.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val replacedCount: Int
)

@HiltViewModel
class TeamCardVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    private val mentorRepo: MentorRepository,
    private val prefs: com.speleo.start.data.local.PreferencesManager
) : ViewModel() {

    private val _teamCard = MutableStateFlow<TeamCardInfo?>(null)
    val teamCard: StateFlow<TeamCardInfo?> = _teamCard.asStateFlow()

    fun loadTeam(teamId: Long) {
        viewModelScope.launch {
            val team = teamRepo.getTeamById(teamId) ?: return@launch
            val participants = participantRepo.getAllParticipantsByTeam(teamId)

            // Ждём первый эмит из Flow
            val members = mutableListOf<TeamCardMember>()
            participants.first().forEach { p ->
                val person = personRepo.getPersonById(p.personId)
                val mentor = if (p.mentorId != null) mentorRepo.getMentorByParticipantId(p.id) else null
                val mentorPerson = if (mentor != null) personRepo.getPersonById(mentor.personId) else null

                if (person != null) {
                    val age = person.birthDate?.let { birth ->
                        try {
                            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                            val birthDate = sdf.parse(birth)
                            val today = java.util.Calendar.getInstance()
                            val birthCal = java.util.Calendar.getInstance().apply { time = birthDate!! }
                            var a = today.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR)
                            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCal.get(java.util.Calendar.DAY_OF_YEAR)) a--
                            a
                        } catch (e: Exception) { null }
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
                            mentorName = mentorPerson?.let { "${it.lastName} ${it.firstName}" },
                            mentorConfirmed = p.mentorConfirmed,
                            judgeApproved = p.judgeApproved
                        )
                    )
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
                replacedCount = participants.first().count { it.statusMember == "replaced" }
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

    fun getColorMarkType(): String {
        val card = _teamCard.value ?: return "green"
        val hasMinor = card.members.any { it.age != null && it.age in 14..17 && !it.mentorConfirmed }
        val hasChild = card.members.any { it.age != null && it.age < 14 && !it.judgeApproved }
        return when {
            card.status == "disqualified" -> "red"
            hasMinor || hasChild -> "orange"
            else -> "green"
        }
    }

    fun getFabAction(): String? {
        val card = _teamCard.value ?: return null
        return when (card.status) {
            "started" -> "finish"
            "finished" -> "route"
            else -> null
        }
    }
}