package digital.tonima.core.viewmodel

import app.cash.turbine.test
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.sync.WearMessagingHelper
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class AlarmViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockAnalytics: Analytics = mockk(relaxed = true)
    private val mockWearMessagingHelper: WearMessagingHelper = mockk(relaxed = true)

    private lateinit var viewModel: AlarmViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AlarmViewModel(mockAnalytics, mockWearMessagingHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init populates ui state correctly`() =
        runTest {
            viewModel.handleIntent(
                AlarmIntent.Init(
                    eventTitle = "Team Standup",
                    uniqueId = 42,
                    eventId = 100L,
                    startTime = 999L,
                    meetingUrl = "https://meet.google.com/abc",
                    eventLocation = null,
                ),
            )

            val state = viewModel.uiState.value
            assertEquals("Team Standup", state.eventTitle)
            assertEquals(42, state.uniqueId)
            assertEquals(100L, state.eventId)
            assertEquals(999L, state.startTime)
            assertEquals("https://meet.google.com/abc", state.meetingUrl)
            assertTrue(state.hasMeetingUrl)
        }

    @Test
    fun `init with no meeting url sets hasMeetingUrl false`() =
        runTest {
            viewModel.handleIntent(
                AlarmIntent.Init("Event", 1, 1L, 1L, null, null),
            )

            assertFalse(viewModel.uiState.value.hasMeetingUrl)
        }

    @Test
    fun `snooze logs analytics and emits side effects`() =
        runTest {
            viewModel.handleIntent(
                AlarmIntent.Init("Daily", 10, 50L, 800L, null, null),
            )

            viewModel.sideEffect.test {
                viewModel.handleIntent(AlarmIntent.Snooze)

                val broadcast = awaitItem()
                assertTrue(broadcast is AlarmSideEffect.SendSnoozeBroadcast)
                val snooze = broadcast as AlarmSideEffect.SendSnoozeBroadcast
                assertEquals("Daily", snooze.eventTitle)
                assertEquals(10, snooze.uniqueId)
                assertEquals(50L, snooze.eventId)
                assertEquals(800L, snooze.startTime)

                val finish = awaitItem()
                assertTrue(finish is AlarmSideEffect.FinishScreen)

                cancelAndConsumeRemainingEvents()
            }

            verify {
                mockAnalytics.logEvent(
                    Analytics.EVENT_ALARM_SNOOZE,
                    match { it[Analytics.PARAM_SOURCE] == Analytics.SOURCE_ACTIVITY },
                )
            }
            assertFalse(viewModel.didUserStopAlarm)
        }

    @Test
    fun `stop logs analytics and emits finish`() =
        runTest {
            viewModel.handleIntent(
                AlarmIntent.Init("Meeting", 5, 20L, 500L, null, null),
            )

            viewModel.sideEffect.test {
                viewModel.handleIntent(AlarmIntent.Stop)

                val finish = awaitItem()
                assertTrue(finish is AlarmSideEffect.FinishScreen)

                cancelAndConsumeRemainingEvents()
            }

            verify {
                mockAnalytics.logEvent(
                    Analytics.EVENT_ALARM_STOP,
                    match { it[Analytics.PARAM_SOURCE] == Analytics.SOURCE_ACTIVITY },
                )
            }
            assertTrue(viewModel.didUserStopAlarm)
        }

    @Test
    fun `joinMeeting logs both analytics events and emits url and finish`() =
        runTest {
            viewModel.handleIntent(
                AlarmIntent.Init(
                    "Sprint Review",
                    7,
                    30L,
                    600L,
                    "https://meet.google.com/xyz",
                    null,
                ),
            )

            viewModel.sideEffect.test {
                viewModel.handleIntent(AlarmIntent.JoinMeeting)

                val urlEffect = awaitItem()
                assertTrue(urlEffect is AlarmSideEffect.OpenMeetingUrl)
                assertEquals(
                    "https://meet.google.com/xyz",
                    (urlEffect as AlarmSideEffect.OpenMeetingUrl).url,
                )

                val finish = awaitItem()
                assertTrue(finish is AlarmSideEffect.FinishScreen)

                cancelAndConsumeRemainingEvents()
            }

            verify {
                mockAnalytics.logEvent(
                    Analytics.EVENT_ALARM_STOP,
                    match { it["action"] == "join_meeting" },
                )
            }
            verify {
                mockAnalytics.logEvent(Analytics.EVENT_JOIN_MEETING, any())
            }
            assertTrue(viewModel.didUserStopAlarm)
        }

    @Test
    fun `joinMeeting without meeting url does not emit OpenMeetingUrl`() =
        runTest {
            viewModel.handleIntent(
                AlarmIntent.Init("Event", 1, 1L, 1L, null, null),
            )

            viewModel.sideEffect.test {
                viewModel.handleIntent(AlarmIntent.JoinMeeting)

                val finish = awaitItem()
                assertTrue(finish is AlarmSideEffect.FinishScreen)

                cancelAndConsumeRemainingEvents()
            }
        }
}
