package digital.tonima.kairos.wear.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import digital.tonima.core.receiver.AlarmReceiver.Companion.ACTION_ALARM_TRIGGERED
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_ID
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_START_TIME
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.kairos.core.R
import digital.tonima.kairos.wear.WearAlarmActivity

class WearAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGERED) {
            return
        }
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: context.getString(R.string.upcoming_event)
        val uniqueId = intent.getIntExtra(EXTRA_UNIQUE_ID, System.currentTimeMillis().toInt())
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)

        AlarmSoundAndVibrateService.startAlarm(context, eventTitle)

        try {
            val fsIntent = Intent(context, WearAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_EVENT_TITLE, eventTitle)
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_EVENT_START_TIME, startTime)
            }
            context.startActivity(fsIntent)
        } catch (t: Throwable) {
            showFallbackNotification(context, eventTitle, uniqueId, eventId, startTime)
        }
    }

    private fun showFallbackNotification(
        context: Context,
        eventTitle: String,
        uniqueId: Int,
        eventId: Long,
        startTime: Long,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "calendar_alarm_channel"
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.event_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_description)
        }
        notificationManager.createNotificationChannel(channel)

        val fullScreenIntent = Intent(context, WearAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENT_TITLE, eventTitle)
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_START_TIME, startTime)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            uniqueId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(context, WearAlarmActionReceiver::class.java).apply {
            putExtra(EXTRA_UNIQUE_ID, uniqueId)
        }
        val stopActionPendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueId + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(digital.tonima.kairos.core.R.drawable.ic_k_monochrome)
            .setContentTitle(context.getString(R.string.commitment))
            .setContentText(eventTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.stop), stopActionPendingIntent)
            .build()

        notificationManager.notify(uniqueId, notification)
    }
}
