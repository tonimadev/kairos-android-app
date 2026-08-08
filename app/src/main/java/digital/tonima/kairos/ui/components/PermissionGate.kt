package digital.tonima.kairos.ui.components

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import digital.tonima.core.viewmodel.SettingsIntent
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.core.viewmodel.SettingsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    settingsUiState: SettingsUiState,
    settingsViewModel: SettingsViewModel,
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
                onAlreadyAuthorizedClick = { settingsViewModel.handleIntent(SettingsIntent.CheckPermissions) },
                onProvidePermissionClick = openExactAlarmSettings,
                onSkipClick = { settingsViewModel.handleIntent(SettingsIntent.SkipExactAlarmPermission) },
            )
        }

        !settingsUiState.hasFullScreenIntentPermission -> {
            FullScreenIntentPermissionScreen(
                onAlreadyAuthorizedClick = { settingsViewModel.handleIntent(SettingsIntent.CheckPermissions) },
                onOpenSettingsClick = openFullScreenIntentSettings,
                onSkipClick = { settingsViewModel.handleIntent(SettingsIntent.SkipFullScreenIntentPermission) },
            )
        }

        else -> content()
    }
}
