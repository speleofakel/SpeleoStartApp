package com.speleo.start.presentation.screen.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.presentation.SharedState
import com.speleo.start.util.DateValidator
import com.speleo.start.util.normalizeName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamRegisterVM @Inject constructor(
    private val teamRepo: TeamRepository,
    private val personRepo: PersonRepository,
    private val participantRepo: ParticipantRepository,
    private val competitionRepo: CompetitionRepository,
    private val prefs: PreferencesManager,
    private val sharedState: SharedState
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _mentorSearchQuery = MutableStateFlow("")
    val mentorSearchQuery: StateFlow<String> = _mentorSearchQuery.asStateFlow()

    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) personRepo.searchPersons(query) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mentorSearchResults = _mentorSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) {
                personRepo.searchPersons(query).map { list ->
                    list.filter { person ->
                        val age = DateValidator.calculateAge(person.birthDate)
                        age != null && age >= 18
                    }
                }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _minTeamSize = MutableStateFlow(2)
    val minTeamSize: StateFlow<Int> = _minTeamSize.asStateFlow()

    private val _event = MutableSharedFlow<UiEvent>()
    val event: SharedFlow<UiEvent> = _event.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        data class ConflictDialog(val personName: String, val teamNumber: Int, val className: String, val teamId: Long) : UiEvent()
        data class NavigateToTeam(val teamId: Long) : UiEvent()
        object NavigateBack : UiEvent()
    }

    fun loadCompetitionSettings() {
        viewModelScope.launch {
            val cid = prefs.activeCompetitionId
            if (cid == -1L) return@launch
            val comp = competitionRepo.getCompetitionById(cid) ?: return@launch
            val json = comp.settingsJson
            val minSize = try {
                """"min_team_size"\s*:\s*(\d+)""".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 2
            } catch (e: Exception) { 2 }
            _minTeamSize.value = minSize
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onMentorSearchQueryChange(query: String) { _mentorSearchQuery.value = query }

    fun onPersonSelected(person: PersonEntity, onSelect: () -> Unit) {
        viewModelScope.launch {
            val compId = prefs.activeCompetitionId
            if (compId == -1L) return@launch
            val existing = participantRepo.findActiveByPersonAndComp(person.id, compId)
            if (existing != null) {
                val team = teamRepo.getTeamById(existing.teamId)
                if (team != null) {
                    _event.emit(UiEvent.ConflictDialog(
                        personName = "${person.lastName} ${person.firstName}",
                        teamNumber = team.teamNumber,
                        className = team.className,
                        teamId = team.id
                    ))
                    return@launch
                }
            }
            onSelect()
        }
    }

    fun getPersonById(id: Long, callback: (PersonEntity?) -> Unit) {
        viewModelScope.launch { callback(personRepo.getPersonById(id)) }
    }

    fun createQuickMentor(lastName: String, firstName: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            // FIX: Имя обязательно для ментора
            if (firstName.isBlank()) {
                _event.emit(UiEvent.ShowMessage("Введите имя ментора"))
                return@launch
            }

            val normalizedLastName = lastName.normalizeName()
            val normalizedFirstName = firstName.normalizeName()

            val id = personRepo.createPerson(
                lastName = normalizedLastName,
                firstName = normalizedFirstName,
                birthDate = null,
                gender = null
            )
            if (id != -1L) {
                _event.emit(UiEvent.ShowMessage("Ментор создан"))
                onCreated(id)
            } else {
                _event.emit(UiEvent.ShowMessage("Ошибка создания ментора"))
            }
        }
    }

    fun createQuickPerson(
        lastName: String,
        firstName: String,
        middleName: String?,
        birthDate: String?,
        phone: String?,
        gender: String?,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val normalizedLastName = lastName.normalizeName()
            val normalizedFirstName = firstName.normalizeName()
            val normalizedMiddleName = middleName?.normalizeName()

            val id = personRepo.createPerson(
                lastName = normalizedLastName,
                firstName = normalizedFirstName,
                middleName = normalizedMiddleName,
                birthDate = birthDate,
                phone = phone,
                gender = gender
            )
            if (id != -1L) {
                _event.emit(UiEvent.ShowMessage("Персона создана"))
                onCreated(id)
            } else {
                _event.emit(UiEvent.ShowMessage("Ошибка создания персоны"))
            }
        }
    }

    fun registerTeam(className: String, members: List<MemberDraft>) {
        viewModelScope.launch {
            val competitionId = prefs.activeCompetitionId
            if (competitionId == -1L) {
                _event.emit(UiEvent.ShowMessage("Нет активного соревнования"))
                return@launch
            }

            val resolvedMembers = members.map { draft ->
                if (draft.isQuickCreate) {
                    val normalizedLastName = draft.quickLastName.normalizeName()
                    val normalizedFirstName = draft.quickFirstName.normalizeName()
                    val normalizedMiddleName = if (draft.quickMiddleName.isNotBlank()) draft.quickMiddleName.normalizeName() else null

                    val personId = personRepo.createPerson(
                        lastName = normalizedLastName,
                        firstName = normalizedFirstName,
                        middleName = normalizedMiddleName,
                        birthDate = draft.quickBirthDate.ifBlank { null },
                        phone = draft.quickPhone.ifBlank { null },
                        gender = draft.quickGender
                    )
                    if (personId == -1L) {
                        _event.emit(UiEvent.ShowMessage("Ошибка создания персоны"))
                        return@launch
                    }
                    val savedPerson = personRepo.getPersonById(personId)
                    if (savedPerson != null) draft.copy(person = savedPerson, isQuickCreate = false) else draft
                } else draft
            }

            val invalidMembers = resolvedMembers.filter { member ->
                when (member.ageGroup) {
                    AgeGroup.MINOR -> member.mentor == null || !member.mentorConfirmed
                    AgeGroup.CHILD -> member.mentor == null || !member.mentorConfirmed || !member.judgeApproved
                    else -> false
                }
            }
            if (invalidMembers.isNotEmpty()) {
                _event.emit(UiEvent.ShowMessage("Проверьте менторов и разрешения судьи"))
                return@launch
            }

            val maxNumber = teamRepo.getMaxTeamNumber(competitionId, className) ?: 0
            val teamNumber = maxNumber + 1
            val teamId = teamRepo.createTeam(competitionId = competitionId, teamNumber = teamNumber, className = className)
            if (teamId == -1L) {
                _event.emit(UiEvent.ShowMessage("Ошибка создания команды"))
                return@launch
            }

            resolvedMembers.forEachIndexed { index, member ->
                val person = member.person ?: return@forEachIndexed
                val mentorId = member.mentor?.id
                // Там где добавляется участник
                val participantId = participantRepo.addParticipant(
                    teamId = teamId,
                    personId = person.id,
                    role = if (index == 0) "captain" else "member",
                    mentorId = mentorId
                )

// Сразу после добавления обновляем флаги
                if (mentorId != null) {
                    participantRepo.updateMentorAndFlags(
                        id = participantId,
                        mentorId = mentorId,
                        mentorConfirmed = true,
                        judgeApproved = member.judgeApproved
                    )
                }
                if (participantId == -1L) {
                    _event.emit(UiEvent.ShowMessage("Ошибка добавления участника"))
                    return@launch
                }
                if (mentorId != null || member.judgeApproved) {
                    participantRepo.updateMentorAndFlags(
                        id = participantId, mentorId = mentorId,
                        mentorConfirmed = member.mentor != null,
                        judgeApproved = member.judgeApproved
                    )
                }
            }
            _event.emit(UiEvent.ShowMessage("Команда №$teamNumber сохранена"))
            _event.emit(UiEvent.NavigateBack)
        }
    }
}

enum class AgeGroup { ADULT, MINOR, CHILD, UNKNOWN }

data class MemberDraft(
    val person: PersonEntity? = null,
    val isQuickCreate: Boolean = false,
    val ageGroup: AgeGroup = AgeGroup.UNKNOWN,
    val mentor: PersonEntity? = null,
    val mentorConfirmed: Boolean = false,
    val judgeApproved: Boolean = false,
    var quickLastName: String = "",
    var quickFirstName: String = "",
    var quickMiddleName: String = "",
    var quickBirthDate: String = "",
    var quickPhone: String = "",
    var quickGender: String = "male"
) {
    val isValid: Boolean
        get() = when {
            person != null -> true
            isQuickCreate -> quickLastName.isNotBlank() && quickFirstName.isNotBlank() &&
                    (quickBirthDate.isBlank() || (quickBirthDate.length == 10 && DateValidator.isRealDate(quickBirthDate)))
            else -> false
        }
}