package digital.tonima.kairos.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import digital.tonima.core.service.AlarmSchedulingWorker
import logcat.LogPriority
import logcat.logcat

object CalendarChangeObserver {
    private const val UNIQUE_WORK_NAME = "phone-event-sync-onchange"
    private const val RESCHEDULE_WORK_NAME = "reschedule-alarms-onchange"
    private const val DEBOUNCE_MS = 3000L

    @Volatile
    private var initialized = false

    private lateinit var appContext: Context
    private lateinit var handlerThread: HandlerThread
    private var observer: ContentObserver? = null

    private val debounceHandler = Handler(Looper.getMainLooper())
    private val debounceRunnable =
        Runnable {
            tryEnqueueSync()
        }

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext

            handlerThread = HandlerThread("CalendarChangeObserver").apply { start() }
            val handler = Handler(handlerThread.looper)

            observer =
                object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean) {
                        scheduleDebounced()
                    }

                    override fun onChange(
                        selfChange: Boolean,
                        uri: Uri?,
                    ) {
                        scheduleDebounced()
                    }
                }

            try {
                val cr = appContext.contentResolver
                cr.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, observer!!)
                cr.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, observer!!)
                initialized = true
                logcat { "CalendarChangeObserver: registered content observers." }
            } catch (t: Exception) {
                logcat(
                    LogPriority.ERROR,
                ) { "CalendarChangeObserver: failed to register observer: ${t.localizedMessage}" }
            }
        }
    }

    private fun scheduleDebounced() {
        debounceHandler.removeCallbacks(debounceRunnable)
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS)
    }

    private fun tryEnqueueSync() {
        val hasPerm =
            ContextCompat
                .checkSelfPermission(
                    appContext,
                    Manifest.permission.READ_CALENDAR,
                ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            logcat(LogPriority.WARN) { "CalendarChangeObserver: READ_CALENDAR not granted; skipping immediate sync." }
            return
        }
        try {
            val constraints = Constraints.Builder().build()
            val request =
                OneTimeWorkRequestBuilder<PhoneEventSyncWorker>()
                    .setConstraints(constraints)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
            // Reschedule the phone's own alarms too, so an event edited, moved or deleted in the
            // calendar app does not keep ringing at its old time until the next periodic run.
            val reschedule =
                OneTimeWorkRequestBuilder<AlarmSchedulingWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                RESCHEDULE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                reschedule,
            )
            logcat { "CalendarChangeObserver: enqueued watch sync and alarm rescheduling due to calendar change." }
        } catch (t: Exception) {
            logcat(LogPriority.ERROR) { "CalendarChangeObserver: failed to enqueue sync: ${t.localizedMessage}" }
        }
    }
}
