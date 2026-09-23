package digital.tonima.core.service

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.collect.ImmutableList
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.usecases.BriefingResult
import digital.tonima.core.ai.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.data.repository.CalendarRepository
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.utils.WidgetUpdater
import digital.tonima.core.viewmodel.UiText
import digital.tonima.kairos.core.model.Event
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class DailyBriefingWorkerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val generateBriefing: GenerateDailyBriefingUseCase = mockk()
    private val calendarRepository: CalendarRepository = mockk()
    private val widgetUpdater: WidgetUpdater = mockk(relaxed = true)
    private val proUserProvider: ProUserProvider = mockk()

    private val todayEvent = event(id = 1L, day = LocalDate.now(), hour = 10)
    private val otherDayEvent =
        event(id = 2L, day = LocalDate.now().withDayOfMonth(if (LocalDate.now().dayOfMonth == 1) 2 else 1), hour = 10)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        every { proUserProvider.isAiUser } returns MutableStateFlow(true)
        coEvery {
            calendarRepository.getEventsForMonth(
                any(),
                any(),
            )
        } returns ImmutableList.of(todayEvent, otherDayEvent)
    }

    @Test
    fun `a fresh briefing about today's events updates the widget and notifies`() =
        runTest {
            coEvery {
                generateBriefing(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns BriefingResult.Success("**3** reuniões hoje")

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            coVerify { generateBriefing(listOf(todayEvent), any(), any(), any()) }
            coVerify { widgetUpdater.updateDailyBriefingWidget() }
            val notification = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
            assertEquals("3 reuniões hoje", notification.extras.getString(Notification.EXTRA_TEXT))
        }

    @Test
    fun `a briefing already generated today is not notified again`() =
        runTest {
            coEvery { generateBriefing(any(), any(), any(), any()) } returns BriefingResult.Cached("cached")

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
            coVerify(exactly = 0) { widgetUpdater.updateDailyBriefingWidget() }
        }

    @Test
    fun `users without the ai plan get no briefing`() =
        runTest {
            every { proUserProvider.isAiUser } returns MutableStateFlow(false)

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            coVerify(exactly = 0) { generateBriefing(any(), any(), any(), any()) }
        }

    @Test
    fun `a day without events gets no briefing`() =
        runTest {
            coEvery { calendarRepository.getEventsForMonth(any(), any()) } returns ImmutableList.of(otherDayEvent)

            assertEquals(ListenableWorker.Result.success(), worker().doWork())

            coVerify(exactly = 0) { generateBriefing(any(), any(), any(), any()) }
        }

    @Test
    fun `network and quota errors are retried, other errors are not`() =
        runTest {
            mapOf(
                AiErrorType.NETWORK to ListenableWorker.Result.retry(),
                AiErrorType.RATE_LIMITED to ListenableWorker.Result.retry(),
                AiErrorType.SAFETY_BLOCKED to ListenableWorker.Result.failure(),
                AiErrorType.UNKNOWN to ListenableWorker.Result.failure(),
            ).forEach { (type, expected) ->
                coEvery { generateBriefing(any(), any(), any(), any()) } returns
                    BriefingResult.Error(UiText.DynamicString("x"), type)

                assertEquals(type.name, expected, worker().doWork())
            }
        }

    @Test
    fun `unexpected failures are retried`() =
        runTest {
            coEvery { calendarRepository.getEventsForMonth(any(), any()) } throws SecurityException("no permission")

            assertEquals(ListenableWorker.Result.retry(), worker().doWork())
        }

    private fun worker() =
        DailyBriefingWorker(
            app,
            mockk<WorkerParameters>(relaxed = true),
            generateBriefing,
            calendarRepository,
            widgetUpdater,
            proUserProvider,
        )

    private fun event(
        id: Long,
        day: LocalDate,
        hour: Int,
    ) = Event(
        id = id,
        title = "Event $id",
        startTime = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
}
