package digital.tonima.kairos.ui.view

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import digital.tonima.core.model.Event
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.EventSideEffect
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.kairos.BuildConfig.ADMOB_BANNER_AD_UNIT_HOME
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.AdBannerView
import digital.tonima.kairos.ui.components.AiActions
import digital.tonima.kairos.ui.components.AiSuggestionsDialog
import digital.tonima.kairos.ui.components.CreateEventDialog
import digital.tonima.kairos.ui.components.DrawerContent
import digital.tonima.kairos.ui.components.EventActions
import digital.tonima.kairos.ui.components.ExactAlarmPermissionScreen
import digital.tonima.kairos.ui.components.FullScreenIntentPermissionScreen
import digital.tonima.kairos.ui.components.MainContent
import digital.tonima.kairos.ui.components.SettingsActions
import digital.tonima.kairos.ui.components.StandardPermissionsScreen
import digital.tonima.kairos.ui.theme.Dimensions
import kotlinx.coroutines.launch
import logcat.logcat

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

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
    var aiConfirmationData by remember { mutableStateOf<EventSideEffect.RequireUserConfirmation?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val standardPermissionsToRequest =
        remember {
            mutableListOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    val standardPermissionState = rememberMultiplePermissionsState(permissions = standardPermissionsToRequest)

    val locationPermissionsToRequest =
        remember {
            mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    val locationPermissionState = rememberMultiplePermissionsState(permissions = locationPermissionsToRequest)

    LaunchedEffect(uiState.isLocationAlarmEnabled) {
        if (uiState.isLocationAlarmEnabled && !locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

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
        viewModel.handleIntent(EventIntent.CheckPermissions)
    }

    val googleSignInLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.handleIntent(EventIntent.HandleGoogleSignInResult(result.data))
        }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventSideEffect.RequireUserConfirmation -> {
                    aiConfirmationData = effect
                }
                is EventSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
                is EventSideEffect.AIToolError -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
                is EventSideEffect.OpenMeetingUrl -> {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
                    } catch (e: Exception) {
                        logcat { "Failed to open meeting URL: ${e.message}" }
                    }
                }
                is EventSideEffect.CopyToClipboard -> {
                    val clipboard =
                        context.getSystemService(
                            Context.CLIPBOARD_SERVICE,
                        ) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Meeting Link", effect.text)
                    clipboard.setPrimaryClip(clip)
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
                is EventSideEffect.LaunchGoogleSignIn -> {
                    googleSignInLauncher.launch(effect.intent)
                }
            }
        }
    }

    val googleCalendarNotFound = stringResource(R.string.google_calendar_not_found)
    val aiInstruction = stringResource(R.string.ai_briefing_instruction)
    val voiceCapturePrompt = stringResource(R.string.cd_voice_capture)

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                results?.firstOrNull()?.let { spokenText ->
                    viewModel.handleIntent(EventIntent.AskAi(spokenText, aiInstruction))
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.handleIntent(EventIntent.CheckPermissions)
                    viewModel.handleIntent(EventIntent.RefreshEvents)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                isProUser = isProUser,
                isAiUser = isAiUser,
                onUpgradeToProClick = { viewModel.handleIntent(EventIntent.UpgradeToProRequest) },
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
            topBar = { EventTopBar(onOpenMenu = { scope.launch { drawerState.open() } }) },
            bottomBar = {
                EventBottomBar(
                    uiState = uiState,
                    isAiUser = isAiUser,
                    onOpenCalendar = {
                        val intent =
                            context.packageManager
                                .getLaunchIntentForPackage("com.google.android.calendar")
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, googleCalendarNotFound, Toast.LENGTH_SHORT).show()
                        }
                    },
                    handleIntent = viewModel::handleIntent,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.handleIntent(EventIntent.ShowCreateEventDialog()) },
                    containerColor = Color(0xFFDEFA5F),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Alarm",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            },
            floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center,
            containerColor = Color(0xFF25252D),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                EventScreenContent(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    viewModel = viewModel,
                    isProUser = isProUser,
                    standardPermissionState = standardPermissionState,
                    openAppSettings = openAppSettings,
                    openExactAlarmSettings = openExactAlarmSettings,
                    openFullScreenIntentSettings = openFullScreenIntentSettings,
                    launchVoiceCapture = launchVoiceCapture,
                    onSubscriptionRequest = onSubscriptionRequest,
                    windowSizeClass = windowSizeClass,
                )
            }
        }

        if (uiState.showSubscriptionConfirmation) {
            LaunchedEffect(uiState.showSubscriptionConfirmation) {
                onSubscriptionRequest()
                viewModel.handleIntent(EventIntent.DismissUpgradeConfirmation)
            }
        }

        if (uiState.showPurchaseConfirmation) {
            LaunchedEffect(uiState.showPurchaseConfirmation) {
                onPurchaseRequest()
                viewModel.handleIntent(EventIntent.DismissUpgradeConfirmation)
            }
        }

        if (uiState.showRatingBottomSheet) {
            RatingBottomSheet(
                onDismissRequest = { viewModel.handleIntent(EventIntent.RateLater) },
                onRateNow = {
                    context.findActivity()?.let { activity ->
                        viewModel.handleIntent(EventIntent.RateNow(activity))
                    } ?: run {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=${context.packageName}".toUri(),
                            )
                        context.startActivity(intent)
                        viewModel.handleIntent(EventIntent.RateNow())
                    }
                },
                onRateLater = { viewModel.handleIntent(EventIntent.RateLater) },
                onRateNeverShow = { viewModel.handleIntent(EventIntent.RateNever) },
            )
        }

        aiConfirmationData?.let { data ->
            AlertDialog(
                onDismissRequest = {
                    aiConfirmationData = null
                    viewModel.handleIntent(EventIntent.RejectPendingAction)
                },
                title = { Text(text = data.title.asString(context)) },
                text = { Text(text = data.message.asString(context)) },
                confirmButton = {
                    TextButton(onClick = {
                        aiConfirmationData = null
                        viewModel.handleIntent(EventIntent.ApprovePendingAction)
                    }) {
                        Text(text = stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        aiConfirmationData = null
                        viewModel.handleIntent(EventIntent.RejectPendingAction)
                    }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun EventScreenContent(
    paddingValues: PaddingValues,
    uiState: EventScreenUiState,
    viewModel: EventViewModel,
    isProUser: Boolean,
    standardPermissionState: MultiplePermissionsState,
    openAppSettings: () -> Unit,
    openExactAlarmSettings: () -> Unit,
    openFullScreenIntentSettings: () -> Unit,
    launchVoiceCapture: () -> Unit,
    onSubscriptionRequest: () -> Unit,
    windowSizeClass: WindowSizeClass?,
) {
    val context = LocalContext.current
    val cannotOpenEvent = stringResource(R.string.cannot_open_event)
    val aiInstruction = stringResource(R.string.ai_briefing_instruction)

    if (uiState.showCreateEventDialog) {
        CreateEventDialog(
            onDismiss = { viewModel.handleIntent(EventIntent.DismissCreateEventDialog) },
            onCreate = { calendarId, title, desc, loc, start, end, allDay ->
                viewModel.handleIntent(
                    EventIntent
                        .CreateEvent(
                            calendarId,
                            title,
                            desc,
                            loc,
                            start,
                            end,
                            allDay,
                        ),
                )
            },
            availableCalendars = uiState.availableCalendars,
            initialDate = uiState.selectedDate,
            voiceEventData = uiState.voiceEventData,
        )
    }
    if (uiState.showAiSuggestionsDialog) {
        AiSuggestionsDialog(
            onDismiss = { viewModel.handleIntent(EventIntent.DismissAiSuggestionsDialog) },
            onSuggestionClick = { suggestion ->
                viewModel.handleIntent(
                    EventIntent
                        .AskAi(suggestion, aiInstruction),
                )
            },
            onVoiceClick = {
                viewModel.handleIntent(EventIntent.DismissAiSuggestionsDialog)
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
        AdBannerView(adId = ADMOB_BANNER_AD_UNIT_HOME, isProUser = isProUser)
        Box(modifier = Modifier.weight(1f)) {
            when {
                !uiState.hasCalendarPermission || !uiState.hasPostNotificationsPermission -> {
                    StandardPermissionsScreen(
                        onSettingsClick = openAppSettings,
                        onRetryClick = { standardPermissionState.launchMultiplePermissionRequest() },
                    )
                }

                !uiState.hasExactAlarmPermission -> {
                    ExactAlarmPermissionScreen(
                        onAlreadyAuthorizedClick = { viewModel.handleIntent(EventIntent.CheckPermissions) },
                        onProvidePermissionClick = openExactAlarmSettings,
                        onSkipClick = { viewModel.handleIntent(EventIntent.SkipExactAlarmPermission) },
                    )
                }

                !uiState.hasFullScreenIntentPermission -> {
                    FullScreenIntentPermissionScreen(
                        onAlreadyAuthorizedClick = { viewModel.handleIntent(EventIntent.CheckPermissions) },
                        onOpenSettingsClick = openFullScreenIntentSettings,
                        onSkipClick = { viewModel.handleIntent(EventIntent.SkipFullScreenIntentPermission) },
                    )
                }

                else -> {
                    MainContent(
                        uiState = uiState,
                        eventActions =
                            EventActions(
                                onRefresh = { viewModel.handleIntent(EventIntent.RefreshEvents) },
                                onEventToggle = { event, enabled, all ->
                                    viewModel.handleIntent(
                                        EventIntent
                                            .ToggleEventAlarm(event, enabled, all),
                                    )
                                },
                                onEventVibrateToggle = { event, enabled ->
                                    viewModel.handleIntent(
                                        EventIntent
                                            .ToggleEventVibrate(event, enabled),
                                    )
                                },
                                onMonthChanged = { viewModel.handleIntent(EventIntent.ChangeMonth(it)) },
                                onDateSelected = { viewModel.handleIntent(EventIntent.SelectDate(it)) },
                                onEventClick = { event: Event ->
                                    val uri =
                                        ContentUris.withAppendedId(
                                            CalendarContract.Events.CONTENT_URI,
                                            event.id,
                                        )
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, uri).apply {
                                            putExtra("beginTime", event.startTime)
                                        }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, cannotOpenEvent, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReturnToToday = { viewModel.handleIntent(EventIntent.ReturnToToday) },
                                onSearchQueryChanged = {
                                    viewModel.handleIntent(
                                        EventIntent.SearchQueryChanged(it),
                                    )
                                },
                                onJoinMeeting = { url ->
                                    viewModel.handleIntent(EventIntent.JoinMeeting(url))
                                },
                                onCopyMeetingUrl = { url ->
                                    viewModel.handleIntent(EventIntent.CopyMeetingUrl(url))
                                },
                                onFetchWeather = { viewModel.handleIntent(EventIntent.FetchWeather) },
                            ),
                        settingsActions =
                            SettingsActions(
                                onToggle = { viewModel.handleIntent(EventIntent.ToggleGlobalAlarms(it)) },
                                onDismissAutostart = { viewModel.handleIntent(EventIntent.DismissAutostartSuggestion) },
                                onVibrateToggle = { viewModel.handleIntent(EventIntent.ToggleVibrateOnly(it)) },
                                onAllDayAlarmsToggle = { viewModel.handleIntent(EventIntent.ToggleAllDayAlarms(it)) },
                                onAllDayAlarmHourChanged = {
                                    viewModel.handleIntent(
                                        EventIntent.UpdateAllDayAlarmHour(it),
                                    )
                                },
                                onAlarmOffsetChanged = { viewModel.handleIntent(EventIntent.UpdateAlarmOffset(it)) },
                                onSnoozeTimeChanged = {
                                    viewModel.handleIntent(EventIntent.UpdateSnoozeTime(it))
                                },
                                onSkipWeekendsToggle = {
                                    viewModel.handleIntent(EventIntent.ToggleSkipWeekends(it))
                                },
                                onAutoDismissMinutesChanged = {
                                    viewModel.handleIntent(EventIntent.UpdateAutoDismissMinutes(it))
                                },
                                onCalendarFilterToggle = { id, enabled ->
                                    viewModel.handleIntent(
                                        EventIntent
                                            .ToggleCalendarFilter(id, enabled),
                                    )
                                },
                                onLocationAlarmToggle = {
                                    viewModel.handleIntent(
                                        EventIntent.ToggleLocationAlarm(it),
                                    )
                                },
                                onTransportModeChanged = {
                                    viewModel.handleIntent(EventIntent.ChangeTransportMode(it))
                                },
                                onTemperatureUnitToggle = {
                                    viewModel.handleIntent(EventIntent.ToggleTemperatureUnit(it))
                                },
                                onGoogleSignInClick = {
                                    viewModel.handleIntent(EventIntent.SignInWithGoogle)
                                },
                                onGoogleSignOutClick = {
                                    viewModel.handleIntent(EventIntent.SignOutFromGoogle)
                                },
                            ),
                        aiActions =
                            AiActions(
                                onGenerateBriefing = {
                                    viewModel.handleIntent(
                                        EventIntent
                                            .GenerateDailyBriefing(aiInstruction),
                                    )
                                },
                                onUpgradeToPro = { viewModel.handleIntent(EventIntent.UpgradeToProIARequest) },
                                onSubscriptionRequest = onSubscriptionRequest,
                                onVoiceCaptureClick = { viewModel.handleIntent(EventIntent.ShowAiSuggestionsDialog) },
                                onClearAiResponse = { viewModel.handleIntent(EventIntent.ClearAiResponse) },
                                onSpeakAiResponse = { viewModel.handleIntent(EventIntent.SpeakAiResponse) },
                                onStopSpeaking = { viewModel.handleIntent(EventIntent.StopSpeaking) },
                                onReply = launchVoiceCapture,
                            ),
                        windowSizeClass = windowSizeClass,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTopBar(onOpenMenu: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.alarm),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        actions = {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.cd_open_menu),
                    tint = Color.White,
                )
            }
        },
    )
}

@Composable
private fun EventBottomBar(
    uiState: EventScreenUiState,
    isAiUser: Boolean,
    onOpenCalendar: () -> Unit,
    handleIntent: (EventIntent) -> Unit,
) {
    val context = LocalContext.current
    val dailyBriefingPrompt = stringResource(R.string.prompt_daily_briefing)
    NavigationBar(
        containerColor = Color(0xFF2C2C38),
        contentColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        val navItemColors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFDEFA5F),
                selectedTextColor = Color(0xFFDEFA5F),
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color(0xFFB0B0C0),
                unselectedTextColor = Color(0xFFB0B0C0),
            )

        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Rounded.Alarm, contentDescription = stringResource(R.string.alarms)) },
            label = { Text(stringResource(R.string.alarms)) },
            colors = navItemColors,
        )

        if (isAiUser) {
            NavigationBarItem(
                selected = false,
                onClick = { onOpenCalendar() },
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.calendar)) },
                label = { Text(stringResource(R.string.calendar)) },
                colors = navItemColors,
            )

            Spacer(modifier = Modifier.weight(1f))

            NavigationBarItem(
                selected = false,
                onClick = {
                    handleIntent(
                        EventIntent.GenerateDailyBriefing(dailyBriefingPrompt),
                    )
                },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = stringResource(R.string.ai_briefing)) },
                label = { Text(stringResource(R.string.briefing)) },
                colors = navItemColors,
            )
            NavigationBarItem(
                selected = false,
                onClick = {
                    handleIntent(EventIntent.ShowAiSuggestionsDialog)
                },
                icon = { Icon(Icons.Rounded.Mic, contentDescription = stringResource(R.string.voice)) },
                label = { Text(stringResource(R.string.voice)) },
                colors = navItemColors,
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))

            NavigationBarItem(
                selected = false,
                onClick = { onOpenCalendar() },
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.calendar)) },
                label = { Text(stringResource(R.string.calendar)) },
                colors = navItemColors,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingBottomSheet(
    onDismissRequest: () -> Unit,
    onRateNow: () -> Unit,
    onRateLater: () -> Unit,
    onRateNeverShow: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.rate_app_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
            Text(
                text = stringResource(R.string.rate_app_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = Center,
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            Button(onClick = onRateNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rate_now)) }
            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
            TextButton(
                onClick = onRateLater,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.rate_later)) }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimensions.SpacingSmall),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TextButton(onClick = onRateNeverShow, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.rate_never), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        }
    }
}
