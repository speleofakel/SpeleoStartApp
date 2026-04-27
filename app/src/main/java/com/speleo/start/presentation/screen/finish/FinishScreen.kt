package com.speleo.start.presentation.screen.finish

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishScreen(
    onBack: () -> Unit,
    vm: FinishVM = hiltViewModel()
) {
    val teams by vm.startedTeams.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadStartedTeams() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Финиш") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Выберите команды:", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (teams.isEmpty()) {
                Text("Нет команд на дистанции", fontSize = 16.sp)
            } else {
                LazyColumn {
                    items(teams, key = { it.id }) { team ->
                        val isSelected = selectedIds.contains(team.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { vm.toggleSelection(team.id) }
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Text("№${team.number} (${team.className}-й кл)", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { vm.confirmFinish() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("ПОДТВЕРДИТЬ ФИНИШ (${selectedIds.size})")
            }
        }
    }
}