package digital.tonima.kairos.ui.view

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.SettingsActions
import digital.tonima.kairos.ui.theme.Dimensions
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: EventScreenUiState,
    settingsActions: SettingsActions,
) {
    BackHandler {
        settingsActions.onCloseSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = settingsActions.onCloseSettings) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val currentOffset = AlarmOffset.fromMinutes(uiState.alarmOffsetMinutes)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.PaddingNormal),
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
                        contentDescription = stringResource(R.string.vibrate_only),
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

            CustomRingtoneSection(
                customRingtoneUri = uiState.customRingtoneUri,
                onCustomRingtoneSelected = settingsActions.onCustomRingtoneSelected,
            )

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
                        contentDescription = stringResource(R.string.skip_weekends_label),
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

            var offsetExpanded by remember { mutableStateOf(false) }
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
                        contentDescription = stringResource(R.string.use_celsius_for_weather),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimensions.IconSizeSmall),
                    )
                    Text(
                        stringResource(R.string.use_celsius_for_weather),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Switch(
                    checked = uiState.isTemperatureInCelsius,
                    onCheckedChange = settingsActions.onTemperatureUnitToggle,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))

            IntegrationsSection(
                isGoogleConnected = uiState.isGoogleConnected,
                onGoogleSignInClick = settingsActions.onGoogleSignInClick,
                onGoogleSignOutClick = settingsActions.onGoogleSignOutClick,
            )
        }
    }
}

@Composable
fun CustomRingtoneSection(
    customRingtoneUri: String?,
    onCustomRingtoneSelected: (String?) -> Unit,
) {
    val context = LocalContext.current

    val ringtonePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                onCustomRingtoneSelected(uri?.toString())
            }
        }

    val defaultRingtoneName = stringResource(R.string.default_ringtone)
    val ringtoneName =
        remember(customRingtoneUri, defaultRingtoneName) {
            if (customRingtoneUri == null) {
                defaultRingtoneName
            } else {
                RingtoneManager.getRingtone(context, customRingtoneUri.toUri())?.getTitle(context)
                    ?: defaultRingtoneName
            }
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
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = stringResource(R.string.custom_ringtone),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.IconSizeSmall),
            )
            Column {
                Text(
                    stringResource(R.string.custom_ringtone),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    ringtoneName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(
            onClick = {
                val intent =
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_TYPE,
                            RingtoneManager.TYPE_ALARM or
                                RingtoneManager.TYPE_NOTIFICATION or
                                RingtoneManager.TYPE_RINGTONE,
                        )
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            customRingtoneUri?.toUri(),
                        )
                    }
                ringtonePickerLauncher.launch(intent)
            },
        ) {
            Text(stringResource(R.string.change))
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
                contentDescription = stringResource(R.string.all_day_alarms),
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

@Composable
private fun IntegrationsSection(
    isGoogleConnected: Boolean,
    onGoogleSignInClick: () -> Unit,
    onGoogleSignOutClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
    ) {
        Text(
            text = stringResource(R.string.integrations),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        stringResource(
                            if (isGoogleConnected) R.string.google_logout_title else R.string.google_login_title,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.google_login_description),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = if (isGoogleConnected) onGoogleSignOutClick else onGoogleSignInClick,
                modifier = Modifier.padding(start = Dimensions.PaddingSmall),
            ) {
                Text(
                    text = stringResource(if (isGoogleConnected) R.string.logout else R.string.login_google),
                    color =
                        if (isGoogleConnected) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
