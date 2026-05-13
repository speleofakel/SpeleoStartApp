package com.speleo.start.presentation.screen.home

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speleo.start.data.TestDataGenerator
import com.speleo.start.data.local.PreferencesManager
import com.speleo.start.data.local.dao.AppSettingsDao
import com.speleo.start.data.local.dao.CompetitionDao
import com.speleo.start.data.local.dao.ParticipantDao
import com.speleo.start.data.local.dao.PersonDao
import com.speleo.start.data.local.dao.TeamDao
import com.speleo.start.data.local.dao.TeamRouteCardDao
import com.speleo.start.data.local.entity.CompetitionEntity
import com.speleo.start.data.local.entity.ParticipantEntity
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.data.local.entity.TeamEntity
import com.speleo.start.data.local.entity.TeamRouteCardEntity
import com.speleo.start.data.repository.CompetitionRepository
import com.speleo.start.data.repository.ParticipantRepository
import com.speleo.start.data.repository.PersonRepository
import com.speleo.start.data.repository.TeamRepository
import com.speleo.start.data.repository.TeamRouteCardRepository
import com.speleo.start.presentation.TimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

data class PendingTeamInfo(
    val id: Long,
    val number: Int,
    val className: String,
    val membersShort: String
)

sealed class HomeUiEvent {
    data class ShowMessage(val message: String) : HomeUiEvent()
    data class ShowConfirmDialog(val title: String, val message: String, val onConfirm: () -> Unit) : HomeUiEvent()
}

@HiltViewModel
class HomeVM @Inject constructor(
    private val generator: TestDataGenerator,
    private val prefs: PreferencesManager,
    private val competitionRepo: CompetitionRepository,
    private val teamRepo: TeamRepository,
    private val participantRepo: ParticipantRepository,
    private val personRepo: PersonRepository,
    private val competitionDao: CompetitionDao,
    private val teamDao: TeamDao,
    private val participantDao: ParticipantDao,
    private val personDao: PersonDao,
    private val teamRouteCardRepo: TeamRouteCardRepository,
    private val teamRouteCardDao: TeamRouteCardDao,
    private val appSettingsDao: AppSettingsDao,
    val timerManager: TimerManager,
    @ApplicationContext private val context: Context
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

    private val _pendingRouteCards = MutableStateFlow(Pair(0, 0))
    val pendingRouteCards: StateFlow<Pair<Int, Int>> = _pendingRouteCards.asStateFlow()

    private val _pendingTeams = MutableStateFlow<List<PendingTeamInfo>>(emptyList())
    val pendingTeams: StateFlow<List<PendingTeamInfo>> = _pendingTeams.asStateFlow()

    private val _event = MutableSharedFlow<HomeUiEvent>()
    val event: SharedFlow<HomeUiEvent> = _event.asSharedFlow()

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
            try {
                val cid = prefs.activeCompetitionId
                if (cid == -1L) {
                    _event.emit(HomeUiEvent.ShowMessage("Нет активного соревнования"))
                    return@launch
                }

                val competition = competitionRepo.getCompetitionById(cid) ?: return@launch
                val teams = teamRepo.getTeamsByCompetition(cid).first()
                val allPersons = personRepo.getAllPersons().first()

                val exportData = JSONObject().apply {
                    put("version", 2)
                    put("exportDate", System.currentTimeMillis())
                    put("competition", JSONObject().apply {
                        put("id", competition.id)
                        put("name", competition.name)
                        put("shortName", competition.shortName)
                        put("date", competition.date)
                        put("place", competition.place)
                        put("discipline", competition.discipline)
                        put("system", competition.system ?: "")
                        put("settingsJson", competition.settingsJson)
                        put("isArchived", competition.isArchived)
                    })
                    put("teams", JSONArray().apply {
                        teams.forEach { team ->
                            put(JSONObject().apply {
                                put("id", team.id)
                                put("teamNumber", team.teamNumber)
                                put("className", team.className)
                                put("status", team.status)
                                put("colorMark", team.colorMark)
                                put("startTimestamp", team.startTimestamp ?: 0)
                                put("finishTimestamp", team.finishTimestamp ?: 0)
                                put("skipCount", team.skipCount)
                                put("checkpointsEntered", team.checkpointsEntered)
                                put("finalPlace", team.finalPlace ?: 0)
                            })
                        }
                    })
                    put("persons", JSONArray().apply {
                        allPersons.forEach { person ->
                            put(JSONObject().apply {
                                put("id", person.id)
                                put("lastName", person.lastName)
                                put("firstName", person.firstName)
                                put("middleName", person.middleName ?: "")
                                put("nickname", person.nickname ?: "")
                                put("birthDate", person.birthDate ?: "")
                                put("phone", person.phone ?: "")
                                put("email", person.email ?: "")
                                put("gender", person.gender ?: "")
                                put("note", person.note ?: "")
                                put("blacklisted", person.blacklisted)
                            })
                        }
                    })
                    put("participants", JSONArray().apply {
                        teams.forEach { team ->
                            val participants = participantRepo.getParticipantsByTeam(team.id).first()
                            participants.forEach { p ->
                                put(JSONObject().apply {
                                    put("teamId", p.teamId)
                                    put("personId", p.personId)
                                    put("role", p.role)
                                    put("status", p.status)
                                    put("mentorId", p.mentorId ?: 0)
                                    put("mentorConfirmed", p.mentorConfirmed)
                                    put("judgeApproved", p.judgeApproved)
                                })
                            }
                        }
                    })
                    put("routeCards", JSONArray().apply {
                        teams.forEach { team ->
                            val routeCards = teamRouteCardRepo.getRouteCardByTeamFirst(team.id)
                            routeCards.forEach { rc ->
                                put(JSONObject().apply {
                                    put("teamId", rc.teamId)
                                    put("checkpointId", rc.checkpointId)
                                    put("taken", rc.taken)
                                    put("takenWithError", rc.takenWithError)
                                    put("offsetTime", rc.offsetTime ?: 0)
                                    put("penalty", rc.penalty)
                                    put("judgeConfirmed", rc.judgeConfirmed)
                                    put("secretaryConfirmed", rc.secretaryConfirmed)
                                })
                            }
                        }
                    })
                }

                val jsonString = exportData.toString(2)
                val fileName = "speleo_export_${competition.shortName}_${System.currentTimeMillis()}.json"

                // Сохраняем в Documents
                val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs()
                }
                val file = File(documentsDir, fileName)
                file.writeText(jsonString)

                _event.emit(HomeUiEvent.ShowMessage("Экспорт сохранён: ${file.absolutePath}"))
            } catch (e: Exception) {
                _event.emit(HomeUiEvent.ShowMessage("Ошибка экспорта: ${e.message}"))
            }
        }
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            try {
                val json = JSONObject(jsonString)
                val version = json.optInt("version", 1)

                // Очищаем текущие данные
                generator.clearAll()

                // Восстанавливаем соревнование
                val compJson = json.getJSONObject("competition")
                val competition = CompetitionEntity(
                    name = compJson.getString("name"),
                    shortName = compJson.optString("shortName", ""),
                    date = compJson.getString("date"),
                    place = compJson.getString("place"),
                    discipline = compJson.optString("discipline", "underground"),
                    system = compJson.optString("system", "").ifBlank { null },
                    settingsJson = compJson.optString("settingsJson", "{}"),
                    isArchived = compJson.optBoolean("isArchived", false)
                )
                val competitionId = competitionDao.insert(competition)

                // Восстанавливаем персон
                val personsArray = json.getJSONArray("persons")
                val personIdMap = mutableMapOf<Long, Long>()
                for (i in 0 until personsArray.length()) {
                    val p = personsArray.getJSONObject(i)
                    val newId = personDao.insert(
                        PersonEntity(
                            lastName = p.getString("lastName"),
                            firstName = p.getString("firstName"),
                            middleName = p.optString("middleName", "").ifBlank { null },
                            nickname = p.optString("nickname", "").ifBlank { null },
                            birthDate = p.optString("birthDate", "").ifBlank { null },
                            phone = p.optString("phone", "").ifBlank { null },
                            email = p.optString("email", "").ifBlank { null },
                            gender = p.optString("gender", "").ifBlank { null },
                            note = p.optString("note", "").ifBlank { null },
                            blacklisted = p.optBoolean("blacklisted", false)
                        )
                    )
                    personIdMap[p.getLong("id")] = newId
                }

                // Восстанавливаем команды
                val teamsArray = json.getJSONArray("teams")
                val teamIdMap = mutableMapOf<Long, Long>()
                for (i in 0 until teamsArray.length()) {
                    val t = teamsArray.getJSONObject(i)
                    val startTs = t.optLong("startTimestamp", 0)
                    val finishTs = t.optLong("finishTimestamp", 0)
                    val newId = teamDao.insert(
                        TeamEntity(
                            competitionId = competitionId,
                            teamNumber = t.getInt("teamNumber"),
                            className = t.getString("className"),
                            status = t.getString("status"),
                            colorMark = t.optString("colorMark", "green"),
                            startTimestamp = if (startTs > 0) startTs else null,
                            finishTimestamp = if (finishTs > 0) finishTs else null,
                            skipCount = t.optInt("skipCount", 0),
                            checkpointsEntered = t.getBoolean("checkpointsEntered"),
                            finalPlace = t.optInt("finalPlace", 0).takeIf { it > 0 }
                        )
                    )
                    teamIdMap[t.getLong("id")] = newId
                }

                // Восстанавливаем участников
                val participantsArray = json.optJSONArray("participants")
                if (participantsArray != null) {
                    for (i in 0 until participantsArray.length()) {
                        val p = participantsArray.getJSONObject(i)
                        val newTeamId = teamIdMap[p.getLong("teamId")] ?: continue
                        val newPersonId = personIdMap[p.getLong("personId")] ?: continue
                        participantDao.insert(
                            ParticipantEntity(
                                teamId = newTeamId,
                                personId = newPersonId,
                                role = p.getString("role"),
                                status = p.optString("status", "active"),
                                mentorId = p.optLong("mentorId", 0).takeIf { it > 0 },
                                mentorConfirmed = p.getBoolean("mentorConfirmed"),
                                judgeApproved = p.getBoolean("judgeApproved")
                            )
                        )
                    }
                }

                // Восстанавливаем путевые листы
                val routeCardsArray = json.optJSONArray("routeCards")
                if (routeCardsArray != null) {
                    for (i in 0 until routeCardsArray.length()) {
                        val rc = routeCardsArray.getJSONObject(i)
                        val newTeamId = teamIdMap[rc.getLong("teamId")] ?: continue
                        teamRouteCardDao.insert(
                            TeamRouteCardEntity(
                                teamId = newTeamId,
                                checkpointId = rc.getLong("checkpointId"),
                                taken = rc.getBoolean("taken"),
                                takenWithError = rc.getBoolean("takenWithError"),
                                offsetTime = rc.optLong("offsetTime", 0).takeIf { it > 0 },
                                penalty = rc.getInt("penalty"),
                                judgeConfirmed = rc.getBoolean("judgeConfirmed"),
                                secretaryConfirmed = rc.getBoolean("secretaryConfirmed")
                            )
                        )
                    }
                }

                prefs.activeCompetitionId = competitionId
                _event.emit(HomeUiEvent.ShowMessage("Импорт завершён"))
                loadActiveCompetition(competitionId)
            } catch (e: Exception) {
                _event.emit(HomeUiEvent.ShowMessage("Ошибка импорта: ${e.message}"))
            }
        }
    }
}