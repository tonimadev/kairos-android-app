package digital.tonima.kairos.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}
