package digital.tonima.kairos.receivers

import android.app.AlarmManager
import android.app.Application
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
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.AlarmSchedulingWorker
import digital.tonima.core.service.DailyBriefingWorker
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/** Resolves the receiver's Hilt entry point to a test double. */
class WakeUpReceiverTestApp :
    Application(),
    GeneratedComponentManager<Any> {
    lateinit var preferences: AppPreferencesRepository
    lateinit var crashReporter: CrashReporter

    override fun generatedComponent(): Any =
        object : GeneratedComponent, WakeUpReceiver.WakeUpEntryPoint {
            override fun appPreferencesRepository() = preferences

            override fun crashReporter() = crashReporter
        }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = WakeUpReceiverTestApp::class)
class WakeUpReceiverTest {
    private lateinit var app: WakeUpReceiverTestApp
    private val preferences: AppPreferencesRepository = mockk(relaxed = true)
    private val crashReporter: CrashReporter = mockk(relaxed = true)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.preferences = preferences
        app.crashReporter = crashReporter
        // Enqueued work runs immediately in the test WorkManager; a stub worker keeps the real
        // workers (and their Hilt dependencies) out of this test, which only checks what is enqueued.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(StubWorkerFactory)
                .build(),
        )
    }

    @Test
    fun `boot completed reschedules all alarms`() {
        WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertRescheduleEnqueued()
    }

    @Test
    fun `app update reschedules all alarms`() {
        WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        assertRescheduleEnqueued()
    }

    @Test
    fun `granting the exact alarm permission reschedules all alarms`() {
        WakeUpReceiver().onReceive(app, Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))

        assertRescheduleEnqueued()
    }

    @Test
    fun `repeated boot broadcasts do not pile up reschedule work`() {
        repeat(3) { WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED)) }

        assertEquals(1, workInfos(RESCHEDULE_WORK).size)
    }

    @Test
    fun `unrelated broadcasts do not reschedule`() {
        WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_SCREEN_ON))

        assertTrue(workInfos(RESCHEDULE_WORK).isEmpty())
        assertTrue(workInfos(BRIEFING_WORK).isEmpty())
    }

    @Test
    fun `first unlock of the day records the wake-up and requests the daily briefing`() {
        every { preferences.getWakeUpHistory() } returns flowOf(listOf(yesterdayAtNine()))

        WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_USER_PRESENT))

        coVerify(timeout = 5_000) { preferences.addWakeUpTimestamp(any()) }
        awaitWork(BRIEFING_WORK)
        assertTrue(workInfos(BRIEFING_WORK).single().tags.contains(DailyBriefingWorker::class.java.name))
    }

    @Test
    fun `later unlocks on the same day are ignored`() {
        every { preferences.getWakeUpHistory() } returns flowOf(listOf(System.currentTimeMillis()))

        WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_USER_PRESENT))

        coVerify(timeout = 5_000) { preferences.getWakeUpHistory() }
        Thread.sleep(200) // the receiver works on Dispatchers.IO; give it time to (not) write
        coVerify(exactly = 0) { preferences.addWakeUpTimestamp(any()) }
        assertTrue(workInfos(BRIEFING_WORK).isEmpty())
    }

    @Test
    fun `a storage failure while recording the wake-up is reported instead of crashing`() {
        val failure = IOException("disk error")
        every { preferences.getWakeUpHistory() } returns flow { throw failure }

        WakeUpReceiver().onReceive(app, Intent(Intent.ACTION_USER_PRESENT))

        verify(timeout = 5_000) { crashReporter.recordNonFatal(failure, any()) }
        assertTrue(workInfos(BRIEFING_WORK).isEmpty())
    }

    private fun assertRescheduleEnqueued() {
        val infos = workInfos(RESCHEDULE_WORK)
        assertEquals(1, infos.size)
        assertTrue(infos.single().tags.contains(AlarmSchedulingWorker::class.java.name))
    }

    private fun awaitWork(name: String) {
        val deadline = System.currentTimeMillis() + 5_000
        while (workInfos(name).isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(20)
    }

    private fun workInfos(name: String): List<WorkInfo> =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork(name).get()

    private fun yesterdayAtNine() =
        LocalDate.now().minusDays(1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

    private companion object {
        const val RESCHEDULE_WORK = "reschedule_alarms_boot"
        const val BRIEFING_WORK = "daily_briefing_wakeup"
    }
}
