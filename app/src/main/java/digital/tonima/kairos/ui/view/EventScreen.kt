@file:Suppress("TooManyFunctions", "LongParameterList")

package digital.tonima.kairos.ui.view

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.app.Activity.RESULT_OK
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract.Events.CONTENT_URI
import android.speech.RecognizerIntent.EXTRA_RESULTS
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import digital.tonima.core.model.Event
import digital.tonima.core.viewmodel.AiIntent.AskAi
import digital.tonima.core.viewmodel.AiIntent.ClearAiResponse
import digital.tonima.core.viewmodel.AiIntent.CloseChatDetail
import digital.tonima.core.viewmodel.AiIntent.CloseChatHistoryScreen
import digital.tonima.core.viewmodel.AiIntent.CreateNewChat
import digital.tonima.core.viewmodel.AiIntent.DeleteChat
import digital.tonima.core.viewmodel.AiIntent.DismissAiSuggestionsDialog
import digital.tonima.core.viewmodel.AiIntent.GenerateDailyBriefing
import digital.tonima.core.viewmodel.AiIntent.OpenChatDetail
import digital.tonima.core.viewmodel.AiIntent.OpenChatHistoryScreen
import digital.tonima.core.viewmodel.AiIntent.ShowAiSuggestionsDialog
import digital.tonima.core.viewmodel.AiIntent.SpeakAiResponse
import digital.tonima.core.viewmodel.AiIntent.StopSpeaking
import digital.tonima.core.viewmodel.AiSideEffect
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.AiViewModel
import digital.tonima.core.viewmodel.AuthIntent.SignInWithGoogle
import digital.tonima.core.viewmodel.AuthIntent.SignOutFromGoogle
import digital.tonima.core.viewmodel.AuthViewModel
import digital.tonima.core.viewmodel.EventIntent.ChangeBottomTab
import digital.tonima.core.viewmodel.EventIntent.ChangeMonth
import digital.tonima.core.viewmodel.EventIntent.CloseImportCalendarScreen
import digital.tonima.core.viewmodel.EventIntent.CloseManageCalendarsScreen
import digital.tonima.core.viewmodel.EventIntent.CopyMeetingUrl
import digital.tonima.core.viewmodel.EventIntent.CreateEvent
import digital.tonima.core.viewmodel.EventIntent.DismissCreateEventDialog
import digital.tonima.core.viewmodel.EventIntent.FetchWeather
import digital.tonima.core.viewmodel.EventIntent.JoinMeeting
import digital.tonima.core.viewmodel.EventIntent.OpenImportCalendarScreen
import digital.tonima.core.viewmodel.EventIntent.OpenManageCalendarsScreen
import digital.tonima.core.viewmodel.EventIntent.RefreshEvents
import digital.tonima.core.viewmodel.EventIntent.ReturnToToday
import digital.tonima.core.viewmodel.EventIntent.SearchQueryChanged
import digital.tonima.core.viewmodel.EventIntent.SelectDate
import digital.tonima.core.viewmodel.EventIntent.ShowCreateEventDialog
import digital.tonima.core.viewmodel.EventIntent.ToggleCalendarFilter
import digital.tonima.core.viewmodel.EventIntent.ToggleEventAlarm
import digital.tonima.core.viewmodel.EventIntent.ToggleEventVibrate
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProIARequest
import digital.tonima.core.viewmodel.EventIntent.UpgradeToProRequest
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.core.viewmodel.SettingsIntent.ChangeTransportMode
import digital.tonima.core.viewmodel.SettingsIntent.CloseSettings
import digital.tonima.core.viewmodel.SettingsIntent.DismissAutostartSuggestion
import digital.tonima.core.viewmodel.SettingsIntent.OpenSettings
import digital.tonima.core.viewmodel.SettingsIntent.ToggleAllDayAlarms
import digital.tonima.core.viewmodel.SettingsIntent.ToggleGlobalAlarms
import digital.tonima.core.viewmodel.SettingsIntent.ToggleLocationAlarm
import digital.tonima.core.viewmodel.SettingsIntent.ToggleSkipWeekends
import digital.tonima.core.viewmodel.SettingsIntent.ToggleTemperatureUnit
import digital.tonima.core.viewmodel.SettingsIntent.ToggleVibrateOnly
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAlarmOffset
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAllDayAlarmHour
import digital.tonima.core.viewmodel.SettingsIntent.UpdateAutoDismissMinutes
import digital.tonima.core.viewmodel.SettingsIntent.UpdateCustomRingtoneUri
import digital.tonima.core.viewmodel.SettingsIntent.UpdateSnoozeTime
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.core.viewmodel.SettingsViewModel
import digital.tonima.kairos.BuildConfig.ADMOB_BANNER_AD_UNIT_HOME
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.AdBannerView
import digital.tonima.kairos.ui.components.AiActions
import digital.tonima.kairos.ui.components.AiSuggestionsDialog
import digital.tonima.kairos.ui.components.CreateEventDialog
import digital.tonima.kairos.ui.components.EventActions
import digital.tonima.kairos.ui.components.InsightsContent
import digital.tonima.kairos.ui.components.MainContent
import digital.tonima.kairos.ui.components.PermissionGate
import digital.tonima.kairos.ui.components.SettingsActions

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(
    eventViewModel: EventViewModel = hiltViewModel(),
    aiViewModel: AiViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onPurchaseRequest: () -> Unit,
    onSubscriptionRequest: () -> Unit,
    windowSizeClass: WindowSizeClass? = null,
) {
    val uiState by eventViewModel.uiState.collectAsStateWithLifecycle()
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val isProUser by eventViewModel.isProUser.collectAsStateWithLifecycle()
    val isAiUser by eventViewModel.isAiUser.collectAsStateWithLifecycle()

    var aiConfirmationData by remember { mutableStateOf<AiSideEffect.RequireUserConfirmation?>(null) }
    val context = LocalContext.current

    val standardPermissionsToRequest =
        remember {
            mutableListOf(READ_CALENDAR, WRITE_CALENDAR).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    val standardPermissionState = rememberMultiplePermissionsState(permissions = standardPermissionsToRequest)

    val locationPermissionsToRequest =
        remember {
            mutableListOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        }
    val locationPermissionState = rememberMultiplePermissionsState(permissions = locationPermissionsToRequest)

    val aiInstruction = stringResource(R.string.ai_briefing_instruction)
    val voiceCapturePrompt = stringResource(R.string.cd_voice_capture)

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(
            contract = StartActivityForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val results = result.data?.getStringArrayListExtra(EXTRA_RESULTS)
                results?.firstOrNull()?.let { spokenText ->
                    aiViewModel.handleIntent(AskAi(spokenText, aiInstruction))
                }
            }
        }

    EventScreenInfrastructure(
        eventViewModel = eventViewModel,
        aiViewModel = aiViewModel,
        settingsViewModel = settingsViewModel,
        authViewModel = authViewModel,
        snackbarHostState = snackbarHostState,
        onSubscriptionRequest = onSubscriptionRequest,
        onPurchaseRequest = onPurchaseRequest,
        onSetAiConfirmationData = { aiConfirmationData = it },
    )

    val settingsActions =
        remember(eventViewModel, settingsViewModel, authViewModel) {
            SettingsActions(
                onToggle = { settingsViewModel.handleIntent(ToggleGlobalAlarms(it)) },
                onDismissAutostart = { settingsViewModel.handleIntent(DismissAutostartSuggestion) },
                onVibrateToggle = { settingsViewModel.handleIntent(ToggleVibrateOnly(it)) },
                onAllDayAlarmsToggle = { settingsViewModel.handleIntent(ToggleAllDayAlarms(it)) },
                onAllDayAlarmHourChanged = { settingsViewModel.handleIntent(UpdateAllDayAlarmHour(it)) },
                onAlarmOffsetChanged = { settingsViewModel.handleIntent(UpdateAlarmOffset(it)) },
                onSnoozeTimeChanged = { settingsViewModel.handleIntent(UpdateSnoozeTime(it)) },
                onSkipWeekendsToggle = { settingsViewModel.handleIntent(ToggleSkipWeekends(it)) },
                onAutoDismissMinutesChanged = { settingsViewModel.handleIntent(UpdateAutoDismissMinutes(it)) },
                onCalendarFilterToggle = { id, enabled ->
                    eventViewModel.handleIntent(ToggleCalendarFilter(id, enabled))
                },
                onLocationAlarmToggle = { settingsViewModel.handleIntent(ToggleLocationAlarm(it)) },
                onTransportModeChanged = { settingsViewModel.handleIntent(ChangeTransportMode(it)) },
                onTemperatureUnitToggle = { settingsViewModel.handleIntent(ToggleTemperatureUnit(it)) },
                onGoogleSignInClick = { authViewModel.handleIntent(SignInWithGoogle) },
                onGoogleSignOutClick = { authViewModel.handleIntent(SignOutFromGoogle) },
                onCloseSettings = { settingsViewModel.handleIntent(CloseSettings) },
                onCustomRingtoneSelected = { settingsViewModel.handleIntent(UpdateCustomRingtoneUri(it)) },
            )
        }

    EventScreenShell(
        uiState = uiState,
        isProUser = isProUser,
        isAiUser = isAiUser,
        snackbarHostState = snackbarHostState,
        onUpgradeToPro = { eventViewModel.handleIntent(UpgradeToProRequest) },
        onSettingsClick = { settingsViewModel.handleIntent(OpenSettings) },
        onChatHistoryClick = { aiViewModel.handleIntent(OpenChatHistoryScreen) },
        onImportCalendarClick = { eventViewModel.handleIntent(OpenImportCalendarScreen) },
        onManageCalendarsClick = { eventViewModel.handleIntent(OpenManageCalendarsScreen) },
        onCreateEventClick = { eventViewModel.handleIntent(ShowCreateEventDialog()) },
        onGenerateDailyBriefing = { aiViewModel.handleIntent(GenerateDailyBriefing(it)) },
        onShowAiSuggestions = { aiViewModel.handleIntent(ShowAiSuggestionsDialog) },
        onBottomTabChange = { eventViewModel.handleIntent(ChangeBottomTab(it)) },
    ) { paddingValues ->
        EventScreenRouter(
            uiState = uiState,
            settingsUiState = settingsUiState,
            aiUiState = aiUiState,
            settingsScreen = {
                SettingsScreen(
                    uiState = uiState,
                    settingsUiState = settingsUiState,
                    authUiState = authUiState,
                    settingsActions = settingsActions,
                )
            },
            chatDetailScreen = {
                ChatDetailScreen(
                    messages = aiUiState.chatHistory,
                    isAsking = aiUiState.isAskingAi,
                    isSpeaking = aiUiState.isSpeaking,
                    onBack = { aiViewModel.handleIntent(CloseChatDetail) },
                    onSendMessage = { aiViewModel.handleIntent(AskAi(it, aiInstruction)) },
                    onSpeakToggle = {
                        launchVoiceCapture(
                            context,
                            voiceCapturePrompt,
                            speechRecognizerLauncher,
                        )
                    },
                )
            },
            chatHistoryScreen = {
                ChatHistoryScreen(
                    conversations = aiUiState.conversations,
                    onBack = { aiViewModel.handleIntent(CloseChatHistoryScreen) },
                    onConversationClick = { aiViewModel.handleIntent(OpenChatDetail(it)) },
                    onCreateNewChat = { aiViewModel.handleIntent(CreateNewChat(it)) },
                    onDeleteConversation = { aiViewModel.handleIntent(DeleteChat(it)) },
                )
            },
            importCalendarScreen = {
                ImportCalendarScreen(
                    onNavigateBack = { eventViewModel.handleIntent(CloseImportCalendarScreen) },
                )
            },
            manageCalendarsScreen = {
                ManageCalendarsScreen(
                    onNavigateBack = { eventViewModel.handleIntent(CloseManageCalendarsScreen) },
                )
            },
            mainContent = {
                EventScreenContent(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    aiUiState = aiUiState,
                    settingsUiState = settingsUiState,
                    eventViewModel = eventViewModel,
                    aiViewModel = aiViewModel,
                    settingsViewModel = settingsViewModel,
                    isProUser = isProUser,
                    standardPermissionState = standardPermissionState,
                    locationPermissionState = locationPermissionState,
                    onSubscriptionRequest = onSubscriptionRequest,
                    windowSizeClass = windowSizeClass,
                    settingsActions = settingsActions,
                    launchVoiceCapture = {
                        launchVoiceCapture(
                            context,
                            voiceCapturePrompt,
                            speechRecognizerLauncher,
                        )
                    },
                )
            },
        )
    }

    EventScreenDialogs(
        uiState = uiState,
        aiUiState = aiUiState,
        aiConfirmationData = aiConfirmationData,
        onClearAiConfirmation = { aiConfirmationData = null },
        eventViewModel = eventViewModel,
        aiViewModel = aiViewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun EventScreenContent(
    paddingValues: PaddingValues,
    uiState: EventScreenUiState,
    aiUiState: AiUiState,
    settingsUiState: SettingsUiState,
    eventViewModel: EventViewModel,
    aiViewModel: AiViewModel,
    settingsViewModel: SettingsViewModel,
    isProUser: Boolean,
    standardPermissionState: MultiplePermissionsState,
    locationPermissionState: MultiplePermissionsState,
    onSubscriptionRequest: () -> Unit,
    windowSizeClass: WindowSizeClass?,
    settingsActions: SettingsActions,
    launchVoiceCapture: () -> Unit,
) {
    val context = LocalContext.current
    val cannotOpenEvent = stringResource(R.string.cannot_open_event)
    val aiInstruction = stringResource(R.string.ai_briefing_instruction)

    if (uiState.showCreateEventDialog) {
        CreateEventDialog(
            onDismiss = { eventViewModel.handleIntent(DismissCreateEventDialog) },
            onCreate = { calendarId, title, desc, loc, start, end, allDay ->
                eventViewModel.handleIntent(
                    CreateEvent(calendarId, title, desc, loc, start, end, allDay),
                )
            },
            availableCalendars = uiState.availableCalendars,
            initialDate = uiState.selectedDate,
            voiceEventData = aiUiState.voiceEventData,
        )
    }

    if (aiUiState.showAiSuggestionsDialog) {
        AiSuggestionsDialog(
            onDismiss = { aiViewModel.handleIntent(DismissAiSuggestionsDialog) },
            onSuggestionClick = { suggestion ->
                aiViewModel.handleIntent(AskAi(suggestion, aiInstruction))
            },
            onVoiceClick = {
                aiViewModel.handleIntent(DismissAiSuggestionsDialog)
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
            PermissionGate(
                settingsUiState = settingsUiState,
                settingsViewModel = settingsViewModel,
                standardPermissionState = standardPermissionState,
                locationPermissionState = locationPermissionState,
                openAppSettings = { openAppSettings(context) },
                openExactAlarmSettings = { openExactAlarmSettings(context) },
                openFullScreenIntentSettings = { openFullScreenIntentSettings(context) },
            ) {
                if (uiState.selectedBottomTab == 1) {
                    InsightsContent(
                        uiState = uiState,
                        onIntent = eventViewModel::handleIntent,
                    )
                } else {
                    MainContent(
                        uiState = uiState,
                        settingsUiState = settingsUiState,
                        eventActions =
                            EventActions(
                                onRefresh = { eventViewModel.handleIntent(RefreshEvents) },
                                onEventToggle = { event, enabled, all ->
                                    eventViewModel.handleIntent(ToggleEventAlarm(event, enabled, all))
                                },
                                onEventVibrateToggle = { event, enabled ->
                                    eventViewModel.handleIntent(ToggleEventVibrate(event, enabled))
                                },
                                onMonthChanged = { eventViewModel.handleIntent(ChangeMonth(it)) },
                                onDateSelected = { eventViewModel.handleIntent(SelectDate(it)) },
                                onEventClick = { event: Event ->
                                    val uri = ContentUris.withAppendedId(CONTENT_URI, event.id)
                                    val intent =
                                        Intent(Intent.ACTION_VIEW, uri).apply {
                                            putExtra("beginTime", event.startTime)
                                        }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, cannotOpenEvent, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReturnToToday = { eventViewModel.handleIntent(ReturnToToday) },
                                onSearchQueryChanged = { eventViewModel.handleIntent(SearchQueryChanged(it)) },
                                onJoinMeeting = { url -> eventViewModel.handleIntent(JoinMeeting(url)) },
                                onCopyMeetingUrl = { url -> eventViewModel.handleIntent(CopyMeetingUrl(url)) },
                                onFetchWeather = { eventViewModel.handleIntent(FetchWeather) },
                            ),
                        settingsActions = settingsActions,
                        aiActions =
                            AiActions(
                                onGenerateBriefing = { aiViewModel.handleIntent(GenerateDailyBriefing(aiInstruction)) },
                                onUpgradeToPro = { eventViewModel.handleIntent(UpgradeToProIARequest) },
                                onSubscriptionRequest = onSubscriptionRequest,
                                onVoiceCaptureClick = { aiViewModel.handleIntent(ShowAiSuggestionsDialog) },
                                onClearAiResponse = { aiViewModel.handleIntent(ClearAiResponse) },
                                onSpeakAiResponse = { aiViewModel.handleIntent(SpeakAiResponse) },
                                onStopSpeaking = { aiViewModel.handleIntent(StopSpeaking) },
                                onReply = launchVoiceCapture,
                            ),
                        windowSizeClass = windowSizeClass,
                        aiUiState = aiUiState,
                    )
                }
            }
        }
    }
}
