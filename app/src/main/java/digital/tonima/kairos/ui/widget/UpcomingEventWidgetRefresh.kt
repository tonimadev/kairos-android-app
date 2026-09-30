package digital.tonima.kairos.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import logcat.logcat

/** Keeps the upcoming event widget current without waking the device just to redraw it. */
object UpcomingEventWidgetRefresh {
    const val ACTION_REFRESH = "digital.tonima.kairos.ui.widget.REFRESH_UPCOMING_EVENT"
    private const val REQUEST_CODE = 0x0E7E

    /** Redraws the widget now, if the user has placed one. */
    fun refreshNow(context: Context) {
        if (!hasWidgets(context)) return
        context.sendBroadcast(refreshIntent(context))
    }

    /**
     * Redraws the widget at [triggerAtMillis]. The alarm is inexact and not a wake-up one: while the
     * screen is off nobody sees the widget, so it is enough to redraw it the next time the device is awake.
     */
    fun schedule(
        context: Context,
        triggerAtMillis: Long?,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = refreshPendingIntent(context)
        if (triggerAtMillis == null) {
            alarmManager.cancel(pendingIntent)
            return
        }
        alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
        logcat { "UpcomingEventWidget: next refresh at $triggerAtMillis" }
    }

    fun cancel(context: Context) = schedule(context, null)

    private fun hasWidgets(context: Context): Boolean =
        AppWidgetManager.getInstance(context)
            ?.getAppWidgetIds(ComponentName(context, UpcomingEventWidgetReceiver::class.java))
            ?.isNotEmpty() == true

    private fun refreshIntent(context: Context): Intent =
        Intent(context, UpcomingEventWidgetReceiver::class.java).setAction(ACTION_REFRESH)

    private fun refreshPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            refreshIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
