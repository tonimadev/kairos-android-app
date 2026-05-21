package digital.tonima.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import digital.tonima.kairos.core.R

object NotificationHelper {
    const val CHANNEL_DAILY_BRIEFING = "daily_briefing_channel"
    const val CHANNEL_DEVICE_HEALTH_ALERT = "device_health_alert_channel"
    private const val NOTIFICATION_ID_DAILY_BRIEFING = 1001
    private const val NOTIFICATION_ID_DEVICE_HEALTH_ALERT = 1002

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

            // Device Health Alert Channel
            val healthName = context.getString(R.string.device_health_alert_title)
            val healthImportance = NotificationManager.IMPORTANCE_DEFAULT
            val healthChannel = NotificationChannel(CHANNEL_DEVICE_HEALTH_ALERT, healthName, healthImportance)
            notificationManager.createNotificationChannel(healthChannel)
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

    fun showDeviceHealthAlertNotification(
        context: Context,
        content: String,
    ) {
        val title = context.getString(R.string.device_health_alert_title)

        val builder =
            NotificationCompat.Builder(context, CHANNEL_DEVICE_HEALTH_ALERT)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_DEVICE_HEALTH_ALERT, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }
}
