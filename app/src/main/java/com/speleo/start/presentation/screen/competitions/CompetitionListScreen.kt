package com.speleo.start.presentation.screen.competitions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompetitionListScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onSettings: (Long) -> Unit,
    vm: CompetitionListVM = hiltViewModel()
) {
    val competitions by vm.activeCompetitions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Соревнования") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
                Text("➕ Новое соревнование")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (competitions.isEmpty()) {
                Text("Нет активных соревнований", fontSize = 16.sp)
            } else {
                LazyColumn {
                    items(competitions, key = { it.id }) { competition ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            vm.selectCompetition(competition.id)
                                            onBack()
                                        },
                                        onLongClick = { }
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(competition.name, style = MaterialTheme.typography.titleSmall)
                                    Text("${competition.date} · ${competition.place}", fontSize = 14.sp)
                                    Text(
                                        if (competition.isArchived) "📦 Архив" else "🟢 Активно",
                                        fontSize = 12.sp,
                                        color = if (competition.isArchived) Color.Gray else Color(0xFF00A86B)
                                    )
                                }
                                IconButton(onClick = { onSettings(competition.id) }) {
                                    Text("⚙️", fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}