package digital.tonima.kairos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.util.openAutostartSettings
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.R.drawable.alarm
import digital.tonima.kairos.R.drawable.ic_expand_less
import digital.tonima.kairos.R.drawable.ic_expand_more
import digital.tonima.kairos.R.drawable.vibration
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.R.drawable.date_range
import digital.tonima.kairos.ui.theme.Dimensions
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanel(
    uiState: EventScreenUiState,
    settingsActions: SettingsActions,
) {
    val context = LocalContext.current
    var offsetExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    val currentOffset = AlarmOffset.fromMinutes(uiState.alarmOffsetMinutes)

    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
    ) {
        AlarmsToggleRow(
            modifier = Modifier.padding(top = Dimensions.PaddingSmall),
            alarmsEnabled = uiState.isGlobalAlarmEnabled,
            onToggle = settingsActions.onToggle,
        )

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel =
                            stringResource(
                                if (settingsExpanded) R.string.cd_collapse_settings else R.string.cd_expand_settings,
                            ),
                    ) { settingsExpanded = !settingsExpanded },
            shape = RoundedCornerShape(Dimensions.RadiusLarge),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.ElevationExtraSmall),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.PaddingNormal, vertical = Dimensions.PaddingDefault),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                ) {
                    Icon(
                        painter = painterResource(alarm),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimensions.IconSizeTiny),
                    )
                    Text(
                        text = stringResource(R.string.alarm_offset_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!settingsExpanded) {
                        Text(
                            text = "· ${offsetLabel(currentOffset)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Icon(
                    painter =
                        painterResource(
                            if (settingsExpanded) ic_expand_less else ic_expand_more,
                        ),
                    contentDescription =
                        stringResource(
                            if (settingsExpanded) R.string.cd_collapse_settings else R.string.cd_expand_settings,
                        ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.IconSizeSmall),
                )
            }

            AnimatedVisibility(
                visible = settingsExpanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            start = Dimensions.PaddingNormal,
                            end = Dimensions.PaddingNormal,
                            bottom = Dimensions.PaddingNormal,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingDefault),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                        ) {
                            Icon(
                                painter = painterResource(vibration),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimensions.IconSizeSmall),
                            )
                            Text(
                                stringResource(R.string.vibrate_only),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Switch(checked = uiState.vibrateOnly, onCheckedChange = settingsActions.onVibrateToggle)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                        ) {
                            Icon(
                                painter = painterResource(date_range),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimensions.IconSizeSmall),
                            )
                            Text(
                                stringResource(R.string.all_day_alarms),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Switch(
                            checked = uiState.allDayAlarmsEnabled,
                            onCheckedChange = settingsActions.onAllDayAlarmsToggle,
                        )
                    }

                    if (uiState.allDayAlarmsEnabled) {
                        Text(
                            text =
                                stringResource(R.string.all_day_alarm_time) +
                                    ": ${"%02d:00".format(uiState.allDayAlarmHour)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = uiState.allDayAlarmHour.toFloat(),
                            onValueChange = { settingsActions.onAllDayAlarmHourChanged(it.roundToInt()) },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = offsetExpanded,
                        onExpandedChange = { offsetExpanded = !offsetExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = offsetLabel(currentOffset),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.alarm_offset_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = offsetExpanded) },
                            modifier =
                                Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                            shape = RoundedCornerShape(Dimensions.RadiusMedium),
                        )
                        ExposedDropdownMenu(
                            expanded = offsetExpanded,
                            onDismissRequest = { offsetExpanded = false },
                        ) {
                            AlarmOffset.entries.forEach { offset ->
                                DropdownMenuItem(
                                    text = { Text(offsetLabel(offset)) },
                                    onClick = {
                                        settingsActions.onAlarmOffsetChanged(offset)
                                        offsetExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text =
                                stringResource(R.string.snooze_time_label) +
                                    ": ${uiState.snoozeTimeMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = uiState.snoozeTimeMinutes.toFloat(),
                            onValueChange = { settingsActions.onSnoozeTimeChanged(it.roundToInt()) },
                            valueRange = 5f..60f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.availableCalendars.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))
                        Text(
                            text = stringResource(R.string.calendar_filter_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        uiState.availableCalendars.forEach { calendar ->
                            val isChecked =
                                uiState.enabledCalendarIds.isEmpty() ||
                                    uiState.enabledCalendarIds.contains(calendar.id)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = calendar.displayName.ifBlank { calendar.accountName },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = calendar.accountName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        settingsActions.onCalendarFilterToggle(calendar.id, checked)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.audioWarning != AudioWarningState.NORMAL) {
            RingerModeWarningCard(ringerMode = uiState.audioWarning)
        }

        if (uiState.showAutostartSuggestion) {
            AutostartSuggestionCard(
                onOpenSettings = { openAutostartSettings(context) },
                onDismiss = settingsActions.onDismissAutostart,
            )
        }
    }
}

@Composable
private fun offsetLabel(offset: AlarmOffset): String =
    when (offset) {
        AlarmOffset.AT_TIME -> stringResource(R.string.alarm_offset_at_time)
        AlarmOffset.FIFTEEN_MINUTES -> stringResource(R.string.alarm_offset_15_min)
        AlarmOffset.THIRTY_MINUTES -> stringResource(R.string.alarm_offset_30_min)
        AlarmOffset.ONE_HOUR -> stringResource(R.string.alarm_offset_1_hour)
    }
