package digital.tonima.kairos.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.data.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.data.usecases.DepartureInfo
import digital.tonima.core.data.usecases.GetEventsForMonthUseCase
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.sync.WearSyncSchema
import digital.tonima.kairos.core.model.Event
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PhoneEventSyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val getEventsForMonth: GetEventsForMonthUseCase = mockk()
    private val proUserProvider: ProUserProvider = mockk()
    private val calculateDepartureTime: CalculateDepartureTimeUseCase = mockk()
    private val dataClient: DataClient = mockk()
    private val crashReporter: CrashReporter = mockk(relaxed = true)
    private val sent = slot<PutDataRequest>()

    @Before
    fun setUp() {
        mockkStatic(Wearable::class)
        every { Wearable.getDataClient(any<Context>()) } returns dataClient
        every { dataClient.putDataItem(capture(sent)) } returns Tasks.forResult(mockk())
        every { proUserProvider.isAiUser } returns MutableStateFlow(false)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `sends the next 24 hours of events with what the watch alarm needs`() =
        runTest {
            val now = System.currentTimeMillis()
            val meeting =
                Event(
                    id = 1L,
                    title = "Planning",
                    startTime = now + hours(2),
                    endTime = now + hours(3),
                    meetingUrl = "https://meet.google.com/abc",
                    isRecurring = true,
                )
            val tomorrowLate = Event(id = 2L, title = "Too late", startTime = now + hours(30))
            val started = Event(id = 3L, title = "Started", startTime = now - hours(1))
            givenEvents(meeting, tomorrowLate, started)

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            val events = sentEvents()
            assertEquals(1, events.size)
            with(events.single()) {
                assertEquals(1L, getLong(WearSyncSchema.KEY_ID))
                assertEquals("Planning", getString(WearSyncSchema.KEY_TITLE))
                assertEquals(meeting.startTime, getLong(WearSyncSchema.KEY_START))
                assertEquals(meeting.endTime, getLong(WearSyncSchema.KEY_END))
                assertEquals("https://meet.google.com/abc", getString(WearSyncSchema.KEY_MEETING_URL))
                assertTrue(getBoolean(WearSyncSchema.KEY_RECUR))
            }
            assertEquals(WearSyncSchema.PATH_EVENTS_24H, sent.captured.uri.path)
            assertTrue("Sync must be urgent so the watch updates promptly", sent.captured.isUrgent)
        }

    @Test
    fun `an empty day is still sent so the watch can clear deleted events`() =
        runTest {
            givenEvents()

            worker().doWork()

            assertTrue(sent.isCaptured)
            assertTrue(sentEvents().isEmpty())
        }

    @Test
    fun `location and traffic data are only sent to ai users`() =
        runTest {
            val located =
                Event(
                    id = 1L,
                    title = "Dentist",
                    startTime = System.currentTimeMillis() + hours(2),
                    location = "Rua Augusta",
                )
            givenEvents(located)

            worker().doWork()

            with(sentEvents().single()) {
                assertFalse(containsKey(WearSyncSchema.KEY_LOCATION))
                assertFalse(containsKey(WearSyncSchema.KEY_DEPARTURE_TIME))
            }
        }

    @Test
    fun `ai users get location and departure time`() =
        runTest {
            every { proUserProvider.isAiUser } returns MutableStateFlow(true)
            val located =
                Event(
                    id = 1L,
                    title = "Dentist",
                    startTime = System.currentTimeMillis() + hours(2),
                    location = "Rua Augusta",
                )
            coEvery { calculateDepartureTime(located) } returns DepartureInfo(located.startTime - hours(1), 40)
            givenEvents(located)

            worker().doWork()

            with(sentEvents().single()) {
                assertEquals("Rua Augusta", getString(WearSyncSchema.KEY_LOCATION))
                assertEquals(located.startTime - hours(1), getLong(WearSyncSchema.KEY_DEPARTURE_TIME))
                assertEquals(40, getInt(WearSyncSchema.KEY_TRAVEL_TIME))
            }
        }

    @Test
    fun `failures are retried`() =
        runTest {
            coEvery { getEventsForMonth(any()) } throws SecurityException("no calendar permission")

            assertEquals(ListenableWorker.Result.retry(), worker().doWork())
            verify(exactly = 0) { crashReporter.recordNonFatal(any(), any()) }
        }

    @Test
    fun `an unexpected sync failure is retried and reported`() =
        runTest {
            val failure = IllegalStateException("data layer unavailable")
            coEvery { getEventsForMonth(any()) } throws failure

            assertEquals(ListenableWorker.Result.retry(), worker().doWork())
            verify { crashReporter.recordNonFatal(failure, any()) }
        }

    private fun worker() =
        PhoneEventSyncWorker(
            context,
            mockk<WorkerParameters>(relaxed = true),
            getEventsForMonth,
            proUserProvider,
            calculateDepartureTime,
            crashReporter,
        )

    private fun givenEvents(vararg events: Event) {
        coEvery { getEventsForMonth(any()) } returnsMany listOf(events.toList(), emptyList())
    }

    private fun sentEvents(): List<DataMap> =
        DataMap.fromByteArray(checkNotNull(sent.captured.data)).getDataMapArrayList(WearSyncSchema.KEY_EVENTS).orEmpty()

    private fun hours(value: Long) = TimeUnit.HOURS.toMillis(value)
}
