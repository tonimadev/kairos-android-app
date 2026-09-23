package digital.tonima.core.ai.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.repository.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DailyBriefingRepositoryImplTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = DailyBriefingRepositoryImpl(context)

    @Before
    fun setUp() =
        runTest {
            context.dataStore.edit { it.clear() }
        }

    @Test
    fun `nothing is stored before the first briefing`() =
        runTest {
            assertNull(repository.getDailyBriefing().first())
            assertNull(repository.getLastGeneratedDate())
        }

    @Test
    fun `a saved briefing is read back with its date`() =
        runTest {
            val date = LocalDate.of(2026, 9, 23)

            repository.saveDailyBriefing("Bom dia!", date)

            assertEquals("Bom dia!", repository.getDailyBriefing().first())
            assertEquals(date, repository.getLastGeneratedDate())
        }
}
