package digital.tonima.kairos.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.tonima.kairos.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionRequestScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun standardPermissionsScreen_showsTitlesAndButtons() {
        composeRule.setContent {
            StandardPermissionsScreen(onSettingsClick = {}, onRetryClick = {})
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.initial_permissions_required))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.permissions_disclaimer)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.open_settings)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.try_again)).assertIsDisplayed()
    }

    @Test
    fun exactAlarmPermissionScreen_showsTitlesAndButtons() {
        var skipped = false
        var alreadyAuthorized = false
        var providePermission = false

        composeRule.setContent {
            ExactAlarmPermissionScreen(
                onAlreadyAuthorizedClick = { alreadyAuthorized = true },
                onProvidePermissionClick = { providePermission = true },
                onSkipClick = { skipped = true },
            )
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.exact_alarm_permission)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.exact_alarm_permission_disclaimer))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.provide_permission)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.already_authorized)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.skip)).assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.skip)).performClick()
        assert(skipped)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.already_authorized)).performClick()
        assert(alreadyAuthorized)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.provide_permission)).performClick()
        assert(providePermission)
    }

    @Test
    fun fullScreenIntentPermissionScreen_showsTitlesAndButtons() {
        var skipped = false
        var alreadyAuthorized = false
        var openSettings = false

        composeRule.setContent {
            FullScreenIntentPermissionScreen(
                onAlreadyAuthorizedClick = { alreadyAuthorized = true },
                onOpenSettingsClick = { openSettings = true },
                onSkipClick = { skipped = true },
            )
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.full_screen_permission)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.full_screen_permission_disclaimer))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.open_settings)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.already_authorized)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.skip)).assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.skip)).performClick()
        assert(skipped)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.already_authorized)).performClick()
        assert(alreadyAuthorized)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.open_settings)).performClick()
        assert(openSettings)
    }
}
