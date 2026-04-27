package com.speleo.start.presentation.screen.checkpoints

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

fun filterDigitsPreserveCursor(oldValue: TextFieldValue, newValue: String, maxLength: Int): TextFieldValue {
    val filtered = newValue.filter { it.isDigit() }.take(maxLength)
    val removedCount = newValue.length - filtered.length
    val newCursorPos = (oldValue.selection.start - removedCount).coerceIn(0, filtered.length)
    return TextFieldValue(filtered, TextRange(newCursorPos))
}

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
                title = { Text("Настройка КП", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } },
                actions = { IconButton(onClick = vm::reorderCheckpoints) { Icon(Icons.Default.Save, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::showAddDialog, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (checkpoints.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📋 Нет КП", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Нажмите + для добавления", fontSize = 14.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(checkpoints) { index, cp ->
                        CheckpointCard(
                            checkpoint = cp,
                            onToggleType = { vm.toggleCheckpointType(cp.id) },
                            onToggleClass2 = { vm.toggleClass2(cp.id, cp.forClass2) },
                            onToggleClass3 = { vm.toggleClass3(cp.id, cp.forClass3) },
                            onWeightChange = { vm.updateWeight(cp.id, it) },
                            onNormChange = { vm.updateNormative(cp.id, it) },
                            onPenaltyChange = { vm.updatePenalty(cp.id, it) },
                            onToggleWait = { vm.toggleTrackWaitTime(cp.id, cp.trackWaitTime) },
                            onMoveUp = if (index > 0) { { vm.moveCheckpoint(index, index - 1) } } else null,
                            onMoveDown = if (index < checkpoints.size - 1) { { vm.moveCheckpoint(index, index + 1) } } else null,
                            onDelete = { vm.deleteCheckpoint(cp.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCheckpointDialog(
            onDismiss = { vm.hideAddDialog() },
            onConfirm = { w, n, p, c2, c3 ->
                vm.addCheckpoint(w, n, p, c2, c3)
                vm.hideAddDialog()
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
    onNormChange: (String) -> Unit,
    onPenaltyChange: (Int) -> Unit,
    onToggleWait: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val isTech = checkpoint.type == "technical"
    val cardBg = Color(0xFF1E293B)
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusManager.clearFocus() }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isTech) Color(0xFF38BDF8) else Color(0xFF334155)),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, bottom = 12.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SingleTypeToggle(isTech = isTech, onToggle = onToggleType)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ClassPill(
                            active = checkpoint.forClass2,
                            text = "2-й кл",
                            activeColor = Color(0xFF22C55E),
                            onClick = onToggleClass2
                        )
                        ClassPill(
                            active = checkpoint.forClass3,
                            text = "3-й кл",
                            activeColor = Color(0xFF38BDF8),
                            onClick = onToggleClass3
                        )
                    }

                    CompactDigitField(
                        value = checkpoint.weight.toString(),
                        maxLength = 2,
                        onValueChange = { it.toIntOrNull()?.let(onWeightChange) },
                        label = "Вес",
                        modifier = Modifier.width(60.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (onMoveUp != null) {
                            SmallIconBtn(Icons.Default.ArrowUpward, enabled = true, onClick = onMoveUp)
                        }
                        if (onMoveDown != null) {
                            SmallIconBtn(Icons.Default.ArrowDownward, enabled = true, onClick = onMoveDown)
                        }
                        SmallIconBtn(Icons.Default.Delete, enabled = true, color = Color(0xFFEF4444), onClick = onDelete)
                    }
                }

                if (isTech) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = 14.sp)
                        CompactDigitField(
                            value = checkpoint.normativeSeconds ?: "0",
                            maxLength = 3,
                            onValueChange = onNormChange,
                            modifier = Modifier.width(70.dp)
                        )
                        Text("сек", fontSize = 11.sp, color = Color(0xFF94A3B8))

                        Spacer(Modifier.weight(1f))

                        Text("⚡", fontSize = 14.sp)
                        CompactDigitField(
                            value = (checkpoint.bonusPoints ?: 0).toString(),
                            maxLength = 1,
                            onValueChange = { it.toIntOrNull()?.let(onPenaltyChange) },
                            modifier = Modifier.width(45.dp)
                        )

                        Spacer(Modifier.weight(1f))

                        Text("⏱️", fontSize = 14.sp)
                        Text(
                            text = if (checkpoint.trackWaitTime) "Да" else "Нет",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (checkpoint.trackWaitTime) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.clickable {
                                onToggleWait()
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }

        Text(
            text = "${checkpoint.displayNumber}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isTech) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 16.dp, y = (-12).dp)
                .background(
                    color = cardBg,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 0.dp)
        )
    }
}

@Composable
fun SingleTypeToggle(isTech: Boolean, onToggle: () -> Unit) {
    val bg = if (isTech) Color(0xFF38BDF8) else Color.Transparent
    val border = if (isTech) Color(0xFF38BDF8) else Color(0xFF475569)
    val text = if (isTech) Color.Black else Color(0xFF94A3B8)

    Surface(
        modifier = Modifier
            .clickable { onToggle() }
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Color.Transparent
    ) {
        Text(text = if (isTech) "ТХ" else "КП", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = text)
    }
}

@Composable
fun ClassPill(active: Boolean, text: String, activeColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (active) activeColor.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(1.dp, if (active) activeColor else Color(0xFF475569))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = if (active) activeColor else Color(0xFF64748B),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CompactDigitField(
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    // Убираем фокус при нажатии Enter или при потере фокуса
    val onValueChange = { newTfv: TextFieldValue ->
        val updated = filterDigitsPreserveCursor(textFieldValue, newTfv.text, maxLength)
        textFieldValue = updated
        if (updated.text != value) {
            onValueChange(updated.text)
        }
    }

    Box(modifier = modifier.height(36.dp)) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (!focusState.isFocused && textFieldValue.text.isNotEmpty()) {
                        // При потере фокуса перемещаем курсор в конец
                        textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
                    }
                },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF38BDF8),
                focusedSupportingTextColor = Color.Transparent,
                unfocusedSupportingTextColor = Color.Transparent
            ),
            shape = RoundedCornerShape(6.dp)
        )

        // Кастомный лейбл поверх поля
        if (label.isNotEmpty() && !isFocused && value.isEmpty()) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun SmallIconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, color: Color = Color(0xFF94A3B8), onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) color else Color(0xFF334155),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AddCheckpointDialog(onDismiss: () -> Unit, onConfirm: (Int, Int, Int, Boolean, Boolean) -> Unit) {
    var weight by remember { mutableStateOf("1") }
    var norm by remember { mutableStateOf("00:00") }
    var pen by remember { mutableStateOf("0") }
    var c2 by remember { mutableStateOf(true) }
    var c3 by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { focusManager.clearFocus() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("➕ Добавить КП", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = weight,
                    onValueChange = { if (it.all { c -> c.isDigit() }) weight = it },
                    label = { Text("Вес", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        cursorColor = Color(0xFF38BDF8)
                    )
                )

                OutlinedTextField(
                    value = norm,
                    onValueChange = { norm = it },
                    label = { Text("Норматив (ММ:СС)", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        cursorColor = Color(0xFF38BDF8)
                    )
                )

                OutlinedTextField(
                    value = pen,
                    onValueChange = { if (it.all { c -> c.isDigit() }) pen = it },
                    label = { Text("Штраф", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        cursorColor = Color(0xFF38BDF8)
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = c2,
                            onCheckedChange = { c2 = it },
                            colors = CheckboxDefaults.colors(checkmarkColor = Color.White)
                        )
                        Text("2-й класс", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = c3,
                            onCheckedChange = { c3 = it },
                            colors = CheckboxDefaults.colors(checkmarkColor = Color.White)
                        )
                        Text("3-й класс", color = Color.White)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = Color(0xFF94A3B8)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onConfirm(
                                weight.toIntOrNull() ?: 1,
                                parseNorm(norm),
                                pen.toIntOrNull() ?: 0,
                                c2,
                                c3
                            )
                        }
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}

private fun parseNorm(input: String): Int {
    val parts = input.split(":")
    return if (parts.size == 2) {
        (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    } else {
        input.toIntOrNull() ?: 0
    }
}