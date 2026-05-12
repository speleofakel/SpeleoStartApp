package com.speleo.start.presentation.screen.teamcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.presentation.component.PersonSearchDialog

enum class CheckpointEditMode {
    VIEW,
    QUICK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCardScreen(
    teamId: Long,
    onBack: () -> Unit,
    vm: TeamCardVM = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showReplaceDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showMentorDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showDisbandDialog by remember { mutableStateOf(false) }
    var disbandReason by remember { mutableStateOf("") }
    var showMasterPasswordDialog by remember { mutableStateOf(false) }
    var masterPassword by remember { mutableStateOf("") }
    var showFinishTimeDialog by remember { mutableStateOf(false) }
    var finishTimeHours by remember { mutableStateOf("") }
    var finishTimeMinutes by remember { mutableStateOf("") }
    var finishTimeSeconds by remember { mutableStateOf("") }
    var replacedHistoryExpanded by remember { mutableStateOf(false) }
    var showFinishPasswordDialog by remember { mutableStateOf(false) }
    var finishPassword by remember { mutableStateOf("") }

    var checkpointEditMode by remember { mutableStateOf(CheckpointEditMode.VIEW) }
    var techDialogCheckpointId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(teamId) {
        vm.loadData(teamId)
    }

    // НОВЫЙ LaunchedEffect для авто-входа в режим редактирования
    LaunchedEffect(uiState.teamInfo, uiState.mode) {
        val team = uiState.teamInfo
        val isMasterMode = uiState.mode == TeamCardMode.MASTER_EDIT
        val isEditMode = uiState.mode == TeamCardMode.EDIT

        if (team != null &&
            team.status == "finished" &&
            !team.checkpointsEntered &&
            uiState.mode == TeamCardMode.VIEW &&
            !isMasterMode &&
            !isEditMode) {
            // Автоматически входим в режим быстрого редактирования
            vm.enterQuickEditMode()
            checkpointEditMode = CheckpointEditMode.QUICK
        }
    }

    LaunchedEffect(uiState.teamInfo?.checkpointsEntered, uiState.isJudgeSigned) {
        if (uiState.teamInfo?.checkpointsEntered == true && checkpointEditMode == CheckpointEditMode.QUICK) {
            checkpointEditMode = CheckpointEditMode.VIEW
        }
    }

    LaunchedEffect(Unit) {
        vm.event.collect { event ->
            when (event) {
                is TeamCardUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TeamCardUiEvent.ShowMasterPasswordDialog -> {
                    showMasterPasswordDialog = true
                }
                is TeamCardUiEvent.ShowFinishTimeDialog -> {
                    val seconds = event.currentSeconds
                    finishTimeHours = (seconds / 3600).toString()
                    finishTimeMinutes = ((seconds % 3600) / 60).toString()
                    finishTimeSeconds = (seconds % 60).toString()
                    showFinishTimeDialog = true
                }
                is TeamCardUiEvent.RouteCardLocked -> {
                    checkpointEditMode = CheckpointEditMode.VIEW
                    snackbarHostState.showSnackbar(
                        "🔒 Путевой лист закрыт. Если позже заметите разногласия — исправить можно только через дебаг."
                    )
                }
                TeamCardUiEvent.NavigateBack -> {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Карточка команды") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Назад")
                    }
                },
                actions = {
                    if (uiState.mode == TeamCardMode.EDIT) {
                        TextButton(onClick = { vm.cancelEdit() }) {
                            Text("Отмена")
                        }
                    }
                    if (uiState.mode == TeamCardMode.MASTER_EDIT) {
                        TextButton(onClick = { vm.saveMasterChanges() }) {
                            Text("💾 Сохранить")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.teamInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Команда не найдена", fontSize = 18.sp)
            }
        } else {
            val teamInfo = uiState.teamInfo!!
            val canEdit = teamInfo.status == "registered" || teamInfo.status == "started"
            val isMasterMode = uiState.mode == TeamCardMode.MASTER_EDIT
            val isEditMode = uiState.mode == TeamCardMode.EDIT
            val isFinished = teamInfo.status == "finished"

            val canQuickEdit = isFinished && !isMasterMode && !teamInfo.checkpointsEntered

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { TeamHeaderCard(teamInfo = teamInfo) }

                item {
                    val relativeTimes = vm.getRelativeTimes()
                    TimesCard(
                        startTime = relativeTimes.startTime,
                        finishTime = relativeTimes.finishTime,
                        status = uiState.teamInfo?.status ?: "",
                        onFinishTimeLongClick = { showFinishPasswordDialog = true }
                    )
                }

                item {
                    RouteCardStatusCard(
                        stats = uiState.routeCardStats,
                        checkpointsEntered = uiState.teamInfo?.checkpointsEntered ?: false,
                        status = uiState.teamInfo?.status ?: "",
                        isMasterMode = isMasterMode,
                        isSecretarySigned = uiState.isSecretarySigned,
                        isJudgeSigned = uiState.isJudgeSigned,
                        onMasterUnlock = { showMasterPasswordDialog = true },
                        onSecretarySign = { vm.signAsSecretary() },
                        onJudgeSign = { vm.signAsJudge() },
                        mode = uiState.mode,
                        isQuickEditMode = checkpointEditMode == CheckpointEditMode.QUICK,
                        onToggleQuickEdit = {
                            if (canQuickEdit) {
                                if (checkpointEditMode == CheckpointEditMode.VIEW) {
                                    vm.enterQuickEditMode()
                                    checkpointEditMode = CheckpointEditMode.QUICK
                                } else {
                                    checkpointEditMode = CheckpointEditMode.VIEW
                                }
                            }
                        },
                        routeCardEntries = uiState.routeCardEntries,
                        onCycleCheckpoint = { checkpointId ->
                            if (checkpointEditMode == CheckpointEditMode.QUICK) {
                                vm.cycleCheckpointState(checkpointId)
                            }
                        },
                        onTechnicalLongPress = { checkpointId ->
                            if (checkpointEditMode == CheckpointEditMode.QUICK) {
                                techDialogCheckpointId = checkpointId
                            }
                        },
                        onShowTechDialog = { checkpointId ->
                            techDialogCheckpointId = checkpointId
                        }
                    )
                }

                item {
                    Text(
                        text = "👥 Состав (${uiState.members.size})",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.members) { member ->
                    MemberCard(
                        member = member,
                        isCaptain = member.role == "captain",
                        canEdit = canEdit && !isEditMode && !isMasterMode,
                        onReplace = {
                            showReplaceDialog = Pair(member.participantId, "${member.lastName} ${member.firstName}")
                        },
                        onRemove = { vm.removeMember(member.participantId) },
                        onToggleJudgeApproved = { vm.toggleJudgeApproved(member.participantId) },

                        onAssignMentor = {
                            showMentorDialog = Pair(member.participantId, "${member.lastName} ${member.firstName}")
                        }
                    )
                }

                if (uiState.replacedHistory.isNotEmpty()) {
                    item {
                        ReplacedHistorySection(
                            replacedMembers = uiState.replacedHistory,
                            isExpanded = replacedHistoryExpanded,
                            onToggle = { replacedHistoryExpanded = !replacedHistoryExpanded }
                        )
                    }
                }

                if (canEdit && !isEditMode && !isMasterMode) {
                    item {
                        Button(
                            onClick = { showDisbandDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("🚫 РАСФОРМИРОВАТЬ КОМАНДУ")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    if (showFinishPasswordDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showFinishPasswordDialog = false
                password = ""
            },
            title = { Text("Изменение времени финиша") },
            text = {
                Column {
                    Text("Введите пароль судьи для изменения времени финиша")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (vm.onFinishTimeLongClick(password)) {
                            showFinishPasswordDialog = false
                        }
                        password = ""
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFinishPasswordDialog = false
                    password = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (techDialogCheckpointId != null) {
        val entry = uiState.routeCardEntries.find { it.checkpointId == techDialogCheckpointId }
        if (entry != null && entry.type == "technical") {
            TechnicalCheckpointDialog(
                checkpoint = entry,
                onSave = { offsetTime, penalty ->
                    vm.updateCheckpointOffsetTime(techDialogCheckpointId!!, offsetTime)
                    vm.updateCheckpointPenalty(techDialogCheckpointId!!, penalty)
                    techDialogCheckpointId = null
                },
                onDismiss = { techDialogCheckpointId = null }
            )
        } else {
            techDialogCheckpointId = null
        }
    }

    if (showReplaceDialog != null) {
        val (participantId, participantName) = showReplaceDialog!!
        PersonSearchDialog(
            title = "Заменить: $participantName",
            onPersonSelected = { person ->
                vm.replaceMember(participantId, person.id)
                showReplaceDialog = null
            },
            onQuickCreate = { fullName ->
                vm.createQuickPerson(fullName) { newPersonId ->
                    vm.replaceMember(participantId, newPersonId)
                    showReplaceDialog = null
                }
            },
            onDismiss = { showReplaceDialog = null },
            filterForMentors = false
        )
    }

    if (showMentorDialog != null) {
        val (participantId, participantName) = showMentorDialog!!
        PersonSearchDialog(
            title = "Выбрать ментора для $participantName",
            onPersonSelected = { person ->
                vm.assignMentor(participantId, person.id)
                showMentorDialog = null
            },
            onQuickCreate = { fullName ->
                vm.createQuickPerson(fullName) { newPersonId ->
                    vm.assignMentor(participantId, newPersonId)
                    showMentorDialog = null
                }
            },
            onDismiss = { showMentorDialog = null },
            filterForMentors = true
        )
    }

    if (showDisbandDialog) {
        AlertDialog(
            onDismissRequest = { showDisbandDialog = false },
            title = { Text("Расформировать команду") },
            text = {
                Column {
                    Text("Команда №${uiState.teamInfo?.number} будет расформирована.")
                    Text("Участники станут свободными агентами.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = disbandReason,
                        onValueChange = { disbandReason = it },
                        label = { Text("Причина (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (uiState.teamInfo?.status == "started") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Для расформирования команды на дистанции требуется пароль мастера",
                            fontSize = 11.sp,
                            color = androidx.compose.ui.graphics.Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.disbandTeam(disbandReason)
                        showDisbandDialog = false
                        disbandReason = ""
                    }
                ) {
                    Text("РАСФОРМИРОВАТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDisbandDialog = false
                    disbandReason = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showMasterPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showMasterPasswordDialog = false
                masterPassword = ""
            },
            title = { Text("Мастер-правка") },
            text = {
                Column {
                    Text("Введите мастер-пароль для разблокировки путевого листа.")
                    Text("Статусы подписей будут сохранены.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = masterPassword,
                        onValueChange = { masterPassword = it },
                        label = { Text("Мастер-пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vm.enterMasterEditMode(masterPassword)) {
                            showMasterPasswordDialog = false
                        }
                        masterPassword = ""
                    }
                ) {
                    Text("РАЗБЛОКИРОВАТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMasterPasswordDialog = false
                    masterPassword = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showFinishTimeDialog) {
        SideEffect {
            android.util.Log.d("TeamCardScreen", "Showing finish time dialog")
        }
        AlertDialog(
            onDismissRequest = {
                showFinishTimeDialog = false
                finishTimeHours = ""
                finishTimeMinutes = ""
                finishTimeSeconds = ""
            },
            title = { Text("Корректировка времени финиша") },
            text = {
                Column {
                    Text("Введите относительное время от старта соревнований в формате ЧЧ:ММ:СС", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = finishTimeHours,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    finishTimeHours = it
                                }
                            },
                            label = { Text("Часы") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = finishTimeMinutes,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    finishTimeMinutes = it
                                }
                            },
                            label = { Text("Минуты") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = finishTimeSeconds,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                    finishTimeSeconds = it
                                }
                            },
                            label = { Text("Секунды") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = finishTimeHours.toIntOrNull() ?: 0
                        val minutes = finishTimeMinutes.toIntOrNull() ?: 0
                        val seconds = finishTimeSeconds.toIntOrNull() ?: 0
                        val totalSeconds = hours * 3600 + minutes * 60 + seconds
                        if (totalSeconds >= 0) {
                            vm.adjustFinishTime(totalSeconds)
                        }
                        showFinishTimeDialog = false
                        finishTimeHours = ""
                        finishTimeMinutes = ""
                        finishTimeSeconds = ""
                    }
                ) {
                    Text("СОХРАНИТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFinishTimeDialog = false
                    finishTimeHours = ""
                    finishTimeMinutes = ""
                    finishTimeSeconds = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}