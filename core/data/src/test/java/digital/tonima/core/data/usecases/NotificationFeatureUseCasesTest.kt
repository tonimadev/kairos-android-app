package digital.tonima.core.data.usecases

import app.cash.turbine.test
import digital.tonima.core.notifications.SuggestedEvent
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.kairos.core.model.DeviceCalendar
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFeatureUseCasesTest {
    private val preferences: AppPreferencesRepository = mockk()
    private val calendars: GetAvailableCalendarsUseCase = mockk()
    private val createEvent: CreateEventUseCase = mockk()
    private val suggestion = SuggestedEvent(title = "Dentista", startMillis = 1_000L, endMillis = 2_000L)

    @Test
    fun `features are combined from the three opt in preferences`() =
        runTest {
            every { preferences.isNotificationDedupEnabled() } returns flowOf(true)
            every { preferences.isFocusDigestEnabled() } returns flowOf(false)
            every { preferences.isEventSuggestionsEnabled() } returns flowOf(true)

            ObserveNotificationFeaturesUseCase(preferences)().test {
                val features = awaitItem()
                assertEquals(NotificationFeatures(true, false, true), features)
                assertTrue(features.anyEnabled)
                awaitComplete()
            }
        }

    @Test
    fun `nothing is enabled by default`() {
        assertFalse(NotificationFeatures().anyEnabled)
    }

    @Test
    fun `listener access comes from the permission manager`() {
        val permissionManager: PermissionManager = mockk()
        every { permissionManager.hasNotificationListenerAccess() } returns true

        assertTrue(HasNotificationListenerAccessUseCase(permissionManager)())
    }

    @Test
    fun `accepted suggestion is created in the first writable calendar`() =
        runTest {
            coEvery { calendars() } returns
                listOf(DeviceCalendar(7L, "Work", "me@x.com"), DeviceCalendar(8L, "Home", "me@x.com"))
            coEvery { createEvent(7L, "Dentista", null, null, 1_000L, 2_000L, false) } returns 99L

            val created = CreateSuggestedEventUseCase(calendars, createEvent)(suggestion)

            assertTrue(created)
            coVerify { createEvent(7L, "Dentista", null, null, 1_000L, 2_000L, false) }
        }

    @Test
    fun `no calendar available means the event is not created`() =
        runTest {
            coEvery { calendars() } returns emptyList()

            assertFalse(CreateSuggestedEventUseCase(calendars, createEvent)(suggestion))
            coVerify(exactly = 0) { createEvent(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `a failed insert is reported as not created`() =
        runTest {
            coEvery { calendars() } returns listOf(DeviceCalendar(7L, "Work", "me@x.com"))
            coEvery { createEvent(any(), any(), any(), any(), any(), any(), any()) } returns null

            assertFalse(CreateSuggestedEventUseCase(calendars, createEvent)(suggestion))
        }
}
