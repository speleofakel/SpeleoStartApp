package com.speleo.start.presentation.screen.persons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speleo.start.util.DateValidator
import com.speleo.start.util.PhoneFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonNewScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: PersonNewVM = hiltViewModel()
) {
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var showMiddleName by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf("") }
    var noBirthDate by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var phoneState by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf("") }
    var showEmail by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("male") }

    val age = remember(birthDate) {
        if (!noBirthDate && birthDate.length == 10 && DateValidator.isValid(birthDate)) {
            try {
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                val birth = sdf.parse(birthDate)
                val today = Calendar.getInstance()
                val birthCal = Calendar.getInstance().apply { time = birth!! }
                var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--
                age
            } catch (e: Exception) { null }
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая персона") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Назад") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Фамилия
            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it },
                label = { Text("Фамилия") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            // Имя
            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it },
                label = { Text("Имя") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            // Отчество (скрытое)
            TextButton(onClick = { showMiddleName = !showMiddleName }) {
                Text(if (showMiddleName) "▾ Отчество" else "▸ Отчество")
            }
            AnimatedVisibility(visible = showMiddleName) {
                OutlinedTextField(
                    value = middleName, onValueChange = { middleName = it },
                    label = { Text("Отчество") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
            // Дата рождения
            if (!noBirthDate) {
                DateOfBirthField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                age?.let {
                    val color = when {
                        it < 14 -> Color(0xFFD32F2F)
                        it in 14..17 -> Color(0xFFFF8C00)
                        it > 70 -> Color(0xFFFF8C00)
                        else -> Color(0xFF00A86B)
                    }
                    Text("Возраст: $it лет", color = color, fontSize = 14.sp)
                    if (it >= 18) {
                        Text("👨‍🏫 Может быть ментором", color = Color(0xFF1E5A7A), fontSize = 13.sp)
                    }
                }
            }
            // Чекбокс «Без даты рождения»
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = noBirthDate, onCheckedChange = { noBirthDate = it })
                Text("Без даты рождения", fontSize = 14.sp)
            }
            // Телефон
            OutlinedTextField(
                value = phoneState,
                onValueChange = { newVal ->
                    val digits = newVal.text.filter { it.isDigit() }
                    val formatted = PhoneFormatter.formatAsYouType(digits)
                    phoneState = TextFieldValue(formatted, TextRange(formatted.length))
                    phone = formatted
                },
                label = { Text("Телефон") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            // Email (скрытый)
            TextButton(onClick = { showEmail = !showEmail }) {
                Text(if (showEmail) "▾ Email" else "▸ Email")
            }
            AnimatedVisibility(visible = showEmail) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            // Пол
            Text("Пол", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to "Мужской", "female" to "Женский").forEach { (v, l) ->
                    FilterChip(selected = gender == v, onClick = { gender = v }, label = { Text(l) })
                }
            }
            // Кнопка сохранения
            Button(
                onClick = {
                    vm.savePerson(
                        lastName, firstName,
                        middleName.ifBlank { null },
                        if (noBirthDate) null else birthDate.ifBlank { null },
                        phone.ifBlank { null }, gender,
                        canBeMentor = age != null && age >= 18,
                    ) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = lastName.isNotBlank() && firstName.isNotBlank()
            ) {
                Text("СОХРАНИТЬ")
            }
        }
    }
}

@Composable
fun DateOfBirthField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) { if (value != state.text) state = TextFieldValue(value, TextRange(value.length)) }

    OutlinedTextField(
        value = state,
        onValueChange = { newVal ->
            val formatted = DateValidator.formatAsYouType(newVal.text)
            val digitsBefore = newVal.text.take(newVal.selection.start).count { it.isDigit() }
            val dots = (digitsBefore / 2).coerceAtMost(2)
            val newCursor = minOf(digitsBefore + dots, formatted.length)
            state = newVal.copy(text = formatted, selection = TextRange(newCursor))
            if (formatted != value) onValueChange(formatted)
        },
        label = { Text("Дата рождения (ДД.ММ.ГГГГ)") },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}