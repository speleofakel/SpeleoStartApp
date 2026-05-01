package com.speleo.start.presentation.screen.competitions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.data.local.entity.MasterRouteCardEntity
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompetitionSettingsScreen(
    competitionId: Long,
    onBack: () -> Unit,
    onCheckpoints: (Long) -> Unit,
    vm: CompetitionSettingsVM = hiltViewModel()
) {
    val competitionName by vm.competitionName.collectAsStateWithLifecycle()
    val startInterval by vm.startInterval.collectAsStateWithLifecycle()
    val controlTime2 by vm.controlTime2.collectAsStateWithLifecycle()
    val controlTime3 by vm.controlTime3.collectAsStateWithLifecycle()
    val minTeamSize by vm.minTeamSize.collectAsStateWithLifecycle()
    val checkpoints by vm.checkpoints.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val editingCheckpoint by vm.editingCheckpoint.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Загрузка при входе
    LaunchedEffect(competitionId) {
        vm.loadCompetition(competitionId)
    }

    // Обработка событий
    LaunchedEffect(Unit) {
        vm.event.collectLatest { event ->
            when (event) {
                is CompetitionSettingsVM.SettingsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is CompetitionSettingsVM.SettingsUiEvent.NavigateBack -> {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки: $competitionName",
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { vm.saveSettings() }) {
                        Text("💾 Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══════════════════════════════════════
            // БЛОК: КОНТРОЛЬНЫЕ ПУНКТЫ
            // ═══════════════════════════════════════
            BlockTitle(icon = "🗺️", title = "КОНТРОЛЬНЫЕ ПУНКТЫ")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (checkpoints.isEmpty()) {
                        // Состояние «Нет КП»
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Нет контрольных пунктов.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Нажмите кнопку ниже, чтобы добавить.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { onCheckpoints(competitionId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("➕ КП")
                            }
                        }
                    } else {
                        // КЛАСС 2
                        CheckpointClassRow(
                            label = "2",
                            checkpoints = checkpoints.filter { it.forClass2 },
                            onCheckpointClick = { vm.editCheckpoint(it) }
                        )

                        // КЛАСС 3
                        CheckpointClassRow(
                            label = "3",
                            checkpoints = checkpoints.filter { it.forClass3 },
                            onCheckpointClick = { vm.editCheckpoint(it) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = { onCheckpoints(competitionId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("✏️ Редактировать КП")
                        }
                    }
                }
            }

            HorizontalDivider()

            // ═══════════════════════════════════════
            // БЛОК: ПАРАМЕТРЫ СОРЕВНОВАНИЯ
            // ═══════════════════════════════════════
            BlockTitle(icon = "⚙️", title = "ПАРАМЕТРЫ СОРЕВНОВАНИЯ")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsNumberField(
                        value = startInterval,
                        onValueChange = { vm.updateInterval(it) },
                        label = "Интервал старта (секунды)"
                    )

                    SettingsNumberField(
                        value = controlTime2,
                        onValueChange = { vm.updateControlTime2(it) },
                        label = "Контрольное время (2-й класс, минуты)"
                    )

                    SettingsNumberField(
                        value = controlTime3,
                        onValueChange = { vm.updateControlTime3(it) },
                        label = "Контрольное время (3-й класс, минуты)"
                    )

                    SettingsNumberField(
                        value = minTeamSize,
                        onValueChange = { vm.updateMinTeamSize(it) },
                        label = "Минимальный состав команды"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { vm.saveSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("СОХРАНИТЬ ВСЕ НАСТРОЙКИ")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ═══════════════════════════════════════
    // ДИАЛОГ РЕДАКТИРОВАНИЯ КП
    // ═══════════════════════════════════════
    editingCheckpoint?.let { cp ->
        CheckpointEditDialog(
            checkpoint = cp,
            onDismiss = { vm.closeEditDialog() },
            onTypeChange = { newType ->
                vm.updateCheckpointType(cp.id, newType)
            }
        )
    }
}

// ═══════════════════════════════════════════
// КОМПОНЕНТЫ
// ═══════════════════════════════════════════

@Composable
private fun BlockTitle(icon: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckpointClassRow(
    label: String,
    checkpoints: List<MasterRouteCardEntity>,
    onCheckpointClick: (MasterRouteCardEntity) -> Unit
) {
    if (checkpoints.isEmpty()) return

    Box(modifier = Modifier.fillMaxWidth()) {
        // Рамка с КП
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp), // Место для круглого label
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                checkpoints.forEach { cp ->
                    CheckpointCluster(
                        checkpoint = cp,
                        onClick = { onCheckpointClick(cp) }
                    )
                }
            }
        }

        // Круглый label с подсветкой и обводкой
        // Центр совпадает с пересечением границ обводки
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-10).dp, y = (3).dp) // Половина размера круга
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CheckpointCluster(
    checkpoint: MasterRouteCardEntity,
    onClick: () -> Unit
) {
    val backgroundColor = when (checkpoint.type) {
        "technical" -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = checkpoint.displayNumber.toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingsNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Только цифры, макс 4 символа
            val filtered = newValue.filter { it.isDigit() }.take(4)
            onValueChange(filtered)
        },
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
    )
}

@Composable
private fun CheckpointEditDialog(
    checkpoint: MasterRouteCardEntity,
    onDismiss: () -> Unit,
    onTypeChange: (String) -> Unit
) {
    val isTechnical = checkpoint.type == "technical"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("КП №${checkpoint.displayNumber}")
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Вес (только для чтения)
                OutlinedTextField(
                    value = checkpoint.displayNumber.toString(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Вес") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Тип: Обычный / Технический
                Text("Тип:", fontSize = 14.sp, fontWeight = FontWeight.Medium)

                // Используем Row с весами напрямую здесь
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Обычный
                    val normalBg = if (!isTechnical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val normalContent = if (!isTechnical) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                    OutlinedButton(
                        onClick = { onTypeChange("normal") },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = normalBg,
                            contentColor = normalContent
                        )
                    ) {
                        Text("Обычный", fontSize = 13.sp)
                    }

                    // Технический
                    val techBg = if (isTechnical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val techContent = if (isTechnical) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                    OutlinedButton(
                        onClick = { onTypeChange("technical") },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = techBg,
                            contentColor = techContent
                        )
                    ) {
                        Text("Технический", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun TypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Text(text, fontSize = 13.sp)
    }
}