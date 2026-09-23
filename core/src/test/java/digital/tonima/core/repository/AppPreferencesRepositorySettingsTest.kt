package digital.tonima.core.repository

import android.app.Application
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Settings and counters not covered by AppPreferencesRepositoryImplTest, against a real DataStore. */
@RunWith(RobolectricTestRunner::class)
class AppPreferencesRepositorySettingsTest {
    private lateinit var repository: AppPreferencesRepositoryImpl

    @Before
    fun setUp() =
        runTest {
            val context = RuntimeEnvironment.getApplication() as Application
            context.dataStore.edit { it.clear() }
            repository = AppPreferencesRepositoryImpl(context)
        }

    @Test
    fun `boolean settings have safe defaults and persist changes`() =
        runTest {
            with(repository) {
                assertRoundTrip(isSkipWeekendsEnabled(), default = false) { setSkipWeekendsEnabled(it) }
                assertRoundTrip(isLocationAlarmEnabled(), default = false) { setLocationAlarmEnabled(it) }
                assertRoundTrip(isExactAlarmPermissionSkipped(), default = false) { setExactAlarmPermissionSkipped(it) }
                assertRoundTrip(isFullScreenIntentPermissionSkipped(), default = false) {
                    setFullScreenIntentPermissionSkipped(it)
                }
                assertRoundTrip(isTemperatureInCelsius(), default = true) { setTemperatureInCelsius(it) }
                assertRoundTrip(isAutoJoinEnabled(), default = false) { setAutoJoinEnabled(it) }
                assertRoundTrip(isAutoFocusModeEnabled(), default = false) { setAutoFocusModeEnabled(it) }
                assertRoundTrip(isOnboardingCompleted(), default = false) { setOnboardingCompleted(it) }
                assertRoundTrip(isProUser(), default = false) { setProUser(it) }
                assertRoundTrip(isAiUser(), default = false) { setAiUser(it) }
            }
        }

    @Test
    fun `numeric and text settings have documented defaults`() =
        runTest {
            assertEquals(10, repository.getAutoDismissMinutes().first())
            assertEquals("driving", repository.getPreferredTransportMode().first())
            assertNull(repository.getPreferredCity().first())
            assertNull(repository.getCustomRingtoneUri().first())

            repository.setAutoDismissMinutes(3)
            repository.setPreferredTransportMode("transit")
            repository.setPreferredCity("São Paulo")

            assertEquals(3, repository.getAutoDismissMinutes().first())
            assertEquals("transit", repository.getPreferredTransportMode().first())
            assertEquals("São Paulo", repository.getPreferredCity().first())
        }

    @Test
    fun `vibrate-only event ids persist`() =
        runTest {
            assertEquals(emptySet<String>(), repository.getVibrateOnlyEventIds().first())

            repository.setVibrateOnlyEventIds(setOf("1", "2"))

            assertEquals(setOf("1", "2"), repository.getVibrateOnlyEventIds().first())
        }

    @Test
    fun `custom ringtone can be chosen and reset to the default`() =
        runTest {
            repository.setCustomRingtoneUri("content://media/internal/audio/media/42")
            assertEquals("content://media/internal/audio/media/42", repository.getCustomRingtoneUri().first())

            repository.setCustomRingtoneUri(null)

            assertNull(repository.getCustomRingtoneUri().first())
        }

    @Test
    fun `snooze and ai usage counters start at zero and increment`() =
        runTest {
            assertEquals(0, repository.getSnoozeCount().first())
            assertEquals(0, repository.getAiUsageCount().first())

            repeat(3) { repository.incrementSnoozeCount() }
            repository.incrementAiUsageCount()

            assertEquals(3, repository.getSnoozeCount().first())
            assertEquals(1, repository.getAiUsageCount().first())
        }

    @Test
    fun `wake-up history is returned oldest first`() =
        runTest {
            repository.addWakeUpTimestamp(3_000L)
            repository.addWakeUpTimestamp(1_000L)
            repository.addWakeUpTimestamp(2_000L)

            assertEquals(listOf(1_000L, 2_000L, 3_000L), repository.getWakeUpHistory().first())
        }

    @Test
    fun `wake-up history keeps only the 14 most recent entries`() =
        runTest {
            (1L..20L).forEach { repository.addWakeUpTimestamp(it * 1_000L) }

            assertEquals((7L..20L).map { it * 1_000L }, repository.getWakeUpHistory().first())
        }

    @Test
    fun `the same wake-up is not recorded twice`() =
        runTest {
            repository.addWakeUpTimestamp(1_000L)
            repository.addWakeUpTimestamp(1_000L)

            assertEquals(listOf(1_000L), repository.getWakeUpHistory().first())
        }

    private suspend fun assertRoundTrip(
        flow: Flow<Boolean>,
        default: Boolean,
        set: suspend (Boolean) -> Unit,
    ) {
        assertEquals(default, flow.first())
        set(!default)
        assertEquals(!default, flow.first())
    }
}
