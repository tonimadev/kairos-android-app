package digital.tonima.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import digital.tonima.kairos.core.R

/**
 * Used when the system refuses to start [AlarmSoundAndVibrateService] from the background
 * (e.g. the alarm was scheduled inexactly because the exact-alarm permission is missing).
 * Posts a full-screen alarm notification that opens the alarm screen; once that activity is
 * in the foreground it is allowed to start the service, which then plays the alarm sound.
 */
internal object AlarmFallbackNotification {
    const val CHANNEL_ID = "calendar_alarm_fallback_channel"
    private const val PHONE_ALARM_ACTIVITY = "digital.tonima.kairos.ui.view.AlarmActivity"
    private const val WEAR_ALARM_ACTIVITY = "digital.tonima.kairos.wear.WearAlarmActivity"

    fun post(
        context: Context,
        alarmExtras: Intent,
        eventTitle: String?,
        uniqueId: Int,
    ) {
        val isWatch = context.packageManager.hasSystemFeature("android.hardware.type.watch")
        val activityIntent =
            Intent(alarmExtras).apply {
                action = null
                setClassName(context.packageName, if (isWatch) WEAR_ALARM_ACTIVITY else PHONE_ALARM_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AlarmSoundAndVibrateService.EXTRA_START_ALARM_SOUND, true)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                uniqueId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val nm = context.getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.event_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_description)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        nm.createNotificationChannel(channel)

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(context.getString(R.string.event_alarm))
                .setContentText(eventTitle ?: context.getString(R.string.upcoming_event))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .build()

        // Same id as the service's foreground notification, so it is replaced once the
        // alarm screen starts the service.
        nm.notify(AlarmSoundAndVibrateService.NOTIFICATION_ID, notification)
    }
}
