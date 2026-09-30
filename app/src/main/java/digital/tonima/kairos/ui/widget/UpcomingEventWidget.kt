package digital.tonima.kairos.ui.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.data.usecases.GetNextEventUseCase
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.kairos.MainActivity
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Event
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import digital.tonima.kairos.R as AppR

/** Home screen widget with the next event and a countdown to it. Available to every user. */
class UpcomingEventWidget : GlanceAppWidget() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpcomingEventEntryPoint {
        fun getNextEventUseCase(): GetNextEventUseCase

        fun permissionManager(): PermissionManager

        fun crashReporter(): CrashReporter
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, UpcomingEventEntryPoint::class.java)
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val nextEvent = loadNextEvent(entryPoint)
        val content =
            when {
                nextEvent.isFailure -> UpcomingEventContent.Unavailable
                !entryPoint.permissionManager().hasCalendarPermission() -> UpcomingEventContent.NoCalendarPermission
                else -> upcomingEventContent(nextEvent.getOrNull(), now, zone)
            }
        UpcomingEventWidgetRefresh.schedule(context, nextWidgetRefreshAt(nextEvent.getOrNull(), now, zone))

        provideContent {
            GlanceTheme {
                WidgetContent(content)
            }
        }
    }

    private suspend fun loadNextEvent(entryPoint: UpcomingEventEntryPoint): Result<Event?> =
        try {
            Result.success(entryPoint.getNextEventUseCase()())
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logcat(LogPriority.WARN) { "UpcomingEventWidget: calendar permission missing: ${e.message}" }
            Result.success(null)
        } catch (e: Exception) {
            entryPoint.crashReporter().recordNonFatal(e, "UpcomingEventWidget: failed to load the next event")
            Result.failure(e)
        }

    @Composable
    private fun WidgetContent(content: UpcomingEventContent) {
        val context = LocalContext.current
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
        ) {
            Header()
            Spacer(modifier = GlanceModifier.height(6.dp))
            when (content) {
                is UpcomingEventContent.Next -> NextEvent(content)
                UpcomingEventContent.NoEvents -> Message(context.getString(R.string.widget_no_upcoming_events))
                UpcomingEventContent.NoCalendarPermission ->
                    Message(context.getString(R.string.widget_calendar_permission_needed))
                UpcomingEventContent.Unavailable -> Message(context.getString(R.string.widget_events_unavailable))
            }
        }
    }

    @Composable
    private fun Header() {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_k_monochrome),
                contentDescription = context.getString(R.string.cd_app_logo),
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = context.getString(R.string.widget_upcoming_event_name),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun NextEvent(content: UpcomingEventContent.Next) {
        val context = LocalContext.current
        val event = content.event
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    GlanceModifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(GlanceTheme.colors.primary),
            ) {}
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = event.title.ifBlank { context.getString(R.string.event_untitled) },
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    maxLines = 2,
                )
                Text(
                    text = whenLabel(context, content),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        CountdownLabel(content.countdown)
        event.location?.takeIf { it.isNotBlank() }?.let { location ->
            Text(
                text = location,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun CountdownLabel(countdown: Countdown) {
        val context = LocalContext.current
        when (countdown) {
            // Without an explicit size the RemoteViews container takes the remaining height and pushes the
            // location out of the widget.
            is Countdown.StartsAt ->
                AndroidRemoteViews(
                    remoteViews = countdownTimer(context, countdown.startTime),
                    modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
                )
            Countdown.Now -> CountdownText(context.getString(R.string.widget_event_happening_now))
            is Countdown.InDays ->
                CountdownText(
                    context.resources.getQuantityString(
                        R.plurals.widget_countdown_in_days,
                        countdown.days,
                        countdown.days,
                    ),
                )
        }
    }

    @Composable
    private fun CountdownText(text: String) {
        Text(
            text = text,
            style =
                TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            maxLines = 1,
        )
    }

    @Composable
    private fun Message(text: String) {
        Text(
            text = text,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                ),
        )
    }

    /** A system-driven timer, so the countdown keeps ticking without waking the app every second. */
    private fun countdownTimer(
        context: Context,
        startTime: Long,
    ): RemoteViews {
        val base = SystemClock.elapsedRealtime() + (startTime - System.currentTimeMillis())
        return RemoteViews(context.packageName, AppR.layout.widget_upcoming_event_countdown).apply {
            setChronometer(
                AppR.id.upcoming_event_countdown,
                base,
                context.getString(R.string.widget_countdown_starts_in),
                true,
            )
            setChronometerCountDown(AppR.id.upcoming_event_countdown, true)
        }
    }

    private fun whenLabel(
        context: Context,
        content: UpcomingEventContent.Next,
    ): String {
        val event = content.event
        val dayLabel =
            when (content.day) {
                EventDay.TODAY -> context.getString(R.string.widget_event_today)
                EventDay.TOMORROW -> context.getString(R.string.widget_event_tomorrow)
                EventDay.LATER -> {
                    val locale = context.resources.configuration.locales[0]
                    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEMMMd")
                    eventDate(event, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, locale))
                }
            }
        val timeLabel =
            if (event.isAllDay) {
                context.getString(R.string.all_day_event)
            } else {
                DateFormat.getTimeFormat(context).format(Date.from(Instant.ofEpochMilli(event.startTime)))
            }
        return "$dayLabel · $timeLabel"
    }
}

class UpcomingEventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingEventWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != UpcomingEventWidgetRefresh.ACTION_REFRESH) {
            super.onReceive(context, intent)
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                glanceAppWidget.updateAll(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "UpcomingEventWidget: refresh failed: ${e.message}" }
                EntryPointAccessors
                    .fromApplication(
                        context.applicationContext,
                        UpcomingEventWidget.UpcomingEventEntryPoint::class.java,
                    )
                    .crashReporter()
                    .recordNonFatal(e, "UpcomingEventWidget: refresh failed")
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        UpcomingEventWidgetRefresh.cancel(context)
    }
}
