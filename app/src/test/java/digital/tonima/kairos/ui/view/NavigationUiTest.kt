package digital.tonima.kairos.ui.view

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.DrawerContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Drawer, onboarding and the main screen shell (top bar, bottom bar, FAB). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
class NavigationUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()

    // region Drawer

    @Test
    fun `free users are offered to remove ads`() {
        drawer(isProUser = false, isAiUser = false)

        compose.onNodeWithText(string(R.string.remove_ads)).performClick()

        assertEquals(listOf("upgrade", "close"), calls)
        compose.onAllNodesWithText(string(R.string.drawer_ai_assistant)).assertCountEquals(0)
    }

    @Test
    fun `pro users see their plan and no remove ads entry`() {
        drawer(isProUser = true, isAiUser = false)

        compose.onNodeWithText(string(R.string.pro_label)).assertExists()
        compose.onAllNodesWithText(string(R.string.remove_ads)).assertCountEquals(0)
    }

    @Test
    fun `ai users reach the assistant and can manage the subscription in the Play Store`() {
        drawer(isProUser = true, isAiUser = true)

        compose.onNodeWithText(string(R.string.drawer_ai_assistant)).performClick()
        compose.onNodeWithText(string(R.string.manage_subscription)).performClick()

        assertEquals(listOf("chat", "close", "close"), calls)
        val store = shadowOf(compose.activity).nextStartedActivity
        assertEquals("https://play.google.com/store/account/subscriptions", store.dataString)
    }

    @Test
    fun `drawer entries open their destinations and close the drawer`() {
        drawer(isProUser = true, isAiUser = false)

        compose.onNodeWithText(string(R.string.settings)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.import_calendar)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.manage_calendars)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.our_other_apps)).performScrollTo().performClick()

        assertEquals(
            listOf("settings", "close", "import", "close", "manage", "close", "otherApps", "close"),
            calls,
        )
    }

    @Test
    fun `sharing the app opens the share sheet with the invite text`() {
        drawer(isProUser = true, isAiUser = false)

        compose.onNodeWithText(string(R.string.share_app)).performScrollTo().performClick()

        val chooser = shadowOf(compose.activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(string(R.string.share_text), send.getStringExtra(Intent.EXTRA_TEXT))
    }

    private fun drawer(
        isProUser: Boolean,
        isAiUser: Boolean,
    ) {
        compose.setContent {
            DrawerContent(
                isProUser = isProUser,
                isAiUser = isAiUser,
                onUpgradeToProClick = { calls += "upgrade" },
                onOurOtherAppsClick = { calls += "otherApps" },
                onSettingsClick = { calls += "settings" },
                onChatHistoryClick = { calls += "chat" },
                onImportCalendarClick = { calls += "import" },
                onManageCalendarsClick = { calls += "manage" },
                onCloseDrawer = { calls += "close" },
            )
        }
    }

    // endregion

    // region Onboarding

    @Test
    fun `onboarding walks through every page before finishing`() {
        var finished = 0
        compose.setContent { OnboardingScreen(onFinish = { finished++ }) }

        compose.onNodeWithText(string(R.string.onboarding_title_1)).assertExists()
        compose.onNodeWithText(string(R.string.onboarding_btn_next)).performClick()
        compose.onNodeWithText(string(R.string.onboarding_title_2)).assertExists()
        compose.onNodeWithText(string(R.string.onboarding_btn_next)).performClick()
        compose.onNodeWithText(string(R.string.onboarding_title_3)).assertExists()
        assertEquals("Must not finish before the last page", 0, finished)

        compose.onNodeWithText(string(R.string.onboarding_btn_start)).performClick()

        assertEquals(1, finished)
    }

    // endregion

    // region Shell

    @Test
    fun `shell bottom bar switches tabs and the fab creates an event`() {
        shell(isAiUser = false)

        compose.onNodeWithText(string(R.string.insights_title)).performClick()
        compose.onNodeWithText(string(R.string.alarms)).performClick()
        compose.onNodeWithContentDescription("Add Alarm").performClick()

        assertEquals(listOf("tab:1", "tab:0", "create"), calls)
        compose.onAllNodesWithText(string(R.string.ai_tab_label)).assertCountEquals(0)
    }

    @Test
    fun `ai users get the ai tab`() {
        shell(isAiUser = true)

        compose.onNodeWithText(string(R.string.ai_tab_label)).performClick()

        assertEquals(listOf("ai"), calls)
    }

    @Test
    fun `menu button opens the drawer`() {
        shell(isAiUser = false)
        compose.onNodeWithText(string(R.string.settings)).assertIsNotDisplayed()

        compose.onNodeWithContentDescription(string(R.string.cd_open_menu)).performClick()
        compose.onNodeWithText(string(R.string.settings)).performScrollTo().performClick()

        assertEquals(listOf("settings"), calls)
    }

    private fun shell(isAiUser: Boolean) {
        compose.setContent {
            EventScreenShell(
                uiState = EventScreenUiState(isAiUser = isAiUser),
                isProUser = true,
                isAiUser = isAiUser,
                snackbarHostState = SnackbarHostState(),
                onUpgradeToPro = { calls += "upgrade" },
                onSettingsClick = { calls += "settings" },
                onChatHistoryClick = { calls += "chat" },
                onImportCalendarClick = { calls += "import" },
                onManageCalendarsClick = { calls += "manage" },
                onCreateEventClick = { calls += "create" },
                onShowAiSuggestions = { calls += "ai" },
                onBottomTabChange = { calls += "tab:$it" },
            ) { Text("content") }
        }
    }

    // endregion

    private fun string(resId: Int) = app.getString(resId)
}
