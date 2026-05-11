package com.speleo.start.presentation.screen.teamcard

import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.local.dao.AppSettingsDao
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber
import java.util.regex.Pattern

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
    private val appSettingsDao: AppSettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamCardUiState())
    val uiState: StateFlow<TeamCardUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<TeamCardUiEvent>(extraBufferCapacity = 1)
    val event: SharedFlow<TeamCardUiEvent> = _event.asSharedFlow()

    private var teamId: Long = -1L
    private var competitionId: Long = -1L
    private var competitionStartTimestamp: Long = 0L

    private var masterEditCache: List<RouteCardEntryUi>? = null

    // Регулярное выражение для валидации MM:CC
    private val TIME_MM_SS_PATTERN = Pattern.compile("^([0-5]?[0-9]):([0-5][0-9])$")

    // ============================================================
    // ЗАГРУЗКА ДАННЫХ
    // ============================================================

    fun loadData(teamId: Long) {
        this.teamId = teamId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            loadCompetitionStartTimestamp()
            loadTeamAndMembers()
            loadRouteCard()
            loadReplacedHistory()
            loadSignatureStatus()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadCompetitionStartTimestamp() {
        competitionStartTimestamp = appSettingsDao.get(TimerManager.KEY_START_TIMESTAMP)?.toLongOrNull() ?: 0L
        Timber.d("Competition start timestamp: $competitionStartTimestamp")
        _uiState.update { it.copy(competitionStartTimestamp = competitionStartTimestamp) }
    }

    private suspend fun loadSignatureStatus() {
        val entries = teamRouteCardRepo.getRouteCardByTeamFirst(teamId)
        val allSecretary = entries.isNotEmpty() && entries.all { it.secretaryConfirmed }
        val allJudge = entries.isNotEmpty() && entries.all { it.judgeConfirmed }
        _uiState.update {
            it.copy(
                isSecretarySigned = allSecretary,
                isJudgeSigned = allJudge
            )
        }
    }

    private suspend fun loadTeamAndMembers() {
        val team = teamRepo.getTeamById(teamId) ?: return
        competitionId = team.competitionId

        // ВРЕМЕННЫЙ ЛОГ - удалить после отладки
        Timber.e("!!! TEAM DATA: id=${team.id}, startTimestamp=${team.startTimestamp}, finishTimestamp=${team.finishTimestamp}")

        Timber.d("Team loaded: id=${team.id}, status=${team.status}, startTimestamp=${team.startTimestamp}, finishTimestamp=${team.finishTimestamp}, checkpointsEntered=${team.checkpointsEntered}")

        val participants = participantRepo.getAllParticipantsByTeam(teamId).first()
        val activeParticipants = participants.filter { it.status == "active" }

        val members = mutableListOf<MemberUi>()
        for (participant in activeParticipants) {
            val person = personRepo.getPersonById(participant.personId) ?: continue
            val age = AgeCalculator.calculateAge(person.birthDate)

            var mentorName: String? = null
            if (participant.mentorId != null) {
                val mentor = mentorRepo.getMentorByParticipantId(participant.id)
                if (mentor != null) {
                    val mentorPerson = personRepo.getPersonById(mentor.personId)
                    mentorName = mentorPerson?.let { "${it.lastName} ${it.firstName}" }
                }
            }

            members.add(
                MemberUi(
                    participantId = participant.id,
                    personId = person.id,
                    firstName = person.firstName,
                    lastName = person.lastName,
                    nickname = person.nickname,
                    age = age,
                    phone = person.phone,
                    role = participant.role,
                    mentorName = mentorName,
                    mentorConfirmed = participant.mentorConfirmed,
                    judgeApproved = participant.judgeApproved
                )
            )
        }

        val colorMark = calculateColorMark(members, team.status)

        _uiState.update {
            it.copy(
                teamInfo = TeamInfo(
                    id = team.id,
                    number = team.teamNumber,
                    className = team.className,
                    status = team.status,
                    colorMark = colorMark,
                    startTimestamp = team.startTimestamp,
                    finishTimestamp = team.finishTimestamp,
                    checkpointsEntered = team.checkpointsEntered
                ),
                members = members
            )
        }
    }

    private suspend fun loadRouteCard() {
        val masterList = masterRouteCardRepo.getRouteCardByCompetitionFirst(competitionId)
        val teamEntries = teamRouteCardRepo.getRouteCardByTeamFirst(teamId)

        val entries = masterList.map { master ->
            val existing = teamEntries.find { it.checkpointId == master.id }
            RouteCardEntryUi(
                checkpointId = master.id,
                displayNumber = master.displayNumber,
                weight = master.weight,
                type = master.type,
                taken = existing?.taken ?: false,
                takenWithError = existing?.takenWithError ?: false,
                offsetTime = existing?.offsetTime?.let { formatSecondsToMmSs(it.toInt()) } ?: "",
                penalty = existing?.penalty ?: 0,
                secretaryConfirmed = existing?.secretaryConfirmed ?: false,
                judgeConfirmed = existing?.judgeConfirmed ?: false
            )
        }

        val takenCount = entries.count { it.taken }
        val totalCount = entries.size

        _uiState.update {
            it.copy(
                routeCardEntries = entries,
                routeCardStats = RouteCardStats(takenCount, totalCount)
            )
        }
    }

    private suspend fun loadReplacedHistory() {
        val participants = participantRepo.getAllParticipantsByTeam(teamId).first()
        val replaced = participants.filter { it.status == "replaced" }
        val replacedNames = mutableListOf<String>()

        for (replacedParticipant in replaced) {
            val person = personRepo.getPersonById(replacedParticipant.personId)
            if (person != null) {
                replacedNames.add("${person.lastName} ${person.firstName}")
            }
        }

        _uiState.update { it.copy(replacedHistory = replacedNames) }
    }

    // ============================================================
    // РАСЧЁТ ЦВЕТОВОЙ МЕТКИ
    // ============================================================

    private fun calculateColorMark(members: List<MemberUi>, status: String): String {
        if (status == "disqualified") return "СНЯТЫ"

        val hasMinor = members.any { it.age != null && it.age in 14..17 && !it.mentorConfirmed }
        val hasChild = members.any { it.age != null && it.age < 14 && !it.judgeApproved }
        val hasMinorWithMentor = members.any { it.age != null && it.age in 14..17 && it.mentorConfirmed }
        val hasChildWithPermission = members.any { it.age != null && it.age < 14 && it.judgeApproved }

        return when {
            hasChild -> "<14 !!!"
            hasMinor -> "<17 !!!"
            hasChildWithPermission -> "<14ок"
            hasMinorWithMentor -> "14+"
            else -> "18+"
        }
    }

    // ============================================================
    // ОБНОВЛЕНИЕ СТАТИСТИКИ КП
    // ============================================================

    private fun updateRouteCardStats() {
        val entries = _uiState.value.routeCardEntries
        val stats = RouteCardStats(
            takenCount = entries.count { it.taken },
            totalCount = entries.size
        )
        _uiState.update { it.copy(routeCardStats = stats) }
    }

    // ============================================================
    // УПРАВЛЕНИЕ РЕЖИМАМИ
    // ============================================================

    fun enterEditMode() {
        val currentState = _uiState.value
        val teamInfo = currentState.teamInfo ?: return

        if (teamInfo.status == "finished" && !teamInfo.checkpointsEntered) {
            initializeRouteCardEntriesIfNeeded()
            _uiState.update { it.copy(mode = TeamCardMode.EDIT) }
        }
    }

    fun enterQuickEditMode() {
        initializeRouteCardEntriesIfNeeded()
        _uiState.update { it.copy(mode = TeamCardMode.EDIT) }
    }

    fun enterMasterEditMode(password: String): Boolean {
        if (password == "devdebug") {
            masterEditCache = _uiState.value.routeCardEntries.map { it.copy() }
            _uiState.update { it.copy(mode = TeamCardMode.MASTER_EDIT) }
            emitEvent(TeamCardUiEvent.ShowMessage("🔓 Мастер-режим активирован"))
            return true
        }
        emitEvent(TeamCardUiEvent.ShowMessage("❌ Неверный мастер-пароль"))
        return false
    }

    fun cancelEdit() {
        viewModelScope.launch {
            if (_uiState.value.mode == TeamCardMode.MASTER_EDIT && masterEditCache != null) {
                _uiState.update {
                    it.copy(
                        mode = TeamCardMode.VIEW,
                        routeCardEntries = masterEditCache ?: emptyList()
                    )
                }
                updateRouteCardStats()
                masterEditCache = null
            } else {
                saveAllCurrentEntriesToDb()
                _uiState.update { it.copy(mode = TeamCardMode.VIEW) }
            }
            loadRouteCard()
            loadSignatureStatus()
        }
    }

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ ЗАПИСЕЙ ПУТЕВОГО ЛИСТА
    // ============================================================

    private fun initializeRouteCardEntriesIfNeeded() {
        viewModelScope.launch {
            val existingEntries = teamRouteCardRepo.getRouteCardByTeamFirst(teamId)
            if (existingEntries.isEmpty()) {
                val masterList = masterRouteCardRepo.getRouteCardByCompetitionFirst(competitionId)
                for (master in masterList) {
                    val newEntry = TeamRouteCardEntity(
                        teamId = teamId,
                        checkpointId = master.id,
                        taken = false,
                        takenWithError = false,
                        offsetTime = null,
                        penalty = 0,
                        judgeConfirmed = false,
                        secretaryConfirmed = false
                    )
                    teamRouteCardRepo.saveEntry(newEntry)
                }
                Timber.d("Initialized ${masterList.size} empty route card entries for team $teamId")
                loadRouteCard()
                loadSignatureStatus()
            }
        }
    }

    // ============================================================
    // РЕДАКТИРОВАНИЕ ПУТЕВОГО ЛИСТА
    // ============================================================

    fun updateCheckpointTaken(checkpointId: Long, taken: Boolean) {
        val currentState = _uiState.value
        val entries = currentState.routeCardEntries.toMutableList()
        val index = entries.indexOfFirst { it.checkpointId == checkpointId }
        if (index == -1) return

        val old = entries[index]
        entries[index] = old.copy(
            taken = taken,
            takenWithError = if (!taken) false else old.takenWithError
        )

        _uiState.update { it.copy(routeCardEntries = entries) }
        updateRouteCardStats()

        if (currentState.mode == TeamCardMode.EDIT) {
            saveEntryToDb(entries[index])
        }
    }

    fun updateCheckpointError(checkpointId: Long, withError: Boolean) {
        val currentState = _uiState.value
        val entries = currentState.routeCardEntries.toMutableList()
        val index = entries.indexOfFirst { it.checkpointId == checkpointId }
        if (index == -1) return

        val old = entries[index]
        if (!old.taken) return

        entries[index] = old.copy(takenWithError = withError)
        _uiState.update { it.copy(routeCardEntries = entries) }
        updateRouteCardStats()

        if (currentState.mode == TeamCardMode.EDIT) {
            saveEntryToDb(entries[index])
        }
    }

    fun updateCheckpointOffsetTime(checkpointId: Long, offsetTime: String) {
        // Валидация формата MM:CC (но диалог уже не пропустит неверные данные)
        // Всё равно проверим на всякий случай
        if (offsetTime.isNotBlank() && !isValidTimeFormat(offsetTime)) {
            // Молча игнорируем, диалог уже показал ошибку
            return
        }

        val currentState = _uiState.value
        val entries = currentState.routeCardEntries.toMutableList()
        val index = entries.indexOfFirst { it.checkpointId == checkpointId }
        if (index == -1) return

        entries[index] = entries[index].copy(offsetTime = offsetTime)
        _uiState.update { it.copy(routeCardEntries = entries) }
        updateRouteCardStats()

        if (currentState.mode == TeamCardMode.EDIT) {
            saveEntryToDb(entries[index])
        }
    }

    fun updateCheckpointPenalty(checkpointId: Long, penalty: Int) {
        val currentState = _uiState.value
        val entries = currentState.routeCardEntries.toMutableList()
        val index = entries.indexOfFirst { it.checkpointId == checkpointId }
        if (index == -1) return

        entries[index] = entries[index].copy(penalty = penalty)
        _uiState.update { it.copy(routeCardEntries = entries) }
        updateRouteCardStats()

        if (currentState.mode == TeamCardMode.EDIT) {
            saveEntryToDb(entries[index])
        }
    }

    private fun saveEntryToDb(entry: RouteCardEntryUi) {
        viewModelScope.launch {
            val offsetSeconds = parseMmSsToSeconds(entry.offsetTime)
            val teamRouteCard = TeamRouteCardEntity(
                teamId = teamId,
                checkpointId = entry.checkpointId,
                taken = entry.taken,
                takenWithError = entry.takenWithError,
                offsetTime = if (offsetSeconds != null) offsetSeconds.toLong() else null,
                penalty = entry.penalty,
                judgeConfirmed = entry.judgeConfirmed,
                secretaryConfirmed = entry.secretaryConfirmed
            )
            teamRouteCardRepo.saveEntry(teamRouteCard)
        }
    }

    // ============================================================
    // ПОДПИСИ — С СОХРАНЕНИЕМ ДАННЫХ ПЕРЕД ПОДПИСАНИЕМ
    // ============================================================

    fun signAsSecretary() {
        viewModelScope.launch {
            if (_uiState.value.isSecretarySigned) {
                emitEvent(TeamCardUiEvent.ShowMessage("Уже подписано секретарём"))
                return@launch
            }

            // 1. Сохраняем все текущие изменения в БД
            saveAllCurrentEntriesToDb()

            // 2. Проставляем подписи секретаря
            val currentEntries = _uiState.value.routeCardEntries
            for (entry in currentEntries) {
                teamRouteCardRepo.confirmBySecretary(teamId, entry.checkpointId)
            }

            // 3. Обновляем состояние
            _uiState.update { it.copy(isSecretarySigned = true) }
            emitEvent(TeamCardUiEvent.ShowMessage("✅ Подписано секретарём"))

            // 4. Проверяем возможность закрытия ПЛ и обновляем статус checkpointsEntered
            checkAndLockRouteCard()

            // 5. Перезагружаем все данные
            loadRouteCard()
            updateRouteCardStats()
            loadTeamAndMembers()  // Важно! Обновляем checkpointsEntered в teamInfo
            loadSignatureStatus()
        }
    }

    fun signAsJudge() {
        viewModelScope.launch {
            if (_uiState.value.isJudgeSigned) {
                emitEvent(TeamCardUiEvent.ShowMessage("Уже подписано судьёй"))
                return@launch
            }

            // 1. Сохраняем все текущие изменения в БД
            saveAllCurrentEntriesToDb()

            // 2. Проставляем подписи судьи
            val currentEntries = _uiState.value.routeCardEntries
            val timestamp = System.currentTimeMillis()
            for (entry in currentEntries) {
                teamRouteCardRepo.confirmByJudge(teamId, entry.checkpointId, timestamp)
            }

            // 3. Обновляем состояние
            _uiState.update { it.copy(isJudgeSigned = true) }
            emitEvent(TeamCardUiEvent.ShowMessage("✅ Подписано судьёй"))

            // 4. Проверяем возможность закрытия ПЛ и обновляем статус checkpointsEntered
            checkAndLockRouteCard()

            // 5. Перезагружаем все данные
            loadRouteCard()
            updateRouteCardStats()
            loadTeamAndMembers()  // Важно! Обновляем checkpointsEntered в teamInfo
            loadSignatureStatus()
        }
    }

    /**
     * Сохраняет все текущие записи путевого листа в БД
     */
    private suspend fun saveAllCurrentEntriesToDb() {
        for (entry in _uiState.value.routeCardEntries) {
            val offsetSeconds = parseMmSsToSeconds(entry.offsetTime)
            val existing = teamRouteCardRepo.getEntry(teamId, entry.checkpointId)

            if (existing != null) {
                val updated = existing.copy(
                    taken = entry.taken,
                    takenWithError = entry.takenWithError,
                    offsetTime = if (offsetSeconds != null) offsetSeconds.toLong() else null,
                    penalty = entry.penalty,
                    secretaryConfirmed = entry.secretaryConfirmed,
                    judgeConfirmed = entry.judgeConfirmed
                )
                teamRouteCardRepo.saveEntry(updated)
            } else {
                val newEntry = TeamRouteCardEntity(
                    teamId = teamId,
                    checkpointId = entry.checkpointId,
                    taken = entry.taken,
                    takenWithError = entry.takenWithError,
                    offsetTime = if (offsetSeconds != null) offsetSeconds.toLong() else null,
                    penalty = entry.penalty,
                    secretaryConfirmed = entry.secretaryConfirmed,
                    judgeConfirmed = entry.judgeConfirmed
                )
                teamRouteCardRepo.saveEntry(newEntry)
            }
        }
        Timber.d("Saved all ${_uiState.value.routeCardEntries.size} entries to DB before signing")
    }

    private suspend fun checkAndLockRouteCard() {
        val entries = teamRouteCardRepo.getRouteCardByTeamFirst(teamId)
        val allConfirmed = entries.isNotEmpty() && entries.all { it.secretaryConfirmed && it.judgeConfirmed }

        Timber.d("checkAndLockRouteCard: entries.size=${entries.size}, allConfirmed=$allConfirmed")

        if (allConfirmed) {
            val team = teamRepo.getTeamById(teamId)
            if (team != null && !team.checkpointsEntered) {
                teamRepo.updateTeam(team.copy(checkpointsEntered = true))
                Timber.d("Team ${team.teamNumber} checkpointsEntered set to true")

                // Обновляем состояние UI
                _uiState.update { state ->
                    state.copy(
                        mode = TeamCardMode.VIEW,
                        isSecretarySigned = true,
                        isJudgeSigned = true,
                        teamInfo = state.teamInfo?.copy(checkpointsEntered = true)
                    )
                }
                emitEvent(TeamCardUiEvent.RouteCardLocked)
            }
        }
    }

    // ============================================================
    // МАСТЕР-СОХРАНЕНИЕ
    // ============================================================

    fun saveMasterChanges() {
        viewModelScope.launch {
            val currentSignatures = teamRouteCardRepo.getRouteCardByTeamFirst(teamId)
                .associate { it.checkpointId to Pair(it.secretaryConfirmed, it.judgeConfirmed) }

            for (entry in _uiState.value.routeCardEntries) {
                try {
                    val signatures = currentSignatures[entry.checkpointId] ?: Pair(false, false)
                    val offsetSeconds = parseMmSsToSeconds(entry.offsetTime)

                    val existing = teamRouteCardRepo.getEntry(teamId, entry.checkpointId)

                    if (existing != null) {
                        val updated = existing.copy(
                            taken = entry.taken,
                            takenWithError = entry.takenWithError,
                            offsetTime = if (offsetSeconds != null) offsetSeconds.toLong() else null,
                            penalty = entry.penalty,
                            secretaryConfirmed = signatures.first,
                            judgeConfirmed = signatures.second
                        )
                        teamRouteCardRepo.saveEntry(updated)
                    } else {
                        val newEntry = TeamRouteCardEntity(
                            teamId = teamId,
                            checkpointId = entry.checkpointId,
                            taken = entry.taken,
                            takenWithError = entry.takenWithError,
                            offsetTime = if (offsetSeconds != null) offsetSeconds.toLong() else null,
                            penalty = entry.penalty,
                            secretaryConfirmed = signatures.first,
                            judgeConfirmed = signatures.second
                        )
                        teamRouteCardRepo.saveEntry(newEntry)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save CP${entry.displayNumber}")
                }
            }

            val team = teamRepo.getTeamById(teamId)
            if (team != null && !team.checkpointsEntered) {
                teamRepo.updateTeam(team.copy(checkpointsEntered = true))
            }

            updateRouteCardStats()
            _uiState.update { state ->
                state.copy(
                    mode = TeamCardMode.VIEW,
                    isSecretarySigned = true,
                    isJudgeSigned = true,
                    teamInfo = state.teamInfo?.copy(checkpointsEntered = true)
                )
            }

            masterEditCache = null
            emitEvent(TeamCardUiEvent.ShowMessage("✅ Изменения сохранены"))
            checkAndLockRouteCard()
            loadRouteCard()
            loadTeamAndMembers()
        }
    }

    // ============================================================
    // КОРРЕКТИРОВКА ВРЕМЕНИ ФИНИША
    // ============================================================

    fun adjustFinishTime(relativeSeconds: Int) {
        viewModelScope.launch {
            val team = _uiState.value.teamInfo ?: return@launch
            if (competitionStartTimestamp == 0L) {
                emitEvent(TeamCardUiEvent.ShowMessage("Соревнование не запущено"))
                return@launch
            }

            val absoluteTimestamp = competitionStartTimestamp + (relativeSeconds * 1000L)
            Timber.d("adjustFinishTime: relativeSeconds=$relativeSeconds, absoluteTimestamp=$absoluteTimestamp")

            teamRepo.setFinishTimestamp(team.id, absoluteTimestamp)
            teamRepo.updateTeamStatus(team.id, "finished")

            emitEvent(TeamCardUiEvent.ShowMessage("Время финиша обновлено"))
            loadData(teamId)
        }
    }

    fun onFinishTimeLongClick(password: String): Boolean {
        return if (password == "1234" || password == "devdebug") {
            showFinishTimeDialog()
            true
        } else {
            emitEvent(TeamCardUiEvent.ShowMessage("❌ Неверный пароль"))
            false
        }
    }

    fun showFinishTimeDialog() {
        viewModelScope.launch {
            val finishTimestamp = _uiState.value.teamInfo?.finishTimestamp
            val currentSeconds = if (finishTimestamp != null && competitionStartTimestamp > 0) {
                ((finishTimestamp - competitionStartTimestamp) / 1000).toInt()
            } else {
                0
            }
            _event.emit(TeamCardUiEvent.ShowFinishTimeDialog(currentSeconds))
        }
    }

    // ============================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    private fun emitEvent(event: TeamCardUiEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    fun getRelativeTimes(): RelativeTimes {
        val team = _uiState.value.teamInfo ?: return RelativeTimes("—:—:—", "—:—:—")

        if (competitionStartTimestamp == 0L) {
            return RelativeTimes("—:—:—", "—:—:—")
        }

        val startSec = team.startTimestamp?.let {
            val diff = (it - competitionStartTimestamp) / 1000
            if (diff < 0) 0 else diff.toInt()
        }

        val finishSec = team.finishTimestamp?.let {
            val diff = (it - competitionStartTimestamp) / 1000
            if (diff < 0) 0 else diff.toInt()
        }

        return RelativeTimes(
            startTime = if (startSec != null) formatRelativeTime(startSec) else "—:—:—",
            finishTime = if (finishSec != null) formatRelativeTime(finishSec) else "—:—:—"
        )
    }

    private fun formatRelativeTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatSecondsToMmSs(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Валидация формата MM:CC
     * Минуты: 00-59, Секунды: 00-59
     */
    private fun isValidTimeFormat(input: String): Boolean {
        if (input.isBlank()) return true  // Пустая строка допустима (нет отсечки)
        return TIME_MM_SS_PATTERN.matcher(input).matches()
    }

    private fun parseMmSsToSeconds(input: String): Int? {
        if (input.isBlank()) return null
        val matcher = TIME_MM_SS_PATTERN.matcher(input)
        if (!matcher.matches()) return null
        val minutes = matcher.group(1).toInt()
        val seconds = matcher.group(2).toInt()
        return minutes * 60 + seconds
    }

    // ============================================================
    // УПРАВЛЕНИЕ УЧАСТНИКАМИ
    // ============================================================

    fun replaceMember(participantId: Long, newPersonId: Long) {
        viewModelScope.launch {
            try {
                val oldParticipant = participantRepo.getParticipantById(participantId) ?: return@launch
                val team = teamRepo.getTeamById(oldParticipant.teamId) ?: return@launch

                val existingInComp = participantRepo.findActiveByPersonAndComp(newPersonId, team.competitionId)
                if (existingInComp != null) {
                    emitEvent(TeamCardUiEvent.ShowMessage("Персона уже в другой команде"))
                    return@launch
                }

                participantRepo.updateParticipantStatus(oldParticipant.id, "replaced")

                participantRepo.addParticipant(
                    teamId = team.id,
                    personId = newPersonId,
                    role = oldParticipant.role,
                    mentorId = oldParticipant.mentorId
                )

                emitEvent(TeamCardUiEvent.ShowMessage("Участник заменён"))
                loadData(teamId)
            } catch (e: Exception) {
                emitEvent(TeamCardUiEvent.ShowMessage("Ошибка: ${e.message}"))
            }
        }
    }

    fun removeMember(participantId: Long) {
        viewModelScope.launch {
            try {
                val participant = participantRepo.getParticipantById(participantId) ?: return@launch
                val team = teamRepo.getTeamById(participant.teamId) ?: return@launch

                val activeMembers = participantRepo.getActiveParticipantsByTeam(team.id).first()
                if (activeMembers.size <= 1) {
                    emitEvent(TeamCardUiEvent.ShowMessage("Нельзя удалить последнего участника"))
                    return@launch
                }

                participantRepo.updateParticipantStatus(participant.id, "free_agent")
                emitEvent(TeamCardUiEvent.ShowMessage("Участник исключён"))
                loadData(team.id)
            } catch (e: Exception) {
                emitEvent(TeamCardUiEvent.ShowMessage("Ошибка: ${e.message}"))
            }
        }
    }

    fun disbandTeam(reason: String) {
        viewModelScope.launch {
            val team = _uiState.value.teamInfo ?: return@launch

            when (team.status) {
                "finished", "lost", "disqualified" -> {
                    emitEvent(TeamCardUiEvent.ShowMessage("Нельзя расформировать ${team.status} команду"))
                    return@launch
                }
                "started" -> {
                    emitEvent(TeamCardUiEvent.ShowMasterPasswordDialog {
                        viewModelScope.launch {
                            performDisband(reason)
                        }
                    })
                    return@launch
                }
            }

            performDisband(reason)
        }
    }

    private suspend fun performDisband(reason: String) {
        val team = _uiState.value.teamInfo ?: return

        val activeParticipants = participantRepo.getActiveParticipantsByTeam(team.id).first()
        for (participant in activeParticipants) {
            participantRepo.updateParticipantStatus(participant.id, "free_agent")
        }

        teamRepo.updateTeamStatus(team.id, "disqualified")

        val reasonText = if (reason.isNotBlank()) " Причина: $reason" else ""
        emitEvent(TeamCardUiEvent.ShowMessage("Команда №${team.number} расформирована.$reasonText"))
        loadData(team.id)
    }

    fun createQuickPerson(fullName: String, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val parts = fullName.split(" ")
            val lastName = parts.getOrNull(0)?.normalizeName() ?: ""
            val firstName = parts.getOrNull(1)?.normalizeName() ?: ""

            val personId = personRepo.createPerson(
                lastName = lastName,
                firstName = firstName
            )

            if (personId != -1L) {
                onResult(personId)
            } else {
                emitEvent(TeamCardUiEvent.ShowMessage("Ошибка создания персоны"))
            }
        }
    }

    fun cycleCheckpointState(checkpointId: Long) {
        val currentState = _uiState.value

        // В мастер-режиме тоже должны работать клики
        val isEditable = currentState.mode == TeamCardMode.EDIT || currentState.mode == TeamCardMode.MASTER_EDIT

        if (!isEditable) {
            Timber.d("cycleCheckpointState: not editable, mode=${currentState.mode}")
            return
        }

        val entries = currentState.routeCardEntries.toMutableList()
        val index = entries.indexOfFirst { it.checkpointId == checkpointId }
        if (index == -1) return

        val current = entries[index]

        val newEntry = when {
            !current.taken -> current.copy(taken = true, takenWithError = false)
            current.taken && !current.takenWithError -> current.copy(takenWithError = true)
            else -> current.copy(taken = false, takenWithError = false)
        }

        entries[index] = newEntry
        _uiState.update { it.copy(routeCardEntries = entries) }
        updateRouteCardStats()

        viewModelScope.launch {
            saveEntryToDb(newEntry)
        }
    }

    fun assignMentor(participantId: Long, mentorPersonId: Long) {
        viewModelScope.launch {
            try {
                val participant = participantRepo.getParticipantById(participantId) ?: return@launch
                val mentorPerson = personRepo.getPersonById(mentorPersonId) ?: return@launch

                val mentorAge = AgeCalculator.calculateAge(mentorPerson.birthDate)
                if (mentorAge == null || mentorAge < 18) {
                    emitEvent(TeamCardUiEvent.ShowMessage("Ментор должен быть старше 18 лет"))
                    return@launch
                }

                var mentorEntity = mentorRepo.getMentorByPersonId(mentorPersonId)
                if (mentorEntity == null) {
                    val newId = mentorRepo.createMentor(mentorPersonId)
                    if (newId == -1L) {
                        emitEvent(TeamCardUiEvent.ShowMessage("Ошибка создания ментора"))
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

                emitEvent(TeamCardUiEvent.ShowMessage("Ментор назначен"))
                loadData(participant.teamId)
            } catch (e: Exception) {
                emitEvent(TeamCardUiEvent.ShowMessage("Ошибка: ${e.message}"))
            }
        }
    }
}