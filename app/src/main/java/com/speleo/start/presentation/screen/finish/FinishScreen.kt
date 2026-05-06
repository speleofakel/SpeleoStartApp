package com.speleo.start.presentation.screen.finish

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinishScreen(
    onBack: () -> Unit,
    vm: FinishVM = hiltViewModel()
) {
    val teams by vm.startedTeams.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    var showTimeAdjustDialog by remember { mutableStateOf<Long?>(null) }
    var adjustingTeamNumber by remember { mutableStateOf(0) }
    var adjustingTeamId by remember { mutableStateOf<Long?>(null) }
    var adjustedHours by remember { mutableStateOf("") }
    var adjustedMinutes by remember { mutableStateOf("") }
    var adjustedSeconds by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.loadStartedTeams()
        vm.captureFinishTime()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Финиш") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Выберите команды:", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (teams.isEmpty()) {
                Text("Нет команд на дистанции", fontSize = 16.sp)
            } else {
                LazyColumn {
                    items(teams, key = { it.id }) { team ->
                        val isSelected = selectedIds.contains(team.id)
                        val interactionSource = remember { MutableInteractionSource() }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { vm.toggleSelection(team.id) },
                                    onLongClick = {
                                        // ✅ ДОБАВЛЕНА КОРРЕКТИРОВКА ВРЕМЕНИ
                                        if (team.hasFinished) {
                                            adjustingTeamId = team.id
                                            adjustingTeamNumber = team.number
                                            adjustedHours = ""
                                            adjustedMinutes = ""
                                            adjustedSeconds = ""
                                            showTimeAdjustDialog = team.id
                                        }
                                    }
                                )
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Column {
                                    Text(
                                        "№${team.number} (${team.className}-й кл)",
                                        fontSize = 16.sp
                                    )
                                    if (team.hasFinished) {
                                        Text(
                                            "📌 Долгий тап для корректировки времени",
                                            fontSize = 10.sp,
                                            color = androidx.compose.ui.graphics.Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.confirmFinish() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("ПОДТВЕРДИТЬ ФИНИШ (${selectedIds.size})")
            }
        }
    }

    // Диалог корректировки времени
    if (showTimeAdjustDialog != null && adjustingTeamId != null) {
        AlertDialog(
            onDismissRequest = {
                showTimeAdjustDialog = null
                adjustingTeamId = null
            },
            title = { Text("Корректировка времени финиша") },
            text = {
                Column {
                    Text("Команда №$adjustingTeamNumber", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = adjustedHours,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() })
                                    adjustedHours = it
                            },
                            label = { Text("Часы") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = adjustedMinutes,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() })
                                    adjustedMinutes = it
                            },
                            label = { Text("Минуты") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = adjustedSeconds,
                            onValueChange = {
                                if (it.length <= 2 && it.all { c -> c.isDigit() })
                                    adjustedSeconds = it
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
                        val hours = adjustedHours.toIntOrNull() ?: 0
                        val minutes = adjustedMinutes.toIntOrNull() ?: 0
                        val seconds = adjustedSeconds.toIntOrNull() ?: 0
                        val totalSeconds = hours * 3600 + minutes * 60 + seconds

                        if (totalSeconds > 0 && adjustingTeamId != null) {
                            val timestamp = totalSeconds * 1000L
                            scope.launch {
                                vm.adjustFinishTime(adjustingTeamId!!, timestamp)
                            }
                        }
                        showTimeAdjustDialog = null
                        adjustingTeamId = null
                        adjustedHours = ""
                        adjustedMinutes = ""
                        adjustedSeconds = ""
                    },
                    enabled = adjustedHours.isNotBlank() || adjustedMinutes.isNotBlank() || adjustedSeconds.isNotBlank()
                ) {
                    Text("СОХРАНИТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimeAdjustDialog = null
                    adjustingTeamId = null
                    adjustedHours = ""
                    adjustedMinutes = ""
                    adjustedSeconds = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}