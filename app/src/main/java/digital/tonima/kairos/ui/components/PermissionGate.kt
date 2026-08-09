package digital.tonima.kairos.ui.components

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import digital.tonima.core.viewmodel.SettingsUiState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    settingsUiState: SettingsUiState,
    onCheckPermissions: () -> Unit,
    onSkipExactAlarmPermission: () -> Unit,
    onSkipFullScreenIntentPermission: () -> Unit,
    standardPermissionState: MultiplePermissionsState,
    locationPermissionState: MultiplePermissionsState,
    openAppSettings: () -> Unit,
    openExactAlarmSettings: () -> Unit,
    openFullScreenIntentSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    when {
        !settingsUiState.hasCalendarPermission || !settingsUiState.hasPostNotificationsPermission -> {
            StandardPermissionsScreen(
                onSettingsClick = openAppSettings,
                onRetryClick = { standardPermissionState.launchMultiplePermissionRequest() },
            )
        }

        settingsUiState.isLocationAlarmEnabled && !locationPermissionState.allPermissionsGranted -> {
            StandardPermissionsScreen(
                onSettingsClick = openAppSettings,
                onRetryClick = { locationPermissionState.launchMultiplePermissionRequest() },
            )
        }

        !settingsUiState.hasExactAlarmPermission -> {
            ExactAlarmPermissionScreen(
                onAlreadyAuthorizedClick = onCheckPermissions,
                onProvidePermissionClick = openExactAlarmSettings,
                onSkipClick = onSkipExactAlarmPermission,
            )
        }

        !settingsUiState.hasFullScreenIntentPermission -> {
            FullScreenIntentPermissionScreen(
                onAlreadyAuthorizedClick = onCheckPermissions,
                onOpenSettingsClick = openFullScreenIntentSettings,
                onSkipClick = onSkipFullScreenIntentPermission,
            )
        }

        else -> content()
    }
}
