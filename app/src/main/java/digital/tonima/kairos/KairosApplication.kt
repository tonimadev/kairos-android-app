package digital.tonima.kairos

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.AlarmSchedulingWorker
import digital.tonima.core.utils.DeviceInfoUtils
import digital.tonima.core.utils.NotificationHelper
import digital.tonima.kairos.service.CalendarChangeObserver
import digital.tonima.kairos.service.PhoneEventSyncWorker.Companion.enqueuePeriodic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogPriority.VERBOSE
import logcat.logcat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class KairosApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    @Inject
    lateinit var proUserProvider: ProUserProvider

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { initializationStatus ->
            logcat(LogPriority.INFO) { "MobileAds initialized: $initializationStatus" }
        }
        try {
            WorkManager.initialize(this, workManagerConfiguration)
        } catch (e: IllegalStateException) {
            logcat(LogPriority.WARN) {
                "WorkManager already initialized, probably by a ContentProvider. Ignoring."
            }
        }
        setupLogger()
        NotificationHelper.createNotificationChannels(this)

        logcat(LogPriority.INFO) {
            "Kairos running in: ${DeviceInfoUtils.getProfileDescription(this)}"
        }

        setupRecurringWork()
        enqueuePeriodic(this)
        try {
            CalendarChangeObserver.init(this)
        } catch (t: Throwable) {
            logcat(
                LogPriority.ERROR,
            ) { "KairosApplication: failed to init CalendarChangeObserver: ${t.localizedMessage}" }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val installationDate = appPreferencesRepository.getInstallationDate().first()
            if (installationDate == 0L) {
                appPreferencesRepository.setInstallationDate(System.currentTimeMillis())
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setupRecurringWork()
        }
    }

    private fun setupLogger() {
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = VERBOSE)
    }

    private fun setupRecurringWork() {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

        val workManager = WorkManager.getInstance(applicationContext)

        val initialRequest =
            OneTimeWorkRequestBuilder<AlarmSchedulingWorker>()
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniqueWork(
            "initial-event-scheduler",
            ExistingWorkPolicy.KEEP,
            initialRequest,
        )

        val repeatingRequest =
            PeriodicWorkRequestBuilder<AlarmSchedulingWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "event-scheduler",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest,
        )
    }
}
