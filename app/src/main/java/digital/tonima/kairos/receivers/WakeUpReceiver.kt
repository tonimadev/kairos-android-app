package digital.tonima.kairos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.DailyBriefingWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.logcat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WakeUpReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WakeUpEntryPoint {
        fun appPreferencesRepository(): AppPreferencesRepository
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val repository =
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WakeUpEntryPoint::class.java,
                ).appPreferencesRepository()

            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()
                val history = repository.getWakeUpHistory().first()

                // Verificar se já registramos um wake-up hoje
                val today = LocalDate.now()
                val alreadyRegisteredToday =
                    history.any {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == today
                    }

                if (!alreadyRegisteredToday) {
                    logcat { "WakeUpReceiver: Registering first wake-up of the day: $now" }
                    repository.addWakeUpTimestamp(now)

                    // Disparar o DailyBriefingWorker para usuários PRO
                    val workRequest = OneTimeWorkRequestBuilder<DailyBriefingWorker>().build()
                    WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
                }
            }
        }
    }
}
