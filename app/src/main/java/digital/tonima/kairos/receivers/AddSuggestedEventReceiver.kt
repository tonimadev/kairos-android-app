package digital.tonima.kairos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.analytics.coroutineExceptionHandler
import digital.tonima.core.data.usecases.CreateSuggestedEventUseCase
import digital.tonima.core.notifications.SuggestedEvent
import digital.tonima.core.utils.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

/** Handles the "Create" action of an event suggestion notification. */
@AndroidEntryPoint
class AddSuggestedEventReceiver : BroadcastReceiver() {
    @Inject
    lateinit var createSuggestedEventUseCase: CreateSuggestedEventUseCase

    @Inject
    lateinit var crashReporter: CrashReporter

    private val receiverScope by lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO + crashReporter.coroutineExceptionHandler("AddSuggestedEventReceiver"),
        )
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_CREATE) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val start = intent.getLongExtra(EXTRA_START, -1L)
        val end = intent.getLongExtra(EXTRA_END, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (start <= 0L || end <= start || notificationId < 0) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val created = createSuggestedEventUseCase(SuggestedEvent(title, start, end))
                NotificationHelper.showEventSuggestionResult(appContext, notificationId, created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                // Calendar permission revoked since the suggestion was shown.
                logcat(LogPriority.WARN) { "Suggested event not created, calendar permission missing: ${e.message}" }
                NotificationHelper.showEventSuggestionResult(appContext, notificationId, success = false)
            } catch (e: RuntimeException) {
                crashReporter.recordNonFatal(e, "AddSuggestedEventReceiver: failed to create suggested event")
                NotificationHelper.showEventSuggestionResult(appContext, notificationId, success = false)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CREATE = "digital.tonima.kairos.ACTION_CREATE_SUGGESTED_EVENT"
        const val EXTRA_TITLE = "EXTRA_SUGGESTION_TITLE"
        const val EXTRA_START = "EXTRA_SUGGESTION_START"
        const val EXTRA_END = "EXTRA_SUGGESTION_END"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_SUGGESTION_NOTIFICATION_ID"
    }
}
