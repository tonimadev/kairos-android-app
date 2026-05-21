package digital.tonima.kairos.ui.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.viewmodel.AlarmIntent
import digital.tonima.core.viewmodel.AlarmSideEffect
import digital.tonima.core.viewmodel.AlarmViewModel
import digital.tonima.kairos.BuildConfig.ADMOB_BANNER_AD_UNIT_ALARM_ACTIVITY
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.AdBannerView
import digital.tonima.kairos.ui.theme.KairosTheme
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    @Inject
    lateinit var proUserProvider: ProUserProvider

    private val viewModel: AlarmViewModel by viewModels()

    private val finishReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action == AlarmSoundAndVibrateService.ACTION_FINISH_ALARM_ACTIVITY) {
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        super.onCreate(savedInstanceState)

        viewModel.handleIntent(
            AlarmIntent.Init(
                eventTitle =
                    intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE)
                        ?: getString(R.string.upcoming_event),
                uniqueId = intent.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1),
                eventId = intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L),
                startTime = intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, -1L),
                meetingUrl = intent.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL),
            ),
        )

        collectSideEffects()

        setContent {
            val isProUser by proUserProvider.isProUser.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            KairosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                                    .verticalScroll(rememberScrollState())
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
                                text = uiState.eventTitle,
                                fontSize = 32.sp,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Spacer(modifier = Modifier.height(48.dp))

                            if (uiState.hasMeetingUrl) {
                                Button(
                                    onClick = {
                                        viewModel.handleIntent(AlarmIntent.JoinMeeting)
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 60.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                ) {
                                    Text(
                                        text = getString(R.string.disable_alarm_and_join_meeting),
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        uiState.meetingUrl?.let { url ->
                                            val clipboard =
                                                getSystemService(
                                                    Context.CLIPBOARD_SERVICE,
                                                ) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Meeting Link", url)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(
                                                this@AlarmActivity,
                                                getString(R.string.link_copied),
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 60.dp),
                                ) {
                                    Text(
                                        text = getString(R.string.copy_meeting_link),
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.handleIntent(AlarmIntent.Snooze)
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .heightIn(min = 60.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                        ),
                                ) {
                                    Text(
                                        text = getString(R.string.snooze),
                                        fontSize = 18.sp,
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.handleIntent(AlarmIntent.Stop)
                                    },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .heightIn(min = 60.dp),
                                ) {
                                    Text(
                                        text = getString(R.string.stop),
                                        fontSize = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun collectSideEffects() {
        lifecycleScope.launch {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    AlarmSideEffect.FinishScreen -> {
                        AlarmSoundAndVibrateService.stopAlarm(
                            this@AlarmActivity,
                            Analytics.SOURCE_ACTIVITY,
                        )
                        finish()
                    }
                    is AlarmSideEffect.OpenMeetingUrl -> {
                        try {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)),
                            )
                        } catch (e: Exception) {
                            logcat { "Failed to open meeting URL: ${e.message}" }
                        }
                    }
                    is AlarmSideEffect.SendSnoozeBroadcast -> {
                        val snoozeIntent =
                            Intent(this@AlarmActivity, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_SNOOZE
                                putExtra(AlarmReceiver.EXTRA_SOURCE, Analytics.SOURCE_ACTIVITY)
                                putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, effect.eventTitle)
                                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, effect.uniqueId)
                                putExtra(AlarmReceiver.EXTRA_EVENT_ID, effect.eventId)
                                putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, effect.startTime)
                            }
                        sendBroadcast(snoozeIntent)
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
        if (viewModel.didUserStopAlarm) {
            AlarmSoundAndVibrateService.stopAlarm(this)
        }
    }
}
