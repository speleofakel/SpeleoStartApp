package com.speleo.start.presentation.screen.routecard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
fun RouteCardScreen(
    teamId: Long,
    onBack: () -> Unit,
    vm: RouteCardVM = hiltViewModel()
) {
    val entries by vm.entries.collectAsStateWithLifecycle()

    LaunchedEffect(teamId) { vm.loadRouteCard(teamId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Путевой лист") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("КП №${entry.displayNumber} (вес ${entry.weight})", fontSize = 16.sp)
                            Text(if (entry.type == "technical") "Тех" else "КП", fontSize = 14.sp)
                        }
                        Column {
                            Checkbox(checked = entry.taken, onCheckedChange = { vm.toggleTaken(index) })
                            Text("Взят", fontSize = 10.sp)
                        }
                        if (entry.taken) {
                            Column {
                                Checkbox(checked = entry.takenWithError, onCheckedChange = { vm.toggleError(index) })
                                Text("Ошибка", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { vm.save() }, modifier = Modifier.fillMaxWidth()) {
                    Text("СОХРАНИТЬ")
                }
            }
        }
    }
}