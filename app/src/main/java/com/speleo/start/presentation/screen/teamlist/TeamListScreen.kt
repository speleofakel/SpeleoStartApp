package com.speleo.start.presentation.screen.teamlist

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TeamListScreen(
    onBack: () -> Unit,
    onTeamClick: (Long) -> Unit,
    vm: TeamListVM = hiltViewModel()
) {
    val teams by vm.teams.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    var selectedTeamForTimeAdjust by remember { mutableStateOf<TeamListInfo?>(null) }
    var adjustedHours by remember { mutableStateOf("") }
    var adjustedMinutes by remember { mutableStateOf("") }
    var adjustedSeconds by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadTeams() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Команды") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.onSearchQueryChange(it) },
                label = { Text("🔍 Поиск по номеру или фамилии") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { vm.onSearchQueryChange("") }) {
                            Text("✖️", fontSize = 16.sp)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (teams.isEmpty()) {
                Text("Нет команд", fontSize = 16.sp)
            } else {
                LazyColumn {
                    items(teams, key = { it.id }) { team ->
                        val interactionSource = remember { MutableInteractionSource() }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        vm.selectTeam(team.id)
                                        onTeamClick(team.id)
                                    },
                                    onLongClick = {
                                        if (team.status == "finished") {
                                            selectedTeamForTimeAdjust = team
                                            adjustedHours = ""
                                            adjustedMinutes = ""
                                            adjustedSeconds = ""
                                        }
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row {
                                        Text(
                                            "№${team.number} (${team.className}-й кл)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            " · ${team.statusText}",
                                            fontSize = 13.sp,
                                            color = when (team.status) {
                                                "registered" -> Color(0xFF00A86B)
                                                "started" -> Color(0xFFFF8C00)
                                                "finished" -> Color(0xFF2E7D32)
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                    Text(
                                        team.colorMark,
                                        fontSize = 13.sp,
                                        color = when {
                                            team.colorMark.contains("!!!") -> Color(0xFFD32F2F)
                                            team.colorMark == "СНЯТЫ" -> Color(0xFFD32F2F)
                                            else -> Color(0xFF00A86B)
                                        }
                                    )
                                }
                                if (team.memberNames.isNotBlank()) {
                                    Text(
                                        team.memberNames,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                                if (team.status == "finished") {
                                    Text(
                                        "📌 Долгий тап для корректировки времени",
                                        fontSize = 10.sp,
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

    // Диалог корректировки времени финиша
    selectedTeamForTimeAdjust?.let { team ->
        AlertDialog(
            onDismissRequest = { selectedTeamForTimeAdjust = null },
            title = { Text("Корректировка времени финиша") },
            text = {
                Column {
                    Text("Команда №${team.number}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Текущий статус: ${team.statusText}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Введите время в формате ЧЧ:ММ:СС",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = adjustedHours.toIntOrNull() ?: 0
                        val minutes = adjustedMinutes.toIntOrNull() ?: 0
                        val seconds = adjustedSeconds.toIntOrNull() ?: 0
                        val totalSeconds = hours * 3600 + minutes * 60 + seconds

                        if (totalSeconds > 0) {
                            val timestamp = totalSeconds * 1000L
                            vm.adjustFinishTime(team.id, timestamp)
                        }
                        selectedTeamForTimeAdjust = null
                    },
                    enabled = adjustedHours.isNotBlank() || adjustedMinutes.isNotBlank() || adjustedSeconds.isNotBlank()
                ) {
                    Text("СОХРАНИТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTeamForTimeAdjust = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}