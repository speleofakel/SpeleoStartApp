package com.speleo.start.presentation.screen.competitions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionSettingsScreen(
    competitionId: Long,
    onBack: () -> Unit,
    onCheckpoints: (Long) -> Unit,
    vm: CompetitionSettingsVM = hiltViewModel()
) {
    val startInterval by vm.startInterval.collectAsStateWithLifecycle()
    val controlTime2 by vm.controlTime2.collectAsStateWithLifecycle()
    val controlTime3 by vm.controlTime3.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadSettings() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки соревнования") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Интервал (сек)", fontSize = 14.sp)
            OutlinedTextField(
                value = startInterval,
                onValueChange = { vm.updateInterval(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("КВ 2-й класс (мин)", fontSize = 14.sp)
            OutlinedTextField(
                value = controlTime2,
                onValueChange = { vm.updateControlTime2(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("КВ 3-й класс (мин)", fontSize = 14.sp)
            OutlinedTextField(
                value = controlTime3,
                onValueChange = { vm.updateControlTime3(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { vm.saveSettings() }, modifier = Modifier.fillMaxWidth()) {
                Text("СОХРАНИТЬ")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onCheckpoints(competitionId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🗺️ Настроить КП")
            }
        }
    }
}