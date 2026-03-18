package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.analytics.Analytics
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel
    @Inject
    constructor(
        private val analytics: Analytics,
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
                )
            }
        }

        private fun onSnooze() {
            val state = _uiState.value

            analytics.logEvent(
                Analytics.EVENT_ALARM_SNOOZE,
                mapOf(
                    Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY,
                    Analytics.PARAM_EVENT_TITLE to state.eventTitle.take(100),
                ),
            )

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
            val state = _uiState.value
            userStoppedAlarm = true

            analytics.logEvent(
                Analytics.EVENT_ALARM_STOP,
                mapOf(
                    Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY,
                    Analytics.PARAM_EVENT_TITLE to state.eventTitle.take(100),
                ),
            )

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
            analytics.logEvent(
                Analytics.EVENT_JOIN_MEETING,
                mapOf(
                    Analytics.PARAM_EVENT_TITLE to state.eventTitle.take(100),
                ),
            )

            state.meetingUrl?.let { url ->
                _sideEffect.trySend(AlarmSideEffect.OpenMeetingUrl(url))
            }
            _sideEffect.trySend(AlarmSideEffect.FinishScreen)
        }
    }
