package com.speleo.start.presentation.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.presentation.Screen
import kotlinx.coroutines.launch

// Data class для пункта меню
data class MenuTile(
    val icon: String,
    val title: String,
    val subtitle: String? = null,
    val route: Screen? = null,
    val alwaysEnabled: Boolean = false
)

// Список пунктов меню (subtitle будут обновлены динамически)
val MenuTiles = listOf(
    MenuTile("📋", "Соревнования", "Управление", Screen.Competitions, alwaysEnabled = true),
    MenuTile("📄", "Путевые", "0/0", null, alwaysEnabled = false),
    MenuTile("⏱️", "Старт", "Очередь: 0", Screen.Start, alwaysEnabled = false),
    MenuTile("🏁", "Финиш", "На дист.: 0", Screen.Finish, alwaysEnabled = false),
    MenuTile("📝", "Регистрация", "Создание команд", Screen.Register, alwaysEnabled = false),
    MenuTile("📋", "Команды", "Список", Screen.TeamList, alwaysEnabled = false),
    MenuTile("👤", "Персоны", "База", Screen.Persons, alwaysEnabled = true),
    MenuTile("🏆", "Итоги", "Результаты", Screen.Results, alwaysEnabled = false),
    MenuTile("⚙️", "Настройки", null, Screen.Settings, alwaysEnabled = true),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    onNavigateToRouteCard: (Long) -> Unit, // ← НОВЫЙ ПАРАМЕТР
    homeVM: HomeVM = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.screenWidthDp >= 600) 3 else 2
    var showDevMenu by remember { mutableStateOf(false) }
    var showStopTimerDialog by remember { mutableStateOf(false) }
    var stopTimerPassword by remember { mutableStateOf("") }
    var showTeamPickerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val competitionName by homeVM.competitionName.collectAsStateWithLifecycle()
    val hasActiveCompetition by homeVM.hasActiveCompetition.collectAsStateWithLifecycle()
    val isArchived by homeVM.isArchived.collectAsStateWithLifecycle()
    val mainTimer by homeVM.mainTimer.collectAsStateWithLifecycle()
    val stats by homeVM.stats.collectAsStateWithLifecycle()
    val finishedTeams by homeVM.finishedTeams.collectAsStateWithLifecycle()
    val pendingRouteCards by homeVM.pendingRouteCards.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(hasActiveCompetition, isArchived) {
        if (hasActiveCompetition && !isArchived) {
            homeVM.restoreTimer()
            homeVM.loadFinishedTeams()
        } else if (isArchived) {
            homeVM.stopTimer()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (competitionName.isNotBlank()) competitionName else "🏆 СпелеоСтарт",
                        fontSize = 18.sp
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(
                start = 8.dp, end = 8.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = 8.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                when {
                    !hasActiveCompetition -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚠️", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Нет активного соревнования",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Выберите или создайте соревнование",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigate(Screen.Competitions) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ВЫБРАТЬ СОРЕВНОВАНИЕ")
                                }
                            }
                        }
                    }
                    isArchived -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📦", fontSize = 24.sp)
                                    Text(competitionName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("АРХИВ", fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Режим просмотра. Новые старты и финиши недоступны.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onNavigate(Screen.Competitions) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ВЫБРАТЬ ДРУГОЕ СОРЕВНОВАНИЕ")
                                }
                            }
                        }
                    }
                    else -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(competitionName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("🟢 Активно", color = Color(0xFF00A86B), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                MainTimerDisplay(
                                    mainTimer = mainTimer,
                                    onLongPress = { showStopTimerDialog = true }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${stats.onCourse}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("🏃 На трассе", fontSize = 10.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${stats.finished}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("🏁 Финиш", fontSize = 10.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${stats.disqualified}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("⚠️ DQ", fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onNavigate(Screen.Start) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text("⏱️ Старт", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onNavigate(Screen.Finish) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text("🏁 Финиш", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(MenuTiles) { tile ->
                // Динамическое обновление subtitle для плитки "Путевые"
                val dynamicSubtitle = when {
                    tile.title == "Путевые" && pendingRouteCards.second > 0 ->
                        "${pendingRouteCards.first}/${pendingRouteCards.second}"
                    tile.title == "Путевые" -> "0/0"
                    else -> tile.subtitle
                }

                MenuTileCard(
                    tile = tile.copy(subtitle = dynamicSubtitle),
                    onNavigate = onNavigate,
                    onNavigateToRouteCard = onNavigateToRouteCard,
                    onLongClick = if (tile.route == Screen.Settings) {{ showDevMenu = true }} else null,
                    onTileClick = { clickedTile ->
                        when {
                            clickedTile.title == "Путевые" && hasActiveCompetition && !isArchived -> {
                                if (pendingRouteCards.first > 0) {
                                    showTeamPickerDialog = true
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Нет ожидающих путевых листов")
                                    }
                                }
                            }
                            clickedTile.route != null -> onNavigate(clickedTile.route)
                        }
                    },
                    hasActiveCompetition = hasActiveCompetition,
                    isArchived = isArchived,
                    pendingCount = pendingRouteCards.first,
                    totalCount = pendingRouteCards.second
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { homeVM.exportData() }, modifier = Modifier.weight(1f)) {
                        Text("📤 Экспорт", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { homeVM.importData() }, modifier = Modifier.weight(1f)) {
                        Text("📥 Импорт", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showDevMenu) {
        AlertDialog(
            onDismissRequest = { showDevMenu = false },
            title = { Text("🛠 Меню разработчика") },
            text = {
                Column {
                    TextButton(onClick = {
                        homeVM.generateTestData()
                        showDevMenu = false
                    }) { Text("🧪 Заполнить тестовыми") }
                    TextButton(onClick = {
                        homeVM.clearTestData()
                        showDevMenu = false
                    }) { Text("🗑️ Очистить тестовые") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevMenu = false }) { Text("Закрыть") }
            }
        )
    }

    if (showStopTimerDialog) {
        AlertDialog(
            onDismissRequest = {
                showStopTimerDialog = false
                stopTimerPassword = ""
            },
            title = { Text("Остановка главного таймера") },
            text = {
                Column {
                    Text("Вы действительно хотите остановить главный таймер?")
                    Text("Это действие требует пароля судьи.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stopTimerPassword,
                        onValueChange = { stopTimerPassword = it },
                        label = { Text("Пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (homeVM.stopMainTimer(stopTimerPassword)) {
                            showStopTimerDialog = false
                            stopTimerPassword = ""
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Неверный пароль")
                            }
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
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог выбора команды для путевого листа
    if (showTeamPickerDialog) {
        AlertDialog(
            onDismissRequest = { showTeamPickerDialog = false },
            title = { Text("Выбор команды") },
            text = {
                Column {
                    Text("Выберите команду для заполнения путевого листа:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (finishedTeams.isEmpty()) {
                        Text("Нет команд с незаполненными путевыми листами", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            finishedTeams.forEach { team ->
                                Button(
                                    onClick = {
                                        onNavigateToRouteCard(team.id) // ← ИСПОЛЬЗУЕМ НОВЫЙ CALLBACK
                                        showTeamPickerDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("№${team.number} (${team.className}-й класс)", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTeamPickerDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTimerDisplay(
    mainTimer: Long,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalSec = mainTimer / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val hundredths = (mainTimer % 1000) / 10

    val baseSize = 36.sp
    val smallSize = 24.sp
    val timerColor = Color(0xFF00C853)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { },
            onLongClick = onLongPress
        ),
        verticalAlignment = Alignment.Bottom
    ) {
        if (hours > 0) {
            Text(
                text = "%02d:".format(hours),
                fontSize = baseSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
        } else {
            Text(
                text = "00:",
                fontSize = smallSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = timerColor.copy(alpha = 0.5f)
            )
        }
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            fontSize = baseSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = timerColor
        )
        Text(
            text = ".%02d".format(hundredths),
            fontSize = smallSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = timerColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuTileCard(
    tile: MenuTile,
    onNavigate: (Screen) -> Unit,
    onNavigateToRouteCard: (Long) -> Unit,
    onLongClick: (() -> Unit)? = null,
    onTileClick: (MenuTile) -> Unit,
    hasActiveCompetition: Boolean = false,
    isArchived: Boolean = false,
    pendingCount: Int = 0,
    totalCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }

    val enabled = when {
        tile.alwaysEnabled -> true
        isArchived && tile.route == Screen.Results -> true
        isArchived -> false
        !hasActiveCompetition -> false
        else -> true
    }

    // Цвет subtitle для плитки "Путевые"
    val subtitleColor = when {
        tile.title == "Путевые" && pendingCount > 0 && pendingCount == totalCount -> Color(0xFF00A86B)
        tile.title == "Путевые" && pendingCount > 0 -> Color(0xFFFF8C00)
        tile.title == "Путевые" -> Color.Gray
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (enabled) {
                            onTileClick(tile)
                        }
                    },
                    onLongClick = { onLongClick?.invoke() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = tile.icon,
                fontSize = 28.sp,
                color = if (enabled) Color.Unspecified else Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tile.title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (enabled) Color.Unspecified else Color.Gray
            )
            if (tile.subtitle != null) {
                Text(
                    text = tile.subtitle,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = subtitleColor
                )
            }
        }
    }
}