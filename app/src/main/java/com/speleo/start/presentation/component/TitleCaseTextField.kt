package com.speleo.start.presentation.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.speleo.start.util.StringExt.toTitleCase

@Composable
fun TitleCaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    onNext: (() -> Unit)? = null
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var hasFocus by remember { mutableStateOf(false) }

    // Синхронизация с внешним value (только если не в процессе ввода)
    LaunchedEffect(value) {
        if (textFieldValue.text != value && textFieldValue.composition == null) {
            val newSelection = if (hasFocus && value.isNotEmpty()) {
                TextRange(0, value.length) // сохраняем выделение при фокусе
            } else {
                TextRange(value.length)
            }
            textFieldValue = TextFieldValue(text = value, selection = newSelection)
        }
    }

    // Выделить всё при получении фокуса (если поле не пустое)
    LaunchedEffect(hasFocus, value) {
        if (hasFocus && value.isNotEmpty() && textFieldValue.composition == null) {
            textFieldValue = textFieldValue.copy(selection = TextRange(0, value.length))
        }
    }

    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val raw = newValue.text

            // Пробел в конце → переход на следующее поле
            if (raw.endsWith(" ") && raw.trim().isNotEmpty() && newValue.composition == null) {
                val trimmed = raw.trim().toTitleCase()
                textFieldValue = TextFieldValue(trimmed, TextRange(trimmed.length))
                onValueChange(trimmed)
                onNext?.invoke() ?: focusManager.moveFocus(FocusDirection.Down)
                return@OutlinedTextField
            }

            // Форматируем ТОЛЬКО когда IME закончил композицию
            if (newValue.composition == null) {
                val formatted = raw.toTitleCase()

                // Если текст был выделен полностью и пользователь начал печатать — курсор в конце
                val wasFullySelected = textFieldValue.selection.start == 0 &&
                        textFieldValue.selection.end == textFieldValue.text.length
                val newCursor = if (wasFullySelected || newValue.selection.start == raw.length) {
                    formatted.length
                } else {
                    newValue.selection.start.coerceIn(0, formatted.length)
                }

                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(newCursor)
                )

                if (formatted != value) {
                    onValueChange(formatted)
                }
            } else {
                // Во время композиции: не форматируем, чтобы не ломать IME
                textFieldValue = newValue
            }
        },
        label = { Text(label) },
        modifier = modifier
            .onFocusChanged { focusState ->
                val wasFocused = hasFocus
                hasFocus = focusState.isFocused
                // При получении фокуса: если поле не пустое — выделяем весь текст
                if (focusState.isFocused && !wasFocused && value.isNotEmpty()) {
                    textFieldValue = textFieldValue.copy(selection = TextRange(0, value.length))
                }
            },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() ?: focusManager.moveFocus(FocusDirection.Down) }
        )
    )
}