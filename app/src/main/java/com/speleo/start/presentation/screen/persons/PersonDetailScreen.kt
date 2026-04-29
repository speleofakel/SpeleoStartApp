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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.speleo.start.util.AgeCalculator
import com.speleo.start.util.DateValidator
import com.speleo.start.util.PhoneFormatter
import com.speleo.start.presentation.component.TitleCaseTextField
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    vm: PersonDetailVM = hiltViewModel()
) {
    val person by vm.person.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    // Режим редактирования
    var isEditing by remember { mutableStateOf(false) }

    // Поля для редактирования
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(personId) {
        vm.loadPerson(personId)
    }

    // Синхронизация полей при загрузке персоны
    LaunchedEffect(person) {
        person?.let { p ->
            lastName = p.lastName
            firstName = p.firstName
            middleName = p.middleName ?: ""
            nickname = p.nickname ?: ""
            birthDate = p.birthDate ?: ""
            phone = p.phone ?: ""
            email = p.email ?: ""
            gender = p.gender
            note = p.note ?: ""
        }
    }

    Scaffold(
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
                        if (isEditing) isEditing = false else onBack()
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
                                    email = email.ifBlank { null },
                                    gender = gender,
                                    note = note.ifBlank { null }
                                ) {
                                    isEditing = false
                                }
                            }
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
            val age = AgeCalculator.calculateAge(if (isEditing) birthDate else p.birthDate)
            val ageMark = AgeCalculator.getAgeColorMark(if (isEditing) birthDate else p.birthDate)
            val ageColor = Color(android.graphics.Color.parseColor(ageMark.colorHex))
            val initial = (if (isEditing) lastName else p.lastName).take(1).uppercase()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Аватар
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

                // Кнопки фото (только в режиме просмотра)
                if (!isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { /* TODO: тост */ }) {
                            Text("📷", fontSize = 14.sp)
                        }
                        OutlinedButton(onClick = { /* TODO: загрузка */ }) {
                            Text("🖼️", fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // === РЕЖИМ ПРОСМОТРА ===
                if (!isEditing) {
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
                        if (age != null) {
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
                    PersonFieldRO(label = "EMAIL", value = p.email ?: "—")

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

                    // История участия
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

                    // Заметки
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

                    // Кнопки действий
                    Button(
                        onClick = { /* TODO: Поделиться (Итерация 6) */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📤 ПОДЕЛИТЬСЯ КАРТОЧКОЙ")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { /* TODO: Чёрный список (Итерация 6) */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("🚫 ДОБАВИТЬ В ЧЁРНЫЙ СПИСОК")
                    }

                    // === РЕЖИМ РЕДАКТИРОВАНИЯ ===
                    // === РЕЖИМ РЕДАКТИРОВАНИЯ ===
                } else {
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

                    TitleCaseTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        label = "Отчество",
                        modifier = Modifier.fillMaxWidth()
                    )

                    TitleCaseTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = "Позывной",
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Дата рождения с маской
                    DateOfBirthFieldEdit(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (age != null) {
                        Text(
                            "Возраст: $age лет",
                            color = ageColor,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = PhoneFormatter.formatAsYouType(it.filter { c -> c.isDigit() }) },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )

                    // Пол
                    Text("Пол", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male" to "Мужской", "female" to "Женский").forEach { (v, l) ->
                            FilterChip(
                                selected = gender == v,
                                onClick = { gender = v },
                                label = { Text(l) }
                            )
                        }
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

// Поле просмотра (read-only)
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

// Поле даты рождения с маской (для редактирования)
// === Исправленная функция ===
@Composable
private fun DateOfBirthFieldEdit(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // НЕ используем value как ключ!
    var state by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Синхронизация только извне
    LaunchedEffect(value) {
        if (state.text != value) {
            state = TextFieldValue(value, TextRange(value.length))
        }
    }

    OutlinedTextField(
        value = state,
        onValueChange = { newValue ->
            // Форматируем: оставляем только цифры, максимум 8
            val digits = newValue.text.filter { it.isDigit() }.take(8)
            val formatted = buildString {
                for (i in digits.indices) {
                    if (i == 2 || i == 4) append(".")
                    append(digits[i])
                }
            }

            // Расчёт курсора
            val oldCursor = newValue.selection.start
            val digitsBefore = newValue.text.take(oldCursor).count { it.isDigit() }
            val dotsBefore = ((digitsBefore - 1) / 2).coerceIn(0, 2)
            val newCursor = (digitsBefore + dotsBefore).coerceIn(0, formatted.length)

            state = TextFieldValue(
                text = formatted,
                selection = TextRange(newCursor),
                composition = newValue.composition  // ← СОХРАНЯЕМ composition!
            )

            if (formatted != value) {
                onValueChange(formatted)
            }
        },
        label = { Text("Дата рождения (ДД.ММ.ГГГГ)") },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}