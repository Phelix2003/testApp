package com.example.apptest2.ui.components

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun SelectAllOnFocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (String, (String) -> Unit, Modifier) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(0, 0)))
    }

    // Synchroniser avec la valeur externe
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, TextRange(0, 0))
        }
    }

    content(
        textFieldValue.text,
        { newValue ->
            textFieldValue = TextFieldValue(newValue, TextRange(newValue.length))
            onValueChange(newValue)
        },
        modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && textFieldValue.selection.length == 0) {
                // Sélectionner tout le texte quand le champ reçoit le focus
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0, textFieldValue.text.length)
                )
            }
        }
    )
}
