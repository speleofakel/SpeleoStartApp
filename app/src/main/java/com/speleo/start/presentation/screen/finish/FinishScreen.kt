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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinishScreen(
    onBack: () -> Unit,
    vm: FinishVM = hiltViewModel()
) {
    val allTeams by vm.allTeams.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val filteredTeams by vm.filteredTeams.collectAsStateWithLifecycle()
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
                title = { Text("Финиш", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("◀", fontSize = 18.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Поле поиска
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.onSearchQueryChange(it) },
                label = { Text("🔍 Поиск по номеру или фамилии") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = { vm.onSearchQueryChange("") }) {
                            Text("✖️", fontSize = 14.sp)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Счётчик выбранных команд
            Text(
                text = "Выбрано команд: ${selectedIds.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selectedIds.isNotEmpty()) Color(0xFF00A86B) else Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (allTeams.isEmpty()) {
                Text(
                    text = "Нет команд на дистанции",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                // Список команд с прокруткой
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTeams, key = { it.id }) { team ->
                        val isSelected = selectedIds.contains(team.id)
                        val interactionSource = remember { MutableInteractionSource() }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { vm.toggleSelection(team.id) },
                                    onLongClick = {
                                        if (team.hasFinished) {
                                            adjustingTeamId = team.id
                                            adjustingTeamNumber = team.number
                                            adjustedHours = ""
                                            adjustedMinutes = ""
                                            adjustedSeconds = ""
                                            showTimeAdjustDialog = team.id
                                        }
                                    }
                                ),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { vm.toggleSelection(team.id) }
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "№${team.number} (${team.className}-й класс)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = team.members,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (team.hasFinished) {
                                        Text(
                                            text = "📌 Долгий тап для корректировки времени",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                // Цветовая метка
                                ColorMarkLabel(colorMark = team.colorMark)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка подтверждения — всегда видна внизу
            Button(
                onClick = { vm.confirmFinish() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedIds.isNotEmpty()) Color(0xFF00A86B) else Color.Gray
                )
            ) {
                Text(
                    text = "ПОДТВЕРДИТЬ ФИНИШ (${selectedIds.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
                    Text("Команда №$adjustingTeamNumber", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Введите относительное время от старта соревнования", fontSize = 12.sp, color = Color.Gray)
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
                    Text(
                        text = "Формат: ЧЧ:ММ:СС (пример: 01:45:30)",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
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

@Composable
private fun ColorMarkLabel(colorMark: String) {
    val (bgColor, textColor) = when (colorMark) {
        "18+" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "14+", "<14ок" -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
        "<17 !!!", "<14 !!!" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "СНЯТЫ" -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
        else -> Color(0xFFE0E0E0) to Color.Gray
    }

    androidx.compose.material3.Card(
        modifier = Modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bgColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Text(
            text = colorMark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}