package digital.tonima.kairos.wear.sync

import android.content.Context
import androidx.core.content.edit
import digital.tonima.core.model.Event
import digital.tonima.core.sync.WearSyncSchema.KEY_ALL_DAY
import digital.tonima.core.sync.WearSyncSchema.KEY_DEPARTURE_TIME
import digital.tonima.core.sync.WearSyncSchema.KEY_ID
import digital.tonima.core.sync.WearSyncSchema.KEY_LOCATION
import digital.tonima.core.sync.WearSyncSchema.KEY_RECUR
import digital.tonima.core.sync.WearSyncSchema.KEY_START
import digital.tonima.core.sync.WearSyncSchema.KEY_TITLE
import digital.tonima.core.sync.WearSyncSchema.KEY_TRAVEL_TIME
import org.json.JSONArray
import org.json.JSONObject

object WearEventCache {
    private const val PREF = "PhoneEventsCache"
    private const val KEY_JSON = "json"

    fun save(
        context: Context,
        events: List<Event>,
    ) {
        val arr = JSONArray()
        events.forEach { e ->
            val o = JSONObject()
            o.put(KEY_ID, e.id)
            o.put(KEY_TITLE, e.title)
            o.put(KEY_START, e.startTime)
            o.put(KEY_RECUR, e.isRecurring)
            o.put(KEY_ALL_DAY, e.isAllDay)
            o.put(KEY_LOCATION, e.location)
            o.put(KEY_DEPARTURE_TIME, e.departureTime)
            o.put(KEY_TRAVEL_TIME, e.travelTimeMinutes)
            arr.put(o)
        }
        context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putString(KEY_JSON, arr.toString())
            }
    }

    fun load(context: Context): List<Event> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_JSON, null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = ArrayList<Event>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Event(
                        id = o.getLong(KEY_ID),
                        title = o.getString(KEY_TITLE),
                        startTime = o.getLong(KEY_START),
                        isRecurring = o.optBoolean(KEY_RECUR, false),
                        isAllDay = o.optBoolean(KEY_ALL_DAY, false),
                        location = if (o.has(KEY_LOCATION)) o.getString(KEY_LOCATION) else null,
                        departureTime = if (o.has(KEY_DEPARTURE_TIME)) o.getLong(KEY_DEPARTURE_TIME) else null,
                        travelTimeMinutes = if (o.has(KEY_TRAVEL_TIME)) o.getInt(KEY_TRAVEL_TIME) else null,
                    ),
                )
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
