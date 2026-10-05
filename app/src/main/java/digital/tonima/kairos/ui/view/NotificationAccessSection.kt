package digital.tonima.kairos.ui.view

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ui.theme.Dimensions
import digital.tonima.kairos.service.KairosNotificationListenerService
import digital.tonima.kairos.ui.components.SettingsActions
import logcat.LogPriority
import logcat.logcat

/**
 * Opt-in features that need notification access. Enabling any of them first shows a prominent
 * disclosure of what is read and where it goes, and only then sends the user to the system screen.
 */
@Composable
fun NotificationAccessSection(
    settingsUiState: SettingsUiState,
    settingsActions: SettingsActions,
) {
    val context = LocalContext.current
    // What to do once the user accepts the disclosure; null means "just open the system screen".
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDisclosure by remember { mutableStateOf(false) }

    val requestEnable: (enable: () -> Unit) -> Unit = { enable ->
        if (settingsUiState.hasNotificationListenerAccess) {
            enable()
        } else {
            pendingEnable = enable
            showDisclosure = true
        }
    }
    val toggle: (Boolean, (Boolean) -> Unit) -> Unit = { on, apply ->
        if (on) requestEnable { apply(true) } else apply(false)
    }
    val anyEnabled =
        settingsUiState.isNotificationDedupEnabled ||
            settingsUiState.isFocusDigestEnabled ||
            settingsUiState.isEventSuggestionsEnabled

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingDefault)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.IconSizeSmall),
            )
            Text(
                stringResource(R.string.notif_features_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(R.string.notif_features_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        NotificationFeatureRow(
            icon = Icons.Rounded.NotificationsOff,
            label = stringResource(R.string.notif_dedup_label),
            description = stringResource(R.string.notif_dedup_description),
            checked = settingsUiState.isNotificationDedupEnabled,
            onCheckedChange = { toggle(it, settingsActions.onNotificationDedupToggle) },
        )
        NotificationFeatureRow(
            icon = Icons.Rounded.DoNotDisturbOn,
            label = stringResource(R.string.notif_digest_label),
            description = stringResource(R.string.notif_digest_description),
            checked = settingsUiState.isFocusDigestEnabled,
            onCheckedChange = { toggle(it, settingsActions.onFocusDigestToggle) },
        )
        NotificationFeatureRow(
            icon = Icons.Rounded.EventAvailable,
            label = stringResource(R.string.notif_suggestions_label),
            description = stringResource(R.string.notif_suggestions_description),
            checked = settingsUiState.isEventSuggestionsEnabled,
            onCheckedChange = { toggle(it, settingsActions.onEventSuggestionsToggle) },
        )

        if (anyEnabled && !settingsUiState.hasNotificationListenerAccess) {
            Text(
                stringResource(R.string.notif_access_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(
                onClick = {
                    pendingEnable = null
                    showDisclosure = true
                },
            ) { Text(stringResource(R.string.notif_open_access_settings)) }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.notif_disclosure_title)) },
            text = { Text(stringResource(R.string.notif_disclosure_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisclosure = false
                        pendingEnable?.invoke()
                        pendingEnable = null
                        openNotificationAccessSettings(context)
                    },
                ) { Text(stringResource(R.string.notif_disclosure_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDisclosure = false
                        pendingEnable = null
                    },
                ) { Text(stringResource(R.string.notif_disclosure_cancel)) }
            },
        )
    }
}

@Composable
private fun NotificationFeatureRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.IconSizeSmall),
            )
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Opens this app's own toggle when the system supports it (API 30+), else the general list. */
private fun openNotificationAccessSettings(context: Context) {
    val detail =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, KairosNotificationListenerService::class.java).flattenToString(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(detail)
    } catch (e: ActivityNotFoundException) {
        logcat("NotificationAccess", LogPriority.WARN) { "Notification access detail screen unavailable: ${e.message}" }
        try {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (inner: ActivityNotFoundException) {
            logcat(
                "NotificationAccess",
                LogPriority.WARN,
            ) { "Notification access settings unavailable: ${inner.message}" }
        }
    }
}
