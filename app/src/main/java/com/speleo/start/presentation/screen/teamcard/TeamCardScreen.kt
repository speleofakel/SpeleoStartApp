package com.speleo.start.presentation.screen.teamcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCardScreen(
    teamId: Long,
    onBack: () -> Unit,
    vm: TeamCardVM = hiltViewModel()
) {
    val card by vm.teamCard.collectAsStateWithLifecycle()

    LaunchedEffect(teamId) { vm.loadTeam(teamId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карточка команды") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        },
        floatingActionButton = {
            val fabAction = vm.getFabAction()
            if (fabAction != null) {
                FloatingActionButton(
                    onClick = { },
                    containerColor = Color(0xFFD32F2F)
                ) {
                    Text(if (fabAction == "finish") "🏁" else "📄", fontSize = 20.sp)
                }
            }
        }
    ) { padding ->
        if (card != null) {
            val info = card!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("№${info.number} · ${info.className}-й класс", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Статус: ${info.status}")
                            Text("Метка: ${vm.getColorMarkText()}")
                        }
                    }
                }

                item { Text("👥 Состав (${info.members.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                itemsIndexed(info.members) { _, member ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${member.lastName} ${member.firstName}${member.nickname?.let { " «$it»" } ?: ""}", fontWeight = FontWeight.Bold)
                            if (member.age != null) Text("Возраст: ${member.age} лет", fontSize = 14.sp)
                            if (member.phone != null) Text("📞 ${member.phone}", fontSize = 14.sp)
                            if (member.mentorName != null) Text("👨‍🏫 Ментор: ${member.mentorName} ${if (member.mentorConfirmed) "✅" else "⚠️"}", fontSize = 14.sp)
                            if (member.judgeApproved) Text("✅ Разрешено судьёй", color = Color(0xFF2E7D32), fontSize = 14.sp)
                            Text("Роль: ${if (member.role == "captain") "Капитан" else "Участник"}", fontSize = 14.sp)
                        }
                    }
                }

                if (info.replacedCount > 0) {
                    item {
                        Text("🔄 История замен (${info.replacedCount})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("(свёрнуто)", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка...", fontSize = 18.sp)
            }
        }
    }
}