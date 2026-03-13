package digital.tonima.kairos.wear.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import digital.tonima.core.model.Event
import digital.tonima.core.sync.WearSyncSchema.KEY_ALL_DAY
import digital.tonima.core.sync.WearSyncSchema.KEY_DEPARTURE_TIME
import digital.tonima.core.sync.WearSyncSchema.KEY_EVENTS
import digital.tonima.core.sync.WearSyncSchema.KEY_ID
import digital.tonima.core.sync.WearSyncSchema.KEY_LOCATION
import digital.tonima.core.sync.WearSyncSchema.KEY_RECUR
import digital.tonima.core.sync.WearSyncSchema.KEY_START
import digital.tonima.core.sync.WearSyncSchema.KEY_TITLE
import digital.tonima.core.sync.WearSyncSchema.KEY_TRAVEL_TIME
import digital.tonima.core.sync.WearSyncSchema.PATH_EVENTS_24H
import digital.tonima.kairos.wear.WorkNames
import logcat.logcat

class WearEventListenerService : WearableListenerService() {
    override fun onCreate() {
        super.onCreate()
        logcat { "WearEventListenerService created" }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        val events = mutableListOf<Event>()
        dataEvents.use { buffer ->
            buffer.forEach { event ->
                val path = event.dataItem.uri.path ?: ""
                if (
                    event.type == DataEvent.TYPE_CHANGED &&
                    (path == PATH_EVENTS_24H || path.startsWith(PATH_EVENTS_24H))
                ) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val list = dataMap.getDataMapArrayList(KEY_EVENTS)
                        list?.forEach { dm ->
                            val id = dm.getLong(KEY_ID)
                            val title = dm.getString(KEY_TITLE) ?: "(sem título)"
                            val start = dm.getLong(KEY_START)
                            val rec = dm.getBoolean(KEY_RECUR)
                            val allDay = dm.getBoolean(KEY_ALL_DAY)
                            val location = dm.getString(KEY_LOCATION)
                            val departureTime =
                                if (dm.containsKey(
                                        KEY_DEPARTURE_TIME,
                                    )
                                ) {
                                    dm.getLong(KEY_DEPARTURE_TIME)
                                } else {
                                    null
                                }
                            val travelTime = if (dm.containsKey(KEY_TRAVEL_TIME)) dm.getInt(KEY_TRAVEL_TIME) else null

                            events.add(
                                Event(
                                    id = id,
                                    title = title,
                                    startTime = start,
                                    isRecurring = rec,
                                    isAllDay = allDay,
                                    location = location,
                                    departureTime = departureTime,
                                    travelTimeMinutes = travelTime,
                                ),
                            )
                        }
                    } catch (t: Throwable) {
                        logcat { "Wear listener parse error: ${t.localizedMessage}" }
                    }
                }
            }
        }
        if (events.isNotEmpty()) {
            logcat { "Wear received ${events.size} events from phone." }
            WearEventCache.save(this, events)
            sendBroadcast(android.content.Intent(SyncActions.ACTION_EVENTS_UPDATED))
            triggerScheduling(this)
        } else {
            logcat { "Wear received data change but no events found." }
        }
    }

    private fun triggerScheduling(context: Context) {
        val work = OneTimeWorkRequestBuilder<CachedEventSchedulingWorker>().build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WorkNames.UNIQUE_SCHEDULE_NOW,
                ExistingWorkPolicy.REPLACE,
                work,
            )
    }
}
