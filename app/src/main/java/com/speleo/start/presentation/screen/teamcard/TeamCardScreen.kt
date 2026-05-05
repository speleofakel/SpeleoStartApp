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
import androidx.compose.foundation.lazy.items
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
    val routeEntries by vm.routeEntries.collectAsStateWithLifecycle()
    val routeStats by vm.routeStats.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var showDisbandDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var showMentorDialog by remember { mutableStateOf(false) }
    var showReplacedHistory by remember { mutableStateOf(false) }
    var showMasterUnlockDialog by remember { mutableStateOf(false) }
    var selectedParticipantId by remember { mutableStateOf<Long?>(null) }
    var selectedParticipantName by remember { mutableStateOf("") }
    var selectedParticipantForMentor by remember { mutableStateOf<TeamCardMember?>(null) }
    var disbandReason by remember { mutableStateOf("") }
    var disbandPassword by remember { mutableStateOf("") }
    var masterPassword by remember { mutableStateOf("") }

    LaunchedEffect(teamId) {
        vm.loadTeam(teamId)
        vm.loadRouteCard(teamId)
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
                    vm.loadRouteCard(event.teamId)
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
        if (card == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Загрузка...", fontSize = 18.sp)
            }
        } else {
            val info = card!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ===== ОСНОВНАЯ ИНФОРМАЦИЯ =====
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
                        }
                    }
                }

                // ===== СТАТИСТИКА ПУТЕВОГО ЛИСТА =====
                item {
                    RouteCardStatsCard(
                        stats = routeStats,
                        onMasterUnlock = { showMasterUnlockDialog = true }
                    )
                }

                // ===== СПИСОК КП =====
                if (routeEntries.isNotEmpty()) {
                    item {
                        Text(
                            text = "🗺️ ПУТЕВОЙ ЛИСТ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(routeEntries) { entry ->
                        RouteCardEntryItem(entry = entry)
                    }
                }

                // ===== СОСТАВ КОМАНДЫ =====
                item {
                    Text(
                        text = "👥 Состав (${info.members.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(info.members) { member ->
                    TeamMemberCard(
                        member = member,
                        isCaptain = member.role == "captain",
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

                // ===== ИСТОРИЯ ЗАМЕН =====
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
        }
    }

    // ===== ДИАЛОГИ =====

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
            filterForMentors = true,
            onDismiss = {
                showMentorDialog = false
                selectedParticipantForMentor = null
            }
        )
    }

    // Диалог мастер-разблокировки путевого листа
    if (showMasterUnlockDialog) {
        AlertDialog(
            onDismissRequest = {
                showMasterUnlockDialog = false
                masterPassword = ""
            },
            title = { Text("Разблокировка путевого листа") },
            text = {
                Column {
                    Text("Введите мастер-пароль для временной разблокировки путевого листа.")
                    Text("После сохранения повторное подтверждение НЕ ТРЕБУЕТСЯ.", fontSize = 12.sp, color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
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
                        if (masterPassword == "devdebug") {
                            vm.unlockRouteCardForEdit(card!!.teamId)
                            onNavigateToRouteCard?.invoke(card!!.teamId)
                            showMasterUnlockDialog = false
                            masterPassword = ""
                        } else {
                            // Неверный пароль
                        }
                    },
                    enabled = masterPassword.isNotBlank()
                ) {
                    Text("РАЗБЛОКИРОВАТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMasterUnlockDialog = false
                    masterPassword = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun RouteCardStatsCard(
    stats: RouteCardStats,
    onMasterUnlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Статус ПЛ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (stats.isFullyConfirmed) "✅ ПУТЕВОЙ ЛИСТ ПОДТВЕРЖДЁН" else "📋 ПУТЕВОЙ ЛИСТ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (stats.isFullyConfirmed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
                if (stats.isFullyConfirmed) {
                    TextButton(
                        onClick = onMasterUnlock,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("🔓 Мастер-правка", fontSize = 10.sp, color = Color(0xFFF57C00))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.takenCount}/${stats.totalCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF00A86B)
                    )
                    Text("Взято КП", fontSize = 10.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.totalScore}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Text("Баллов", fontSize = 10.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stats.totalPenalty}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFD32F2F)
                    )
                    Text("Штраф", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Время
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Старт", fontSize = 10.sp, color = Color.Gray)
                    Text(stats.startTime, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Column {
                    Text("Финиш", fontSize = 10.sp, color = Color.Gray)
                    Text(stats.finishTime, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Column {
                    Text("Отсечка", fontSize = 10.sp, color = Color.Gray)
                    Text(stats.totalOffsetTime, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Column {
                    Text("Чистое", fontSize = 10.sp, color = Color.Gray)
                    Text(stats.netTime, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RouteCardEntryItem(entry: RouteCardEntryItem) {
    val statusColor = when {
        entry.taken && entry.takenWithError -> Color(0xFFFF8C00)
        entry.taken -> Color(0xFF00A86B)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.displayNumber}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Вес: ${entry.weight}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (entry.type == "technical") {
                        Text(
                            text = "ТЕХНИЧЕСКИЙ",
                            fontSize = 10.sp,
                            color = Color(0xFFF57C00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val statusText = when {
                    entry.taken && entry.takenWithError -> "⚠️ Взят с ошибкой"
                    entry.taken -> "✅ Взят"
                    else -> "❌ Не взят"
                }
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )

                if (entry.type == "technical" && entry.taken) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (entry.offsetTime != null && entry.offsetTime != "00:00") {
                            Text(
                                text = "⏱️ Отсечка: ${entry.offsetTime}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        if (entry.penalty > 0) {
                            Text(
                                text = "💰 Штраф: ${entry.penalty}",
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
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