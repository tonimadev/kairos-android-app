package digital.tonima.kairos.wear.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import digital.tonima.kairos.wear.WorkNames
import digital.tonima.kairos.wear.sync.CachedEventSchedulingWorker
import logcat.logcat

class WearBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            logcat { "WearBootReceiver: Reprogramming wear alarms after $action" }
            val workRequest = OneTimeWorkRequestBuilder<CachedEventSchedulingWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WorkNames.UNIQUE_SCHEDULE_NOW + "_boot",
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
        }
    }
}
