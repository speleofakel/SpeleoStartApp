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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompetitionListScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onSettings: (Long) -> Unit,
    onCompetitionSelected: () -> Unit = {},
    vm: CompetitionListVM = hiltViewModel()
) {
    val competitions by vm.activeCompetitions.collectAsStateWithLifecycle()
    val activeId by vm.activeCompetitionId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.event.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        val isActive = activeId == competition.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .combinedClickable(
                                    onClick = {
                                        vm.selectCompetition(competition.id)
                                        onCompetitionSelected()
                                    },
                                    onLongClick = { }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isActive) 4.dp else 1.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            competition.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isActive) {
                                            Text(
                                                "✓ АКТИВНО",
                                                fontSize = 11.sp,
                                                color = Color(0xFF00A86B),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "${competition.date} · ${competition.place}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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