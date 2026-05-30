package digital.tonima.kairos.wear.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import digital.tonima.core.model.Event
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.kairos.wear.ui.components.AppHeaderTitle
import digital.tonima.kairos.wear.ui.components.CalendarFilterChip
import digital.tonima.kairos.wear.ui.components.CalendarFilterHeader
import digital.tonima.kairos.wear.ui.components.EventCard
import digital.tonima.kairos.wear.ui.components.EventsListSection
import digital.tonima.kairos.wear.ui.components.EventsSectionHeader
import digital.tonima.kairos.wear.ui.components.ExactAlarmPermissionChip
import digital.tonima.kairos.wear.ui.components.GlobalAlarmsToggle
import digital.tonima.kairos.wear.ui.components.OpenOnPhoneChip
import digital.tonima.kairos.wear.ui.components.VersionFooter
import digital.tonima.kairos.wear.ui.components.VibrateOnlyToggle
import digital.tonima.kairos.wear.ui.components.WearOsPermissionsScreenContent
import digital.tonima.kairos.wear.ui.theme.Dimensions
import digital.tonima.kairos.wear.ui.theme.KairosTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WearApp(
    viewModel: EventViewModel = hiltViewModel(),
    wearCalendarViewModel: WearCalendarViewModel = hiltViewModel(),
    permissionManager: PermissionManager,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val next24hEvents by wearCalendarViewModel.next24hEvents.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    val standardPermissionsToRequest =
        remember {
            permissionManager.calendarPermissions.toMutableList().apply {
                addAll(permissionManager.notificationPermissions)
            }
        }

    val standardPermissionState =
        rememberMultiplePermissionsState(permissions = standardPermissionsToRequest)

    LaunchedEffect(standardPermissionState.allPermissionsGranted) {
        if (!standardPermissionState.allPermissionsGranted) {
            standardPermissionState.launchMultiplePermissionRequest()
        }
        viewModel.handleIntent(EventIntent.CheckPermissions)
    }

    LaunchedEffect(uiState.hasCalendarPermission) {
        if (uiState.hasCalendarPermission) {
            viewModel.handleIntent(EventIntent.LoadCalendars)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.handleIntent(EventIntent.CheckPermissions)
                    wearCalendarViewModel.handleIntent(WearCalendarIntent.RequestRescan)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AppScaffold {
        ScreenScaffold(
            scrollState = listState,
            timeText = { TimeText() },
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = Dimensions.PaddingLarge,
                        start = Dimensions.PaddingSmall,
                        end = Dimensions.PaddingSmall,
                        bottom = Dimensions.PaddingLarge,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState,
            ) {
                val needsStandardPermissions = !standardPermissionState.allPermissionsGranted
                val needsExactAlarmPermission = !uiState.hasExactAlarmPermission
                if (needsStandardPermissions) {
                    item {
                        WearOsPermissionsScreenContent(
                            onSettingsClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            },
                            onRetryClick = { standardPermissionState.launchMultiplePermissionRequest() },
                        )
                    }
                } else {
                    if (needsExactAlarmPermission) {
                        item { ExactAlarmPermissionChip() }
                    }
                    item { AppHeaderTitle() }
                    item {
                        Spacer(Modifier.height(Dimensions.SpacingSmall))
                        GlobalAlarmsToggle(
                            checked = uiState.isGlobalAlarmEnabled,
                            onCheckedChange = {
                                    isChecked ->
                                viewModel.handleIntent(EventIntent.ToggleGlobalAlarms(isChecked))
                            },
                        )
                    }
                    item {
                        Spacer(Modifier.height(Dimensions.SpacingExtraSmall))
                        VibrateOnlyToggle(
                            checked = uiState.vibrateOnly,
                            onCheckedChange = { enabled ->
                                viewModel.handleIntent(EventIntent.ToggleVibrateOnly(enabled))
                            },
                        )
                    }
                    if (uiState.availableCalendars.isNotEmpty()) {
                        item { CalendarFilterHeader() }
                        items(uiState.availableCalendars) { calendar ->
                            CalendarFilterChip(
                                calendar = calendar,
                                enabledCalendarIds = uiState.enabledCalendarIds,
                                onToggle = { calendarId, enabled ->
                                    viewModel.handleIntent(
                                        EventIntent.ToggleCalendarFilter(calendarId, enabled),
                                    )
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(Dimensions.SpacingSmall)) }
                    item { EventsSectionHeader() }
                    item {
                        EventsListSection(
                            events = next24hEvents,
                            isRefreshing = false,
                            isGlobalAlarmEnabled = uiState.isGlobalAlarmEnabled,
                            onEventToggle = { event, isEnabled, applyToSeries ->
                                viewModel.handleIntent(EventIntent.ToggleEventAlarm(event, isEnabled, applyToSeries))
                            },
                        )
                    }
                    item {
                        Spacer(Modifier.height(Dimensions.SpacingSmall))
                        OpenOnPhoneChip(onClick = {
                            digital.tonima.kairos.wear.ui.actions.OpenOnPhone
                                .launch(context)
                        })
                    }
                    item { VersionFooter() }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:wearos_large_round")
@Composable
fun WearAppPreview() {
    KairosTheme {
        val listState = rememberScalingLazyListState()
        val sampleEvents =
            listOf(
                Event(
                    id = 1L,
                    title = "Reunião de projeto",
                    startTime = System.currentTimeMillis() + 60 * 60 * 1000,
                    isAlarmEnabled = true,
                    isRecurring = false,
                ),
                Event(
                    id = 2L,
                    title = "Aula de Yoga",
                    startTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000,
                    isAlarmEnabled = false,
                    isRecurring = true,
                ),
            )
        AppScaffold {
            ScreenScaffold(
                scrollState = listState,
                timeText = { TimeText() },
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = Dimensions.PaddingLarge,
                            start = Dimensions.PaddingSmall,
                            end = Dimensions.PaddingSmall,
                            bottom = Dimensions.PaddingLarge,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = listState,
                ) {
                    item { AppHeaderTitle() }
                    item { Spacer(Modifier.height(Dimensions.SpacingSmall)) }
                    item { OpenOnPhoneChip(onClick = {}) }
                    item { Spacer(Modifier.height(8.dp)) }
                    item { GlobalAlarmsToggle(checked = true, onCheckedChange = {}) }
                    item { Spacer(Modifier.height(4.dp)) }
                    item { VibrateOnlyToggle(checked = false, onCheckedChange = {}) }
                    item { EventsSectionHeader() }
                    items(sampleEvents) { event ->
                        EventCard(
                            event = event,
                            isGloballyEnabled = true,
                            onToggle = {},
                        )
                    }
                    item { VersionFooter() }
                }
            }
        }
    }
}
