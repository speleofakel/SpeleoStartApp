package com.speleo.start.presentation.screen.persons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speleo.start.presentation.component.TitleCaseTextField
import com.speleo.start.util.DateValidator
import com.speleo.start.util.PhoneFormatter
import com.speleo.start.util.normalizeName
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
    var nickname by remember { mutableStateOf("") }
    var showExtraFields by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf("") }
    var noBirthDate by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var phoneState by remember { mutableStateOf(TextFieldValue("")) }
    var gender by remember { mutableStateOf("male") }

    val isDateValid = birthDate.isBlank() ||
            (birthDate.length == 10 && DateValidator.isRealDate(birthDate))

    val age = remember(birthDate) {
        if (!noBirthDate && birthDate.length == 10 && DateValidator.isRealDate(birthDate)) {
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
            TitleCaseTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Фамилия",
                modifier = Modifier.fillMaxWidth()
            )

            TitleCaseTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "Имя",
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = { showExtraFields = !showExtraFields }) {
                Text(if (showExtraFields) "▾ Дополнительно" else "▸ Дополнительно")
            }
            AnimatedVisibility(visible = showExtraFields) {
                Column {
                    TitleCaseTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        label = "Отчество",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TitleCaseTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = "Позывной",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!noBirthDate) {
                DateOfBirthField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isDateValid && birthDate.isNotBlank()
                )
                if (!isDateValid && birthDate.isNotBlank()) {
                    Text(
                        "Некорректная дата",
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = noBirthDate, onCheckedChange = { noBirthDate = it })
                Text("Без даты рождения", fontSize = 14.sp)
            }

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

            Text("Пол", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to "Мужской", "female" to "Женский").forEach { (v, l) ->
                    FilterChip(selected = gender == v, onClick = { gender = v }, label = { Text(l) })
                }
            }

            Button(
                onClick = {
                    val normalizedLastName = lastName.normalizeName()
                    val normalizedFirstName = firstName.normalizeName()
                    val normalizedMiddleName = middleName.ifBlank { null }?.normalizeName()
                    val normalizedNickname = nickname.ifBlank { null }?.normalizeName()

                    vm.savePerson(
                        lastName = normalizedLastName,
                        firstName = normalizedFirstName,
                        middleName = normalizedMiddleName,
                        nickname = normalizedNickname,
                        birthDate = if (noBirthDate) null else birthDate.ifBlank { null },
                        phone = phone.ifBlank { null },
                        gender = gender
                    ) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = lastName.isNotBlank() && firstName.isNotBlank() && isDateValid
            ) {
                Text("СОХРАНИТЬ")
            }
        }
    }
}

@Composable
fun DateOfBirthField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var state by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (state.text != value) {
            state = TextFieldValue(value, TextRange(value.length))
        }
    }

    OutlinedTextField(
        value = state,
        onValueChange = { newVal ->
            val formatted = DateValidator.formatAsYouType(newVal.text)

            if (formatted == newVal.text) {
                state = newVal
                if (formatted != value) onValueChange(formatted)
                return@OutlinedTextField
            }

            val oldCursor = newVal.selection.start
            val digitsBeforeCursor = newVal.text.take(oldCursor).count { it.isDigit() }
            val dotsBeforeCursor = (digitsBeforeCursor / 2).coerceAtMost(2)
            val newCursor = (digitsBeforeCursor + dotsBeforeCursor).coerceIn(0, formatted.length)

            state = newVal.copy(
                text = formatted,
                selection = TextRange(newCursor)
            )
            if (formatted != value) onValueChange(formatted)
        },
        label = { Text("Дата рождения (ДД.ММ.ГГГГ)") },
        modifier = modifier,
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}