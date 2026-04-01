package digital.tonima.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import digital.tonima.core.receiver.SyncAlertDismissReceiver
import digital.tonima.kairos.core.R

object NotificationHelper {
    const val CHANNEL_DAILY_BRIEFING = "daily_briefing_channel"
    const val CHANNEL_SYNC_ALERT = "sync_alert_channel"
    private const val NOTIFICATION_ID_DAILY_BRIEFING = 1001
    private const val NOTIFICATION_ID_SYNC_ALERT = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.daily_briefing_title)
            val descriptionText = context.getString(R.string.notification_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(CHANNEL_DAILY_BRIEFING, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // Sync Alert Channel
            val syncName = context.getString(R.string.no_events_upcoming_24h_title)
            val syncImportance = NotificationManager.IMPORTANCE_DEFAULT
            val syncChannel = NotificationChannel(CHANNEL_SYNC_ALERT, syncName, syncImportance)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }

    fun showDailyBriefingNotification(
        context: Context,
        title: String,
        content: String,
    ) {
        val cleanedContent = content.replace("**", "")
        val builder =
            NotificationCompat.Builder(context, CHANNEL_DAILY_BRIEFING)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(title)
                .setContentText(cleanedContent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cleanedContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_DAILY_BRIEFING, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }

    fun showSyncAlertNotification(context: Context) {
        val title = context.getString(R.string.no_events_upcoming_24h_title)
        val content = context.getString(R.string.no_events_upcoming_24h_content)
        val dismissActionTitle = context.getString(R.string.no_events_upcoming_24h_dismiss_today)

        val dismissIntent =
            Intent(context, SyncAlertDismissReceiver::class.java).apply {
                action = SyncAlertDismissReceiver.ACTION_DISMISS_SYNC_ALERT
                putExtra(SyncAlertDismissReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_SYNC_ALERT)
            }
        val dismissPendingIntent =
            PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_SYNC_ALERT,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat.Builder(context, CHANNEL_SYNC_ALERT)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(
                    0, // No icon for the action
                    dismissActionTitle,
                    dismissPendingIntent,
                )

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_SYNC_ALERT, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }
}
