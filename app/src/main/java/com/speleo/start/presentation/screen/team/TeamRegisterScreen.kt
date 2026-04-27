package com.speleo.start.presentation.screen.team

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
fun TeamRegisterScreen(
    onBack: () -> Unit,
    vm: TeamRegisterVM = hiltViewModel()
) {
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    var selectedClass by remember { mutableStateOf("2") }
    var selectedPersonIds by remember { mutableStateOf(listOf<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Регистрация команды") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Класс:", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("2" to "2-й класс", "3" to "3-й класс").forEach { (v, l) ->
                    FilterChip(
                        selected = selectedClass == v,
                        onClick = { selectedClass = v },
                        label = { Text(l) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.onSearchQueryChange(it) },
                label = { Text("🔍 Поиск участника по фамилии") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Выбрано: ${selectedPersonIds.size} участников", fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(searchResults) { person ->
                    val isSelected = selectedPersonIds.contains(person.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                selectedPersonIds = if (isSelected) {
                                    selectedPersonIds - person.id
                                } else {
                                    selectedPersonIds + person.id
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Column {
                                Text("${person.lastName} ${person.firstName}", style = MaterialTheme.typography.titleSmall)
                                if (person.birthDate != null) {
                                    Text("📅 ${person.birthDate}", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    vm.registerTeam(selectedClass, selectedPersonIds) {
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPersonIds.size >= 2
            ) {
                Text("СОХРАНИТЬ КОМАНДУ")
            }
        }
    }
}