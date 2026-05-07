package com.speleo.start.presentation.screen.teamcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// ОСНОВНАЯ ИНФОРМАЦИЯ О КОМАНДЕ
// ============================================================

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

// ============================================================
// ВРЕМЕНА СТАРТА/ФИНИША (с кнопкой ✏️)
// ============================================================

@Composable
fun TimesCard(
    startTime: String,
    finishTime: String,
    status: String,
    onFinishTimeEdit: () -> Unit
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
            // СТАРТ
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

            // ФИНИШ с кнопкой редактирования
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ФИНИШ", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = finishTime,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            finishTime == "—:—:—" -> Color.Gray
                            status == "finished" -> Color(0xFF2196F3)
                            else -> Color(0xFF9E9E9E)
                        }
                    )
                }

                // Кнопка редактирования (только для finished команд)
                if (status == "finished" && finishTime != "—:—:—") {
                    IconButton(
                        onClick = onFinishTimeEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✏️", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ============================================================
// СТАТУС ПУТЕВОГО ЛИСТА
// ============================================================

@Composable
fun RouteCardStatusCard(
    stats: RouteCardStats,
    checkpointsEntered: Boolean,
    status: String,
    isMasterMode: Boolean,
    isSecretarySigned: Boolean,
    isJudgeSigned: Boolean,
    onMasterUnlock: () -> Unit,
    onFillRouteCard: () -> Unit,
    onSecretarySign: () -> Unit,
    onJudgeSign: () -> Unit,
    mode: TeamCardMode
) {
    val isFinished = status == "finished"
    val isCancelled = status == "disqualified" || status == "lost"
    val isConfirmed = checkpointsEntered && !isMasterMode
    val isInProgress = isFinished && !checkpointsEntered && !isCancelled && mode == TeamCardMode.EDIT

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
                            isConfirmed -> "✅ ПУТЕВОЙ ЛИСТ"
                            isInProgress -> "📝 ПУТЕВОЙ ЛИСТ"
                            isCancelled -> "❌ ПУТЕВОЙ ЛИСТ"
                            else -> "📋 ПУТЕВОЙ ЛИСТ"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = when {
                            isMasterMode -> Color(0xFFF57C00)
                            isConfirmed -> Color(0xFF2E7D32)
                            isInProgress -> Color(0xFFFF8C00)
                            isCancelled -> Color(0xFFD32F2F)
                            else -> Color.Gray
                        }
                    )
                }
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isMasterMode -> {
                    Text(
                        text = "Редактирование с сохранением подписей",
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
                            enabled = !isJudgeSigned && isSecretarySigned,
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
                !isCancelled && isFinished -> {
                    Text(
                        text = "Не заполнен",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Button(
                        onClick = onFillRouteCard,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("📝 ЗАПОЛНИТЬ ПУТЕВОЙ ЛИСТ", fontSize = 12.sp)
                    }
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

// ============================================================
// ВИЗУАЛИЗАЦИЯ КП (КЛАСТЕРЫ)
// ============================================================

@Composable
fun CheckpointsClusterGrid(
    entries: List<RouteCardEntryUi>,
    onCheckpointClick: ((RouteCardEntryUi) -> Unit)? = null
) {
    val chunked = entries.chunked(6)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { cp ->
                    CheckpointCircle(
                        checkpoint = cp,
                        onClick = { onCheckpointClick?.invoke(cp) }
                    )
                }
                repeat(6 - row.size) {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckpointCircle(
    checkpoint: RouteCardEntryUi,
    onClick: () -> Unit
) {
    val color = when {
        checkpoint.taken && checkpoint.takenWithError -> Color(0xFFFF8C00)
        checkpoint.taken -> Color(0xFF00A86B)
        else -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${checkpoint.displayNumber}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (checkpoint.type == "technical" && checkpoint.taken && !checkpoint.takenWithError) {
            Text(
                text = "*",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

// ============================================================
// СПИСОК КП ДЛЯ РЕДАКТИРОВАНИЯ
// ============================================================

@Composable
fun EditableCheckpointList(
    entries: List<RouteCardEntryUi>,
    onTakenChange: (Long, Boolean) -> Unit,
    onErrorChange: (Long, Boolean) -> Unit,
    onOffsetTimeChange: (Long, String) -> Unit,
    onPenaltyChange: (Long, Int) -> Unit,
    isEditable: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        entries.forEach { entry ->
            EditableCheckpointRow(
                entry = entry,
                onTakenChange = { onTakenChange(entry.checkpointId, it) },
                onErrorChange = { onErrorChange(entry.checkpointId, it) },
                onOffsetTimeChange = { onOffsetTimeChange(entry.checkpointId, it) },
                onPenaltyChange = { onPenaltyChange(entry.checkpointId, it) },
                isEditable = isEditable
            )
        }
    }
}

@Composable
private fun EditableCheckpointRow(
    entry: RouteCardEntryUi,
    onTakenChange: (Boolean) -> Unit,
    onErrorChange: (Boolean) -> Unit,
    onOffsetTimeChange: (String) -> Unit,
    onPenaltyChange: (Int) -> Unit,
    isEditable: Boolean
) {
    val isTechnical = entry.type == "technical"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTechnical) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "№${entry.displayNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(40.dp)
                )

                Text(
                    text = "Вес:${entry.weight}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(60.dp)
                )

                Button(
                    onClick = { onTakenChange(!entry.taken) },
                    enabled = isEditable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entry.taken) Color(0xFF00A86B) else Color.Gray
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("ok", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = { onErrorChange(!entry.takenWithError) },
                    enabled = isEditable && entry.taken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entry.takenWithError) Color(0xFFFF8C00) else Color.Gray
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Ошибка", color = Color.White, fontSize = 12.sp)
                }
            }

            if (isTechnical) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = entry.offsetTime,
                        onValueChange = onOffsetTimeChange,
                        label = { Text("Отсечка (ММ:СС)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = isEditable && entry.taken
                    )

                    OutlinedTextField(
                        value = entry.penalty.toString(),
                        onValueChange = { it.toIntOrNull()?.let(onPenaltyChange) },
                        label = { Text("Штраф") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = isEditable && entry.taken
                    )
                }
            }
        }
    }
}

// ============================================================
// УЧАСТНИК
// ============================================================

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

    val needsMentor = member.age != null && member.age < 18 && member.mentorName == null

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

// ============================================================
// ИСТОРИЯ ЗАМЕН
// ============================================================

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

// ============================================================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ============================================================

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