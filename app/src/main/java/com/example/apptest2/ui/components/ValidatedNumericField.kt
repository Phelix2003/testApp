package com.example.apptest2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ValidatedNumericField(
    value: Long,
    onValueChange: (Long) -> Unit,
    label: String,
    range: LongRange = 0L..Long.MAX_VALUE,
    step: Long = 1L,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    showStepButtons: Boolean = true
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    var isValid by remember { mutableStateOf(true) }
    var hasFocus by remember { mutableStateOf(false) }

    // Validation de la valeur
    val validateAndUpdate = { input: String ->
        val numericValue = input.toLongOrNull()
        when {
            input.isEmpty() -> {
                isValid = false
                textValue = input
            }
            numericValue == null -> {
                isValid = false
                textValue = input
            }
            numericValue !in range -> {
                isValid = false
                textValue = input
            }
            else -> {
                isValid = true
                textValue = input
                onValueChange(numericValue)
            }
        }
    }

    // Correction automatique quand le champ perd le focus
    val handleFocusLost = {
        if (!isValid || textValue.isEmpty()) {
            // Restaurer la dernière valeur valide
            textValue = value.toString()
            isValid = true
        }
    }

    if (showStepButtons) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            // Bouton diminuer
            IconButton(
                onClick = {
                    val newValue = (value - step).coerceIn(range)
                    onValueChange(newValue)
                },
                enabled = enabled && value > range.first
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Diminuer")
            }

            // Champ de saisie
            OutlinedTextField(
                value = textValue,
                onValueChange = validateAndUpdate,
                label = { Text(label) },
                isError = !isValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = enabled,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        val wasFocused = hasFocus
                        hasFocus = focusState.isFocused

                        if (wasFocused && !focusState.isFocused) {
                            handleFocusLost()
                        }
                    },
                supportingText = {
                    when {
                        !isValid && textValue.toLongOrNull() == null -> {
                            Text("Valeur numérique requise", color = MaterialTheme.colorScheme.error)
                        }
                        !isValid && textValue.toLongOrNull() != null -> {
                            Text("Valeur doit être entre ${range.first} et ${range.last}", color = MaterialTheme.colorScheme.error)
                        }
                        supportingText != null -> {
                            Text(supportingText)
                        }
                        else -> {
                            Text("${range.first} - ${range.last}")
                        }
                    }
                }
            )

            // Bouton augmenter
            IconButton(
                onClick = {
                    val newValue = (value + step).coerceIn(range)
                    onValueChange(newValue)
                },
                enabled = enabled && value < range.last
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Augmenter")
            }
        }
    } else {
        // Version sans boutons +/-
        OutlinedTextField(
            value = textValue,
            onValueChange = validateAndUpdate,
            label = { Text(label) },
            isError = !isValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = enabled,
            singleLine = true,
            modifier = modifier
                .onFocusChanged { focusState ->
                    val wasFocused = hasFocus
                    hasFocus = focusState.isFocused

                    if (wasFocused && !focusState.isFocused) {
                        handleFocusLost()
                    }
                },
            supportingText = {
                when {
                    !isValid && textValue.toLongOrNull() == null -> {
                        Text("Valeur numérique requise", color = MaterialTheme.colorScheme.error)
                    }
                    !isValid && textValue.toLongOrNull() != null -> {
                        Text("Valeur doit être entre ${range.first} et ${range.last}", color = MaterialTheme.colorScheme.error)
                    }
                    supportingText != null -> {
                        Text(supportingText)
                    }
                    else -> {
                        Text("${range.first} - ${range.last}")
                    }
                }
            }
        )
    }
}

@Composable
fun ValidatedIntField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    range: IntRange = 0..Int.MAX_VALUE,
    step: Int = 1,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    showStepButtons: Boolean = true
) {
    ValidatedNumericField(
        value = value.toLong(),
        onValueChange = { newValue -> onValueChange(newValue.toInt()) },
        label = label,
        range = range.first.toLong()..range.last.toLong(),
        step = step.toLong(),
        modifier = modifier,
        enabled = enabled,
        supportingText = supportingText,
        showStepButtons = showStepButtons
    )
}
