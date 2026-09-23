package digital.tonima.kairos.service

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.CalendarContract
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
import digital.tonima.core.service.AlarmSchedulingWorker
import digital.tonima.kairos.receivers.ManagedProfileReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** Things that react to calendar or profile changes by enqueuing sync / rescheduling work. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarChangeTriggersTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(StubWorkerFactory)
                .build(),
        )
        // CalendarChangeObserver is a process-wide singleton; start every test from a clean slate.
        CalendarChangeObserver::class.java.getDeclaredField(
            "initialized",
        ).apply { isAccessible = true }.setBoolean(null, false)
    }

    // region ManagedProfileReceiver

    @Test
    fun `work profile coming back reschedules alarms and resyncs the watch`() {
        listOf(
            Intent.ACTION_MANAGED_PROFILE_AVAILABLE,
            Intent.ACTION_MANAGED_PROFILE_UNLOCKED,
            Intent.ACTION_USER_UNLOCKED,
        )
            .forEach { action ->
                ManagedProfileReceiver().onReceive(app, Intent(action))

                assertEnqueued("profile-resume-alarms", AlarmSchedulingWorker::class.java)
                assertEnqueued("profile-resume-sync", PhoneEventSyncWorker::class.java)
            }
    }

    @Test
    fun `work profile being paused schedules nothing`() {
        ManagedProfileReceiver().onReceive(app, Intent(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE))

        assertTrue(workInfos("profile-resume-alarms").isEmpty())
        assertTrue(workInfos("profile-resume-sync").isEmpty())
    }

    // endregion

    // region CalendarChangeObserver

    @Test
    fun `a burst of calendar changes triggers a single watch sync after the debounce`() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        CalendarChangeObserver.init(app)

        repeat(5) { notifyCalendarChanged() }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertTrue("Must wait for the burst to settle", workInfos(OBSERVER_WORK).isEmpty())

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

        assertEnqueued(OBSERVER_WORK, PhoneEventSyncWorker::class.java)
    }

    @Test
    fun `calendar changes are ignored without calendar permission`() {
        CalendarChangeObserver.init(app)

        notifyCalendarChanged()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))

        assertTrue(workInfos(OBSERVER_WORK).isEmpty())
    }

    // endregion

    private fun notifyCalendarChanged() {
        app.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
        // The observer runs on its own HandlerThread; let it deliver the change.
        Thread.sleep(50)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun assertEnqueued(
        name: String,
        worker: Class<*>,
    ) {
        val infos = workInfos(name)
        assertEquals(name, 1, infos.size)
        assertTrue(name, infos.single().tags.contains(worker.name))
    }

    private fun workInfos(name: String): List<WorkInfo> =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork(name).get()

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
        const val OBSERVER_WORK = "phone-event-sync-onchange"
    }
}
