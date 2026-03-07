package digital.tonima.kairos.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.kairos.core.R
import digital.tonima.kairos.wear.ui.theme.KairosTheme

@AndroidEntryPoint
class WearAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val title =
            intent?.getStringExtra(EXTRA_EVENT_TITLE)
                ?: getString(R.string.upcoming_event)

        setContent {
            KairosTheme {
                WearAlarmScreen(
                    title = title,
                    onStop = {
                        AlarmSoundAndVibrateService.stopAlarm(this)
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
    onStop: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(onClick = onStop) {
            Text(text = stringResource(R.string.stop))
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
            onStop = {},
        )
    }
}
