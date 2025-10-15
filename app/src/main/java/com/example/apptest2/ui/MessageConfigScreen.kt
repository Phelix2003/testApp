package com.example.apptest2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageConfigScreen(
    buttonNumber: Int,
    currentConfig: MessageConfig,
    onConfigChange: (MessageConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(currentConfig) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration - Bouton $buttonNumber") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onConfigChange(config)
                            onBack()
                        }
                    ) {
                        Text("SAUVEGARDER")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nom du message
            OutlinedTextField(
                value = config.name,
                onValueChange = { config = config.copy(name = it) },
                label = { Text("Nom du bouton") },
                modifier = Modifier.fillMaxWidth()
            )

            // Configuration détaillée de l'événement
            DetailedEventConfigSection(
                detailedEventConfig = config.detailedEventConfig,
                onConfigChange = { newDetailedEventConfig ->
                    config = config.copy(detailedEventConfig = newDetailedEventConfig)
                }
            )
        }
    }
}

@Composable
fun DetailedEventConfigSection(
    detailedEventConfig: DetailedEventConfig,
    onConfigChange: (DetailedEventConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configuration Event Détaillée",
                style = MaterialTheme.typography.titleMedium
            )

            // Timing section
            TimingConfigSection(
                rStartEventMs = detailedEventConfig.rStartEventMs,
                rStopEventMs = detailedEventConfig.rStopEventMs,
                mask = detailedEventConfig.mask,
                onTimingChange = { startMs, stopMs, mask ->
                    onConfigChange(detailedEventConfig.copy(
                        rStartEventMs = startMs,
                        rStopEventMs = stopMs,
                        mask = mask
                    ))
                }
            )

            // Localization section
            LocalizationConfigSection(
                localizationConfig = detailedEventConfig.localization,
                onConfigChange = { newLocalization ->
                    onConfigChange(detailedEventConfig.copy(localization = newLocalization))
                }
            )

            // Effect section
            DetailedEffectConfigSection(
                effectConfig = detailedEventConfig.effect,
                onConfigChange = { newEffect ->
                    onConfigChange(detailedEventConfig.copy(effect = newEffect))
                }
            )

            // Layer section
            LayerConfigSection(
                layerConfig = detailedEventConfig.layer,
                onConfigChange = { newLayer ->
                    onConfigChange(detailedEventConfig.copy(layer = newLayer))
                }
            )
        }
    }
}

@Composable
fun TimingConfigSection(
    rStartEventMs: Long,
    rStopEventMs: Long,
    mask: Int,
    onTimingChange: (Long, Long, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⏱️ Timing & Masque",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = rStartEventMs.toString(),
                onValueChange = { value ->
                    value.toLongOrNull()?.let { startMs ->
                        if (startMs >= 0 && startMs < rStopEventMs) {
                            onTimingChange(startMs, rStopEventMs, mask)
                        }
                    }
                },
                label = { Text("Début (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rStopEventMs.toString(),
                onValueChange = { value ->
                    value.toLongOrNull()?.let { stopMs ->
                        if (stopMs > rStartEventMs) {
                            onTimingChange(rStartEventMs, stopMs, mask)
                        }
                    }
                },
                label = { Text("Fin (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mask.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { maskValue ->
                        if (maskValue in 0..255) {
                            onTimingChange(rStartEventMs, rStopEventMs, maskValue)
                        }
                    }
                },
                label = { Text("Masque (0-255)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LocalizationConfigSection(
    localizationConfig: LocalizationConfig,
    onConfigChange: (LocalizationConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📍 Localisation",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = localizationConfig.mapId.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { mapId ->
                        if (mapId in 0..255) {
                            onConfigChange(localizationConfig.copy(mapId = mapId))
                        }
                    }
                },
                label = { Text("Map ID (0-255)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = localizationConfig.focus.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { focus ->
                            if (focus in 0..255) {
                                onConfigChange(localizationConfig.copy(focus = focus))
                            }
                        }
                    },
                    label = { Text("Focus (×10cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = localizationConfig.zoom.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { zoom ->
                            if (zoom in 0..255) {
                                onConfigChange(localizationConfig.copy(zoom = zoom))
                            }
                        }
                    },
                    label = { Text("Zoom (×10cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            GoboTypeSelector(
                selectedGoboType = localizationConfig.goboType,
                onGoboTypeSelected = { newGoboType ->
                    onConfigChange(localizationConfig.copy(goboType = newGoboType))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoboTypeSelector(
    selectedGoboType: GoboType,
    onGoboTypeSelected: (GoboType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Type GOBO",
            style = MaterialTheme.typography.labelMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = when (selectedGoboType) {
                    GoboType.NONE -> "Aucun"
                    GoboType.POINT -> "Point"
                    GoboType.LINE -> "Ligne"
                    GoboType.POLYGON -> "Polygone"
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                GoboType.entries.forEach { goboType ->
                    DropdownMenuItem(
                        text = {
                            Text(when (goboType) {
                                GoboType.NONE -> "Aucun"
                                GoboType.POINT -> "Point"
                                GoboType.LINE -> "Ligne"
                                GoboType.POLYGON -> "Polygone"
                            })
                        },
                        onClick = {
                            onGoboTypeSelected(goboType)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedEffectConfigSection(
    effectConfig: DetailedEffectConfig,
    onConfigChange: (DetailedEffectConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✨ Effet",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            EventStyleSelector(
                selectedStyle = effectConfig.style,
                onStyleSelected = { newStyle ->
                    onConfigChange(effectConfig.copy(style = newStyle))
                }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = effectConfig.frequency.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { frequency ->
                            if (frequency in 1..255) {
                                onConfigChange(effectConfig.copy(frequency = frequency))
                            }
                        }
                    },
                    label = { Text("Fréq. (Hz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = effectConfig.duration.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { duration ->
                            if (duration in 0..255) {
                                onConfigChange(effectConfig.copy(duration = duration))
                            }
                        }
                    },
                    label = { Text("Durée (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = effectConfig.intensity.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { intensity ->
                        if (intensity in 0..255) {
                            onConfigChange(effectConfig.copy(intensity = intensity))
                        }
                    }
                },
                label = { Text("Intensité (0-255)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            EventColorPicker(
                color = effectConfig.color,
                onColorChange = { newColor ->
                    onConfigChange(effectConfig.copy(color = newColor))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventStyleSelector(
    selectedStyle: EventStyle,
    onStyleSelected: (EventStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Style",
            style = MaterialTheme.typography.labelMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedStyle.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EventStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(style.name) },
                        onClick = {
                            onStyleSelected(style)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventColorPicker(
    color: EventColor,
    onColorChange: (EventColor) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Couleur RGBW + Vibration",
            style = MaterialTheme.typography.labelMedium
        )

        // Première ligne : Rouge, Vert, Bleu
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = color.red.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { red ->
                        if (red in 0..255) {
                            onColorChange(color.copy(red = red))
                        }
                    }
                },
                label = { Text("R") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = color.green.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { green ->
                        if (green in 0..255) {
                            onColorChange(color.copy(green = green))
                        }
                    }
                },
                label = { Text("G") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = color.blue.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { blue ->
                        if (blue in 0..255) {
                            onColorChange(color.copy(blue = blue))
                        }
                    }
                },
                label = { Text("B") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        // Deuxième ligne : Blanc, Vibration
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = color.white.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { white ->
                        if (white in 0..255) {
                            onColorChange(color.copy(white = white))
                        }
                    }
                },
                label = { Text("Blanc") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = color.vibration.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { vibration ->
                        if (vibration in 0..255) {
                            onColorChange(color.copy(vibration = vibration))
                        }
                    }
                },
                label = { Text("Vibration") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        // Aperçu de la couleur RGB
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color(color.red, color.green, color.blue)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RGBW(${color.red},${color.green},${color.blue},${color.white}) V:${color.vibration}",
                    fontSize = 10.sp,
                    color = if (color.red + color.green + color.blue > 384) {
                        androidx.compose.ui.graphics.Color.Black
                    } else {
                        androidx.compose.ui.graphics.Color.White
                    }
                )
            }
        }
    }
}

@Composable
fun LayerConfigSection(
    layerConfig: LayerConfig,
    onConfigChange: (LayerConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎭 Layer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = layerConfig.nbr.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { nbr ->
                            if (nbr in 0..255) {
                                onConfigChange(layerConfig.copy(nbr = nbr))
                            }
                        }
                    },
                    label = { Text("Numéro") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = layerConfig.opacity.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { opacity ->
                            if (opacity in 0..255) {
                                onConfigChange(layerConfig.copy(opacity = opacity))
                            }
                        }
                    },
                    label = { Text("Opacité (0-255)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            BlendingModeSelector(
                selectedBlendingMode = layerConfig.blendingMode,
                onBlendingModeSelected = { newBlendingMode ->
                    onConfigChange(layerConfig.copy(blendingMode = newBlendingMode))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendingModeSelector(
    selectedBlendingMode: BlendingMode,
    onBlendingModeSelected: (BlendingMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Mode de mélange",
            style = MaterialTheme.typography.labelMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = when (selectedBlendingMode) {
                    BlendingMode.NORMAL -> "Normal"
                    BlendingMode.ADD -> "Addition"
                    BlendingMode.AND -> "ET logique"
                    BlendingMode.SUBSTRACT -> "Soustraction"
                    BlendingMode.MULTIPLY -> "Multiplication"
                    BlendingMode.DIVIDE -> "Division"
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                BlendingMode.entries.forEach { blendingMode ->
                    DropdownMenuItem(
                        text = {
                            Text(when (blendingMode) {
                                BlendingMode.NORMAL -> "Normal"
                                BlendingMode.ADD -> "Addition"
                                BlendingMode.AND -> "ET logique"
                                BlendingMode.SUBSTRACT -> "Soustraction"
                                BlendingMode.MULTIPLY -> "Multiplication"
                                BlendingMode.DIVIDE -> "Division"
                            })
                        },
                        onClick = {
                            onBlendingModeSelected(blendingMode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
