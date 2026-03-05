package digital.tonima.kairos.receivers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import digital.tonima.core.receiver.AlarmReceiver.Companion.ACTION_ALARM_TRIGGERED
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_ID
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_START_TIME
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.view.AlarmActivity
import logcat.logcat

class AlarmNotificationReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_ALARM_TRIGGERED) {
            return
        }

        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: context.getString(R.string.upcoming_event)
        val uniqueId = intent.getIntExtra(EXTRA_UNIQUE_ID, System.currentTimeMillis().toInt())
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "calendar_alarm_channel"

        val channel =
            NotificationChannel(
                channelId,
                context.getString(R.string.event_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_description)
            }
        notificationManager.createNotificationChannel(channel)

        val fullScreenIntent =
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_EVENT_TITLE, eventTitle)
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_EVENT_START_TIME, startTime)
            }
        val fullScreenPendingIntent =
            PendingIntent.getActivity(
                context,
                uniqueId,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val soundAlarmIntent =
            Intent(context, AlarmActionReceiver::class.java).apply {
                putExtra(EXTRA_UNIQUE_ID, uniqueId)
            }
        val stopActionPendingIntent =
            getBroadcast(
                context,
                uniqueId + 1,
                soundAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notificationTitle = context.getString(R.string.commitment)
        val notificationBuilder =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(notificationTitle)
                .setContentText(eventTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.stop), stopActionPendingIntent)

        val canUseFullScreenIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.USE_FULL_SCREEN_INTENT,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    logcat { "Full-screen intent permission is granted" }
                    true
                } else {
                    logcat { "FALLBACK: USE_FULL_SCREEN_INTENT permission is NOT granted, using regular notification" }
                    false
                }
            } else {
                logcat { "Full-screen intent available on Android 11+" }
                true
            }

        if (canUseFullScreenIntent) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
            logcat { "Notification will use FULL-SCREEN INTENT for event: $eventTitle" }
        } else {
            notificationBuilder.setContentIntent(fullScreenPendingIntent)
            logcat { "Notification will use REGULAR (non-full-screen) intent for event: $eventTitle" }
        }

        notificationManager.notify(uniqueId, notificationBuilder.build())
    }
}
