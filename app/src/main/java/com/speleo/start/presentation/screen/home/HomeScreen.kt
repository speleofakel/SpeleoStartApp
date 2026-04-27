package com.speleo.start.presentation.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.screenWidthDp >= 600) 3 else 2
    var showDevMenu by remember { mutableStateOf(false) }
    val homeVM: HomeVM = hiltViewModel()
    val competitionName by homeVM.competitionName.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(competitionName.ifBlank { "🏆 СпелеоСтарт" }, fontSize = 18.sp) })
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
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(competitionName.ifBlank { "Нет активного соревнования" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("🟢 Активно", color = Color(0xFF00A86B), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("01:24:36.12", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Color(0xFF00C853))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("6", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("🏃 На трассе", fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("12", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("🏁 Финиш", fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("1", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("⚠️ DQ", fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onNavigate(Screen.Start) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                                Text("⏱️ Старт", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { onNavigate(Screen.Finish) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                                Text("🏁 Финиш", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            items(MenuTiles) { tile ->
                MenuTileCard(
                    tile = tile,
                    onNavigate = onNavigate,
                    onLongClick = if (tile.route == Screen.Settings) {{ showDevMenu = true }} else null
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                        Text("📤 Экспорт", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuTileCard(
    tile: MenuTile,
    onNavigate: (Screen) -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { tile.route?.let { onNavigate(it) } },
                    onLongClick = { onLongClick?.invoke() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = tile.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = tile.title, fontSize = 12.sp, textAlign = TextAlign.Center)
            if (tile.subtitle != null) {
                Text(text = tile.subtitle!!, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

data class MenuTile(
    val icon: String,
    val title: String,
    val subtitle: String? = null,
    val route: Screen? = null
)

val MenuTiles = listOf(
    MenuTile("📋", "Соревнования", "Бяки 2026", Screen.Competitions),
    MenuTile("📄", "Путевые", "Ожидают: 4", Screen.RouteCardDetail),
    MenuTile("⏱️", "Старт", "Очередь: 6", Screen.Start),
    MenuTile("🏁", "Финиш", "На дист.: 6", Screen.Finish),
    MenuTile("📝", "Регистрация", "Зарег.: 18", Screen.Register),
    MenuTile("📋", "Команды", "2кл:6 · 3кл:4", Screen.TeamList),
    MenuTile("👤", "Персоны", "В базе: 20", Screen.Persons),
    MenuTile("🏆", "Итоги", "Завершено: 5", Screen.Results),
    MenuTile("⚙️", "Настройки", null, Screen.Settings),
)