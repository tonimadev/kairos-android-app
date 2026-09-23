package digital.tonima.core.data.usecases

import com.google.common.collect.ImmutableList
import digital.tonima.core.data.repository.CalendarRepository
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.repository.RingerModeRepository
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.model.DeviceCalendar
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataDelegatingUseCasesTest {
    private val preferences: AppPreferencesRepository = mockk(relaxed = true)
    private val calendarRepository: CalendarRepository = mockk()

    @Test
    fun `app status is read from the preferences`() =
        runTest {
            every { preferences.getCustomRingtoneUri() } returns flowOf("content://tone")
            every { preferences.isOnboardingCompleted() } returns flowOf(true)
            every { preferences.getSnoozeCount() } returns flowOf(3)
            every { preferences.getAiUsageCount() } returns flowOf(5)
            every { preferences.getWakeUpHistory() } returns flowOf(listOf(1L))
            val status = ObserveAppStatusUseCase(preferences)

            assertEquals("content://tone", status.getCustomRingtoneUri().first())
            assertTrue(status.isOnboardingCompleted().first())
            assertEquals(3, status.getSnoozeCount().first())
            assertEquals(5, status.getAiUsageCount().first())
            assertEquals(listOf(1L), status.getWakeUpHistory().first())
        }

    @Test
    fun `app status updates are written to the preferences`() =
        runTest {
            val update = UpdateAppStatusUseCase(preferences)

            update.incrementAiUsageCount()
            update.setCustomRingtoneUri(null)
            update.incrementSnoozeCount()
            update.addWakeUpTimestamp(42L)

            coVerify { preferences.incrementAiUsageCount() }
            coVerify { preferences.setCustomRingtoneUri(null) }
            coVerify { preferences.incrementSnoozeCount() }
            coVerify { preferences.addWakeUpTimestamp(42L) }
        }

    @Test
    fun `snooze alarms go through the scheduler`() {
        val scheduler = mockk<EventAlarmScheduler>(relaxed = true)

        ScheduleSnoozeAlarmUseCase(scheduler)("Reunião", 7, 1L, 1_000L, "https://meet.google.com/x")

        verify { scheduler.scheduleSnooze("Reunião", 7, 1L, 1_000L, "https://meet.google.com/x") }
    }

    @Test
    fun `observing the ringer mode starts the repository first`() =
        runTest {
            val state = MutableStateFlow(AudioWarningState.SILENT)
            val repository =
                mockk<RingerModeRepository>(relaxed = true) { every { ringerMode } returns state }

            assertEquals(AudioWarningState.SILENT, ObserveRingerModeUseCase(repository)().first())
            verify { repository.startObserving() }
        }

    @Test
    fun `calendar management delegates to the calendar repository`() =
        runTest {
            val calendars = ImmutableList.of(DeviceCalendar(1L, "Pessoal", "me"))
            coEvery { calendarRepository.getAvailableCalendars() } returns calendars
            coEvery { calendarRepository.deleteCalendar(1L) } returns true
            coEvery { calendarRepository.updateCalendar(1L, "Trabalho", 5) } returns false

            assertEquals(calendars, GetAvailableCalendarsUseCase(calendarRepository)())
            assertTrue(DeleteCalendarUseCase(calendarRepository)(1L))
            assertEquals(false, UpdateCalendarUseCase(calendarRepository)(1L, "Trabalho", 5))
        }
}
