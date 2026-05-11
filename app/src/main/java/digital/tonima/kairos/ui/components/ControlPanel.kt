package digital.tonima.kairos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Vibration
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.util.openAutostartSettings
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanel(
    uiState: EventScreenUiState,
    settingsActions: SettingsActions,
) {
    val context = LocalContext.current
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

        SettingsCard(
            settingsExpanded = settingsExpanded,
            onSettingsExpandedChange = { settingsExpanded = it },
            uiState = uiState,
            settingsActions = settingsActions,
            currentOffset = currentOffset,
        )

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
private fun SettingsCard(
    settingsExpanded: Boolean,
    onSettingsExpandedChange: (Boolean) -> Unit,
    uiState: EventScreenUiState,
    settingsActions: SettingsActions,
    currentOffset: AlarmOffset,
) {
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
                ) { onSettingsExpandedChange(!settingsExpanded) },
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
                    imageVector = Icons.Rounded.Notifications,
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
                imageVector =
                    if (settingsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
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
            SettingsContent(
                uiState = uiState,
                settingsActions = settingsActions,
                currentOffset = currentOffset,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: EventScreenUiState,
    settingsActions: SettingsActions,
    currentOffset: AlarmOffset,
) {
    var offsetExpanded by remember { mutableStateOf(false) }

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
                    imageVector = Icons.Rounded.Vibration,
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
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimensions.IconSizeSmall),
                )
                Text(
                    stringResource(R.string.skip_weekends_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Switch(checked = uiState.skipWeekends, onCheckedChange = settingsActions.onSkipWeekendsToggle)
        }

        AllDayAlarmsSection(
            enabled = uiState.allDayAlarmsEnabled,
            onToggle = settingsActions.onAllDayAlarmsToggle,
            hour = uiState.allDayAlarmHour,
            onHourChange = settingsActions.onAllDayAlarmHourChanged,
        )

        AlarmOffsetDropdown(
            expanded = offsetExpanded,
            onExpandedChange = { offsetExpanded = it },
            currentOffset = currentOffset,
            onOffsetChange = settingsActions.onAlarmOffsetChanged,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))

        LocationAlarmSection(
            isAiUser = uiState.isAiUser,
            isEnabled = uiState.isLocationAlarmEnabled,
            onToggle = settingsActions.onLocationAlarmToggle,
            preferredTransportMode = uiState.preferredTransportMode,
            onTransportModeChanged = settingsActions.onTransportModeChanged,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))

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

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))

        Column {
            Text(
                text =
                    stringResource(R.string.auto_dismiss_label) +
                        ": ${uiState.autoDismissMinutes} min",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = uiState.autoDismissMinutes.toFloat(),
                onValueChange = { settingsActions.onAutoDismissMinutesChanged(it.roundToInt()) },
                valueRange = 1f..30f,
                steps = 28,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState.availableCalendars.isNotEmpty()) {
            CalendarFilterSection(
                availableCalendars = uiState.availableCalendars,
                enabledCalendarIds = uiState.enabledCalendarIds,
                onCalendarFilterToggle = settingsActions.onCalendarFilterToggle,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))

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
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimensions.IconSizeSmall),
                )
                Text(
                    "Use Celsius for Weather",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Switch(checked = uiState.isTemperatureInCelsius, onCheckedChange = settingsActions.onTemperatureUnitToggle)
        }
    }
}

@Composable
private fun AllDayAlarmsSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    hour: Int,
    onHourChange: (Int) -> Unit,
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
                imageVector = Icons.Rounded.CalendarMonth,
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
            checked = enabled,
            onCheckedChange = onToggle,
        )
    }

    if (enabled) {
        Text(
            text =
                stringResource(R.string.all_day_alarm_time) +
                    ": ${"%02d:00".format(hour)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = hour.toFloat(),
            onValueChange = { onHourChange(it.roundToInt()) },
            valueRange = 0f..23f,
            steps = 22,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmOffsetDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currentOffset: AlarmOffset,
    onOffsetChange: (AlarmOffset) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = offsetLabel(currentOffset),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.alarm_offset_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(Dimensions.RadiusMedium),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            AlarmOffset.entries.forEach { offset ->
                DropdownMenuItem(
                    text = { Text(offsetLabel(offset)) },
                    onClick = {
                        onOffsetChange(offset)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationAlarmSection(
    isAiUser: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    preferredTransportMode: String,
    onTransportModeChanged: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.PaddingTiny),
            ) {
                Text(
                    text = stringResource(R.string.location_alarm_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!isAiUser) {
                    Text(
                        text = stringResource(R.string.pro_ia_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(Dimensions.RadiusSmall))
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text =
                    if (isAiUser) {
                        stringResource(R.string.location_alarm_description)
                    } else {
                        stringResource(R.string.geo_alarm_pro_only)
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isEnabled && isAiUser,
            onCheckedChange = onToggle,
        )
    }

    if (isEnabled) {
        TransportModeDropdown(
            preferredTransportMode = preferredTransportMode,
            onTransportModeChanged = onTransportModeChanged,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportModeDropdown(
    preferredTransportMode: String,
    onTransportModeChanged: (String) -> Unit,
) {
    var transportExpanded by remember { mutableStateOf(false) }
    val transportModes = listOf("driving", "walking", "bicycling", "transit")
    val transportLabels =
        mapOf(
            "driving" to stringResource(R.string.transport_driving),
            "walking" to stringResource(R.string.transport_walking),
            "bicycling" to stringResource(R.string.transport_bicycling),
            "transit" to stringResource(R.string.transport_transit),
        )

    ExposedDropdownMenuBox(
        expanded = transportExpanded,
        onExpandedChange = { transportExpanded = !transportExpanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = transportLabels[preferredTransportMode] ?: preferredTransportMode,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.transport_mode_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = transportExpanded,
                )
            },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(Dimensions.RadiusMedium),
        )
        ExposedDropdownMenu(
            expanded = transportExpanded,
            onDismissRequest = { transportExpanded = false },
        ) {
            transportModes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(transportLabels[mode] ?: mode) },
                    onClick = {
                        onTransportModeChanged(mode)
                        transportExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CalendarFilterSection(
    availableCalendars: List<DeviceCalendar>,
    enabledCalendarIds: Set<Long>,
    onCalendarFilterToggle: (Long, Boolean) -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))
    Text(
        text = stringResource(R.string.calendar_filter_title),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    availableCalendars.forEach { calendar ->
        val isChecked =
            enabledCalendarIds.isEmpty() ||
                enabledCalendarIds.contains(calendar.id)
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
                    onCalendarFilterToggle(calendar.id, checked)
                },
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
