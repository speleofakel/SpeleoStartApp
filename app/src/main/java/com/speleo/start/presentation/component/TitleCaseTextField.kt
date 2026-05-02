package com.speleo.start.presentation.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.speleo.start.util.StringExt.toTitleCase

@Composable
fun TitleCaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var hasFocus by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Синхронизация с внешним value
    LaunchedEffect(value) {
        if (textFieldValue.text != value && textFieldValue.composition == null) {
            val newSelection = if (hasFocus && value.isNotEmpty()) {
                TextRange(0, value.length)
            } else {
                TextRange(value.length)
            }
            textFieldValue = TextFieldValue(text = value, selection = newSelection)
        }
    }

    // Выделить всё при получении фокуса
    LaunchedEffect(hasFocus, value) {
        if (hasFocus && value.isNotEmpty() && textFieldValue.composition == null) {
            textFieldValue = textFieldValue.copy(selection = TextRange(0, value.length))
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val raw = newValue.text
            val oldLength = textFieldValue.text.length
            val newLength = raw.length

            // Проверяем: был ли добавлен пробел в конец?
            val spaceAdded = newLength > oldLength && raw.last() == ' '

            if (spaceAdded && raw.trim().isNotEmpty() && newValue.composition == null) {
                // Убираем пробел, форматируем и переходим на следующий элемент
                val trimmed = raw.trim().toTitleCase()
                textFieldValue = TextFieldValue(trimmed, TextRange(trimmed.length))
                onValueChange(trimmed)
                // Переход на следующий элемент
                if (onNext != null) {
                    onNext()
                } else {
                    focusManager.moveFocus(FocusDirection.Down)
                }
                return@OutlinedTextField
            }

            // Обычное форматирование (без пробела в конце)
            if (newValue.composition == null) {
                val formatted = raw.toTitleCase()

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
                textFieldValue = newValue
            }
        },
        label = { Text(label) },
        modifier = modifier.onFocusChanged { focusState ->
            hasFocus = focusState.isFocused
            if (focusState.isFocused && value.isNotEmpty()) {
                textFieldValue = textFieldValue.copy(selection = TextRange(0, value.length))
            }
        },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (onNext != null) {
                    onNext()
                } else {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            }
        )
    )
}