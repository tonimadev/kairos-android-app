package digital.tonima.kairos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import digital.tonima.core.service.AlarmSchedulingWorker
import digital.tonima.kairos.service.PhoneEventSyncWorker
import logcat.LogPriority
import logcat.logcat

/**
 * BroadcastReceiver that handles Work Profile lifecycle events.
 *
 * When a Work Profile is paused (e.g., end of workday), all scheduled alarms
 * are cancelled. This receiver detects when the profile becomes available again
 * and reschedules all alarms.
 *
 * Key intents:
 * - ACTION_MANAGED_PROFILE_AVAILABLE: Profile is now active
 * - ACTION_MANAGED_PROFILE_UNLOCKED: Profile was unlocked
 * - ACTION_USER_UNLOCKED: User/profile unlocked (API 24+)
 */
class ManagedProfileReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        logcat(LogPriority.INFO) {
            "ManagedProfileReceiver: Received ${intent.action}"
        }

        when (intent.action) {
            Intent.ACTION_MANAGED_PROFILE_AVAILABLE,
            Intent.ACTION_MANAGED_PROFILE_UNLOCKED,
            Intent.ACTION_USER_UNLOCKED,
            -> {
                handleProfileAvailable(context)
            }
            Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE -> {
                logcat(LogPriority.WARN) {
                    "Work Profile paused - all alarms will be cancelled by system"
                }
            }
        }
    }

    /**
     * Reschedules all alarms when the profile becomes available.
     * Uses expedited work to ensure quick execution.
     */
    private fun handleProfileAvailable(context: Context) {
        logcat(LogPriority.INFO) {
            "Work Profile available - rescheduling alarms and syncing calendar"
        }

        try {
            val workManager = WorkManager.getInstance(context)

            // 1. Sync calendar events immediately
            val syncRequest = OneTimeWorkRequestBuilder<PhoneEventSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            workManager.enqueueUniqueWork(
                "profile-resume-sync",
                ExistingWorkPolicy.REPLACE,
                syncRequest,
            )

            // 2. Reschedule alarms immediately
            val alarmRequest = OneTimeWorkRequestBuilder<AlarmSchedulingWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            workManager.enqueueUniqueWork(
                "profile-resume-alarms",
                ExistingWorkPolicy.REPLACE,
                alarmRequest,
            )

            logcat(LogPriority.INFO) {
                "Enqueued profile resume work: sync + alarm rescheduling"
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) {
                "Failed to reschedule after profile resume: ${e.message}"
            }
        }
    }
}
