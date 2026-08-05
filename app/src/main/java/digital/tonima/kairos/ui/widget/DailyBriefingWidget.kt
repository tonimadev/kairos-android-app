package digital.tonima.kairos.ui.widget

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.billing.SubscriptionManager
import digital.tonima.core.model.Event
import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.repository.DailyBriefingRepository
import digital.tonima.kairos.MainActivity
import digital.tonima.kairos.core.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.glance.appwidget.action.actionStartActivity as actionStartAppWidgetActivity

class DailyBriefingWidget : GlanceAppWidget() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyBriefingEntryPoint {
        fun dailyBriefingRepository(): DailyBriefingRepository

        fun calendarRepository(): CalendarRepository

        fun subscriptionManager(): SubscriptionManager
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                DailyBriefingEntryPoint::class.java,
            )
        val repository = entryPoint.dailyBriefingRepository()
        val calendarRepo = entryPoint.calendarRepository()
        val subscriptionManager = entryPoint.subscriptionManager()

        // Buscar eventos de hoje
        val today = LocalDate.now()
        val events =
            try {
                calendarRepo.getEventsForMonth(YearMonth.from(today))
                    .filter { event ->
                        val eventDate =
                            java.time.Instant.ofEpochMilli(event.startTime)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        eventDate == today && event.startTime > System.currentTimeMillis()
                    }
                    .sortedBy { it.startTime }
                    .take(3)
            } catch (e: Exception) {
                emptyList()
            }

        provideContent {
            val briefing by repository.getDailyBriefing().collectAsState(initial = null)
            val isPro by subscriptionManager.isProUser.collectAsState()

            GlanceTheme {
                WidgetContent(briefing, events, isPro)
            }
        }
    }

    @Composable
    private fun WidgetContent(
        briefing: String?,
        events: List<Event>,
        isPro: Boolean,
    ) {
        val context = LocalContext.current

        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
        ) {
            if (isPro) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "✨ " + context.getString(R.string.daily_briefing_title),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Box(
                    modifier =
                        GlanceModifier
                            .fillMaxWidth()
                            .background(GlanceTheme.colors.surface)
                            .padding(8.dp),
                ) {
                    Text(
                        text = briefing ?: context.getString(R.string.widget_no_briefing),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                            ),
                    )
                }

                Spacer(modifier = GlanceModifier.height(12.dp))
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_k_monochrome),
                    contentDescription = null,
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = context.getString(R.string.next_events_title),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            if (events.isNotEmpty()) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    events.forEach { event ->
                        EventItem(event)
                    }
                }
            } else {
                Text(
                    text = context.getString(R.string.no_more_events_today),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                )
            }
        }
    }

    @Composable
    private fun EventItem(event: Event) {
        val context = LocalContext.current
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime =
            java.time.Instant.ofEpochMilli(event.startTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime()
        val timeStr =
            if (event.isAllDay) {
                context.getString(R.string.all_day_event)
            } else {
                startTime.format(
                    timeFormatter,
                )
            }

        val openEventIntent =
            Intent(Intent.ACTION_VIEW).apply {
                data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime)
            }

        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(actionStartAppWidgetActivity(openEventIntent)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    GlanceModifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(GlanceTheme.colors.primary),
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = timeStr,
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = event.title,
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
        }
    }
}

class DailyBriefingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyBriefingWidget()
}
