package digital.tonima.kairos.service

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import digital.tonima.core.sync.WearSyncSchema.PATH_REQUEST_SYNC
import logcat.LogPriority
import logcat.logcat

/**
 * Listens for messages from Wear OS and reacts accordingly.
 * Currently supports a sync request from the watch to immediately push fresh events.
 */
class WearMessagesListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == PATH_REQUEST_SYNC) {
            logcat { "Phone: Received sync request from wear. Enqueuing PhoneEventSyncWorker." }
            enqueuePhoneSync(applicationContext)
        }
    }

    private fun enqueuePhoneSync(context: Context) {
        try {
            // Use expedited work to bypass work profile throttling for wear requests
            val request =
                OneTimeWorkRequestBuilder<PhoneEventSyncWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(context).enqueue(request)
        } catch (t: Throwable) {
            logcat(
                LogPriority.ERROR,
            ) { "Phone: Failed to enqueue PhoneEventSyncWorker on request: ${t.localizedMessage}" }
        }
    }
}
