package digital.tonima.kairos.ui.components

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.model.Weather
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Small stateless cards shown on the main screen: weather, daily briefing, event card and upgrade. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CalendarCardsTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()

    // region WeatherCard

    @Test
    fun `weather card asks for location permission before showing the weather`() {
        weatherCard(weather = null)

        compose.onNodeWithText(string(R.string.provide_permission)).assertExists()
        assertEquals("Weather must not be fetched without permission", emptyList<String>(), calls)
    }

    @Test
    fun `weather is fetched automatically once location is granted`() {
        grantLocation()

        weatherCard(weather = null)

        assertEquals(listOf("fetch"), calls)
        compose.onNodeWithText(string(R.string.loading_weather)).assertExists()
    }

    @Test
    fun `weather shows the temperature in the chosen unit and the description`() {
        grantLocation()

        weatherCard(weather = sunny, celsius = false)

        compose.onNodeWithText("77°F").assertExists()
        compose.onNodeWithText("Clear sky").assertExists()
        assertEquals("Weather already loaded must not be refetched", emptyList<String>(), calls)
    }

    @Test
    fun `weather error offers to try again`() {
        grantLocation()

        weatherCard(weather = null, error = "No connection")
        calls.clear()
        compose.onNodeWithText(string(R.string.try_again)).performClick()

        assertEquals(listOf("fetch"), calls)
    }

    // endregion

    // region DailyBriefingCard

    @Test
    fun `briefing card invites generating a briefing when there is none`() {
        briefingCard(briefing = null, generating = false)

        compose.onNodeWithText(string(R.string.daily_briefing_placeholder)).assertExists()
        compose.onNodeWithText(string(R.string.generate_briefing)).performClick()

        assertEquals(listOf("generate"), calls)
    }

    @Test
    fun `briefing card hides the generate button while generating`() {
        briefingCard(briefing = null, generating = true)

        compose.onAllNodesWithText(string(R.string.generate_briefing)).assertCountEquals(0)
    }

    @Test
    fun `briefing card shows the briefing and lets the user refresh or ask the ai`() {
        briefingCard(briefing = "Three meetings today.", generating = false)

        compose.onNodeWithText("Three meetings today.").assertExists()
        compose.onNodeWithContentDescription(string(R.string.cd_refresh_briefing)).performClick()
        compose.onNodeWithText(string(R.string.ask_ai_label)).performClick()

        assertEquals(listOf("generate", "interact"), calls)
    }

    // endregion

    // region EventCard

    @Test
    fun `event card shows the title and reports taps and toggles`() {
        val event = EventUiModel(id = 1L, title = "Dentist", startTime = 1_800_000_000_000L, isAlarmEnabled = false)
        compose.setContent {
            EventCard(
                event = event,
                isGloballyEnabled = true,
                onToggle = { calls += "toggle:$it" },
                onEventClick = { calls += "click" },
            )
        }

        compose.onNodeWithText("Dentist").performClick()
        compose.onNode(isToggleable()).assertIsOff().performClick()

        assertEquals(listOf("click", "toggle:true"), calls)
    }

    @Test
    fun `recurring events are labelled as repeating`() {
        compose.setContent {
            EventCard(
                event =
                    EventUiModel(
                        id = 1L,
                        title = "Standup",
                        startTime = 1_800_000_000_000L,
                        isRecurring = true,
                        isAlarmEnabled = true,
                    ),
                isGloballyEnabled = true,
                onToggle = {},
                onEventClick = {},
            )
        }

        compose.onNodeWithText(string(R.string.everyday)).assertExists()
        compose.onNode(isToggleable()).assertIsOn()
    }

    // endregion

    // region ProUpgradeCard

    @Test
    fun `upgrade card requests the subscription`() {
        compose.setContent { ProUpgradeCard(onUpgradeClick = { calls += "upgrade" }) }

        compose.onNodeWithText(string(R.string.pro_ia_upgrade_title)).assertExists()
        compose.onNodeWithText(string(R.string.try_pro_plan)).performClick()

        assertEquals(listOf("upgrade"), calls)
    }

    // endregion

    private val sunny =
        Weather(temperature = 77.4, description = "clear sky", icon = "01d", city = "São Paulo", conditionCode = 800)

    private fun weatherCard(
        weather: Weather?,
        error: String? = null,
        celsius: Boolean = true,
    ) {
        compose.setContent {
            WeatherCard(
                weather = weather,
                weatherError = error,
                isTemperatureInCelsius = celsius,
                onFetchWeather = { calls += "fetch" },
            )
        }
    }

    private fun briefingCard(
        briefing: String?,
        generating: Boolean,
    ) {
        compose.setContent {
            DailyBriefingCard(
                briefing = briefing,
                isGenerating = generating,
                onGenerateClick = { calls += "generate" },
                onInteractClick = { calls += "interact" },
            )
        }
    }

    private fun grantLocation() =
        shadowOf(
            app,
        ).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    private fun string(resId: Int) = app.getString(resId)
}
