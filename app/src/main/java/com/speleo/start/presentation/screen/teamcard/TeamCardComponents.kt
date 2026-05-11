package com.speleo.start.presentation.screen.teamcard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TeamHeaderCard(teamInfo: TeamInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "№${teamInfo.number} · ${teamInfo.className}-й класс",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = statusToText(teamInfo.status),
                    fontSize = 14.sp,
                    color = statusToColor(teamInfo.status)
                )
            }
            Text(
                text = teamInfo.colorMark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorMarkToColor(teamInfo.colorMark)
            )
        }
    }
}

@Composable
fun TimesCard(
    startTime: String,
    finishTime: String,
    status: String,
    onFinishTimeLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("СТАРТ", fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = startTime,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (startTime != "—:—:—") Color(0xFF00A86B) else Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ФИНИШ", fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = finishTime,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = if (status == "finished") Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onFinishTimeLongClick() })
                    } else Modifier,
                    color = when {
                        finishTime == "—:—:—" -> Color.Gray
                        status == "finished" -> Color(0xFF2196F3)
                        else -> Color(0xFF9E9E9E)
                    }
                )
                if (status == "finished" && finishTime != "—:—:—") {
                    Text(
                        text = "долгий тап для изменения",
                        fontSize = 9.sp,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteCardStatusCard(
    stats: RouteCardStats,
    checkpointsEntered: Boolean,
    status: String,
    isMasterMode: Boolean,
    isSecretarySigned: Boolean,
    isJudgeSigned: Boolean,
    onMasterUnlock: () -> Unit,
    onSecretarySign: () -> Unit,
    onJudgeSign: () -> Unit,
    mode: TeamCardMode,
    isQuickEditMode: Boolean = false,
    onToggleQuickEdit: () -> Unit = {},
    routeCardEntries: List<RouteCardEntryUi> = emptyList(),
    onCycleCheckpoint: (Long) -> Unit = {},
    onTechnicalLongPress: (Long) -> Unit = {},
    onShowTechDialog: (Long) -> Unit = {}
) {
    val isFinished = status == "finished"
    val isCancelled = status == "disqualified" || status == "lost"
    val isConfirmed = checkpointsEntered && !isMasterMode
    val isInProgress = isFinished && !checkpointsEntered && !isCancelled && (mode == TeamCardMode.EDIT || isQuickEditMode)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCancelled -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                isConfirmed -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                isInProgress -> Color(0xFFFF8C00).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isMasterMode -> "👑 МАСТЕР-РЕЖИМ"
                            isQuickEditMode -> "✏️ РЕДАКТИРОВАНИЕ"
                            isConfirmed -> "✅ ПУТЕВОЙ ЛИСТ"
                            isInProgress -> "📝 ПУТЕВОЙ ЛИСТ"
                            isCancelled -> "❌ ПУТЕВОЙ ЛИСТ"
                            else -> "📋 ПУТЕВОЙ ЛИСТ"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when {
                            isMasterMode -> Color(0xFFF57C00)
                            isQuickEditMode -> Color(0xFF2196F3)
                            isConfirmed -> Color(0xFF2E7D32)
                            isInProgress -> Color(0xFFFF8C00)
                            isCancelled -> Color(0xFFD32F2F)
                            else -> Color.Gray
                        }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stats.takenCount}/${stats.totalCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = when {
                            stats.takenCount == stats.totalCount && !isCancelled -> Color(0xFF2E7D32)
                            stats.takenCount > 0 -> Color(0xFFFF8C00)
                            else -> Color.Gray
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (isFinished && !isMasterMode && !checkpointsEntered && !isQuickEditMode && mode != TeamCardMode.EDIT) {
                        TextButton(
                            onClick = onToggleQuickEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📝 ЗАПОЛНИТЬ",
                                fontSize = 11.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isQuickEditMode) {
                        TextButton(
                            onClick = onToggleQuickEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "💾 СОХРАНИТЬ",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isFinished && !isCancelled && routeCardEntries.isNotEmpty()) {
                // Ключевой момент: кластеры кликабельны в мастер-режиме ИЛИ в режиме быстрого редактирования
                val clustersEditable = isMasterMode || isQuickEditMode || mode == TeamCardMode.EDIT

                InteractiveCheckpointsGrid(
                    entries = routeCardEntries,
                    isEditable = clustersEditable,
                    onCycle = onCycleCheckpoint,
                    onTechnicalLongPress = onTechnicalLongPress,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TechnicalDetailsRow(entries = routeCardEntries)

                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isMasterMode -> {
                    Text(
                        text = "Редактирование с сохранением подписей. Нажмите 💾 СОХРАНИТЬ в верхней панели",
                        fontSize = 11.sp,
                        color = Color(0xFFF57C00)
                    )
                }
                isConfirmed -> {
                    Text(
                        text = "Подтверждён судьёй. Данные закрыты.",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                    TextButton(
                        onClick = onMasterUnlock,
                        modifier = Modifier.padding(start = 0.dp)
                    ) {
                        Text("🔓 Мастер-правка", fontSize = 11.sp, color = Color(0xFFF57C00))
                    }
                }
                isQuickEditMode -> {
                    Text(
                        text = "👆 Тап по кружку — взять КП | Долгий тап на тех. КП — детали",
                        fontSize = 10.sp,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onSecretarySign,
                            modifier = Modifier.weight(1f),
                            enabled = !isSecretarySigned,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSecretarySigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (isSecretarySigned) "✅ Подписано секретарём" else "📝 Подписать секретарём",
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onJudgeSign,
                            modifier = Modifier.weight(1f),
                            enabled = !isJudgeSigned,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isJudgeSigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(
                                text = if (isJudgeSigned) "✅ Подписано судьёй" else "⚖️ Подписать судьёй",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                isInProgress -> {
                    Text(
                        text = "Заполните отметки КП",
                        fontSize = 11.sp,
                        color = Color(0xFFFF8C00)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onSecretarySign,
                            modifier = Modifier.weight(1f),
                            enabled = !isSecretarySigned,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSecretarySigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (isSecretarySigned) "✅ Подписано секретарём" else "📝 Подписать секретарём",
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onJudgeSign,
                            modifier = Modifier.weight(1f),
                            enabled = !isJudgeSigned,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isJudgeSigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(
                                text = if (isJudgeSigned) "✅ Подписано судьёй" else "⚖️ Подписать судьёй",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                !isCancelled && isFinished && !checkpointsEntered -> {
                    Text(
                        text = "Путевой лист не заполнен. Нажмите \"ЗАПОЛНИТЬ\" для начала",
                        fontSize = 11.sp,
                        color = Color(0xFFFF8C00)
                    )
                }
                isCancelled -> {
                    Text(
                        text = if (status == "disqualified") "Команда снята" else "Команда потеряна",
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveCheckpointCluster(
    checkpoint: RouteCardEntryUi,
    isEditable: Boolean,
    onCycle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val state = when {
        !checkpoint.taken -> "NOT_TAKEN"
        checkpoint.taken && checkpoint.takenWithError -> "TAKEN_ERROR"
        else -> "TAKEN_OK"
    }

    val targetColor = when (state) {
        "NOT_TAKEN" -> Color(0xFF9E9E9E)
        "TAKEN_OK" -> if (checkpoint.type == "technical") Color(0xFFF57C00) else Color(0xFF00A86B)
        "TAKEN_ERROR" -> Color(0xFF9E9E9E)
        else -> Color(0xFF9E9E9E)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 150),
        label = "checkpointColor"
    )

    val icon = if (state == "TAKEN_ERROR") "❌" else ""
    val showStar = state == "TAKEN_OK" && checkpoint.type == "technical"

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(animatedColor)
                .pointerInput(isEditable, checkpoint.type) {
                    detectTapGestures(
                        onTap = {
                            if (isEditable) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCycle()
                            }
                        },
                        onLongPress = {
                            if (isEditable && checkpoint.type == "technical") {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (icon.isNotEmpty()) {
                Text(icon, fontSize = 20.sp, color = Color.White)
            } else {
                Text(
                    text = checkpoint.displayNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        if (showStar) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-6).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⭐",
                    fontSize = 15.sp,
                    color = Color.Yellow
                )
            }
        }
    }
}

@Composable
fun InteractiveCheckpointsGrid(
    entries: List<RouteCardEntryUi>,
    isEditable: Boolean,
    onCycle: (Long) -> Unit,
    onTechnicalLongPress: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = entries.chunked(6)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { checkpoint ->
                    InteractiveCheckpointCluster(
                        checkpoint = checkpoint,
                        isEditable = isEditable,
                        onCycle = { onCycle(checkpoint.checkpointId) },
                        onLongPress = { onTechnicalLongPress(checkpoint.checkpointId) },
                        modifier = Modifier.size(56.dp)
                    )
                }
                repeat(6 - row.size) {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}

@Composable
fun TechnicalDetailsRow(
    entries: List<RouteCardEntryUi>,
    modifier: Modifier = Modifier
) {
    val technicalWithData = entries.filter { cp ->
        cp.type == "technical" && (cp.offsetTime.isNotBlank() || cp.penalty != 0)
    }

    if (technicalWithData.isEmpty()) return

    val detailsList = mutableListOf<String>()
    var totalOffsetSeconds = 0

    technicalWithData.forEach { cp ->
        val parts = mutableListOf<String>()

        if (cp.offsetTime.isNotBlank()) {
            val seconds = parseOffsetTimeToSeconds(cp.offsetTime)
            if (seconds != null) {
                parts.add("${seconds} сек")
                totalOffsetSeconds += seconds
            } else {
                parts.add(cp.offsetTime)
            }
        }

        if (cp.penalty != 0) {
            val sign = if (cp.penalty > 0) "-" else "+"
            parts.add("${sign}${kotlin.math.abs(cp.penalty)}Б")
        }

        if (parts.isNotEmpty()) {
            detailsList.add("⭐ КП-${cp.displayNumber} (${parts.joinToString(", ")})")
        }
    }

    val totalMinutes = totalOffsetSeconds / 60
    val totalSeconds = totalOffsetSeconds % 60
    val totalFormatted = String.format("%02d мин %02d сек", totalMinutes, totalSeconds)

    val detailsText = detailsList.joinToString(" | ") + " ||| ∑ $totalFormatted"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = detailsText,
            modifier = Modifier.padding(12.dp),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp
        )
    }
}

private fun parseOffsetTimeToSeconds(offsetTime: String): Int? {
    if (offsetTime.isBlank()) return null
    val parts = offsetTime.split(":")
    if (parts.size != 2) return null
    val minutes = parts[0].toIntOrNull() ?: return null
    val seconds = parts[1].toIntOrNull() ?: return null
    return if (seconds in 0..59) minutes * 60 + seconds else null
}

@Composable
fun TechnicalCheckpointDialog(
    checkpoint: RouteCardEntryUi,
    onSave: (offsetTime: String, penalty: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var offsetTimeField by remember {
        mutableStateOf(
            TextFieldValue(
                text = checkpoint.offsetTime,
                selection = TextRange(checkpoint.offsetTime.length)
            )
        )
    }
    var penalty by remember { mutableStateOf(checkpoint.penalty.toString()) }

    var offsetTimeError by remember { mutableStateOf<String?>(null) }
    var penaltyError by remember { mutableStateOf<String?>(null) }

    // Функция валидации времени
    fun validateOffsetTime(time: String): Boolean {
        if (time.isBlank()) return true  // Пустое поле допустимо
        val pattern = Regex("^([0-5]?[0-9]):([0-5][0-9])$")
        if (!pattern.matches(time)) {
            offsetTimeError = "Используйте формат ММ:СС (00:00 - 59:59)"
            return false
        }
        offsetTimeError = null
        return true
    }

    // Функция валидации штрафа
    fun validatePenalty(penaltyStr: String): Boolean {
        if (penaltyStr.isBlank()) {
            penaltyError = null
            return true
        }
        val value = penaltyStr.toIntOrNull()
        if (value == null) {
            penaltyError = "Введите целое число"
            return false
        }
        if (value < -999 || value > 999) {
            penaltyError = "Штраф должен быть от -999 до 999"
            return false
        }
        penaltyError = null
        return true
    }

    // Проверяем, можно ли сохранять
    val isOffsetTimeValid = validateOffsetTime(offsetTimeField.text)
    val isPenaltyValid = validatePenalty(penalty)
    val isSaveEnabled = isOffsetTimeValid && isPenaltyValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "КП-${checkpoint.displayNumber} (технический)",
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = offsetTimeField,
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.text.filter { it.isDigit() }
                        val trimmed = digitsOnly.take(4)
                        val formatted = when {
                            trimmed.isEmpty() -> ""
                            trimmed.length <= 2 -> trimmed
                            else -> "${trimmed.take(2)}:${trimmed.drop(2).take(2)}"
                        }
                        offsetTimeField = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                        offsetTimeError = null
                    },
                    label = {
                        Text(
                            "Отсечка (ММ:СС)",
                            fontSize = 14.sp
                        )
                    },
                    placeholder = {
                        Text(
                            "мм:сс",
                            fontSize = 14.sp,
                            color = Color.Gray.copy(alpha = 0.4f)
                        )
                    },
                    isError = offsetTimeError != null,
                    supportingText = {
                        Column {
                            if (offsetTimeError != null) {
                                Text(
                                    text = offsetTimeError!!,
                                    fontSize = 11.sp,
                                    color = Color.Red
                                )
                            } else {
                                Text(
                                    text = "Пример: 01:30 (пустое поле = без отсечки)",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace  // моноширинный шрифт для времени
                    )
                )

                OutlinedTextField(
                    value = penalty,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '-' } && newValue.length <= 5) {
                            penalty = newValue
                            penaltyError = null
                        }
                    },
                    label = {
                        Text(
                            "Штраф (баллы)",
                            fontSize = 16.sp
                        )
                    },
                    placeholder = {
                        Text(
                            "5 или -3",
                            fontSize = 18.sp
                        )
                    },
                    isError = penaltyError != null,
                    supportingText = {
                        if (penaltyError != null) {
                            Text(
                                text = penaltyError!!,
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        } else {
                            Text(
                                text = "Положительный = бонус, отрицательный = штраф",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)
                )

                Text(
                    text = "💡 Отсечка всегда вычитается из времени. Формат: ММ:СС (минуты:секунды)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val penaltyInt = penalty.toIntOrNull() ?: 0
                    onSave(offsetTimeField.text, penaltyInt)
                },
                enabled = isSaveEnabled
            ) {
                Text(
                    "💾 СОХРАНИТЬ",
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Отмена",
                    fontSize = 16.sp
                )
            }
        }
    )
}

@Composable
fun MemberCard(
    member: MemberUi,
    isCaptain: Boolean,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onAssignMentor: () -> Unit,
    canEdit: Boolean
) {
    val ageColor = when {
        member.age != null && member.age < 14 -> Color(0xFFD32F2F)
        member.age != null && member.age in 14..17 -> Color(0xFFFF8C00)
        else -> Color(0xFF00A86B)
    }

    val needsMentor = member.age != null && member.age < 18 && !member.mentorConfirmed

    Card(
        modifier = Modifier.fillMaxWidth()
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
                                text = "${member.age} лет",
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

            if (canEdit) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReplace,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("🔄 Заменить", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("❌ Исключить", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReplacedHistorySection(
    replacedMembers: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    if (replacedMembers.isEmpty()) return

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔄 История замен (${replacedMembers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = if (isExpanded) "▲" else "▼",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (isExpanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    replacedMembers.forEach { name ->
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
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
        "lost" -> "Потеряна"
        "disqualified" -> "Снята"
        else -> status
    }
}

private fun statusToColor(status: String): Color {
    return when (status) {
        "registered" -> Color(0xFF00A86B)
        "started" -> Color(0xFFFF8C00)
        "finished" -> Color(0xFF2E7D32)
        else -> Color.Gray
    }
}

private fun colorMarkToColor(colorMark: String): Color {
    return when {
        colorMark.contains("!!!") -> Color(0xFFD32F2F)
        colorMark == "СНЯТЫ" -> Color(0xFFD32F2F)
        colorMark == "18+" -> Color(0xFF00A86B)
        else -> Color(0xFFFFC107)
    }
}