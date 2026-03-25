package digital.tonima.kairos.service

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.sync.WearSyncSchema.EXTRA_UNIQUE_ID
import digital.tonima.core.sync.WearSyncSchema.PATH_DISMISS_ALARM
import digital.tonima.core.sync.WearSyncSchema.PATH_REQUEST_SYNC
import digital.tonima.core.sync.WearSyncSchema.PATH_SNOOZE_ALARM
import logcat.LogPriority
import logcat.logcat

class WearMessagesListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            PATH_REQUEST_SYNC -> {
                logcat { "Phone: Received sync request from wear. Enqueuing PhoneEventSyncWorker." }
                enqueuePhoneSync(applicationContext)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.use { buffer ->
            buffer.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val path = event.dataItem.uri.path ?: ""
                    when (path) {
                        PATH_DISMISS_ALARM -> {
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val uniqueId = dataMap.getInt(EXTRA_UNIQUE_ID, -1)
                            logcat { "Phone: Received dismiss alarm sync for uniqueId: $uniqueId" }
                            AlarmSoundAndVibrateService.stopAlarm(
                                context = applicationContext,
                                source = "WEAR_SYNC",
                                uniqueId = uniqueId,
                            )
                        }
                        PATH_SNOOZE_ALARM -> {
                            // Phone could also react to snooze if it wants to show notification
                            // or schedule local snooze
                            // For now, stopping the local alarm is enough as the snooze will be rescheduled
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val uniqueId = dataMap.getInt(EXTRA_UNIQUE_ID, -1)
                            logcat {
                                "Phone: Received snooze alarm sync for uniqueId: " +
                                    "$uniqueId. Stopping local alarm."
                            }
                            AlarmSoundAndVibrateService.stopAlarm(
                                context = applicationContext,
                                source = "WEAR_SYNC",
                                uniqueId = uniqueId,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun enqueuePhoneSync(context: Context) {
        try {
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
