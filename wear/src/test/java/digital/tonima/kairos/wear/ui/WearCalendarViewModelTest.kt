package digital.tonima.kairos.wear.ui

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.kairos.wear.sync.SyncActions
import digital.tonima.kairos.wear.sync.WearEventCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = android.app.Application::class)
class WearCalendarViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakePrefsRepo : AppPreferencesRepository {
        private val global = MutableStateFlow(true)
        private val disabledInstances = MutableStateFlow<Set<String>>(emptySet())
        private val disabledSeries = MutableStateFlow<Set<String>>(emptySet())
        private val vibrateOnly = MutableStateFlow(false)
        private val autoDismiss = MutableStateFlow(false)
        private val vibrateOnlyEventIds = MutableStateFlow<Set<String>>(emptySet())
        private val installationDate = MutableStateFlow(0L)
        private val ratingPrompted = MutableStateFlow(false)
        private val ratingCompleted = MutableStateFlow(false)
        override fun isGlobalAlarmEnabled() = global as Flow<Boolean>
        override suspend fun setGlobalAlarmEnabled(enabled: Boolean) { global.value = enabled }
        override fun getDisabledEventIds() = disabledInstances as Flow<Set<String>>
        override suspend fun setDisabledEventIds(ids: Set<String>) { disabledInstances.value = ids }
        override fun getDisabledSeriesIds() = disabledSeries as Flow<Set<String>>
        override suspend fun setDisabledSeriesIds(ids: Set<String>) { disabledSeries.value = ids }
        override fun getVibrateOnlyEventIds(): Flow<Set<String>> = vibrateOnlyEventIds
        override suspend fun setVibrateOnlyEventIds(ids: Set<String>) { vibrateOnlyEventIds.value = ids }
        override fun getVibrateOnly() = vibrateOnly as Flow<Boolean>
        override suspend fun setVibrateOnly(enabled: Boolean) { vibrateOnly.value = enabled }
        override fun getAutostartSuggestionDismissed() = autoDismiss as Flow<Boolean>
        override suspend fun setAutostartSuggestionDismissed(dismissed: Boolean) { autoDismiss.value = dismissed }
        override fun getInstallationDate() = installationDate as Flow<Long>
        override suspend fun setInstallationDate(date: Long) { installationDate.value = date }
        override fun isRatingPrompted() = ratingPrompted as Flow<Boolean>
        override suspend fun setRatingPrompted(prompted: Boolean) { ratingPrompted.value = prompted }
        override fun isRatingCompleted() = ratingCompleted as Flow<Boolean>
        override suspend fun setRatingCompleted(completed: Boolean) { ratingCompleted.value = completed }
    }

    @Test
    fun `loads cached events on init`() {
        val now = System.currentTimeMillis()
        val initial = listOf(Event(1, "A", now + 60_000L), Event(2, "B", now + 120_000L))
        WearEventCache.save(context, initial)

        val vm = WearCalendarViewModel(context, FakePrefsRepo())

        // After mapping, default is global enabled and no disabled ids, so isAlarmEnabled should be true
        assertEquals(initial.map { it.copy(isAlarmEnabled = true) }, vm.next24hEvents.value)
    }

    @Test
    fun `updates when ACTION_EVENTS_UPDATED is broadcast`() {
        val now = System.currentTimeMillis()
        WearEventCache.save(context, listOf(Event(1, "First", now + 10_000L)))
        val vm = WearCalendarViewModel(context, FakePrefsRepo())
        assertEquals(1, vm.next24hEvents.value.size)

        // change cache and notify
        WearEventCache.save(context, listOf(Event(2, "Second", now + 20_000L), Event(3, "Third", now + 30_000L)))
        context.sendBroadcast(Intent(SyncActions.ACTION_EVENTS_UPDATED))
        // Ensure the broadcast is processed on the main looper before assertions
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, vm.next24hEvents.value.size)
        assertEquals(2L, vm.next24hEvents.value[0].id)
    }

    @Test
    fun `requestRescan reloads from cache`() {
        val now = System.currentTimeMillis()
        WearEventCache.save(context, listOf(Event(5, "Old", now + 50_000L)))
        val vm = WearCalendarViewModel(context, FakePrefsRepo())
        assertEquals(1, vm.next24hEvents.value.size)

        WearEventCache.save(context, listOf(Event(6, "New", now + 60_000L)))
        vm.requestRescan()

        assertEquals(1, vm.next24hEvents.value.size)
        assertEquals(6L, vm.next24hEvents.value[0].id)
    }
}
