package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.sync.WearMessagingHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel
    @Inject
    constructor(
        private val analytics: Analytics,
        private val wearMessagingHelper: WearMessagingHelper,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AlarmUiState())
        val uiState = _uiState.asStateFlow()

        private val _sideEffect = Channel<AlarmSideEffect>(Channel.BUFFERED)
        val sideEffect = _sideEffect.receiveAsFlow()

        private var userStoppedAlarm = false

        val didUserStopAlarm: Boolean get() = userStoppedAlarm

        fun handleIntent(intent: AlarmIntent) {
            when (intent) {
                is AlarmIntent.Init -> onInit(intent)
                AlarmIntent.Snooze -> onSnooze()
                AlarmIntent.Stop -> onStop()
                AlarmIntent.JoinMeeting -> onJoinMeeting()
                AlarmIntent.OpenMap -> onOpenMap()
            }
        }

        private fun onInit(intent: AlarmIntent.Init) {
            _uiState.update {
                it.copy(
                    eventTitle = intent.eventTitle,
                    uniqueId = intent.uniqueId,
                    eventId = intent.eventId,
                    startTime = intent.startTime,
                    meetingUrl = intent.meetingUrl,
                    eventLocation = intent.eventLocation,
                )
            }
        }

        private fun onSnooze() {
            val state = _uiState.value

            analytics.logEvent(
                Analytics.EVENT_ALARM_SNOOZE,
                mapOf(Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY),
            )

            viewModelScope.launch {
                wearMessagingHelper.sendSnoozeAlarm(
                    uniqueId = state.uniqueId,
                    eventId = state.eventId,
                    eventTitle = state.eventTitle,
                    startTime = state.startTime,
                )
            }

            _sideEffect.trySend(
                AlarmSideEffect.SendSnoozeBroadcast(
                    eventTitle = state.eventTitle,
                    uniqueId = state.uniqueId,
                    eventId = state.eventId,
                    startTime = state.startTime,
                    meetingUrl = state.meetingUrl,
                ),
            )
            _sideEffect.trySend(AlarmSideEffect.FinishScreen)
        }

        private fun onStop() {
            userStoppedAlarm = true
            val state = _uiState.value

            analytics.logEvent(
                Analytics.EVENT_ALARM_STOP,
                mapOf(Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY),
            )

            viewModelScope.launch {
                wearMessagingHelper.sendDismissAlarm(state.uniqueId)
            }

            _sideEffect.trySend(AlarmSideEffect.FinishScreen)
        }

        private fun onJoinMeeting() {
            val state = _uiState.value
            userStoppedAlarm = true

            analytics.logEvent(
                Analytics.EVENT_ALARM_STOP,
                mapOf(
                    Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY,
                    "action" to "join_meeting",
                ),
            )
            analytics.logEvent(Analytics.EVENT_JOIN_MEETING)

            viewModelScope.launch {
                wearMessagingHelper.sendDismissAlarm(state.uniqueId)
            }

            state.meetingUrl?.let { url ->
                _sideEffect.trySend(AlarmSideEffect.OpenMeetingUrl(url))
            }
            _sideEffect.trySend(AlarmSideEffect.FinishScreen)
        }

        private fun onOpenMap() {
            val state = _uiState.value
            userStoppedAlarm = true

            analytics.logEvent(
                Analytics.EVENT_ALARM_STOP,
                mapOf(
                    Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY,
                    "action" to "open_map",
                ),
            )

            viewModelScope.launch {
                wearMessagingHelper.sendDismissAlarm(state.uniqueId)
            }

            state.eventLocation?.let { location ->
                _sideEffect.trySend(AlarmSideEffect.OpenMapUrl(location))
            }
            _sideEffect.trySend(AlarmSideEffect.FinishScreen)
        }
    }
