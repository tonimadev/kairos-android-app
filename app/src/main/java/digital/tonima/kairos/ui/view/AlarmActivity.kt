package digital.tonima.kairos.ui.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.kairos.BuildConfig.ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.AdBannerView
import digital.tonima.kairos.ui.theme.KairosTheme
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var proUserProvider: ProUserProvider

    private var userStoppedAlarm = false

    private val finishReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action == AlarmSoundAndVibrateService.ACTION_FINISH_ALARM_ACTIVITY) {
                    userStoppedAlarm = true
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventTitle =
            intent.getStringExtra("EXTRA_EVENT_TITLE") ?: getString(R.string.upcoming_event)
        val uniqueId = intent.getIntExtra("EXTRA_UNIQUE_ID", -1)
        val eventId = intent.getLongExtra("EXTRA_EVENT_ID", -1L)
        val startTime = intent.getLongExtra("EXTRA_EVENT_START_TIME", -1L)

        setContent {
            val isProUser by proUserProvider.isProUser.collectAsStateWithLifecycle()

            KairosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AdBannerView(
                            adId = ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY,
                            isProUser = isProUser,
                        )
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = getString(R.string.event_alarm),
                                fontSize = 24.sp,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = eventTitle,
                                fontSize = 32.sp,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Spacer(modifier = Modifier.height(48.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Button(
                                    onClick = {
                                        val snoozeIntent =
                                            Intent(this@AlarmActivity, AlarmReceiver::class.java).apply {
                                                action = AlarmReceiver.ACTION_SNOOZE
                                                putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, eventTitle)
                                                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, uniqueId)
                                                putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
                                                putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, startTime)
                                            }
                                        sendBroadcast(snoozeIntent)
                                        finish()
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                        ),
                                ) {
                                    Text(text = getString(R.string.snooze), fontSize = 18.sp)
                                }

                                Button(
                                    onClick = {
                                        userStoppedAlarm = true
                                        AlarmSoundAndVibrateService.stopAlarm(this@AlarmActivity)
                                        finish()
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .height(60.dp),
                                ) {
                                    Text(text = getString(R.string.stop), fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(AlarmSoundAndVibrateService.ACTION_FINISH_ALARM_ACTIVITY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(finishReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(finishReceiver)
        } catch (ignored: IllegalArgumentException) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (userStoppedAlarm) {
            AlarmSoundAndVibrateService.stopAlarm(this)
        }
    }
}
