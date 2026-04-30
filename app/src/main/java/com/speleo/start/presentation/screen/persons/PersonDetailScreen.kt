package com.speleo.start.presentation.screen.persons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.presentation.component.TitleCaseTextField
import com.speleo.start.util.DateValidator
import com.speleo.start.util.PhoneFormatter
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    vm: PersonDetailVM = hiltViewModel()
) {
    val person by vm.person.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEditing by remember { mutableStateOf(false) }

    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(personId) {
        vm.loadPerson(personId)
    }

    LaunchedEffect(person, isEditing) {
        if (!isEditing) {
            person?.let { p ->
                lastName = p.lastName
                firstName = p.firstName
                middleName = p.middleName ?: ""
                nickname = p.nickname ?: ""
                birthDate = p.birthDate ?: ""
                phone = p.phone ?: ""
                gender = p.gender
                note = p.note ?: ""
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.error.collectLatest { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    // Проверка частичной даты
    fun isPartialDateInvalid(date: String): Boolean {
        if (date.isBlank()) return false
        if (date.length < 10) {
            val parts = date.split(".")
            if (parts.isNotEmpty()) {
                val dayPart = parts.getOrNull(0) ?: ""
                if (dayPart.isNotBlank() && dayPart.toIntOrNull() !in 1..31) return true
                val monthPart = parts.getOrNull(1) ?: ""
                if (monthPart.isNotBlank() && monthPart.toIntOrNull() !in 1..12) return true
            }
            return false
        }
        return !DateValidator.isRealDate(date)
    }

    val isDateValid = birthDate.isBlank() ||
            (birthDate.length == 10 && DateValidator.isRealDate(birthDate))

    val isDateError = birthDate.isNotBlank() && isPartialDateInvalid(birthDate)

    val isFormValid = lastName.isNotBlank() &&
            firstName.isNotBlank() &&
            isDateValid

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Редактирование" else {
                            person?.let { "${it.lastName} ${it.firstName}" } ?: "Карточка персоны"
                        },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                        } else {
                            onBack()
                        }
                    }) {
                        Text(if (isEditing) "❌" else "← Назад")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Text("✏️", fontSize = 20.sp)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                vm.savePerson(
                                    lastName = lastName,
                                    firstName = firstName,
                                    middleName = middleName.ifBlank { null },
                                    nickname = nickname.ifBlank { null },
                                    birthDate = birthDate.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    gender = gender,
                                    note = note.ifBlank { null }
                                ) {
                                    isEditing = false
                                }
                            },
                            enabled = isFormValid
                        ) {
                            Text("💾", fontSize = 20.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading || person == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val p = person!!
            val age = DateValidator.calculateAge(birthDate)
            val ageMark = DateValidator.getAgeColorMark(birthDate)
            val ageColor = Color(android.graphics.Color.parseColor(ageMark.colorHex))
            val initial = lastName.take(1).uppercase()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { }) {
                            Text("📷", fontSize = 14.sp)
                        }
                        OutlinedButton(onClick = { }) {
                            Text("🖼️", fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (!isEditing) {
                    // === РЕЖИМ ПРОСМОТРА ===
                    PersonFieldRO(label = "ФАМИЛИЯ", value = p.lastName)
                    PersonFieldRO(label = "ИМЯ", value = p.firstName)
                    PersonFieldRO(label = "ОТЧЕСТВО", value = p.middleName ?: "—")
                    PersonFieldRO(label = "ПОЗЫВНОЙ", value = p.nickname?.let { "«$it»" } ?: "—")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PersonFieldRO(
                            label = "ДАТА РОЖДЕНИЯ",
                            value = p.birthDate ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        if (age != null && age >= 0) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = ageColor.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("ВОЗРАСТ", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "$age лет",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ageColor
                                    )
                                }
                            }
                        }
                    }

                    PersonFieldRO(label = "ТЕЛЕФОН", value = p.phone ?: "—")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ПОЛ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
                        when (p.gender) {
                            "male" -> Text("Мужской", fontSize = 16.sp)
                            "female" -> Text("Женский", fontSize = 16.sp)
                            else -> Text("—", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "🏆 ИСТОРИЯ УЧАСТИЯ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Нет данных", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "📝 ЗАМЕТКИ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(p.note ?: "—", fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📤 ПОДЕЛИТЬСЯ КАРТОЧКОЙ")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            vm.blacklistPerson {
                                vm.loadPerson(personId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (p.blacklisted) "✅ УБРАТЬ ИЗ ЧЁРНОГО СПИСКА" else "🚫 ДОБАВИТЬ В ЧЁРНЫЙ СПИСОК")
                    }
                } else {
                    // === РЕЖИМ РЕДАКТИРОВАНИЯ ===

                    TitleCaseTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Фамилия",
                        modifier = Modifier.fillMaxWidth(),
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    TitleCaseTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "Имя",
                        modifier = Modifier.fillMaxWidth(),
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    TitleCaseTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        label = "Отчество",
                        modifier = Modifier.fillMaxWidth(),
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    TitleCaseTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = "Позывной",
                        modifier = Modifier.fillMaxWidth(),
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // Поле даты рождения
                    var dateFieldState by remember { mutableStateOf(TextFieldValue(birthDate)) }

                    LaunchedEffect(birthDate) {
                        if (dateFieldState.text != birthDate && dateFieldState.composition == null) {
                            dateFieldState = TextFieldValue(birthDate, TextRange(birthDate.length))
                        }
                    }

                    val dateLabel = if (isDateError) {
                        "Некорректная дата"
                    } else {
                        "Дата рождения"
                    }

                    OutlinedTextField(
                        value = dateFieldState,
                        onValueChange = { newState ->
                            val digits = newState.text.filter { it.isDigit() }.take(8)
                            val formatted = buildString {
                                for (i in digits.indices) {
                                    if (i == 2 || i == 4) append(".")
                                    append(digits[i])
                                }
                            }

                            val cursorPos = newState.selection.start
                            val addedChar = newState.text.length > dateFieldState.text.length
                            val newCursor = if (addedChar && formatted.length > dateFieldState.text.length) {
                                cursorPos + 1
                            } else {
                                cursorPos.coerceIn(0, formatted.length)
                            }

                            dateFieldState = newState.copy(
                                text = formatted,
                                selection = TextRange(newCursor.coerceIn(0, formatted.length))
                            )

                            if (formatted != birthDate) {
                                birthDate = formatted
                            }

                            if (formatted.length == 10 && DateValidator.isRealDate(formatted)) {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        },
                        label = { Text(dateLabel) },
                        isError = isDateError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )

                    if (isDateError) {
                        Text(
                            "Некорректная дата",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else if (age != null && age >= 0) {
                        val color = when {
                            age < 14 -> Color(0xFFD32F2F)
                            age in 14..17 -> Color(0xFFFF8C00)
                            age > 70 -> Color(0xFFFF8C00)
                            else -> Color(0xFF00A86B)
                        }
                        Text(
                            "Возраст: $age лет",
                            color = color,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (age >= 18) {
                            Text(
                                "👨‍🏫 Может быть ментором",
                                color = Color(0xFF1E5A7A),
                                fontSize = 13.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = PhoneFormatter.formatAsYouType(it.filter { char -> char.isDigit() })
                        },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )

                    Text("Пол", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = gender == "male",
                            onClick = { gender = "male" },
                            label = { Text("Мужской") }
                        )
                        FilterChip(
                            selected = gender == "female",
                            onClick = { gender = "female" },
                            label = { Text("Женский") }
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Заметки") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PersonFieldRO(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}