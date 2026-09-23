package digital.tonima.kairos.wear.service

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import digital.tonima.kairos.wear.WorkNames
import digital.tonima.kairos.wear.sync.CachedEventSchedulingWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = android.app.Application::class)
class WearBootReceiverTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        // A stub worker keeps the real worker's Hilt dependencies out: this only checks what gets enqueued.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(StubWorkerFactory)
                .build(),
        )
    }

    @Test
    fun `watch reboot reschedules the cached alarms`() {
        WearBootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertRescheduleEnqueued()
    }

    @Test
    fun `app update on the watch reschedules the cached alarms`() {
        WearBootReceiver().onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        assertRescheduleEnqueued()
    }

    @Test
    fun `exact alarm permission change reschedules the cached alarms`() {
        WearBootReceiver().onReceive(context, Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))

        assertRescheduleEnqueued()
    }

    @Test
    fun `unrelated broadcasts are ignored`() {
        WearBootReceiver().onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

        assertTrue(workInfos().isEmpty())
    }

    private fun assertRescheduleEnqueued() {
        val infos = workInfos()
        assertEquals(1, infos.size)
        assertTrue(infos.single().tags.contains(CachedEventSchedulingWorker::class.java.name))
    }

    private fun workInfos(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkNames.UNIQUE_SCHEDULE_NOW + "_boot").get()

    private object StubWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker =
            object : Worker(appContext, workerParameters) {
                override fun doWork() = Result.success()
            }
    }
}
