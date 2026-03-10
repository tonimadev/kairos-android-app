package digital.tonima.kairos.ui.view

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import digital.tonima.core.model.Event
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.kairos.BuildConfig.ADMOB_BANNER_AD_UNIT_HOME
import digital.tonima.kairos.R.drawable
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.R.drawable.date_range
import digital.tonima.kairos.ui.components.AdBannerView
import digital.tonima.kairos.ui.components.AiActions
import digital.tonima.kairos.ui.components.AiSuggestionsDialog
import digital.tonima.kairos.ui.components.AiVoiceInteractionCard
import digital.tonima.kairos.ui.components.CreateEventDialog
import digital.tonima.kairos.ui.components.DrawerContent
import digital.tonima.kairos.ui.components.EventActions
import digital.tonima.kairos.ui.components.ExactAlarmPermissionScreen
import digital.tonima.kairos.ui.components.FullScreenIntentPermissionScreen
import digital.tonima.kairos.ui.components.MainContent
import digital.tonima.kairos.ui.components.SettingsActions
import digital.tonima.kairos.ui.components.SpeedDialFab
import digital.tonima.kairos.ui.components.SpeedDialItem
import digital.tonima.kairos.ui.components.StandardPermissionsScreen
import digital.tonima.kairos.ui.theme.Dimensions
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(
    viewModel: EventViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onPurchaseRequest: () -> Unit,
    onSubscriptionRequest: () -> Unit,
    windowSizeClass: WindowSizeClass? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val isAiUser by viewModel.isAiUser.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val standardPermissionsToRequest =
        remember {
            mutableListOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    val standardPermissionState =
        rememberMultiplePermissionsState(permissions = standardPermissionsToRequest)

    val openAppSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    val openExactAlarmSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM"))
        } else {
            Toast.makeText(context, R.string.not_applicable_on_this_android_version, Toast.LENGTH_SHORT).show()
        }
    }
    val openFullScreenIntentSettings = {
        val intent =
            Intent("android.settings.MANAGE_APP_ALL_ALARMS").apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, R.string.not_applicable_on_this_android_version, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(standardPermissionState.allPermissionsGranted) {
        if (!standardPermissionState.allPermissionsGranted) {
            standardPermissionState.launchMultiplePermissionRequest()
        }
        viewModel.checkAllPermissions()
    }

    LaunchedEffect(uiState.hasCalendarPermission) {
        if (uiState.hasCalendarPermission) {
            viewModel.loadAvailableCalendars()
        }
    }

    val googleCalendarNotFound = stringResource(R.string.google_calendar_not_found)
    val cannotOpenEvent = stringResource(R.string.cannot_open_event)
    val aiInstruction = stringResource(R.string.ai_briefing_instruction)
    val voiceCapturePrompt = stringResource(R.string.cd_voice_capture)

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.firstOrNull()
                if (spokenText != null) {
                    viewModel.askAi(spokenText, aiInstruction)
                }
            }
        }

    val launchVoiceCapture = {
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, voiceCapturePrompt)
            }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.cannot_open_event, Toast.LENGTH_SHORT).show()
        }
    }

    val startVoiceCapture = viewModel::onStartVoiceCaptureRequest

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.checkAllPermissions()
                    viewModel.refresh()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                isProUser = isProUser,
                isAiUser = isAiUser,
                onUpgradeToProClick = viewModel::onUpgradeToProRequest,
                onOurOtherAppsClick = {
                    val browserIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/dev?id=6594602823307179845".toUri(),
                        )
                    context.startActivity(browserIntent)
                },
                onCloseDrawer = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            topBar = {
                EventTopBar(onOpenMenu = { scope.launch { drawerState.open() } })
            },
            floatingActionButton = {
                EventFloatingActionButtons(
                    uiState = uiState,
                    isAiUser = isAiUser,
                    onClearAiResponse = viewModel::clearAiResponse,
                    onSpeakAiResponse = viewModel::speakAiResponse,
                    onStopSpeaking = viewModel::stopSpeaking,
                    onStartVoiceCapture = startVoiceCapture,
                    onOpenCalendar = {
                        val intent =
                            context.packageManager.getLaunchIntentForPackage("com.google.android.calendar")
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    googleCalendarNotFound,
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    },
                    onCreateEvent = viewModel::onCreateEventRequest,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            if (uiState.showCreateEventDialog) {
                CreateEventDialog(
                    onDismiss = viewModel::onCreateEventDismiss,
                    onCreate = viewModel::createEvent,
                    availableCalendars = uiState.availableCalendars,
                    initialDate = uiState.selectedDate,
                    voiceEventData = uiState.voiceEventData,
                )
            }
            if (uiState.showAiSuggestionsDialog) {
                AiSuggestionsDialog(
                    onDismiss = viewModel::onDismissAiSuggestions,
                    onSuggestionClick = { suggestion -> viewModel.askAi(suggestion, aiInstruction) },
                    onVoiceClick = {
                        viewModel.onDismissAiSuggestions()
                        launchVoiceCapture()
                    },
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                AdBannerView(
                    adId = ADMOB_BANNER_AD_UNIT_HOME,
                    isProUser = isProUser,
                )
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    when {
                        !uiState.hasCalendarPermission ||
                            !uiState.hasPostNotificationsPermission -> {
                            StandardPermissionsScreen(
                                onSettingsClick = openAppSettings,
                                onRetryClick = { standardPermissionState.launchMultiplePermissionRequest() },
                            )
                        }

                        !uiState.hasExactAlarmPermission -> {
                            ExactAlarmPermissionScreen(
                                onAlreadyAuthorizedClick = viewModel::checkAllPermissions,
                                onProvidePermissionClick = openExactAlarmSettings,
                                onSkipClick = { viewModel.skipExactAlarmPermission() },
                            )
                        }

                        !uiState.hasFullScreenIntentPermission -> {
                            FullScreenIntentPermissionScreen(
                                onAlreadyAuthorizedClick = viewModel::checkAllPermissions,
                                onOpenSettingsClick = openFullScreenIntentSettings,
                                onSkipClick = { viewModel.skipFullScreenIntentPermission() },
                            )
                        }

                        else -> {
                            MainContent(
                                uiState = uiState,
                                eventActions =
                                    EventActions(
                                        onRefresh = { viewModel.onMonthChanged(uiState.currentMonth, true) },
                                        onEventToggle = viewModel::onEventAlarmToggle,
                                        onEventVibrateToggle = viewModel::onEventVibrateToggle,
                                        onMonthChanged = viewModel::onMonthChanged,
                                        onDateSelected = viewModel::onDateSelected,
                                        onEventClick = { event: Event ->
                                            val uri =
                                                ContentUris.withAppendedId(
                                                    CalendarContract.Events.CONTENT_URI,
                                                    event.id,
                                                )
                                            val intent =
                                                Intent(Intent.ACTION_VIEW, uri).apply {
                                                    putExtra(
                                                        "beginTime",
                                                        event.startTime,
                                                    )
                                                }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast
                                                    .makeText(
                                                        context,
                                                        cannotOpenEvent,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            }
                                        },
                                        onReturnToToday = viewModel::returnToToday,
                                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                                    ),
                                settingsActions =
                                    SettingsActions(
                                        onToggle = viewModel::onAlarmsToggle,
                                        onDismissAutostart = viewModel::dismissAutostartSuggestion,
                                        onVibrateToggle = viewModel::onVibrateOnlyChanged,
                                        onAllDayAlarmsToggle = viewModel::onAllDayAlarmsToggle,
                                        onAllDayAlarmHourChanged = viewModel::onAllDayAlarmHourChanged,
                                        onAlarmOffsetChanged = viewModel::onAlarmOffsetChanged,
                                        onSnoozeTimeChanged = viewModel::onSnoozeTimeChanged,
                                        onCalendarFilterToggle = viewModel::onCalendarFilterToggle,
                                    ),
                                aiActions =
                                    AiActions(
                                        onGenerateBriefing = {
                                            viewModel.generateDailyBriefing(aiInstruction)
                                        },
                                        onUpgradeToPro = viewModel::onUpgradeToProRequest,
                                        onSubscriptionRequest = onSubscriptionRequest,
                                        onVoiceCaptureClick = startVoiceCapture,
                                        onClearAiResponse = viewModel::clearAiResponse,
                                        onSpeakAiResponse = viewModel::speakAiResponse,
                                        onStopSpeaking = viewModel::stopSpeaking,
                                    ),
                                windowSizeClass = windowSizeClass,
                            )
                        }
                    }
                }
            }
        }
        if (uiState.showUpgradeConfirmation) {
            LaunchedEffect(uiState.showUpgradeConfirmation) {
                onPurchaseRequest()
                viewModel.onDismissUpgradeConfirmation()
            }
        }

        if (uiState.showRatingDialog) {
            RatingDialog(
                onDismissRequest = { viewModel.onRatingDialogDismiss() },
                onRateNow = {
                    val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                    context.startActivity(intent)
                    viewModel.onRateNow()
                },
                onRateLater = { viewModel.onRateLater() },
                onRateNeverShow = { viewModel.onRateNeverShow() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTopBar(onOpenMenu: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        navigationIcon = {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    painterResource(drawable.menu),
                    contentDescription = stringResource(R.string.cd_open_menu),
                )
            }
        },
        actions = { },
    )
}

@Composable
private fun EventFloatingActionButtons(
    uiState: digital.tonima.core.viewmodel.EventScreenUiState,
    isAiUser: Boolean,
    onClearAiResponse: () -> Unit,
    onSpeakAiResponse: () -> Unit,
    onStopSpeaking: () -> Unit,
    onStartVoiceCapture: () -> Unit,
    onOpenCalendar: () -> Unit,
    onCreateEvent: () -> Unit,
) {
    val speedDialItems =
        buildList {
            if (isAiUser) {
                add(
                    SpeedDialItem(
                        icon = painterResource(R.drawable.ic_mic),
                        label = stringResource(R.string.cd_voice_capture),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = onStartVoiceCapture,
                    ),
                )
            }
            if (uiState.hasCalendarPermission) {
                add(
                    SpeedDialItem(
                        icon = painterResource(digital.tonima.kairos.R.drawable.ic_add),
                        label = stringResource(R.string.create_event),
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        onClick = onCreateEvent,
                    ),
                )
            }
            if (uiState.hasCalendarPermission &&
                uiState.hasExactAlarmPermission &&
                uiState.hasFullScreenIntentPermission
            ) {
                add(
                    SpeedDialItem(
                        icon = painterResource(date_range),
                        label = stringResource(R.string.open_calendar),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onOpenCalendar,
                    ),
                )
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.PaddingNormal),
        horizontalAlignment = Alignment.End,
    ) {
        AiVoiceInteractionCard(
            question = uiState.lastAiQuestion,
            response = uiState.aiResponse,
            isAsking = uiState.isAskingAi,
            isSpeaking = uiState.isSpeaking,
            onSpeak = onSpeakAiResponse,
            onStopSpeaking = onStopSpeaking,
            onDismiss = onClearAiResponse,
            modifier = Modifier.padding(bottom = Dimensions.SpacingSmall),
        )

        SpeedDialFab(items = speedDialItems)
    }
}

@Composable
private fun RatingDialog(
    onDismissRequest: () -> Unit,
    onRateNow: () -> Unit,
    onRateLater: () -> Unit,
    onRateNeverShow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.rate_app_title)) },
        text = { Text(stringResource(R.string.rate_app_message)) },
        confirmButton = {
            Button(onClick = onRateNow) {
                Text(stringResource(R.string.rate_now))
            }
        },
        dismissButton = {
            Column {
                Button(onClick = onRateLater) {
                    Text(stringResource(R.string.rate_later))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRateNeverShow) {
                    Text(stringResource(R.string.rate_never))
                }
            }
        },
    )
}
