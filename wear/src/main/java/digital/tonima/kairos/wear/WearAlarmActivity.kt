package digital.tonima.kairos.wear

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
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
import digital.tonima.kairos.core.R
import digital.tonima.kairos.wear.ui.theme.Dimensions
import digital.tonima.kairos.wear.ui.theme.KairosTheme
import javax.inject.Inject

@AndroidEntryPoint
class WearAlarmActivity : ComponentActivity() {
    @Inject
    lateinit var analytics: Analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val title = intent?.getStringExtra(EXTRA_EVENT_TITLE) ?: getString(R.string.upcoming_event)
        val uniqueId = intent?.getIntExtra(EXTRA_UNIQUE_ID, -1) ?: -1
        val eventId = intent?.getLongExtra(EXTRA_EVENT_ID, -1L) ?: -1L
        val startTime = intent?.getLongExtra(EXTRA_EVENT_START_TIME, -1L) ?: -1L
        val meetingUrl = intent?.getStringExtra(EXTRA_MEETING_URL)

        // Iniciamos o serviço de som e vibração
        AlarmSoundAndVibrateService.startAlarm(this, title, uniqueId, eventId, startTime, meetingUrl)

        setContent {
            KairosTheme {
                WearAlarmScreen(
                    title = title,
                    meetingUrl = meetingUrl,
                    onJoinMeeting = {
                        analytics.logEvent(
                            Analytics.EVENT_ALARM_STOP,
                            mapOf(
                                Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY,
                                "action" to "join_meeting",
                            ),
                        )
                        AlarmSoundAndVibrateService.stopAlarm(this, Analytics.SOURCE_ACTIVITY)
                        try {
                            val meetingIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl))
                            meetingIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                            meetingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(meetingIntent)
                        } catch (e: Exception) {
                            // No Wear OS, pode falhar se não houver navegador/app.
                        }
                        finish()
                    },
                    onSnooze = {
                        analytics.logEvent(
                            Analytics.EVENT_ALARM_SNOOZE,
                            mapOf(Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY),
                        )
                        val snoozeIntent =
                            Intent(this, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_SNOOZE
                                putExtra(EXTRA_SOURCE, Analytics.SOURCE_ACTIVITY)
                                putExtra(EXTRA_EVENT_TITLE, title)
                                putExtra(EXTRA_UNIQUE_ID, uniqueId)
                                putExtra(EXTRA_EVENT_ID, eventId)
                                putExtra(EXTRA_EVENT_START_TIME, startTime)
                                putExtra(EXTRA_MEETING_URL, meetingUrl)
                            }
                        sendBroadcast(snoozeIntent)
                        finish()
                    },
                    onStop = {
                        analytics.logEvent(
                            Analytics.EVENT_ALARM_STOP,
                            mapOf(Analytics.PARAM_SOURCE to Analytics.SOURCE_ACTIVITY),
                        )
                        AlarmSoundAndVibrateService.stopAlarm(this, Analytics.SOURCE_ACTIVITY)
                        finish()
                    },
                )
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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PaddingNormal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Dimensions.PaddingMedium),
        )

        if (!meetingUrl.isNullOrEmpty()) {
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
