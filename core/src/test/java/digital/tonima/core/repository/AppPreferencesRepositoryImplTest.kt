package digital.tonima.core.repository

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppPreferencesRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var repository: AppPreferencesRepositoryImpl

    @Before
    fun setUp() =
        runTest {
            context = RuntimeEnvironment.getApplication() as Application
            // Clear DataStore to ensure clean slate between tests
            context.dataStore.edit { it.clear() }
            repository = AppPreferencesRepositoryImpl(context)
        }

    @Test
    fun `isGlobalAlarmEnabled defaults to true and can be toggled`() =
        runTest {
            repository.isGlobalAlarmEnabled().test {
                // default
                assertEquals(true, awaitItem())
                // set to false
                repository.setGlobalAlarmEnabled(false)
                assertEquals(false, awaitItem())
                // set back to true
                repository.setGlobalAlarmEnabled(true)
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getDisabledEventIds defaults to empty and persists values`() =
        runTest {
            repository.getDisabledEventIds().test {
                assertEquals(emptySet<String>(), awaitItem())
                val ids = setOf("1", "2", "abc")
                repository.setDisabledEventIds(ids)
                assertEquals(ids, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getDisabledSeriesIds defaults to empty and persists values`() =
        runTest {
            repository.getDisabledSeriesIds().test {
                assertEquals(emptySet<String>(), awaitItem())
                val ids = setOf("10", "20")
                repository.setDisabledSeriesIds(ids)
                assertEquals(ids, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getVibrateOnly defaults to false and can be toggled`() =
        runTest {
            repository.getVibrateOnly().test {
                // default false
                assertEquals(false, awaitItem())
                repository.setVibrateOnly(true)
                assertEquals(true, awaitItem())
                repository.setVibrateOnly(false)
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getAutostartSuggestionDismissed defaults to false and can be set`() =
        runTest {
            repository.getAutostartSuggestionDismissed().test {
                assertEquals(false, awaitItem())
                repository.setAutostartSuggestionDismissed(true)
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getInstallationDate defaults to 0 and can be set`() =
        runTest {
            repository.getInstallationDate().test {
                assertEquals(0L, awaitItem())
                val date = 123456789L
                repository.setInstallationDate(date)
                assertEquals(date, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isRatingPrompted defaults to false and can be set`() =
        runTest {
            repository.isRatingPrompted().test {
                assertEquals(false, awaitItem())
                repository.setRatingPrompted(true)
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `isRatingCompleted defaults to false and can be set`() =
        runTest {
            repository.isRatingCompleted().test {
                assertEquals(false, awaitItem())
                repository.setRatingCompleted(true)
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
