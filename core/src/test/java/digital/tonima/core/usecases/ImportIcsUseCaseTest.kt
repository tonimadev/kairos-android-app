package digital.tonima.core.usecases

import digital.tonima.core.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportIcsUseCaseTest {
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)
    private val toggleEventAlarmUseCase = mockk<ToggleEventAlarmUseCase>(relaxed = true)

    private val useCase = ImportIcsUseCase(calendarRepository, toggleEventAlarmUseCase)

    @Test
    fun `invoke should create calendar and insert events`() =
        runTest {
            val icsContent =
                """
                BEGIN:VCALENDAR
                BEGIN:VEVENT
                UID:test
                DTSTART:20260603T103000Z
                SUMMARY:Reunião
                END:VEVENT
                END:VCALENDAR
                """.trimIndent()

            coEvery { calendarRepository.createLocalCalendar(any(), any()) } returns 99L
            coEvery {
                calendarRepository.insertEvent(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 101L

            val result = useCase(icsContent, "Meu Calendário", 0xFF0000, alarmsEnabled = true)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                calendarRepository.createLocalCalendar("Meu Calendário", 0xFF0000)
            }
            coVerify(exactly = 1) {
                calendarRepository.insertEvent(99L, "Reunião", null, null, any(), 0L, false)
            }
            coVerify(exactly = 0) {
                toggleEventAlarmUseCase.invoke(any(), any(), any())
            } // alarms enabled, doesn't disable
        }

    @Test
    fun `invoke should disable alarms if alarmsEnabled is false`() =
        runTest {
            val icsContent =
                """
                BEGIN:VCALENDAR
                BEGIN:VEVENT
                UID:test2
                DTSTART:20260603T103000Z
                SUMMARY:Reunião
                END:VEVENT
                END:VCALENDAR
                """.trimIndent()

            coEvery { calendarRepository.createLocalCalendar(any(), any()) } returns 99L
            coEvery {
                calendarRepository.insertEvent(any(), any(), any(), any(), any(), any(), any())
            } returns 101L

            val result = useCase(icsContent, "Meu Calendário", 0xFF0000, alarmsEnabled = false)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { toggleEventAlarmUseCase.invoke(any(), false, true) }
        }
}
