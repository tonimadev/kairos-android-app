package digital.tonima.kairos.wear.ui

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.usecases.UpdateAppPreferenceUseCase
import digital.tonima.kairos.wear.sync.SyncActions
import digital.tonima.kairos.wear.sync.WearEventCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = android.app.Application::class)
class WearCalendarViewModelTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Suppress("TooManyFunctions")
    private class FakePrefsRepo : AppPreferencesRepository {
        private val global = MutableStateFlow(true)
        private val disabledInstances = MutableStateFlow<Set<String>>(emptySet())
        private val disabledSeries = MutableStateFlow<Set<String>>(emptySet())
        private val vibrateOnly = MutableStateFlow(false)
        private val autostartSuggestionDismissed = MutableStateFlow(false)
        private val vibrateOnlyEventIds = MutableStateFlow<Set<String>>(emptySet())
        private val installationDate = MutableStateFlow(0L)
        private val ratingPrompted = MutableStateFlow(false)
        private val ratingCompleted = MutableStateFlow(false)
        private val allDayAlarmsEnabled = MutableStateFlow(false)
        private val allDayAlarmHour = MutableStateFlow(9)
        private val alarmOffsetMinutes = MutableStateFlow(0L)
        private val enabledCalendarIds = MutableStateFlow<Set<String>>(emptySet())
        private val snoozeTimeMinutes = MutableStateFlow(10)
        private val skipWeekendsEnabled = MutableStateFlow(false)
        private val autoDismissMinutes = MutableStateFlow(10)
        private val wakeUpHistory = MutableStateFlow<List<Long>>(emptyList())
        private val exactAlarmSkipped = MutableStateFlow(false)
        private val fullScreenIntentSkipped = MutableStateFlow(false)
        private val isTemperatureInCelsius = MutableStateFlow(true)
        private val autoJoinEnabled = MutableStateFlow(false)
        private val autoFocusModeEnabled = MutableStateFlow(false)

        override fun isGlobalAlarmEnabled() = global as Flow<Boolean>

        override suspend fun setGlobalAlarmEnabled(enabled: Boolean) {
            global.value = enabled
        }

        override fun getDisabledEventIds() = disabledInstances as Flow<Set<String>>

        override suspend fun setDisabledEventIds(ids: Set<String>) {
            disabledInstances.value = ids
        }

        override fun getDisabledSeriesIds() = disabledSeries as Flow<Set<String>>

        override suspend fun setDisabledSeriesIds(ids: Set<String>) {
            disabledSeries.value = ids
        }

        override fun getVibrateOnlyEventIds(): Flow<Set<String>> = vibrateOnlyEventIds

        override suspend fun setVibrateOnlyEventIds(ids: Set<String>) {
            vibrateOnlyEventIds.value = ids
        }

        override fun getVibrateOnly() = vibrateOnly as Flow<Boolean>

        override suspend fun setVibrateOnly(enabled: Boolean) {
            vibrateOnly.value = enabled
        }

        override fun getAutostartSuggestionDismissed() = autostartSuggestionDismissed as Flow<Boolean>

        override suspend fun setAutostartSuggestionDismissed(dismissed: Boolean) {
            autostartSuggestionDismissed.value = dismissed
        }

        override fun getInstallationDate() = installationDate as Flow<Long>

        override suspend fun setInstallationDate(date: Long) {
            installationDate.value = date
        }

        override fun isRatingPrompted() = ratingPrompted as Flow<Boolean>

        override suspend fun setRatingPrompted(prompted: Boolean) {
            ratingPrompted.value = prompted
        }

        override fun isRatingCompleted() = ratingCompleted as Flow<Boolean>

        override suspend fun setRatingCompleted(completed: Boolean) {
            ratingCompleted.value = completed
        }

        override fun isAllDayAlarmsEnabled() = allDayAlarmsEnabled as Flow<Boolean>

        override suspend fun setAllDayAlarmsEnabled(enabled: Boolean) {
            allDayAlarmsEnabled.value = enabled
        }

        override fun getAllDayAlarmHour() = allDayAlarmHour as Flow<Int>

        override suspend fun setAllDayAlarmHour(hour: Int) {
            allDayAlarmHour.value = hour
        }

        override fun getAlarmOffsetMinutes() = alarmOffsetMinutes as Flow<Long>

        override suspend fun setAlarmOffsetMinutes(minutes: Long) {
            alarmOffsetMinutes.value = minutes
        }

        override fun getEnabledCalendarIds(): Flow<Set<String>> = enabledCalendarIds

        override suspend fun setEnabledCalendarIds(ids: Set<String>) {
            enabledCalendarIds.value = ids
        }

        override fun getSnoozeTimeMinutes() = snoozeTimeMinutes as Flow<Int>

        override suspend fun setSnoozeTimeMinutes(minutes: Int) {
            snoozeTimeMinutes.value = minutes
        }

        override fun isSkipWeekendsEnabled(): Flow<Boolean> = skipWeekendsEnabled

        override suspend fun setSkipWeekendsEnabled(enabled: Boolean) {
            skipWeekendsEnabled.value = enabled
        }

        override fun getAutoDismissMinutes(): Flow<Int> = autoDismissMinutes

        override suspend fun setAutoDismissMinutes(minutes: Int) {
            autoDismissMinutes.value = minutes
        }

        override fun getWakeUpHistory(): Flow<List<Long>> = wakeUpHistory

        override suspend fun addWakeUpTimestamp(timestamp: Long) {
            wakeUpHistory.value = wakeUpHistory.value + timestamp
        }

        override fun getPreferredCity(): Flow<String?> = flowOf(null)

        override suspend fun setPreferredCity(city: String) {
            // No-op
        }

        override fun isLocationAlarmEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setLocationAlarmEnabled(enabled: Boolean) {
            // No-op
        }

        override fun getPreferredTransportMode(): Flow<String> = flowOf("driving")

        override suspend fun setPreferredTransportMode(mode: String) {
            // No-op
        }

        override fun isExactAlarmPermissionSkipped(): Flow<Boolean> = exactAlarmSkipped

        override suspend fun setExactAlarmPermissionSkipped(skipped: Boolean) {
            exactAlarmSkipped.value = skipped
        }

        override fun isFullScreenIntentPermissionSkipped(): Flow<Boolean> = fullScreenIntentSkipped

        override suspend fun setFullScreenIntentPermissionSkipped(skipped: Boolean) {
            fullScreenIntentSkipped.value = skipped
        }

        override fun isTemperatureInCelsius(): Flow<Boolean> = isTemperatureInCelsius

        override suspend fun setTemperatureInCelsius(isCelsius: Boolean) {
            isTemperatureInCelsius.value = isCelsius
        }

        override fun isAutoJoinEnabled(): Flow<Boolean> = autoJoinEnabled

        override suspend fun setAutoJoinEnabled(enabled: Boolean) {
            autoJoinEnabled.value = enabled
        }

        override fun isAutoFocusModeEnabled(): Flow<Boolean> = autoFocusModeEnabled

        override suspend fun setAutoFocusModeEnabled(enabled: Boolean) {
            autoFocusModeEnabled.value = enabled
        }

        private val customRingtoneUri = MutableStateFlow<String?>(null)

        override fun getCustomRingtoneUri(): Flow<String?> = customRingtoneUri

        override suspend fun setCustomRingtoneUri(uri: String?) {
            customRingtoneUri.value = uri
        }

        private val onboardingCompleted = MutableStateFlow(false)

        override fun isOnboardingCompleted(): Flow<Boolean> = onboardingCompleted

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            onboardingCompleted.value = completed
        }

        private val snoozeCount = MutableStateFlow(0)

        override fun getSnoozeCount(): Flow<Int> = snoozeCount

        override suspend fun incrementSnoozeCount() {
            snoozeCount.value += 1
        }

        private val aiUsageCount = MutableStateFlow(0)

        override fun getAiUsageCount(): Flow<Int> = aiUsageCount

        override suspend fun incrementAiUsageCount() {
            aiUsageCount.value += 1
        }
    }

    private fun createVm(repo: FakePrefsRepo): WearCalendarViewModel {
        val vm =
            WearCalendarViewModel(
                context,
                ObserveAppPreferencesUseCase(repo),
                UpdateAppPreferenceUseCase(repo),
            )
        // Run all init coroutines (reloadFromCache + observePreferences collector)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return vm
    }

    @Test
    fun `loads cached events on init`() {
        val now = System.currentTimeMillis()
        val initial = listOf(Event(1, "A", now + 60_000L), Event(2, "B", now + 120_000L))
        WearEventCache.save(context, initial)

        val vm = createVm(FakePrefsRepo())

        assertEquals(initial.map { it.copy(isAlarmEnabled = true) }, vm.next24hEvents.value)
    }

    @Test
    fun `updates when ACTION_EVENTS_UPDATED is broadcast`() {
        val now = System.currentTimeMillis()
        WearEventCache.save(context, listOf(Event(1, "First", now + 10_000L)))
        val vm = createVm(FakePrefsRepo())
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
        val vm = createVm(FakePrefsRepo())
        assertEquals(1, vm.next24hEvents.value.size)

        WearEventCache.save(context, listOf(Event(6, "New", now + 60_000L)))
        vm.requestRescan()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, vm.next24hEvents.value.size)
        assertEquals(6L, vm.next24hEvents.value[0].id)
    }

    @Test
    fun `isGlobalAlarmEnabled follows preference`() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = FakePrefsRepo()
            val vm = createVm(repo)

            vm.isGlobalAlarmEnabled.test {
                assertEquals(true, awaitItem())

                repo.setGlobalAlarmEnabled(false)
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                assertEquals(false, awaitItem())
            }
        }

    @Test
    fun `toggleGlobalAlarm calls repository`() =
        runTest(UnconfinedTestDispatcher()) {
            val repo = FakePrefsRepo()
            val vm = createVm(repo)

            vm.isGlobalAlarmEnabled.test {
                assertEquals(true, awaitItem())

                vm.toggleGlobalAlarm(false)
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                assertEquals(false, awaitItem())
            }
        }

    @Test
    fun `next24hEvents filters by disabled ids`() =
        runTest {
            val now = System.currentTimeMillis()
            val event1 = Event(1, "A", now + 60_000L)
            val event2 = Event(2, "B", now + 120_000L)
            WearEventCache.save(context, listOf(event1, event2))

            val repo = FakePrefsRepo()
            val vm = createVm(repo)

            assertEquals(2, vm.next24hEvents.value.size)
            assertEquals(true, vm.next24hEvents.value[0].isAlarmEnabled)
            assertEquals(true, vm.next24hEvents.value[1].isAlarmEnabled)

            repo.setDisabledSeriesIds(setOf("1"))
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            assertEquals(2, vm.next24hEvents.value.size)
            assertEquals(
                false,
                vm.next24hEvents.value
                    .find { it.id == 1L }
                    ?.isAlarmEnabled,
            )
            assertEquals(
                true,
                vm.next24hEvents.value
                    .find { it.id == 2L }
                    ?.isAlarmEnabled,
            )
        }
}
