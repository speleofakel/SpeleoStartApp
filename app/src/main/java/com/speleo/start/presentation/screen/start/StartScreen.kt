package com.speleo.start.presentation.screen.start

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onBack: () -> Unit,
    vm: StartVM = hiltViewModel()
) {
    val mainTimer by vm.timer.mainTimer.collectAsStateWithLifecycle()
    val countdown by vm.timer.countdown.collectAsStateWithLifecycle()
    val isPaused by vm.timer.isPaused.collectAsStateWithLifecycle()
    val started by vm.timer.started.collectAsStateWithLifecycle()
    val queue by vm.queue.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadQueue() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Старт") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimerDisplay(mainTimer)
            Text(formatCountdown(countdown), fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)

            Button(onClick = { vm.timer.togglePause() }) {
                Text(if (isPaused) "ПУСК" else "ПАУЗА")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!started) {
                Button(onClick = { vm.timer.startCompetition() }, modifier = Modifier.fillMaxWidth()) {
                    Text("НАЧАТЬ СОРЕВНОВАНИЯ")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("СНЯТЬ") }
                Button(onClick = { vm.timer.resetCountdown() }, modifier = Modifier.weight(1f)) { Text("ПРОПУСК") }
                Button(onClick = { vm.timer.resetCountdown() }, modifier = Modifier.weight(1f)) { Text("СТАРТ") }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Очередь (${queue.size} команд):", fontSize = 16.sp)

            LazyColumn {
                items(queue, key = { it.id }) { team ->
                    Card(modifier = Modifier.fillMaxWidth().padding(2.dp)) {
                        Text("№${team.number} (${team.className}-й кл)", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TimerDisplay(millis: Long, modifier: Modifier = Modifier) {
    val totalSec = millis / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val hundredths = (millis % 1000) / 10

    val baseSize = 50.sp
    val smallSize = 30.sp
    val green = Color(0xFF388E3C)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        if (hours > 0) {
            Text("%02d:".format(hours), fontSize = baseSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = green)
        } else {
            Text("00:", fontSize = smallSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = green.copy(alpha = 0.5f))
        }
        Text("%02d:%02d".format(minutes, seconds), fontSize = baseSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = green)
        Text(".%02d".format(hundredths), fontSize = smallSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = green)
    }
}

private fun formatCountdown(s: Int): String = "%02d:%02d".format(s / 60, s % 60)