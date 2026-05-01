package com.speleo.start.presentation.screen.finish

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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