package com.speleo.start.presentation.screen.routecard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteCardScreen(
    teamId: Long,
    onBack: () -> Unit,
    vm: RouteCardVM = hiltViewModel()
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val teamInfo by vm.teamInfo.collectAsStateWithLifecycle()
    val isReadOnly by vm.isReadOnly.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSecretaryDialog by remember { mutableStateOf(false) }
    var showJudgeDialog by remember { mutableStateOf(false) }
    var secretaryPassword by remember { mutableStateOf("") }
    var judgePassword by remember { mutableStateOf("") }

    LaunchedEffect(teamId) { vm.loadRouteCard(teamId) }

    LaunchedEffect(Unit) {
        vm.event.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Путевой лист") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Загрузка...", fontSize = 18.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp)
            ) {
                if (teamInfo != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isReadOnly) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Команда №${teamInfo!!.teamNumber} (${teamInfo!!.className}-й класс)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Статус: ${teamInfo!!.status}",
                                fontSize = 14.sp
                            )
                            if (!isReadOnly && teamInfo?.status == "finished") {
                                Text(
                                    text = "🔓 РЕЖИМ МАСТЕР-ПРАВКИ",
                                    fontSize = 13.sp,
                                    color = Color(0xFFF57C00),
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (isReadOnly) {
                                Text(
                                    text = "✅ Путевой лист подтверждён",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(entries) { index, entry ->
                        RouteCardEntryCard(
                            entry = entry,
                            isReadOnly = isReadOnly,
                            onTakenChange = { vm.toggleTaken(index) },
                            onErrorChange = { vm.toggleError(index) },
                            onOffsetTimeChange = { vm.updateOffsetTime(index, it) },
                            onPenaltyChange = { vm.updatePenalty(index, it) }
                        )
                    }

                    if (!isReadOnly && entries.isNotEmpty() && teamInfo?.status == "finished") {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { vm.saveMasterChangesAndClose(onBack) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                            ) {
                                Text("💾 СОХРАНИТЬ И ЗАКРЫТЬ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isReadOnly && entries.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showSecretaryDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📝 Секретарь", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { showJudgeDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("⚖️ Судья", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSecretaryDialog) {
        AlertDialog(
            onDismissRequest = {
                showSecretaryDialog = false
                secretaryPassword = ""
            },
            title = { Text("Подтверждение секретарём") },
            text = {
                Column {
                    Text("Введите пароль секретаря")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secretaryPassword,
                        onValueChange = { secretaryPassword = it },
                        label = { Text("Пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.confirmBySecretary(secretaryPassword)
                        showSecretaryDialog = false
                        secretaryPassword = ""
                    }
                ) { Text("ПОДТВЕРДИТЬ") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSecretaryDialog = false
                    secretaryPassword = ""
                }) { Text("Отмена") }
            }
        )
    }

    if (showJudgeDialog) {
        AlertDialog(
            onDismissRequest = {
                showJudgeDialog = false
                judgePassword = ""
            },
            title = { Text("Подтверждение судьёй") },
            text = {
                Column {
                    Text("Введите пароль судьи")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = judgePassword,
                        onValueChange = { judgePassword = it },
                        label = { Text("Пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.confirmByJudge(judgePassword)
                        showJudgeDialog = false
                        judgePassword = ""
                    }
                ) { Text("ПОДТВЕРДИТЬ") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJudgeDialog = false
                    judgePassword = ""
                }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun RouteCardEntryCard(
    entry: RouteCardEntry,
    isReadOnly: Boolean,
    onTakenChange: () -> Unit,
    onErrorChange: () -> Unit,
    onOffsetTimeChange: (String) -> Unit,
    onPenaltyChange: (String) -> Unit
) {
    var offsetTimeText by remember(entry.checkpointId, entry.offsetTime) {
        mutableStateOf(entry.offsetTime)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.type == "technical")
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "КП №${entry.displayNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (entry.type == "technical") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ТЕХНИЧЕСКИЙ",
                            fontSize = 10.sp,
                            color = Color(0xFFF57C00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Вес: ${entry.weight}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = entry.taken,
                        onCheckedChange = { if (!isReadOnly) onTakenChange() },
                        enabled = !isReadOnly
                    )
                    Text("Взят", fontSize = 14.sp)
                }

                if (entry.taken) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = entry.takenWithError,
                            onCheckedChange = { if (!isReadOnly) onErrorChange() },
                            enabled = !isReadOnly
                        )
                        Text("С ошибкой", fontSize = 14.sp, color = Color(0xFFFF8C00))
                    }
                }
            }

            if (entry.type == "technical") {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = offsetTimeText,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { char -> char.isDigit() || char == ':' }
                            if (filtered.length <= 5) {
                                offsetTimeText = filtered
                                if (filtered.isNotBlank() && !filtered.endsWith(":")) {
                                    onOffsetTimeChange(filtered)
                                }
                            }
                        },
                        label = { Text("Отсечка (ММ:СС)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isReadOnly && entry.taken
                    )

                    OutlinedTextField(
                        value = entry.penalty,
                        onValueChange = { newValue ->
                            if (newValue.all { char -> char.isDigit() } && newValue.length <= 3) {
                                onPenaltyChange(newValue)
                            }
                        },
                        label = { Text("Штраф") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isReadOnly && entry.taken
                    )
                }
            }
        }
    }
}