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
import androidx.core.net.toUri
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
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        super.onCreate(savedInstanceState)

        proUserProvider.refresh()

        viewModel.handleIntent(
            AlarmIntent.Init(
                eventTitle =
                    intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE)
                        ?: getString(R.string.upcoming_event),
                uniqueId = intent.getIntExtra(AlarmReceiver.EXTRA_UNIQUE_ID, -1),
                eventId = intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L),
                startTime = intent.getLongExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, -1L),
                meetingUrl = intent.getStringExtra(AlarmReceiver.EXTRA_MEETING_URL),
                eventLocation = intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_LOCATION),
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
                            }

                            if (uiState.hasLocation) {
                                Button(
                                    onClick = { viewModel.handleIntent(AlarmIntent.OpenMap) },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 60.dp),
                                ) {
                                    Text(
                                        text = getString(R.string.open_map_label),
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (uiState.hasMeetingUrl) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        uiState.meetingUrl?.let { url ->
                                            val clipboard =
                                                getSystemService(
                                                    CLIPBOARD_SERVICE,
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
            viewModel.uiState.collect { state ->
                state.sideEffects.forEach { effect ->
                    when (effect) {
                        AlarmSideEffect.FinishScreen -> {
                            AlarmSoundAndVibrateService.stopAlarm(
                                this@AlarmActivity,
                                Analytics.SOURCE_ACTIVITY,
                            )
                            viewModel.onSideEffectConsumed(effect)
                            finish()
                        }
                        is AlarmSideEffect.OpenMeetingUrl -> {
                            try {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW, effect.url.toUri()),
                                )
                            } catch (e: Exception) {
                                logcat { "Failed to open meeting URL: ${e.message}" }
                            }
                            viewModel.onSideEffectConsumed(effect)
                        }
                        is AlarmSideEffect.OpenMapUrl -> {
                            try {
                                val uri = "google.navigation:q=${Uri.encode(effect.location)}".toUri()
                                val mapIntent =
                                    Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                startActivity(mapIntent)
                            } catch (e: Exception) {
                                logcat { "Failed to open navigation intent: ${e.message}" }
                                try {
                                    startActivity(
                                        Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(effect.location)}".toUri()),
                                    )
                                } catch (inner: Exception) {
                                    logcat { "Failed to open geo intent: ${inner.message}" }
                                }
                            }
                            viewModel.onSideEffectConsumed(effect)
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
                            viewModel.onSideEffectConsumed(effect)
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
            registerReceiver(finishReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(finishReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(finishReceiver)
        } catch (e: IllegalArgumentException) {
            logcat {
                e.stackTraceToString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.didUserStopAlarm) {
            AlarmSoundAndVibrateService.stopAlarm(this)
        }
    }
}
