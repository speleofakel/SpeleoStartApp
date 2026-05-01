package com.speleo.start.presentation.screen.teamlist

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamListScreen(
    onBack: () -> Unit,
    onTeamClick: (Long) -> Unit,
    vm: TeamListVM = hiltViewModel()
) {
    val teams by vm.teams.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadTeams() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Команды") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.onSearchQueryChange(it) },
                label = { Text("🔍 Поиск по номеру или фамилии") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (teams.isEmpty()) {
                Text("Нет команд", fontSize = 16.sp)
            } else {
                LazyColumn {
                    items(teams, key = { it.id }) { team ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .clickable {
                                    vm.selectTeam(team.id)
                                    onTeamClick(team.id)
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "№${team.number} (${team.className}-й кл)",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        team.colorMark,
                                        fontSize = 14.sp,
                                        color = when (team.colorMark) {
                                            "СНЯТЫ" -> Color(0xFFD32F2F)
                                            "<17 !!!", "<14 !!!" -> Color(0xFFF57C00)
                                            else -> Color(0xFF00A86B)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (team.memberNames.isNotBlank()) {
                                    Text(
                                        team.memberNames,
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    team.timeInfo,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}