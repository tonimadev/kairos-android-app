package digital.tonima.core.sync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.sync.WearSyncSchema.EXTRA_EVENT_ID
import digital.tonima.core.sync.WearSyncSchema.EXTRA_EVENT_START_TIME
import digital.tonima.core.sync.WearSyncSchema.EXTRA_EVENT_TITLE
import digital.tonima.core.sync.WearSyncSchema.EXTRA_UNIQUE_ID
import digital.tonima.core.sync.WearSyncSchema.PATH_DISMISS_ALARM
import digital.tonima.core.sync.WearSyncSchema.PATH_SNOOZE_ALARM
import kotlinx.coroutines.tasks.await
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to send messages between phone and wear devices.
 */
@Singleton
class WearMessagingHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val messageClient by lazy { Wearable.getMessageClient(context) }
        private val nodeClient by lazy { Wearable.getNodeClient(context) }
        private val dataClient by lazy { Wearable.getDataClient(context) }

        suspend fun sendDismissAlarm(uniqueId: Int) {
            logcat { "Sending dismiss alarm message for uniqueId: $uniqueId" }
            // Use DataItem to ensure it reaches even if app is not in foreground/active immediately
            val putDataMapReq =
                PutDataMapRequest.create(PATH_DISMISS_ALARM).apply {
                    dataMap.putInt(EXTRA_UNIQUE_ID, uniqueId)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            try {
                dataClient.putDataItem(putDataReq).await()
                logcat { "Dismiss alarm message sent successfully" }
            } catch (e: Exception) {
                logcat { "Failed to send dismiss alarm message: ${e.message}" }
            }
        }

        suspend fun sendSnoozeAlarm(
            uniqueId: Int,
            eventId: Long,
            eventTitle: String,
            startTime: Long,
        ) {
            logcat { "Sending snooze alarm message for uniqueId: $uniqueId" }
            val putDataMapReq =
                PutDataMapRequest.create(PATH_SNOOZE_ALARM).apply {
                    dataMap.putInt(EXTRA_UNIQUE_ID, uniqueId)
                    dataMap.putLong(EXTRA_EVENT_ID, eventId)
                    dataMap.putString(EXTRA_EVENT_TITLE, eventTitle)
                    dataMap.putLong(EXTRA_EVENT_START_TIME, startTime)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            try {
                dataClient.putDataItem(putDataReq).await()
                logcat { "Snooze alarm message sent successfully" }
            } catch (e: Exception) {
                logcat { "Failed to send snooze alarm message: ${e.message}" }
            }
        }
    }
