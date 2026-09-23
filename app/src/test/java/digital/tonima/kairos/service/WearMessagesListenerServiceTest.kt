package digital.tonima.kairos.service

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.sync.WearSyncSchema
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WearMessagesListenerServiceTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val dataClient: DataClient = mockk(relaxed = true)
    private lateinit var service: WearMessagesListenerService

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
        mockkStatic(DataMapItem::class, Wearable::class)
        every { Wearable.getDataClient(any<Context>()) } returns dataClient
        service = Robolectric.buildService(WearMessagesListenerService::class.java).create().get()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `dismiss on the watch stops the phone alarm without echoing back`() {
        val uri = Uri.parse("wear://watch" + WearSyncSchema.dismissAlarmPath(4242))

        service.onDataChanged(buffer(changed(uri, 4242)))

        val stop = shadowOf(app).nextStartedService
        assertNotNull(stop)
        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, stop.action)
        assertEquals("WEAR_SYNC", stop.getStringExtra(AlarmSoundAndVibrateService.EXTRA_SOURCE))
        assertEquals(4242, stop.getIntExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, -1))
        verify { dataClient.deleteDataItems(uri) }
    }

    @Test
    fun `snooze on the watch stops the phone alarm`() {
        val uri = Uri.parse("wear://watch" + WearSyncSchema.snoozeAlarmPath(7))

        service.onDataChanged(buffer(changed(uri, 7)))

        val stop = shadowOf(app).nextStartedService
        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, stop.action)
        assertEquals("WEAR_SYNC", stop.getStringExtra(AlarmSoundAndVibrateService.EXTRA_SOURCE))
        assertEquals(7, stop.getIntExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, -1))
    }

    @Test
    fun `dismiss and snooze of different alarms in one batch are all handled`() {
        service.onDataChanged(
            buffer(
                changed(Uri.parse("wear://watch" + WearSyncSchema.dismissAlarmPath(1)), 1),
                changed(Uri.parse("wear://watch" + WearSyncSchema.snoozeAlarmPath(2)), 2),
            ),
        )

        val ids =
            generateSequence { shadowOf(app).nextStartedService }
                .map { it.getIntExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, -1) }
                .toList()
        assertEquals(listOf(1, 2), ids)
    }

    @Test
    fun `unrelated or deleted data items are ignored`() {
        val unrelated = changed(Uri.parse("wear://watch/kairos/something_else"), 1)
        val deleted = changed(Uri.parse("wear://watch" + WearSyncSchema.dismissAlarmPath(2)), 2)
        every { deleted.type } returns DataEvent.TYPE_DELETED

        service.onDataChanged(buffer(unrelated, deleted))

        assertNull(shadowOf(app).nextStartedService)
    }

    @Test
    fun `sync request from the watch enqueues a phone to watch sync`() {
        val message: MessageEvent = mockk(relaxed = true) { every { path } returns WearSyncSchema.PATH_REQUEST_SYNC }

        service.onMessageReceived(message)

        val infos = WorkManager.getInstance(app).getWorkInfosForUniqueWork("phone_sync_immediate").get()
        assertEquals(1, infos.size)
        assertTrue(infos.single().tags.contains(PhoneEventSyncWorker::class.java.name))
    }

    @Test
    fun `other messages do not trigger a sync`() {
        val message: MessageEvent = mockk(relaxed = true) { every { path } returns "/kairos/unknown" }

        service.onMessageReceived(message)

        assertTrue(WorkManager.getInstance(app).getWorkInfosForUniqueWork("phone_sync_immediate").get().isEmpty())
    }

    private fun changed(
        uri: Uri,
        uniqueId: Int,
    ): DataEvent {
        val item: DataItem = mockk { every { this@mockk.uri } returns uri }
        val dataMap = DataMap().apply { putInt(WearSyncSchema.EXTRA_UNIQUE_ID, uniqueId) }
        val mapItem: DataMapItem = mockk { every { this@mockk.dataMap } returns dataMap }
        every { DataMapItem.fromDataItem(item) } returns mapItem
        return mockk {
            every { type } returns DataEvent.TYPE_CHANGED
            every { dataItem } returns item
        }
    }

    private fun buffer(vararg events: DataEvent): DataEventBuffer =
        mockk(relaxed = true) { every { iterator() } answers { events.toMutableList().iterator() } }

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
