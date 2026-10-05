package digital.tonima.kairos.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.analytics.coroutineExceptionHandler
import digital.tonima.core.data.usecases.NotificationFeatures
import digital.tonima.core.data.usecases.ObserveNotificationFeaturesUseCase
import digital.tonima.core.notifications.CalendarNotificationMatcher
import digital.tonima.core.notifications.FiredAlarm
import digital.tonima.core.notifications.FocusDigest
import digital.tonima.core.notifications.FocusFilterPolicy
import digital.tonima.core.notifications.NotificationEventParser
import digital.tonima.core.notifications.NotificationPackages
import digital.tonima.core.notifications.NotificationSignals
import digital.tonima.core.notifications.SuggestionTracker
import digital.tonima.core.utils.NotificationHelper
import digital.tonima.kairos.receivers.AddSuggestedEventReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

/**
 * Optional, opt-in notification features. Only runs once the user grants notification access in
 * the system settings, and each feature is gated by its own preference.
 *
 * Privacy contract: content is analysed in memory only. It is never stored, logged or sent anywhere,
 * and only notifications from the packages allowlisted in [NotificationPackages] are ever read.
 */
@AndroidEntryPoint
class KairosNotificationListenerService : NotificationListenerService() {
    @Inject
    lateinit var observeNotificationFeaturesUseCase: ObserveNotificationFeaturesUseCase

    @Inject
    lateinit var crashReporter: CrashReporter

    private val scope by lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default + crashReporter.coroutineExceptionHandler("NotificationListener"),
        )
    }

    @Volatile
    private var features = NotificationFeatures()

    private val digest = FocusDigest()
    private val suggestionTracker = SuggestionTracker()
    private var digestJob: Job? = null
    private var connectedJobs = mutableListOf<Job>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedJobs +=
            observeNotificationFeaturesUseCase()
                .onEach { features = it }
                .launchIn(scope)
        connectedJobs +=
            NotificationSignals.alarmFired
                .onEach { alarm -> guarded("sweep calendar reminders") { sweepCalendarReminders(alarm) } }
                .launchIn(scope)
        connectedJobs +=
            NotificationSignals.focusEnded
                .onEach { postDigest() }
                .launchIn(scope)
    }

    override fun onListenerDisconnected() {
        connectedJobs.forEach { it.cancel() }
        connectedJobs.clear()
        digestJob?.cancel()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        guarded("handle notification") {
            val pkg = sbn.packageName
            if (pkg == packageName) return@guarded
            when {
                pkg in NotificationPackages.CALENDAR -> if (features.dedupCalendarReminder) dismissIfRecentAlarm(sbn)
                else -> {
                    if (features.focusDigest) holdForFocus(sbn)
                    if (features.eventSuggestions && pkg in NotificationPackages.EVENT_SUGGESTIONS) suggestEvent(sbn)
                }
            }
        }
    }

    // ── Feature 1: calendar reminder that duplicates the Kairos alarm ───────────────────────

    private fun dismissIfRecentAlarm(sbn: StatusBarNotification) {
        if (!sbn.isClearable) return
        val texts = notificationTexts(sbn)
        if (NotificationSignals.recentAlarms().any { CalendarNotificationMatcher.isSameEvent(texts, it.title) }) {
            cancelNotification(sbn.key)
        }
    }

    /** The calendar reminder may already be on screen when the alarm rings, so look at what is showing. */
    private fun sweepCalendarReminders(alarm: FiredAlarm) {
        if (!features.dedupCalendarReminder) return
        activeNotifications
            .orEmpty()
            .filter { it.packageName in NotificationPackages.CALENDAR && it.isClearable }
            .filter { CalendarNotificationMatcher.isSameEvent(notificationTexts(it), alarm.title) }
            .forEach { cancelNotification(it.key) }
    }

    // ── Feature 2: hold chat/e-mail notifications during a meeting and summarize them ───────

    private fun holdForFocus(sbn: StatusBarNotification) {
        val focusEnd = NotificationSignals.activeFocusEnd()
        if (focusEnd == 0L) return
        val notification = sbn.notification
        val shouldHold =
            FocusFilterPolicy.shouldHold(
                packageName = sbn.packageName,
                category = notification.category,
                isOngoing = sbn.isOngoing,
                isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                ownPackage = packageName,
            )
        if (!shouldHold) return
        // The margin makes a snoozed notification wake up after the meeting, when it is no longer held.
        snoozeNotification(sbn.key, focusEnd - System.currentTimeMillis() + SNOOZE_MARGIN_MS)
        synchronized(digest) { digest.add(NotificationPackages.FOCUS_DIGEST.getValue(sbn.packageName)) }
        scheduleDigest(focusEnd)
    }

    private fun scheduleDigest(focusEnd: Long) {
        digestJob?.cancel()
        digestJob =
            scope.launch {
                delay((focusEnd - System.currentTimeMillis()).coerceAtLeast(0L))
                postDigest()
            }
    }

    private fun postDigest() {
        val summary =
            synchronized(digest) {
                digest.summary().also { digest.clear() }
            }
        NotificationHelper.showFocusDigestNotification(applicationContext, summary)
    }

    // ── Feature 3: suggest an event found in a message or e-mail ────────────────────────────

    private fun suggestEvent(sbn: StatusBarNotification) {
        val notification = sbn.notification
        if (sbn.isOngoing || notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val text = notificationTexts(sbn).drop(1).filterNotNull().joinToString("\n")
        val suggestion = NotificationEventParser.parse(text) ?: return
        val key = "${suggestion.title}|${suggestion.startMillis}"
        if (!suggestionTracker.shouldSuggest(key)) return

        val notificationId = SUGGESTION_ID_BASE + (key.hashCode() and Int.MAX_VALUE) % SUGGESTION_ID_RANGE
        val create =
            PendingIntent.getBroadcast(
                this,
                notificationId,
                Intent(this, AddSuggestedEventReceiver::class.java)
                    .setAction(AddSuggestedEventReceiver.ACTION_CREATE)
                    .putExtra(AddSuggestedEventReceiver.EXTRA_TITLE, suggestion.title)
                    .putExtra(AddSuggestedEventReceiver.EXTRA_START, suggestion.startMillis)
                    .putExtra(AddSuggestedEventReceiver.EXTRA_END, suggestion.endMillis)
                    .putExtra(AddSuggestedEventReceiver.EXTRA_NOTIFICATION_ID, notificationId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        NotificationHelper.showEventSuggestionNotification(applicationContext, notificationId, suggestion, create)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────

    /** Title, text and expanded text of a notification, in that order. */
    private fun notificationTexts(sbn: StatusBarNotification): List<String?> {
        val extras = sbn.notification.extras
        return listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
        ).map { it?.toString() }
    }

    /** A listener sits on the process boundary: nothing here may crash the app. */
    private inline fun guarded(
        what: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            // Access revoked while we were working; the system will unbind us.
            logcat(LogPriority.WARN) { "Notification listener: $what lost access: ${e.message}" }
        } catch (e: RuntimeException) {
            crashReporter.recordNonFatal(e, "KairosNotificationListenerService: failed to $what")
        }
    }

    private companion object {
        const val SNOOZE_MARGIN_MS = 1_000L
        const val SUGGESTION_ID_BASE = 2_000
        const val SUGGESTION_ID_RANGE = 10_000
    }
}
