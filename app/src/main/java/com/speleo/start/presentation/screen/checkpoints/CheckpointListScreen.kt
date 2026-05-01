package com.speleo.start.presentation.screen.checkpoints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckpointListScreen(
    competitionId: Long,
    onBack: () -> Unit,
    vm: CheckpointListVM = hiltViewModel()
) {
    val checkpoints by vm.checkpoints.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val showAddDialog by vm.showAddDialogState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(competitionId) { vm.load(competitionId) }

    LaunchedEffect(Unit) {
        vm.event.collectLatest { event ->
            if (event is CheckpointListVM.UiEvent.ShowMessage) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Контрольные пункты", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.reorderCheckpoints() }) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить порядок")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (checkpoints.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📋 Нет контрольных пунктов", fontSize = 18.sp)
                    Text("Нажмите + для добавления", fontSize = 14.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(checkpoints) { index, cp ->
                        CheckpointCard(
                            checkpoint = cp,
                            onToggleType = { vm.toggleCheckpointType(cp.id) },
                            onToggleClass2 = { vm.toggleClass2(cp.id) },
                            onToggleClass3 = { vm.toggleClass3(cp.id) },
                            onWeightChange = { vm.updateWeight(cp.id, it) },
                            onNormChange = { vm.updateNormative(cp.id, it) },
                            onPenaltyChange = { vm.updatePenalty(cp.id, it) },
                            onToggleWait = { vm.toggleTrackWaitTime(cp.id) },
                            onMoveUp = if (index > 0) { { vm.moveCheckpoint(index, index - 1) } } else null,
                            onMoveDown = if (index < checkpoints.size - 1) { { vm.moveCheckpoint(index, index + 1) } } else null,
                            onDelete = { vm.deleteCheckpoint(cp.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCheckpointDialog(
            onDismiss = { vm.hideAddDialog() },
            onConfirm = { weight, normSeconds, penalty, forClass2, forClass3 ->
                vm.addCheckpoint(weight, normSeconds, penalty, forClass2, forClass3)
            }
        )
    }
}

@Composable
fun CheckpointCard(
    checkpoint: CheckpointItem,
    onToggleType: () -> Unit,
    onToggleClass2: () -> Unit,
    onToggleClass3: () -> Unit,
    onWeightChange: (Int) -> Unit,
    onNormChange: (Int) -> Unit,
    onPenaltyChange: (Int) -> Unit,
    onToggleWait: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val isTech = checkpoint.type == "technical"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Строка 1: Номер + кнопки классов + Вес
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Номер и тип
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "№${checkpoint.displayNumber}",
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    // Кнопка переключения типа (КП/ТХ)
                    FilterChip(
                        selected = isTech,
                        onClick = onToggleType,
                        label = { Text(if (isTech) "ТЕХ" else "КП", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFB45309),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Кнопки классов
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = checkpoint.forClass2,
                        onClick = onToggleClass2,
                        label = { Text("2-й", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = checkpoint.forClass3,
                        onClick = onToggleClass3,
                        label = { Text("3-й", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Вес (редактируемый)
                CompactNumberField(
                    value = checkpoint.weight,
                    onValueChange = onWeightChange,
                    label = "Вес",
                    modifier = Modifier.width(70.dp)
                )
            }

            // Строка 2: технические параметры (если есть)
            if (isTech) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Норматив (секунды -> ММ:СС)
                    CompactNormField(
                        seconds = checkpoint.normativeSeconds ?: 0,
                        onValueChange = onNormChange,
                        modifier = Modifier.weight(1f)
                    )

                    // Штраф
                    CompactNumberField(
                        value = checkpoint.bonusPoints ?: 0,
                        onValueChange = onPenaltyChange,
                        label = "Штраф",
                        modifier = Modifier.width(70.dp)
                    )

                    // Отсечка
                    FilterChip(
                        selected = checkpoint.trackWaitTime,
                        onClick = onToggleWait,
                        label = { Text(if (checkpoint.trackWaitTime) "Отс. Да" else "Отс. Нет", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Строка 3: кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", modifier = Modifier.size(18.dp))
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp), tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun CompactNumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            if (newText.all { it.isDigit() } && newText.length <= 3) {
                text = newText
                val intValue = newText.toIntOrNull()
                if (intValue != null && intValue != value) {
                    onValueChange(intValue)
                }
            }
        },
        label = { Text(label, fontSize = 10.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Center)
    )
}

@Composable
fun CompactNormField(
    seconds: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(seconds) {
        mutableStateOf(formatSecondsToMmSs(seconds))
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            val filtered = newText.filter { it.isDigit() || it == ':' }
            if (filtered.length <= 5) {
                text = filtered
                val parsed = parseMmSsToSeconds(filtered)
                if (parsed != null && parsed != seconds) {
                    onValueChange(parsed)
                }
            }
        },
        label = { Text("Норматив", fontSize = 10.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Center)
    )
}

private fun formatSecondsToMmSs(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun parseMmSsToSeconds(input: String): Int? {
    val parts = input.split(":")
    return if (parts.size == 2) {
        val minutes = parts[0].toIntOrNull() ?: return null
        val seconds = parts[1].toIntOrNull() ?: return null
        if (seconds in 0..59) minutes * 60 + seconds else null
    } else {
        input.toIntOrNull()
    }
}

@Composable
fun AddCheckpointDialog(
    onDismiss: () -> Unit,
    onConfirm: (weight: Int, normSeconds: Int, penalty: Int, forClass2: Boolean, forClass3: Boolean) -> Unit
) {
    var weight by remember { mutableStateOf("5") }
    var normSeconds by remember { mutableStateOf("60") }
    var penalty by remember { mutableStateOf("0") }
    var forClass2 by remember { mutableStateOf(true) }
    var forClass3 by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("➕ Добавить контрольный пункт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) weight = it },
                    label = { Text("Вес (баллы)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = normSeconds,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) normSeconds = it },
                    label = { Text("Норматив (секунды)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = penalty,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) penalty = it },
                    label = { Text("Штраф") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = forClass2, onCheckedChange = { forClass2 = it })
                        Text("2-й класс")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = forClass3, onCheckedChange = { forClass3 = it })
                        Text("3-й класс")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toIntOrNull() ?: 1
                    val norm = normSeconds.toIntOrNull() ?: 0
                    val pen = penalty.toIntOrNull() ?: 0
                    if (w in 1..999 && (forClass2 || forClass3)) {
                        onConfirm(w, norm, pen, forClass2, forClass3)
                    }
                },
                enabled = (weight.toIntOrNull() ?: 0) in 1..999 && (forClass2 || forClass3)
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}