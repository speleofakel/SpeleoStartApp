package com.speleo.start.presentation.screen.results

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
fun ResultsScreen(
    onBack: () -> Unit,
    vm: ResultsVM = hiltViewModel()
) {
    val results2 by vm.results2.collectAsStateWithLifecycle()
    val results3 by vm.results3.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadResults() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Итоги") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)
        ) {
            Text("2-й класс", fontSize = 18.sp)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results2) { team ->
                    Card(modifier = Modifier.fillMaxWidth().padding(2.dp)) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Text("№${team.number}", modifier = Modifier.weight(1f))
                            Text("${team.score} баллов", modifier = Modifier.weight(1f))
                            Text(team.time, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("3-й класс", fontSize = 18.sp)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results3) { team ->
                    Card(modifier = Modifier.fillMaxWidth().padding(2.dp)) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Text("№${team.number}", modifier = Modifier.weight(1f))
                            Text("${team.score} баллов", modifier = Modifier.weight(1f))
                            Text(team.time, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}