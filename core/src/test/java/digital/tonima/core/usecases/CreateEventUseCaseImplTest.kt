package digital.tonima.core.usecases

import digital.tonima.core.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CreateEventUseCaseImplTest {
    private lateinit var mockCalendarRepository: CalendarRepository
    private lateinit var createEventUseCase: CreateEventUseCase

    @Before
    fun setup() {
        mockCalendarRepository = mockk()
        createEventUseCase = CreateEventUseCaseImpl(mockCalendarRepository)
    }

    @Test
    fun `invoke calls repository insertEvent`() =
        runTest {
            val calendarId = 1L
            val title = "Test Event"
            val startTime = 1000L
            val endTime = 2000L

            coEvery {
                mockCalendarRepository.insertEvent(calendarId, title, any(), any(), startTime, endTime, any())
            } returns 123L

            val result = createEventUseCase.invoke(calendarId, title, null, null, startTime, endTime, false)

            assertEquals(123L, result)
            coVerify(exactly = 1) {
                mockCalendarRepository.insertEvent(calendarId, title, null, null, startTime, endTime, false)
            }
        }
}
