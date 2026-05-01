package com.speleo.start.presentation.screen.team

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.speleo.start.data.local.entity.PersonEntity
import com.speleo.start.presentation.component.TitleCaseTextField
import com.speleo.start.util.DateValidator
import com.speleo.start.util.PhoneFormatter
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRegisterScreen(
    onBack: () -> Unit,
    onNavigateToTeam: (Long) -> Unit = {},
    vm: TeamRegisterVM = hiltViewModel()
) {
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val minTeamSize by vm.minTeamSize.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedClass by remember { mutableStateOf("2") }
    var members by remember { mutableStateOf(listOf<MemberDraft>()) }
    var showMentorPickerFor by remember { mutableStateOf<Int?>(null) }
    var mentorSearchQuery by remember { mutableStateOf("") }
    var showConflictDialog by remember { mutableStateOf<Pair<String, TeamRegisterVM.UiEvent.ConflictDialog>?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        vm.loadCompetitionSettings()
        vm.event.collectLatest { event ->
            when (event) {
                is TeamRegisterVM.UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is TeamRegisterVM.UiEvent.NavigateBack -> onBack()
                is TeamRegisterVM.UiEvent.ConflictDialog -> showConflictDialog = event.personName to event
                is TeamRegisterVM.UiEvent.NavigateToTeam -> onNavigateToTeam(event.teamId)
            }
        }
    }

    val allRequirementsMet = remember(members) {
        members.all { member ->
            when (member.ageGroup) {
                AgeGroup.ADULT -> true
                AgeGroup.MINOR -> member.mentor != null && member.mentorConfirmed
                AgeGroup.CHILD -> member.mentor != null && member.mentorConfirmed && member.judgeApproved
                AgeGroup.UNKNOWN -> false
            }
        }
    }
    val canSave = members.size >= minTeamSize && allRequirementsMet && members.all { it.isValid }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Регистрация команды") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("← Назад") }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Класс:", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("2" to "2-й класс", "3" to "3-й класс").forEach { (v, l) ->
                    FilterChip(selected = selectedClass == v, onClick = { selectedClass = v }, label = { Text(l) })
                }
            }

            val emptyIndex = members.indexOfFirst { it.person == null && !it.isQuickCreate }
            if (emptyIndex >= 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔍 Поиск участника ${emptyIndex + 1}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { vm.onSearchQueryChange(it) },
                            label = { Text("Фамилия") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Text),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                searchResults.forEach { person ->
                                    PersonSearchResultItem(
                                        person = person,
                                        onClick = {
                                            vm.onPersonSelected(person) {
                                                members = members.toMutableList().also {
                                                    it[emptyIndex] = members[emptyIndex].copy(
                                                        person = person,
                                                        ageGroup = calculateAgeGroup(person.birthDate),
                                                        isQuickCreate = false
                                                    )
                                                }
                                                vm.onSearchQueryChange("")
                                                focusManager.clearFocus()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(visible = searchQuery.length >= 2 && searchResults.isEmpty()) {
                            Column {
                                Text("Ничего не найдено", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                                Button(
                                    onClick = {
                                        members = members.toMutableList().also {
                                            it[emptyIndex] = members[emptyIndex].copy(isQuickCreate = true, quickLastName = searchQuery)
                                        }
                                        vm.onSearchQueryChange("")
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("➕ Создать: $searchQuery") }
                            }
                        }
                    }
                }
            }

            Text("Участники (${members.size}/$minTeamSize мин.):", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            members.forEachIndexed { index, member ->
                MemberCard(
                    index = index, member = member,
                    onRemove = { members = members.toMutableList().also { it.removeAt(index) } },
                    onToggleJudgeApproval = { members = members.toMutableList().also { it[index] = member.copy(judgeApproved = !member.judgeApproved) } },
                    onOpenMentorPicker = { showMentorPickerFor = index },
                    onRemoveMentor = { members = members.toMutableList().also { it[index] = member.copy(mentor = null, mentorConfirmed = false) } },
                    onUpdateQuickFields = { updated -> members = members.toMutableList().also { it[index] = updated } },
                    onAutoOpenMentor = { showMentorPickerFor = index },
                    vm = vm
                )
            }

            if (members.size < 6) {
                OutlinedButton(onClick = { members = members + MemberDraft() }, modifier = Modifier.fillMaxWidth()) {
                    Text("➕ Добавить участника")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { vm.registerTeam(className = selectedClass, members = members) }, modifier = Modifier.fillMaxWidth(), enabled = canSave) {
                Text("СОХРАНИТЬ КОМАНДУ")
            }
            if (!allRequirementsMet && members.isNotEmpty()) {
                Text("⚠️ Проверьте менторов и разрешения судьи", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    showConflictDialog?.let { (name, event) ->
        AlertDialog(
            onDismissRequest = { showConflictDialog = null },
            title = { Text("Участник занят") },
            text = { Text("$name уже числится в команде №${event.teamNumber} (${event.className}-й класс). Перейти в карточку команды?") },
            confirmButton = {
                TextButton(onClick = {
                    showConflictDialog = null
                    onNavigateToTeam(event.teamId)
                }) { Text("Перейти") }
            },
            dismissButton = { TextButton(onClick = { showConflictDialog = null }) { Text("Отмена") } }
        )
    }

    if (showMentorPickerFor != null) {
        val memberIndex = showMentorPickerFor!!
        AlertDialog(
            onDismissRequest = { showMentorPickerFor = null },
            title = { Text("Выбор ментора") },
            text = {
                Column {
                    OutlinedTextField(
                        value = mentorSearchQuery,
                        onValueChange = { mentorSearchQuery = it; vm.onMentorSearchQueryChange(it) },
                        label = { Text("🔍 Фамилия ментора (≥18)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val mentorResults by vm.mentorSearchResults.collectAsStateWithLifecycle()
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(mentorResults) { person ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    members = members.toMutableList().also {
                                        it[memberIndex] = members[memberIndex].copy(mentor = person, mentorConfirmed = true)
                                    }
                                    showMentorPickerFor = null; mentorSearchQuery = ""; vm.onMentorSearchQueryChange("")
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${person.lastName} ${person.firstName}", modifier = Modifier.weight(1f))
                                val age = DateValidator.calculateAge(person.birthDate)
                                if (age != null) Text("$age лет", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                    if (mentorResults.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        var qLast by remember { mutableStateOf("") }
                        var qFirst by remember { mutableStateOf("") }
                        TitleCaseTextField(value = qLast, onValueChange = { qLast = it }, label = "Фамилия ментора", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        TitleCaseTextField(value = qFirst, onValueChange = { qFirst = it }, label = "Имя ментора", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (qLast.isNotBlank()) {
                                    vm.createQuickMentor(lastName = qLast, firstName = qFirst) { newId ->
                                        vm.getPersonById(newId) { person ->
                                            person?.let {
                                                members = members.toMutableList().also { list ->
                                                    list[memberIndex] = list[memberIndex].copy(mentor = person, mentorConfirmed = true)
                                                }
                                            }
                                        }
                                        showMentorPickerFor = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = qLast.isNotBlank()
                        ) { Text("➕ СОЗДАТЬ И ПРИВЯЗАТЬ") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMentorPickerFor = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun PersonSearchResultItem(person: PersonEntity, onClick: () -> Unit) {
    val age = DateValidator.calculateAge(person.birthDate)
    val ageMark = DateValidator.getAgeColorMark(person.birthDate)
    val ageColor = Color(android.graphics.Color.parseColor(ageMark.colorHex))
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(person.lastName.take(1).uppercase(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${person.lastName} ${person.firstName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (person.nickname != null) Text("«${person.nickname}»", fontSize = 11.sp, color = Color.Gray)
            }
            if (age != null) Text("$age лет", fontSize = 13.sp, color = ageColor)
        }
    }
}

@Composable
private fun MemberCard(
    index: Int, member: MemberDraft,
    onRemove: () -> Unit, onToggleJudgeApproval: () -> Unit,
    onOpenMentorPicker: () -> Unit, onRemoveMentor: () -> Unit,
    onUpdateQuickFields: (MemberDraft) -> Unit,
    onAutoOpenMentor: () -> Unit,
    vm: TeamRegisterVM
) {
    // 👇 Защита: вычисляем effectiveAgeGroup если вдруг не задан
    val effectiveAgeGroup = if (member.ageGroup != AgeGroup.UNKNOWN) {
        member.ageGroup
    } else if (member.person != null) {
        calculateAgeGroup(member.person.birthDate)
    } else {
        AgeGroup.UNKNOWN
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = when {
            member.person == null && !member.isQuickCreate -> MaterialTheme.colorScheme.surfaceVariant
            !member.isValid -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surface
        }
    )) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (index == 0) "👑 Капитан" else "👤 Участник", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
                    modifier = Modifier.background(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 2.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            when {
                member.isQuickCreate -> QuickCreateForm(member = member, onUpdate = onUpdateQuickFields, vm = vm, onAutoOpenMentor = onAutoOpenMentor)
                member.person != null -> {
                    val person = member.person!!
                    val age = DateValidator.calculateAge(person.birthDate)
                    val ageMark = DateValidator.getAgeColorMark(person.birthDate)
                    val ageColor = Color(android.graphics.Color.parseColor(ageMark.colorHex))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Text(person.lastName.take(1).uppercase(), fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${person.lastName} ${person.firstName}", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            if (age != null) Text("$age лет · ${ageMark.label}", fontSize = 12.sp, color = ageColor)
                        }
                    }
                    // 👇 Используем effectiveAgeGroup вместо member.ageGroup
                    when (effectiveAgeGroup) {
                        AgeGroup.MINOR, AgeGroup.CHILD -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (member.mentor != null) MentorRow(mentor = member.mentor!!, onRemoveMentor = onRemoveMentor)
                            else OutlinedButton(onClick = onOpenMentorPicker, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("⚠️ Ментор", fontSize = 12.sp)
                            }
                            if (effectiveAgeGroup == AgeGroup.CHILD) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp)) {
                                    Checkbox(checked = member.judgeApproved, onCheckedChange = { onToggleJudgeApproval() }, modifier = Modifier.size(32.dp))
                                    Text("Разрешено судьёй", fontSize = 12.sp)
                                }
                            }
                        }
                        else -> {}
                    }
                }
                else -> Text("Введите фамилию в поле поиска выше", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MentorRow(mentor: PersonEntity, onRemoveMentor: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("👨‍🏫 ${mentor.lastName} ${mentor.firstName}", fontSize = 13.sp, color = Color(0xFF1E5A7A))
        TextButton(onClick = onRemoveMentor, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Сменить", fontSize = 11.sp) }
    }
}

@Composable
private fun QuickCreateForm(
    member: MemberDraft,
    onUpdate: (MemberDraft) -> Unit,
    vm: TeamRegisterVM,
    onAutoOpenMentor: () -> Unit = {}
) {
    var showExtra by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isDateValid = member.quickBirthDate.isBlank() || (member.quickBirthDate.length == 10 && DateValidator.isRealDate(member.quickBirthDate))
    val dateHasError = member.quickBirthDate.isNotBlank() && !isDateValid
    val age = if (member.quickBirthDate.length == 10 && DateValidator.isRealDate(member.quickBirthDate)) DateValidator.calculateAge(member.quickBirthDate) else null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TitleCaseTextField(value = member.quickLastName, onValueChange = { onUpdate(member.copy(quickLastName = it)) }, label = "Фамилия", modifier = Modifier.fillMaxWidth(), onNext = { focusManager.moveFocus(FocusDirection.Down) })
        TitleCaseTextField(value = member.quickFirstName, onValueChange = { onUpdate(member.copy(quickFirstName = it)) }, label = "Имя", modifier = Modifier.fillMaxWidth(), onNext = { focusManager.moveFocus(FocusDirection.Down) })
        TextButton(onClick = { showExtra = !showExtra }, contentPadding = PaddingValues(vertical = 0.dp)) { Text(if (showExtra) "▾ Доп." else "▸ Доп.", fontSize = 12.sp) }
        AnimatedVisibility(visible = showExtra) {
            TitleCaseTextField(value = member.quickMiddleName, onValueChange = { onUpdate(member.copy(quickMiddleName = it)) }, label = "Отчество", modifier = Modifier.fillMaxWidth())
        }

        // === ДАТА РОЖДЕНИЯ (ИСПРАВЛЕННЫЙ КУРСОР) ===
        var dateState by remember { mutableStateOf(TextFieldValue(member.quickBirthDate, TextRange(member.quickBirthDate.length))) }
        LaunchedEffect(member.quickBirthDate) {
            if (dateState.text != member.quickBirthDate) {
                dateState = TextFieldValue(member.quickBirthDate, TextRange(member.quickBirthDate.length))
            }
        }
        OutlinedTextField(
            value = dateState,
            onValueChange = { newVal ->
                val oldCursor = newVal.selection.start
                val digits = newVal.text.filter { it.isDigit() }.take(8)
                val formatted = buildString {
                    for (i in digits.indices) {
                        if (i == 2 || i == 4) append(".")
                        append(digits[i])
                    }
                }
                // 👇 Исправленный пересчёт курсора с учётом авто-точек
                val digitsBeforeCursor = newVal.text.take(oldCursor).count { it.isDigit() }
                val dotsBeforeCursor = (digitsBeforeCursor / 2).coerceAtMost(2)
                val newCursor = (digitsBeforeCursor + dotsBeforeCursor).coerceIn(0, formatted.length)

                dateState = newVal.copy(text = formatted, selection = TextRange(newCursor))
                onUpdate(member.copy(quickBirthDate = formatted))
                if (formatted.length == 10) focusManager.moveFocus(FocusDirection.Down)
            },
            label = { Text(if (dateHasError) "Некорректная дата" else "Дата рождения") },
            isError = dateHasError, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )
        if (dateHasError) Text("Некорректная дата", color = Color(0xFFD32F2F), fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp))
        else if (age != null) {
            val color = when { age < 14 -> Color(0xFFD32F2F); age in 14..17 -> Color(0xFFFF8C00); age > 70 -> Color(0xFFFF8C00); else -> Color(0xFF00A86B) }
            Text("Возраст: $age лет", color = color, fontSize = 12.sp)
        }

        var phoneState by remember { mutableStateOf(TextFieldValue(member.quickPhone, TextRange(member.quickPhone.length))) }
        LaunchedEffect(member.quickPhone) { if (phoneState.text != member.quickPhone) phoneState = TextFieldValue(member.quickPhone, TextRange(member.quickPhone.length)) }
        OutlinedTextField(value = phoneState, onValueChange = { newVal ->
            val digits = newVal.text.filter { it.isDigit() }
            val formatted = PhoneFormatter.formatAsYouType(digits)
            phoneState = TextFieldValue(formatted, TextRange(formatted.length))
            onUpdate(member.copy(quickPhone = formatted))
        }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("male" to "М", "female" to "Ж").forEach { (v, l) -> FilterChip(selected = member.quickGender == v, onClick = { onUpdate(member.copy(quickGender = v)) }, label = { Text(l, fontSize = 12.sp) }) }
        }

        val canSavePerson = member.quickLastName.isNotBlank() && member.quickFirstName.isNotBlank() && isDateValid
        Button(onClick = {
            vm.createQuickPerson(
                lastName = member.quickLastName, firstName = member.quickFirstName, middleName = member.quickMiddleName.ifBlank { null },
                birthDate = member.quickBirthDate.ifBlank { null }, phone = member.quickPhone.ifBlank { null }, gender = member.quickGender
            ) { savedId ->
                vm.getPersonById(savedId) { person ->
                    person?.let {
                        // 👇 КЛЮЧЕВОЙ ФИКС: обновляем ageGroup при сохранении!
                        onUpdate(member.copy(
                            person = it,
                            isQuickCreate = false,
                            ageGroup = calculateAgeGroup(it.birthDate)
                        ))
                        val pAge = DateValidator.calculateAge(it.birthDate)
                        if (pAge != null && pAge < 18) onAutoOpenMentor()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = canSavePerson) { Text("💾 СОХРАНИТЬ", fontSize = 13.sp) }
    }
}

private fun calculateAgeGroup(birthDate: String?): AgeGroup {
    val age = DateValidator.calculateAge(birthDate) ?: return AgeGroup.UNKNOWN
    return when { age >= 18 -> AgeGroup.ADULT; age in 14..17 -> AgeGroup.MINOR; else -> AgeGroup.CHILD }
}