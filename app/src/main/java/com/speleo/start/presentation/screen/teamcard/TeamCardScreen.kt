package com.speleo.start.presentation.screen.teamcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.presentation.component.PersonSearchDialog
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCardScreen(
    teamId: Long,
    onBack: () -> Unit,
    onNavigateToFinish: (() -> Unit)? = null,
    onNavigateToRouteCard: ((Long) -> Unit)? = null,
    vm: TeamCardVM = hiltViewModel()
) {
    val card by vm.teamCard.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var showDisbandDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var showMentorDialog by remember { mutableStateOf(false) }
    var showReplacedHistory by remember { mutableStateOf(false) }
    var selectedParticipantId by remember { mutableStateOf<Long?>(null) }
    var selectedParticipantName by remember { mutableStateOf("") }
    var selectedParticipantForMentor by remember { mutableStateOf<TeamCardMember?>(null) }
    var disbandReason by remember { mutableStateOf("") }
    var disbandPassword by remember { mutableStateOf("") }

    LaunchedEffect(teamId) {
        vm.loadTeam(teamId)
    }

    LaunchedEffect(Unit) {
        vm.event.collectLatest { event ->
            when (event) {
                is TeamCardEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is TeamCardEvent.ShowPasswordDialog -> { }
                is TeamCardEvent.ShowReplaceDialog -> {
                    selectedParticipantId = event.participantId
                    selectedParticipantName = event.currentPersonName
                    showReplaceDialog = true
                }
                is TeamCardEvent.TeamUpdated -> {
                    vm.loadTeam(event.teamId)
                }
            }
        }
    }

    val fabAction = vm.getFabAction()
    val canEdit = vm.canEdit()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Карточка команды") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { showMenu = true }) {
                            Text("⋮", fontSize = 24.sp)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🚫 Расформировать команду") },
                                onClick = {
                                    showMenu = false
                                    showDisbandDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🔄 Сменить класс") },
                                onClick = {
                                    showMenu = false
                                    vm.changeClass("", null)
                                },
                                enabled = card?.status == "registered"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (fabAction != null) {
                FloatingActionButton(
                    onClick = {
                        when (fabAction) {
                            "finish" -> onNavigateToFinish?.invoke()
                            "route" -> onNavigateToRouteCard?.invoke(teamId)
                        }
                    },
                    containerColor = when (fabAction) {
                        "finish" -> Color(0xFFD32F2F)
                        "route" -> Color(0xFF2E7D32)
                        else -> Color.Gray
                    }
                ) {
                    Text(if (fabAction == "finish") "🏁" else "📄", fontSize = 20.sp)
                }
            }
        }
    ) { padding ->
        if (card != null) {
            val info = card!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Основная информация
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "№${info.number} · ${info.className}-й класс",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = vm.getColorMarkText(),
                                    fontSize = 14.sp,
                                    color = Color(0xFFF57C00)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Статус: ${statusToText(info.status)}", fontSize = 14.sp)

                            if (info.checkpointsEntered) {
                                Text(
                                    text = "✅ Путевой лист подтверждён",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            } else if (info.status == "finished" && onNavigateToRouteCard != null) {
                                TextButton(
                                    onClick = { onNavigateToRouteCard(info.teamId) },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "📄 Заполнить путевой лист",
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E5A7A)
                                    )
                                }
                            }
                        }
                    }
                }

                // Состав команды
                item {
                    Text(
                        text = "👥 Состав (${info.members.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(info.members) { index, member ->
                    TeamMemberCard(
                        member = member,
                        isCaptain = index == 0,
                        canEdit = canEdit,
                        onReplace = {
                            selectedParticipantId = member.participantId
                            selectedParticipantName = "${member.lastName} ${member.firstName}"
                            showReplaceDialog = true
                        },
                        onRemove = {
                            vm.removeMember(member.participantId)
                        },
                        onAssignMentor = {
                            selectedParticipantForMentor = member
                            showMentorDialog = true
                        }
                    )
                }

                // История замен - разворачиваемая
                if (info.replacedCount > 0) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReplacedHistory = !showReplacedHistory },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔄 История замен (${info.replacedCount})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (showReplacedHistory) "▲" else "▼",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        if (showReplacedHistory) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (info.replacedMembers.isNotEmpty()) {
                                        info.replacedMembers.forEach { replaced ->
                                            Text(
                                                text = replaced,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Нет записей",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Загрузка...", fontSize = 18.sp)
            }
        }
    }

    // Диалог расформирования
    if (showDisbandDialog) {
        AlertDialog(
            onDismissRequest = { showDisbandDialog = false },
            title = { Text(text = "🚫 Расформировать команду") },
            text = {
                Column {
                    Text(
                        text = "Команда №${card?.number} будет расформирована. Участники станут свободными агентами.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = disbandReason,
                        onValueChange = { disbandReason = it },
                        label = { Text(text = "Причина (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (card?.status == "started") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = disbandPassword,
                            onValueChange = { disbandPassword = it },
                            label = { Text(text = "Пароль судьи") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.disbandTeam(disbandPassword, disbandReason)
                        showDisbandDialog = false
                        disbandReason = ""
                        disbandPassword = ""
                    },
                    enabled = card?.status != "started" || disbandPassword.isNotBlank()
                ) {
                    Text(text = "РАСФОРМИРОВАТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisbandDialog = false }) {
                    Text(text = "Отмена")
                }
            }
        )
    }

    // Диалог замены участника
    if (showReplaceDialog && selectedParticipantId != null) {
        PersonSearchDialog(
            title = "Заменить: $selectedParticipantName",
            onPersonSelected = { person ->
                vm.replaceMember(selectedParticipantId!!, person.id)
                showReplaceDialog = false
                selectedParticipantId = null
                selectedParticipantName = ""
            },
            onQuickCreate = { fullName ->
                vm.createQuickPerson(fullName) { newPerson ->
                    vm.replaceMember(selectedParticipantId!!, newPerson.id)
                    showReplaceDialog = false
                    selectedParticipantId = null
                    selectedParticipantName = ""
                }
            },
            onDismiss = {
                showReplaceDialog = false
                selectedParticipantId = null
                selectedParticipantName = ""
            }
        )
    }

    // Диалог выбора ментора
    if (showMentorDialog && selectedParticipantForMentor != null) {
        val participantId = selectedParticipantForMentor!!.participantId
        val participantName = "${selectedParticipantForMentor!!.lastName} ${selectedParticipantForMentor!!.firstName}"

        PersonSearchDialog(
            title = "Выбрать ментора для $participantName",
            onPersonSelected = { person ->
                vm.assignMentor(participantId, person.id)
                showMentorDialog = false
                selectedParticipantForMentor = null
            },
            onQuickCreate = { fullName ->
                vm.createQuickPerson(fullName) { newPerson ->
                    vm.assignMentor(participantId, newPerson.id)
                    showMentorDialog = false
                    selectedParticipantForMentor = null
                }
            },
            onDismiss = {
                showMentorDialog = false
                selectedParticipantForMentor = null
            }
        )
    }
}

@Composable
fun TeamMemberCard(
    member: TeamCardMember,
    isCaptain: Boolean,
    canEdit: Boolean,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onAssignMentor: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    val ageColor = when {
        member.age != null && member.age < 14 -> Color(0xFFD32F2F)
        member.age != null && member.age in 14..17 -> Color(0xFFFF8C00)
        else -> Color(0xFF00A86B)
    }

    val needsMentor = member.age != null && member.age < 18 && member.mentorName == null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit) { showActions = !showActions }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.lastName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${member.lastName} ${member.firstName}${member.nickname?.let { " «$it»" } ?: ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (member.age != null) {
                            Text(
                                text = "Возраст: ${member.age} лет",
                                fontSize = 13.sp,
                                color = ageColor
                            )
                        }
                        if (member.phone != null) {
                            Text(
                                text = "📞 ${member.phone}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                if (isCaptain) {
                    Text(text = "👑", fontSize = 18.sp)
                }
            }

            if (member.mentorName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "👨‍🏫 Ментор: ${member.mentorName}",
                        fontSize = 13.sp,
                        color = Color(0xFF1E5A7A)
                    )
                    if (!member.mentorConfirmed) {
                        Text(
                            text = " (не подтверждён)",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            } else if (needsMentor && canEdit) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onAssignMentor,
                    modifier = Modifier.padding(start = 0.dp)
                ) {
                    Text(
                        text = "⚠️ Требуется ментор",
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            if (member.judgeApproved) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✅ Разрешено судьёй",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32)
                )
            }

            if (showActions && canEdit) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onReplace()
                            showActions = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🔄 Заменить", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onRemove()
                            showActions = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = "❌ Исключить", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun statusToText(status: String): String {
    return when (status) {
        "registered" -> "Зарегистрирована"
        "started" -> "На дистанции"
        "finished" -> "Финишировала"
        "lost" -> "Потеряна (ПСР)"
        "disqualified" -> "Снята"
        else -> status
    }
}