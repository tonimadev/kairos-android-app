package digital.tonima.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.repository.AppStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SyncAlertDismissReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncAlertEntryPoint {
        fun appStatusRepository(): AppStatusRepository
    }

    companion object {
        const val ACTION_DISMISS_SYNC_ALERT = "digital.tonima.core.ACTION_DISMISS_SYNC_ALERT"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_DISMISS_SYNC_ALERT) {
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }

            val repository =
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SyncAlertEntryPoint::class.java,
                ).appStatusRepository()

            val mutedUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)

            CoroutineScope(Dispatchers.IO).launch {
                repository.setSyncAlertMutedUntil(mutedUntil)
            }
        }
    }
}
