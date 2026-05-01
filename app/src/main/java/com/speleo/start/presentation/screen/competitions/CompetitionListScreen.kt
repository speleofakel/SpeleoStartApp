package com.speleo.start.presentation.screen.competitions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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