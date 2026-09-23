package digital.tonima.kairos.wear.sync

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
import com.google.android.gms.wearable.Wearable
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.sync.WearSyncSchema
import digital.tonima.kairos.core.model.Event
import digital.tonima.kairos.wear.WorkNames
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class WearEventListenerServiceTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val dataClient: DataClient = mockk(relaxed = true)
    private lateinit var service: WearEventListenerService

    @Before
    fun setUp() {
        app.getSharedPreferences("PhoneEventsCache", Context.MODE_PRIVATE).edit().clear().commit()
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
        service = Robolectric.buildService(WearEventListenerService::class.java).create().get()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `events from the phone are cached and scheduled on the watch`() {
        val payload =
            eventsPayload(
                DataMap().apply {
                    putLong(WearSyncSchema.KEY_ID, 1L)
                    putString(WearSyncSchema.KEY_TITLE, "Dentist")
                    putLong(WearSyncSchema.KEY_START, 1_800_000_000_000L)
                    putBoolean(WearSyncSchema.KEY_RECUR, true)
                    putBoolean(WearSyncSchema.KEY_ALL_DAY, false)
                    putString(WearSyncSchema.KEY_LOCATION, "Rua Augusta, 500")
                    putLong(WearSyncSchema.KEY_DEPARTURE_TIME, 1_799_998_000_000L)
                    putInt(WearSyncSchema.KEY_TRAVEL_TIME, 20)
                },
                DataMap().apply {
                    putLong(WearSyncSchema.KEY_ID, 2L)
                    putString(WearSyncSchema.KEY_TITLE, "Holiday")
                    putLong(WearSyncSchema.KEY_START, 1_800_050_000_000L)
                    putBoolean(WearSyncSchema.KEY_ALL_DAY, true)
                },
            )

        service.onDataChanged(buffer(changed(WearSyncSchema.PATH_EVENTS_24H, payload)))

        assertEquals(
            listOf(
                Event(
                    id = 1L,
                    title = "Dentist",
                    startTime = 1_800_000_000_000L,
                    isRecurring = true,
                    location = "Rua Augusta, 500",
                    departureTime = 1_799_998_000_000L,
                    travelTimeMinutes = 20,
                ),
                Event(id = 2L, title = "Holiday", startTime = 1_800_050_000_000L, isAllDay = true),
            ),
            WearEventCache.load(app),
        )
        assertEquals(
            1,
            WorkManager.getInstance(app).getWorkInfosForUniqueWork(WorkNames.UNIQUE_SCHEDULE_NOW).get().size,
        )
        assertTrue(shadowOf(app).broadcastIntents.any { it.action == SyncActions.ACTION_EVENTS_UPDATED })
    }

    @Test
    fun `events without title get the untitled label`() {
        val payload =
            eventsPayload(
                DataMap().apply {
                    putLong(WearSyncSchema.KEY_ID, 1L)
                    putLong(WearSyncSchema.KEY_START, 1_000L)
                },
            )

        service.onDataChanged(buffer(changed(WearSyncSchema.PATH_EVENTS_24H, payload)))

        assertEquals(
            app.getString(digital.tonima.kairos.core.R.string.event_untitled),
            WearEventCache.load(app).single().title,
        )
    }

    @Test
    fun `dismiss from the phone stops the watch alarm without echoing back`() {
        val uri = Uri.parse("wear://phone" + WearSyncSchema.dismissAlarmPath(4242))

        service.onDataChanged(buffer(changed(uri, DataMap().apply { putInt(WearSyncSchema.EXTRA_UNIQUE_ID, 4242) })))

        val stop = shadowOf(app).nextStartedService
        assertNotNull(stop)
        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, stop.action)
        assertEquals("PHONE_SYNC", stop.getStringExtra(AlarmSoundAndVibrateService.EXTRA_SOURCE))
        assertEquals(4242, stop.getIntExtra(AlarmSoundAndVibrateService.EXTRA_UNIQUE_ID, -1))
        verify { dataClient.deleteDataItems(uri) }
    }

    @Test
    fun `snooze from the phone stops the watch alarm`() {
        val uri = Uri.parse("wear://phone" + WearSyncSchema.snoozeAlarmPath(7))

        service.onDataChanged(buffer(changed(uri, DataMap().apply { putInt(WearSyncSchema.EXTRA_UNIQUE_ID, 7) })))

        val stop = shadowOf(app).nextStartedService
        assertEquals(AlarmSoundAndVibrateService.ACTION_STOP_ALARM, stop.action)
        assertEquals("PHONE_SYNC", stop.getStringExtra(AlarmSoundAndVibrateService.EXTRA_SOURCE))
        verify { dataClient.deleteDataItems(uri) }
    }

    @Test
    fun `deleted data items are ignored`() {
        val event = changed(WearSyncSchema.PATH_EVENTS_24H, eventsPayload(DataMap()))
        every { event.type } returns DataEvent.TYPE_DELETED

        service.onDataChanged(buffer(event))

        assertTrue(WearEventCache.load(app).isEmpty())
        assertEquals(null, shadowOf(app).nextStartedService)
    }

    @Test
    fun `a malformed payload does not crash the listener`() {
        val event = changed(WearSyncSchema.PATH_EVENTS_24H, DataMap())
        val item = event.dataItem
        every { DataMapItem.fromDataItem(item) } throws IllegalStateException("corrupt")

        service.onDataChanged(buffer(event))

        assertTrue(WearEventCache.load(app).isEmpty())
    }

    private fun eventsPayload(vararg events: DataMap) =
        DataMap().apply { putDataMapArrayList(WearSyncSchema.KEY_EVENTS, ArrayList(events.toList())) }

    private fun changed(
        path: String,
        dataMap: DataMap,
    ) = changed(Uri.parse("wear://phone$path"), dataMap)

    private fun changed(
        uri: Uri,
        dataMap: DataMap,
    ): DataEvent {
        val item: DataItem = mockk { every { this@mockk.uri } returns uri }
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
