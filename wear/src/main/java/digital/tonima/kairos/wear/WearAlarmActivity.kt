package digital.tonima.kairos.wear

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_ID
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_START_TIME
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_MEETING_URL
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_SOURCE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.viewmodel.AlarmIntent
import digital.tonima.core.viewmodel.AlarmSideEffect
import digital.tonima.core.viewmodel.AlarmViewModel
import digital.tonima.kairos.core.R
import digital.tonima.kairos.wear.ui.theme.Dimensions
import digital.tonima.kairos.wear.ui.theme.KairosTheme
import kotlinx.coroutines.launch
import logcat.logcat

@AndroidEntryPoint
class WearAlarmActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val title = intent?.getStringExtra(EXTRA_EVENT_TITLE) ?: getString(R.string.upcoming_event)
        val uniqueId = intent?.getIntExtra(EXTRA_UNIQUE_ID, -1) ?: -1
        val eventId = intent?.getLongExtra(EXTRA_EVENT_ID, -1L) ?: -1L
        val startTime = intent?.getLongExtra(EXTRA_EVENT_START_TIME, -1L) ?: -1L
        val meetingUrl = intent?.getStringExtra(EXTRA_MEETING_URL)

        viewModel.handleIntent(
            AlarmIntent.Init(
                eventTitle = title,
                uniqueId = uniqueId,
                eventId = eventId,
                startTime = startTime,
                meetingUrl = meetingUrl,
            ),
        )

        AlarmSoundAndVibrateService.startAlarm(this, title, uniqueId, eventId, startTime, meetingUrl)

        collectSideEffects()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            KairosTheme {
                WearAlarmScreen(
                    title = uiState.eventTitle,
                    meetingUrl = uiState.meetingUrl,
                    onJoinMeeting = { viewModel.handleIntent(AlarmIntent.JoinMeeting) },
                    onSnooze = { viewModel.handleIntent(AlarmIntent.Snooze) },
                    onStop = { viewModel.handleIntent(AlarmIntent.Stop) },
                )
            }
        }
    }

    private fun collectSideEffects() {
        lifecycleScope.launch {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    AlarmSideEffect.FinishScreen -> {
                        AlarmSoundAndVibrateService.stopAlarm(
                            this@WearAlarmActivity,
                            Analytics.SOURCE_ACTIVITY,
                        )
                        finish()
                    }
                    is AlarmSideEffect.OpenMeetingUrl -> {
                        try {
                            val meetingIntent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
                            meetingIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                            meetingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(meetingIntent)
                        } catch (e: Exception) {
                            logcat { "Failed to open meeting URL: ${e.message}" }
                        }
                    }
                    is AlarmSideEffect.SendSnoozeBroadcast -> {
                        val snoozeIntent =
                            Intent(
                                this@WearAlarmActivity,
                                AlarmReceiver::class.java,
                            ).apply {
                                action = AlarmReceiver.ACTION_SNOOZE
                                putExtra(EXTRA_SOURCE, Analytics.SOURCE_ACTIVITY)
                                putExtra(EXTRA_EVENT_TITLE, effect.eventTitle)
                                putExtra(EXTRA_UNIQUE_ID, effect.uniqueId)
                                putExtra(EXTRA_EVENT_ID, effect.eventId)
                                putExtra(EXTRA_EVENT_START_TIME, effect.startTime)
                                putExtra(EXTRA_MEETING_URL, effect.meetingUrl)
                            }
                        sendBroadcast(snoozeIntent)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun WearAlarmScreen(
    title: String,
    meetingUrl: String? = null,
    onJoinMeeting: () -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    color = MaterialTheme.colorScheme.primary,
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = Dimensions.PaddingMedium),
                )
            }

            if (!meetingUrl.isNullOrEmpty()) {
                item {
                    Button(
                        onClick = onJoinMeeting,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = Dimensions.PaddingSmall),
                    ) {
                        Text(
                            text = stringResource(R.string.disable_alarm_and_join_meeting),
                            fontSize = Dimensions.ButtonFontSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.snooze),
                            fontSize = Dimensions.ButtonFontSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.stop),
                            fontSize = Dimensions.ButtonFontSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    device = "id:wearos_large_round",
    name = "Wear Alarm Screen",
)
@Composable
fun WearAlarmScreenPreview() {
    KairosTheme {
        WearAlarmScreen(
            title = "Reunião começa em 5 min",
            meetingUrl = "https://meet.google.com/abc-defg-hij",
            onJoinMeeting = {},
            onSnooze = {},
            onStop = {},
        )
    }
}
