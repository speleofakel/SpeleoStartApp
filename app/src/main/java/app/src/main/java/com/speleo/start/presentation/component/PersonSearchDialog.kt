package com.speleo.start.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.presentation.screen.team.TeamRegisterVM
import com.speleo.start.util.DateValidator

@Composable
fun PersonSearchDialog(
    title: String,
    onPersonSelected: (PersonEntity) -> Unit,
    onQuickCreate: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    filterForMentors: Boolean = false, // Новый параметр
    vm: TeamRegisterVM = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()

    // Фильтруем результаты, если нужны только менторы (возраст ≥ 18)
    val filteredResults = remember(searchResults, filterForMentors) {
        if (filterForMentors) {
            searchResults.filter { person ->
                val age = DateValidator.calculateAge(person.birthDate)
                age != null && age >= 18
            }
        } else {
            searchResults
        }
    }

    var showQuickCreateForm by remember { mutableStateOf(false) }
    var quickLastName by remember { mutableStateOf("") }
    var quickFirstName by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            vm.onSearchQueryChange(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (showQuickCreateForm) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Text("➕ Быстрое создание персоны", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quickLastName,
                        onValueChange = { quickLastName = it },
                        label = { Text("Фамилия") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quickFirstName,
                        onValueChange = { quickFirstName = it },
                        label = { Text("Имя (опционально)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val fullName = if (quickFirstName.isNotBlank()) {
                                "$quickLastName $quickFirstName"
                            } else {
                                quickLastName
                            }
                            onQuickCreate?.invoke(fullName)
                            showQuickCreateForm = false
                            quickLastName = ""
                            quickFirstName = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = quickLastName.isNotBlank()
                    ) {
                        Text("СОЗДАТЬ")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showQuickCreateForm = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отмена")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("🔍 Поиск по фамилии") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.length >= 2 && filteredResults.isEmpty()) {
                        Column {
                            Text(
                                text = if (filterForMentors) "Менторы не найдены" else "Ничего не найдено",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (onQuickCreate != null) {
                                Button(
                                    onClick = { showQuickCreateForm = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("➕ Создать: $searchQuery")
                                }
                            }
                        }
                    } else {
                        LazyColumn {
                            items(filteredResults) { person ->
                                PersonSearchResultItem(
                                    person = person,
                                    onClick = { onPersonSelected(person) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun PersonSearchResultItem(person: PersonEntity, onClick: () -> Unit) {
    val age = DateValidator.calculateAge(person.birthDate)
    val ageMark = DateValidator.getAgeColorMark(person.birthDate)
    val ageColor = androidx.compose.ui.graphics.Color(
        android.graphics.Color.parseColor(ageMark.colorHex)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${person.lastName} ${person.firstName}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (person.nickname != null) {
                    Text(
                        text = "«${person.nickname}»",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (age != null) {
                Text(text = "$age лет", fontSize = 13.sp, color = ageColor)
            }
        }
    }
}