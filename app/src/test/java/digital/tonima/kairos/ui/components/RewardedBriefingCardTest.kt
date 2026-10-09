package digital.tonima.kairos.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ads.components.RewardedAdStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RewardedBriefingCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private var watchClicks = 0

    private fun string(id: Int) = app.getString(id)

    private fun render(
        briefing: String? = null,
        isGenerating: Boolean = false,
        adStatus: RewardedAdStatus = RewardedAdStatus.Ready,
    ) = compose.setContent {
        RewardedBriefingCard(
            briefing = briefing,
            isGenerating = isGenerating,
            adStatus = adStatus,
            onWatchAdClick = { watchClicks++ },
        )
    }

    @Test
    fun `a loaded ad can be watched to unlock the briefing`() {
        render(adStatus = RewardedAdStatus.Ready)

        compose.onNodeWithText(string(R.string.briefing_rewarded_watch)).assertIsEnabled().performClick()

        assertEquals(1, watchClicks)
    }

    @Test
    fun `while the ad loads the button is disabled`() {
        render(adStatus = RewardedAdStatus.Loading)

        compose.onNodeWithText(string(R.string.briefing_rewarded_loading)).assertIsNotEnabled()
    }

    @Test
    fun `when no ad can be loaded the user is told and cannot click`() {
        render(adStatus = RewardedAdStatus.Unavailable)

        compose.onNodeWithText(string(R.string.briefing_rewarded_unavailable)).assertIsNotEnabled()
    }

    @Test
    fun `once the briefing exists it replaces the offer`() {
        render(briefing = "Hoje você tem 2 reuniões.")

        compose.onNodeWithText("Hoje você tem 2 reuniões.").assertExists()
        compose.onNodeWithText(string(R.string.briefing_rewarded_watch)).assertDoesNotExist()
    }

    @Test
    fun `while generating no second ad can be started`() {
        render(isGenerating = true)

        compose.onNodeWithText(string(R.string.briefing_rewarded_watch)).assertDoesNotExist()
    }
}
