package com.speleo.start.presentation.screen.start

import android.media.ToneGenerator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.presentation.component.NoActiveCompetitionScreen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    onBack: () -> Unit,
    onNavigateToCompetitions: () -> Unit = {},
    onNavigateToTeamCard: (Long) -> Unit = {},
    onNavigateToFinish: () -> Unit = {},
    vm: StartVM = hiltViewModel()
) {
    val mainTimer by vm.timer.mainTimer.collectAsStateWithLifecycle()
    val countdown by vm.timer.countdown.collectAsStateWithLifecycle()
    val countdownPaused by vm.timer.countdownPaused.collectAsStateWithLifecycle()
    val started by vm.timer.started.collectAsStateWithLifecycle()
    val isFirstStart by vm.timer.isFirstStart.collectAsStateWithLifecycle()
    val queue by vm.queue.collectAsStateWithLifecycle()
    val hasActiveCompetition by vm.hasActiveCompetition.collectAsStateWithLifecycle()
    val validationError by vm.validationError.collectAsStateWithLifecycle()
    val startInterval by vm.startInterval.collectAsStateWithLifecycle()
    val navigateToTeamCard by vm.navigateToTeamCard.collectAsStateWithLifecycle()

    var showStopTimerDialog by remember { mutableStateOf(false) }
    var stopTimerPassword by remember { mutableStateOf("") }
    var stopTimerError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadQueue() }
    LaunchedEffect(queue) { vm.clearValidationError() }

    // Обработка окончания обратного отсчёта
    LaunchedEffect(Unit) {
        vm.event.collectLatest { event ->
            when (event) {
                StartEvent.CountdownFinished -> {
                    vm.startCurrentTeam()
                }
            }
        }
    }

    LaunchedEffect(navigateToTeamCard) {
        navigateToTeamCard?.let {
            onNavigateToTeamCard(it)
            vm.onTeamCardNavigated()
        }
    }

    // Освобождаем звук при выходе
    DisposableEffect(Unit) {
        onDispose {
            vm.timer.releaseToneGenerator()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("СТАРТ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("◀", fontSize = 18.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            if (started) {
                FloatingActionButton(
                    onClick = onNavigateToFinish,
                    shape = CircleShape,
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Text("🏁", fontSize = 24.sp)
                }
            }
        }
    ) { padding ->
        if (!hasActiveCompetition) {
            NoActiveCompetitionScreen(
                onNavigateToCompetitions = onNavigateToCompetitions,
                message = "Нет активного соревнования"
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Главный таймер (оригинальный)
                TimerDisplay(
                    mainTimer,
                    onLongPress = { showStopTimerDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Обратный отсчёт (оригинальный)
                if (started) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatCountdown(countdown),
                                fontSize = if (countdown < 60) 40.sp else 32.sp,
                                color = when {
                                    countdown <= 10 -> Color(0xFFD32F2F)
                                    countdown <= 30 -> Color(0xFFFF8C00)
                                    else -> Color(0xFFFFC107)
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            val nextTeam = queue.firstOrNull()
                            if (nextTeam != null) {
                                Text(
                                    text = "ДО СТАРТА: КОМАНДА #${nextTeam.number} (${nextTeam.className} КЛ)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (countdown > 10) {
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { vm.timer.toggleCountdownPause(canPause = true) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Text(
                                        if (countdownPaused) "▶ ПУСК ОТСЧЁТА" else "⏸ ПАУЗА ОТСЧЁТА",
                                        fontSize = 11.sp
                                    )
                                }
                            } else if (countdown <= 10 && countdown > 0) {
                                Text(
                                    text = "🔊 ЗВУКОВАЯ ИНДИКАЦИЯ АКТИВНА",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопка НАЧАТЬ СОРЕВНОВАНИЯ (оригинальная)
                if (!started) {
                    Button(
                        onClick = { vm.startCompetition() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("НАЧАТЬ СОРЕВНОВАНИЯ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Карточка текущей команды (оригинальная, с небольшими изменениями)
                val currentTeam = queue.firstOrNull()
                if (currentTeam != null && started) {
                    ActiveTeamCard(
                        team = currentTeam,
                        onDisqualify = { vm.disqualifyCurrentTeam() },
                        onSkip = { vm.skipCurrentTeam() },
                        onStart = { vm.startCurrentTeam() },
                        onNavigateToTeamCard = { vm.onTeamClick(currentTeam.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Карточка следующей команды (для отображения "ГОТОВИТСЯ")
                val nextTeamForCard = queue.getOrNull(1)
                if (nextTeamForCard != null && started) {
                    ReadyTeamCard(
                        team = nextTeamForCard,
                        onNavigateToTeamCard = { vm.onTeamClick(nextTeamForCard.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Ошибка валидации (оригинальная)
                if (validationError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "❌ ${validationError?.message}",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Заголовок очереди (оригинальный)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ОЧЕРЕДЬ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "2 кл ▼  3 кл ▼",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Очередь (оригинальная)
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(queue.drop(1), key = { it.id }) { team ->
                        QueueItem(
                            team = team,
                            index = queue.indexOf(team),
                            interval = startInterval,
                            onClick = { vm.onTeamClick(team.id) }
                        )
                    }
                }
            }
        }
    }

    // Диалог остановки таймера (оригинальный)
    if (showStopTimerDialog) {
        AlertDialog(
            onDismissRequest = {
                showStopTimerDialog = false
                stopTimerPassword = ""
                stopTimerError = null
            },
            title = { Text("Остановка главного таймера") },
            text = {
                Column {
                    Text("Вы действительно хотите остановить главный таймер?")
                    Text("Это действие требует пароля судьи.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stopTimerPassword,
                        onValueChange = {
                            stopTimerPassword = it
                            stopTimerError = null
                        },
                        label = { Text("Пароль") },
                        singleLine = true,
                        isError = stopTimerError != null,
                        supportingText = {
                            if (stopTimerError != null) {
                                Text(stopTimerError!!, fontSize = 10.sp, color = Color.Red)
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vm.timer.stopMainTimer(stopTimerPassword)) {
                            showStopTimerDialog = false
                            stopTimerPassword = ""
                            stopTimerError = null
                        } else {
                            stopTimerError = "Неверный пароль"
                            stopTimerPassword = ""
                        }
                    },
                    enabled = stopTimerPassword.isNotBlank()
                ) {
                    Text("ОСТАНОВИТЬ ТАЙМЕР")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStopTimerDialog = false
                    stopTimerPassword = ""
                    stopTimerError = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerDisplay(
    millis: Long,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {}
) {
    val totalSec = millis / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val hundredths = (millis % 1000) / 10

    val baseSize = 48.sp
    val smallSize = 28.sp
    val timerColor = Color(0xFF00C853)
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { },
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (hours > 0) {
                    Text(
                        "%02d:".format(hours),
                        fontSize = baseSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = timerColor
                    )
                } else {
                    Text(
                        "00:",
                        fontSize = smallSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = timerColor.copy(alpha = 0.4f)
                    )
                }
                Text(
                    "%02d:%02d".format(minutes, seconds),
                    fontSize = baseSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
                Text(
                    ".%02d".format(hundredths),
                    fontSize = smallSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }
            Text(
                "ГЛАВНЫЙ ТАЙМЕР (долгий тап → стоп/сброс)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveTeamCard(
    team: StartTeamInfo,
    onDisqualify: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
    onNavigateToTeamCard: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onNavigateToTeamCard
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "#${team.number}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                "НА СТАРТЕ: КОМАНДА #${team.number}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "${team.className}-й класс · ${team.memberCount} участника",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val (markColor, markText) = when (team.colorMark) {
                        "18+" -> Color(0xFF00A86B) to "18+"
                        "14+" -> Color(0xFFFFC107) to "14+"
                        "<14ок" -> Color(0xFFFFC107) to "<14ок"
                        "<17 !!!" -> Color(0xFFFF8C00) to "<17 !!!"
                        "<14 !!!" -> Color(0xFFFF8C00) to "<14 !!!"
                        "СНЯТЫ" -> Color(0xFFD32F2F) to "СНЯТЫ"
                        else -> Color(0xFF00A86B) to "18+"
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = markColor),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            markText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = if (markColor == Color(0xFFFFC107)) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                team.members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "● ${member.lastName} ${member.firstName.firstOrNull()?.plus(".") ?: ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (member.role == "капитан") {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        "КАП",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (member.age < 18) {
                            val mentorStatus = when {
                                member.mentorName == null -> "⚠️ Нет ментора"
                                !member.mentorConfirmed -> "⚠️ Ментор не подтверждён"
                                else -> "✓ ${member.mentorName}"
                            }
                            Text(
                                mentorStatus,
                                fontSize = 10.sp,
                                color = if (member.mentorConfirmed) Color(0xFF00A86B) else Color(0xFFFF8C00)
                            )
                        } else {
                            Text(
                                "${member.age} лет",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (team.hasMentorIssues) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "⚠️ Требуется подтверждение ментора для участников <18",
                        fontSize = 11.sp,
                        color = Color(0xFFFF8C00),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDisqualify,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("СНЯТЬ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("ПРОПУСК", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("СТАРТ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// НОВЫЙ КОМПОНЕНТ: карточка готовящейся команды
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadyTeamCard(
    team: StartTeamInfo,
    onNavigateToTeamCard: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onNavigateToTeamCard
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "#${team.number}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            "ГОТОВИТСЯ: КОМАНДА #${team.number}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${team.className}-й класс · ${team.memberCount} участника",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Показываем только первых двух участников для краткости
            team.members.take(2).forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "● ${member.lastName} ${member.firstName.firstOrNull()?.plus(".") ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (member.age < 18) {
                        val mentorStatus = when {
                            member.mentorName == null -> "⚠️"
                            !member.mentorConfirmed -> "⚠️"
                            else -> "✓"
                        }
                        Text(mentorStatus, fontSize = 10.sp, color = if (member.mentorConfirmed) Color(0xFF00A86B) else Color(0xFFFF8C00))
                    } else {
                        Text("${member.age} лет", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (team.members.size > 2) {
                Text(
                    "... и ещё ${team.members.size - 2}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueItem(
    team: StartTeamInfo,
    index: Int,
    interval: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val estimatedSec = index * interval

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "☰",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 6.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "#${team.number}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        "Команда #${team.number}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${team.className}-й класс · ${team.memberCount} участника",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                if (estimatedSec < 60) "~${estimatedSec}с" else "~${estimatedSec / 60}:${String.format("%02d", estimatedSec % 60)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatCountdown(seconds: Int): String {
    return if (seconds < 60) {
        "%02d".format(seconds)
    } else {
        "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}