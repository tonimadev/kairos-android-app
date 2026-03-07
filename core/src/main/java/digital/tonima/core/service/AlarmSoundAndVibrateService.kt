package digital.tonima.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import digital.tonima.core.repository.AppPreferencesRepositoryImpl
import digital.tonima.kairos.core.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import logcat.logcat

class AlarmSoundAndVibrateService : Service() {
    companion object {
        val VIBRATION_PATTERN = longArrayOf(0, 500, 500)
        const val VIBRATION_REPEAT_INDEX = 0
        const val ACTION_START_ALARM = "digital.tonima.core.service.START_ALARM_SOUND"
        const val ACTION_STOP_ALARM = "digital.tonima.core.service.STOP_ALARM_SOUND"
        const val ACTION_FINISH_ALARM_ACTIVITY = "digital.tonima.core.service.FINISH_ALARM_ACTIVITY"
        private const val NOTIFICATION_CHANNEL_ID = "calendar_alarm_channel"
        const val NOTIFICATION_ID = 0xA11A7

        fun startAlarm(
            context: Context,
            eventTitle: String? = null,
            uniqueId: Int = -1,
            eventId: Long = -1L,
            startTime: Long = -1L,
        ) {
            val intent =
                Intent(context, AlarmSoundAndVibrateService::class.java).apply {
                    action = ACTION_START_ALARM
                    if (!eventTitle.isNullOrEmpty()) {
                        putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_TITLE, eventTitle)
                    }
                    putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_UNIQUE_ID, uniqueId)
                    putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_ID, eventId)
                    putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_START_TIME, startTime)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAlarm(context: Context) {
            val stopIntent =
                Intent(context, AlarmSoundAndVibrateService::class.java).apply {
                    action = ACTION_STOP_ALARM
                }
            ContextCompat.startForegroundService(context, stopIntent)
        }
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        logcat { "AlarmSoundAndVibrateService: onStartCommand - action: ${intent?.action}" }

        when (intent?.action) {
            ACTION_START_ALARM -> {
                stopAndReleaseResources()

                val eventTitle =
                    intent.getStringExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_TITLE)
                val uniqueId =
                    intent.getIntExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_UNIQUE_ID, -1)
                val eventId =
                    intent.getLongExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_ID, -1L)
                val startTime =
                    intent.getLongExtra(
                        digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_START_TIME,
                        -1L,
                    )

                ensureForeground(eventTitle, uniqueId, eventId, startTime)

                val vibrateOnly =
                    try {
                        runBlocking { AppPreferencesRepositoryImpl(applicationContext).getVibrateOnly().first() }
                    } catch (e: Exception) {
                        logcat(logcat.LogPriority.ERROR) {
                            "AlarmSoundAndVibrateService: erro ao ler preferência vibrate-only: " +
                                e.localizedMessage
                        }
                        false
                    }

                vibrator =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        vibratorManager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        getSystemService(VIBRATOR_SERVICE) as Vibrator
                    }
                vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, VIBRATION_REPEAT_INDEX))

                if (!vibrateOnly) {
                    val candidateUris =
                        listOfNotNull(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                        )

                    var obtained: Ringtone? = null
                    var usedUri: Uri? = null
                    for (u in candidateUris) {
                        val r = RingtoneManager.getRingtone(applicationContext, u)
                        if (r != null) {
                            obtained = r
                            usedUri = u
                            break
                        }
                    }

                    if (obtained != null) {
                        ringtone = obtained
                        try {
                            ringtone?.audioAttributes =
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                        } catch (e: Throwable) {
                            logcat(logcat.LogPriority.WARN) {
                                "AlarmSoundAndVibrateService: Falha ao definir AudioAttributes: " +
                                    e.localizedMessage
                            }
                        }
                        try {
                            ringtone?.isLooping = true
                        } catch (e: Throwable) {
                            logcat(logcat.LogPriority.WARN) {
                                "AlarmSoundAndVibrateService: Falha ao definir isLooping: " +
                                    e.localizedMessage
                            }
                        }
                        try {
                            ringtone?.play()
                            logcat { "AlarmSoundAndVibrateService: Ringtone iniciado. Uri usada: $usedUri" }
                        } catch (e: Throwable) {
                            logcat(logcat.LogPriority.ERROR) {
                                "AlarmSoundAndVibrateService: Erro ao tocar Ringtone: " +
                                    e.localizedMessage
                            }
                        }
                    } else {
                        logcat(logcat.LogPriority.ERROR) {
                            "AlarmSoundAndVibrateService: Falha ao obter Ringtone " +
                                "(todas as URIs padrão retornaram null)."
                        }
                    }
                } else {
                    logcat { "AlarmSoundAndVibrateService: Modo somente vibrar ativado. Som não será reproduzido." }
                }
            }
            ACTION_STOP_ALARM -> {
                logcat { "AlarmSoundAndVibrateService: Recebida solicitação para parar alarme." }
                ensureForeground(null)
                stopAndReleaseResources()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                logcat { "AlarmSoundAndVibrateService: Ação desconhecida ou nula." }
            }
        }

        return START_NOT_STICKY
    }

    private fun ensureForeground(
        eventTitle: String?,
        uniqueId: Int = -1,
        eventId: Long = -1L,
        startTime: Long = -1L,
    ) {
        val isWatch = packageManager.hasSystemFeature("android.hardware.type.watch")
        val fullScreenPendingIntent =
            try {
                val activityClassName =
                    if (isWatch) {
                        "digital.tonima.kairos.wear.WearAlarmActivity"
                    } else {
                        "digital.tonima.kairos.ui.view.AlarmActivity"
                    }
                val fullScreenIntent =
                    Intent(
                        applicationContext,
                        Class.forName(activityClassName),
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_TITLE, eventTitle)
                        putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_UNIQUE_ID, uniqueId)
                        putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_ID, eventId)
                        putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_START_TIME, startTime)
                    }
                PendingIntent.getActivity(
                    applicationContext,
                    uniqueId,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            } catch (e: Exception) {
                logcat(logcat.LogPriority.ERROR) {
                    "AlarmSoundAndVibrateService: Erro ao criar fullScreenPendingIntent: " +
                        e.localizedMessage
                }
                null
            }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.event_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_description)
                setShowBadge(false)
            }
        nm.createNotificationChannel(channel)

        val snoozeIntent =
            Intent(this, digital.tonima.core.receiver.AlarmReceiver::class.java).apply {
                action = digital.tonima.core.receiver.AlarmReceiver.ACTION_SNOOZE
                putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_TITLE, eventTitle)
                putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_UNIQUE_ID, uniqueId)
                putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_ID, eventId)
                putExtra(digital.tonima.core.receiver.AlarmReceiver.EXTRA_EVENT_START_TIME, startTime)
            }
        val snoozePendingIntent =
            PendingIntent.getBroadcast(
                this,
                uniqueId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent = Intent(this, AlarmSoundAndVibrateService::class.java).apply { action = ACTION_STOP_ALARM }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val contentText = eventTitle ?: getString(R.string.upcoming_event)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(getString(R.string.event_alarm))
                .setContentText(contentText)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .addAction(0, getString(R.string.snooze), snoozePendingIntent)
                .addAction(0, getString(R.string.stop), stopPendingIntent)

        if (fullScreenPendingIntent != null) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val notification = notificationBuilder.build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopAndReleaseResources() {
        if (ringtone?.isPlaying == true) {
            ringtone?.stop()
            logcat { "AlarmSoundAndVibrateService: Ringtone parado." }
        }
        ringtone = null

        vibrator?.cancel()
        vibrator = null
        logcat { "AlarmSoundAndVibrateService: Vibração cancelada." }
    }

    override fun onDestroy() {
        logcat { "AlarmSoundAndVibrateService: onDestroy chamado. Garantindo que os recursos sejam liberados." }
        stopAndReleaseResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
