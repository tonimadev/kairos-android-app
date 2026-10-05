package digital.tonima.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import digital.tonima.core.notifications.SuggestedEvent
import digital.tonima.kairos.core.R
import logcat.LogPriority
import logcat.logcat

object NotificationHelper {
    const val CHANNEL_DAILY_BRIEFING = "daily_briefing_channel"
    const val CHANNEL_DEVICE_HEALTH_ALERT = "device_health_alert_channel"
    private const val NOTIFICATION_ID_DAILY_BRIEFING = 1001
    const val CHANNEL_FOCUS_DIGEST = "focus_digest_channel"
    const val CHANNEL_EVENT_SUGGESTION = "event_suggestion_channel"
    private const val NOTIFICATION_ID_DEVICE_HEALTH_ALERT = 1002
    private const val NOTIFICATION_ID_FOCUS_DIGEST = 1003
    private const val RESULT_TIMEOUT_MS = 5_000L

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

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_FOCUS_DIGEST,
                    context.getString(R.string.focus_digest_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_EVENT_SUGGESTION,
                    context.getString(R.string.event_suggestion_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
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
                logcat(LogPriority.WARN) { "Daily briefing not shown, notification permission denied: ${e.message}" }
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
                logcat(
                    LogPriority.WARN,
                ) { "Device health alert not shown, notification permission denied: ${e.message}" }
            }
        }
    }

    /** Shows only per-app counts (e.g. "WhatsApp: 8 · Gmail: 4"); the held notifications' content is never read. */
    fun showFocusDigestNotification(
        context: Context,
        summary: List<Pair<String, Int>>,
    ) {
        if (summary.isEmpty()) return
        val text = summary.joinToString(" · ") { (app, count) -> "$app: $count" }
        val builder =
            NotificationCompat.Builder(context, CHANNEL_FOCUS_DIGEST)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(context.getString(R.string.focus_digest_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)

        notifySafely(context, NOTIFICATION_ID_FOCUS_DIGEST, builder, "Focus digest")
    }

    fun showEventSuggestionNotification(
        context: Context,
        notificationId: Int,
        suggestion: SuggestedEvent,
        createAction: PendingIntent,
    ) {
        val whenText =
            DateUtils.formatDateTime(
                context,
                suggestion.startMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
            )
        val builder =
            suggestionBuilder(context)
                .setContentTitle(context.getString(R.string.event_suggestion_title))
                .setContentText(context.getString(R.string.event_suggestion_text, suggestion.title, whenText))
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.event_suggestion_action_create), createAction)

        notifySafely(context, notificationId, builder, "Event suggestion")
    }

    fun showEventSuggestionResult(
        context: Context,
        notificationId: Int,
        success: Boolean,
    ) {
        val message =
            context.getString(
                if (success) R.string.event_suggestion_created else R.string.event_suggestion_failed,
            )
        val builder =
            suggestionBuilder(context)
                .setContentTitle(message)
                .setTimeoutAfter(RESULT_TIMEOUT_MS)
                .setAutoCancel(true)

        notifySafely(context, notificationId, builder, "Event suggestion result")
    }

    // The text comes from another app's notification, so keep it off the lock screen.
    private fun suggestionBuilder(context: Context): NotificationCompat.Builder {
        val publicVersion =
            NotificationCompat.Builder(context, CHANNEL_EVENT_SUGGESTION)
                .setSmallIcon(R.drawable.ic_k_monochrome)
                .setContentTitle(context.getString(R.string.event_suggestion_title))
                .build()
        return NotificationCompat.Builder(context, CHANNEL_EVENT_SUGGESTION)
            .setSmallIcon(R.drawable.ic_k_monochrome)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
    }

    private fun notifySafely(
        context: Context,
        id: Int,
        builder: NotificationCompat.Builder,
        what: String,
    ) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (e: SecurityException) {
            logcat(LogPriority.WARN) { "$what not shown, notification permission denied: ${e.message}" }
        }
    }
}
