package com.nexora.player.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.player.R
import com.nexora.player.equalizer.EqualizerPreferencesRepository
import com.nexora.player.equalizer.EqualizerSessionManager
import com.nexora.player.equalizer.EqualizerSettings
import com.nexora.player.equalizer.NEXORA_CUSTOM_TEMPLATE_ID
import com.nexora.player.equalizer.NexoraEqualizerTemplates
import com.nexora.player.equalizer.resolveCurveForBands
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    audioSessionId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { EqualizerPreferencesRepository(context) }
    val savedSettings by repository.settings.collectAsStateWithLifecycle(initialValue = EqualizerSettings())
    val hardwareInfo = remember(audioSessionId) { EqualizerSessionManager.attach(audioSessionId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var enabled by remember(audioSessionId) { mutableStateOf(savedSettings.enabled) }
    var selectedTemplateId by remember(audioSessionId) { mutableStateOf(savedSettings.templateId) }
    var customName by remember(audioSessionId) { mutableStateOf(savedSettings.customName) }
    var bandCurve by remember(audioSessionId) {
        mutableStateOf(resolveInitialCurve(savedSettings, hardwareInfo.bandCount))
    }

    LaunchedEffect(savedSettings, hardwareInfo.bandCount) {
        enabled = savedSettings.enabled
        selectedTemplateId = savedSettings.templateId
        customName = savedSettings.customName
        bandCurve = resolveInitialCurve(savedSettings, hardwareInfo.bandCount)
    }

    LaunchedEffect(enabled, selectedTemplateId, customName, bandCurve, audioSessionId) {
        EqualizerSessionManager.sync(
            audioSessionId = audioSessionId,
            settings = EqualizerSettings(
                enabled = enabled,
                templateId = selectedTemplateId,
                customName = customName,
                customCurve = bandCurve
            )
        )
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Equalizer, contentDescription = null)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.eq_title), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (hardwareInfo.supported) {
                                    stringResource(R.string.eq_active_bands, hardwareInfo.bandCount)
                                } else {
                                    stringResource(R.string.eq_not_available)
                                }
                            )
                        }
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked ->
                            enabled = checked
                            scope.launch { repository.setEnabled(checked) }
                        }
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.eq_current_state), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (enabled) {
                                stringResource(R.string.eq_active_template, templateLabel(selectedTemplateId, customName))
                            } else {
                                stringResource(R.string.eq_disabled_saved)
                            }
                        )
                        Text(
                            text = stringResource(R.string.eq_applied_hint),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.eq_templates),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(NexoraEqualizerTemplates.all) { template ->
                        EqualizerPresetCard(
                            templateName = stringResource(template.nameRes),
                            templateIcon = template.icon,
                            templateDescription = stringResource(template.descriptionRes),
                            selected = selectedTemplateId == template.id,
                            onClick = {
                                selectedTemplateId = template.id
                                bandCurve = resolveCurveForBands(template, hardwareInfo.bandCount)
                                scope.launch { repository.setTemplateId(template.id) }
                            }
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.eq_bars), fontWeight = FontWeight.SemiBold)
                            AssistChip(
                                onClick = {
                                    val template = NexoraEqualizerTemplates.resolve(selectedTemplateId)
                                    bandCurve = resolveCurveForBands(template, hardwareInfo.bandCount)
                                },
                                label = { Text(stringResource(R.string.eq_restore_template)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.Restore, contentDescription = null)
                                }
                            )
                        }

                        if (hardwareInfo.supported && hardwareInfo.bandCount > 0) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(hardwareInfo.bandCount) { index ->
                                    val centerHz = hardwareInfo.centerFrequenciesHz.getOrNull(index) ?: 0f
                                    val sliderValue = bandCurve.getOrNull(index) ?: 0.5f

                                    VerticalBandControl(
                                        label = formatBandLabel(centerHz, stringResource(R.string.eq_band)),
                                        value = sliderValue,
                                        dbLabel = formatBandDb(
                                            sliderValue,
                                            hardwareInfo.minLevelDb,
                                            hardwareInfo.maxLevelDb
                                        ),
                                        onValueChange = { value ->
                                            val next = bandCurve.toMutableList()
                                            while (next.size < hardwareInfo.bandCount) next.add(0.5f)
                                            next[index] = value.coerceIn(0f, 1f)
                                            bandCurve = next
                                            selectedTemplateId = NEXORA_CUSTOM_TEMPLATE_ID
                                        },
                                        onValueChangeFinished = {
                                            scope.launch {
                                                repository.saveCustomPreset(customName, bandCurve)
                                                repository.setEnabled(enabled)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.eq_unsupported_device),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(stringResource(R.string.eq_customization), fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text(stringResource(R.string.eq_profile_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {
                                    val template = NexoraEqualizerTemplates.resolve(selectedTemplateId)
                                    bandCurve = resolveCurveForBands(template, hardwareInfo.bandCount)
                                },
                                label = { Text(stringResource(R.string.eq_reload)) },
                                leadingIcon = { Icon(imageVector = Icons.Filled.Tune, contentDescription = null) }
                            )
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        repository.saveCustomPreset(customName, bandCurve)
                                        repository.setEnabled(enabled)
                                    }
                                    selectedTemplateId = NEXORA_CUSTOM_TEMPLATE_ID
                                },
                                label = { Text(stringResource(R.string.action_save)) },
                                leadingIcon = { Icon(imageVector = Icons.Filled.Save, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            enabled = false
                            selectedTemplateId = NexoraEqualizerTemplates.flat.id
                            bandCurve = resolveCurveForBands(NexoraEqualizerTemplates.flat, hardwareInfo.bandCount)
                            scope.launch {
                                repository.restoreDefault()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.eq_reset_all))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_close))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerPresetCard(
    templateName: String,
    templateIcon: String,
    templateDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(104.dp),
        shape = RoundedCornerShape(22.dp),
        border = if (selected) {
            BorderStroke(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(templateIcon, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = templateName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = templateDescription,
                    maxLines = 2,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerticalBandControl(
    label: String,
    value: Float,
    dbLabel: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 178.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier
                    .width(178.dp)
                    .height(40.dp)
                    .graphicsLayer(rotationZ = -90f)
            )
        }
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = dbLabel, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun resolveInitialCurve(
    savedSettings: EqualizerSettings,
    bandCount: Int
): List<Float> {
    return if (savedSettings.templateId == NEXORA_CUSTOM_TEMPLATE_ID && savedSettings.customCurve.isNotEmpty()) {
        resolveCurveForBands(savedSettings.customCurve, bandCount)
    } else {
        resolveCurveForBands(NexoraEqualizerTemplates.resolve(savedSettings.templateId), bandCount)
    }
}

@Composable
private fun templateLabel(templateId: String, customName: String): String =
    if (templateId == NEXORA_CUSTOM_TEMPLATE_ID) {
        customName.trim().ifBlank { stringResource(R.string.eq_customization) }
    } else {
        stringResource(NexoraEqualizerTemplates.resolve(templateId).nameRes)
    }

private fun formatBandLabel(centerHz: Float, fallback: String): String {
    if (centerHz <= 0f) return fallback
    return when {
        centerHz >= 1000f -> "${(centerHz / 1000f).roundToInt()} kHz"
        else -> "${centerHz.roundToInt()} Hz"
    }
}

private fun formatBandDb(level: Float, minDb: Float, maxDb: Float): String {
    val db = minDb + level.coerceIn(0f, 1f) * (maxDb - minDb)
    return "${String.format(java.util.Locale.US, "%.1f", db)} dB"
}
