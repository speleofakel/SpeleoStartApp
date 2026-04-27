package com.speleo.start.presentation.screen.persons

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
fun PersonListScreen(
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    vm: PersonListVM = hiltViewModel()
) {
    val persons by vm.persons.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Персоны") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.onSearchQueryChange(it) },
                label = { Text("🔍 Поиск по фамилии") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onAddNew, modifier = Modifier.fillMaxWidth()) {
                Text("➕ Новая персона")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (persons.isEmpty()) {
                Text("Нет персон в базе", fontSize = 16.sp)
            } else {
                LazyColumn {
                    items(persons) { person ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { /* TODO: открыть карточку */ }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${person.lastName} ${person.firstName}", style = MaterialTheme.typography.titleSmall)
                                if (person.birthDate != null) Text("📅 ${person.birthDate}", fontSize = 14.sp)
                                if (person.phone != null) Text("📞 ${person.phone}", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}