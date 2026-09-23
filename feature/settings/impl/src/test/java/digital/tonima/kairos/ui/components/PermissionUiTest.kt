package digital.tonima.kairos.ui.components

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.kairos.core.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalPermissionsApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
class PermissionUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()
    private val standardPermissions: MultiplePermissionsState = mockk(relaxed = true)
    private val locationPermissions: MultiplePermissionsState = mockk(relaxed = true)

    private val allGranted =
        SettingsUiState(
            hasCalendarPermission = true,
            hasPostNotificationsPermission = true,
            hasExactAlarmPermission = true,
            hasFullScreenIntentPermission = true,
        )

    // region PermissionGate

    @Test
    fun `the app is shown once every permission is granted`() {
        gate(allGranted)

        compose.onNodeWithText(APP_CONTENT).assertExists()
    }

    @Test
    fun `calendar or notification permission is asked first and can be retried`() {
        gate(allGranted.copy(hasCalendarPermission = false, hasExactAlarmPermission = false))

        compose.onNodeWithText(string(R.string.initial_permissions_required)).assertExists()
        compose.onNodeWithText(string(R.string.try_again)).performClick()
        compose.onNodeWithText(string(R.string.open_settings)).performClick()

        verify { standardPermissions.launchMultiplePermissionRequest() }
        assertEquals(listOf("appSettings"), calls)
    }

    @Test
    fun `missing notification permission also blocks the app`() {
        gate(allGranted.copy(hasPostNotificationsPermission = false))

        compose.onNodeWithText(string(R.string.initial_permissions_required)).assertExists()
    }

    @Test
    fun `location alarm without location permission asks for it`() {
        every { locationPermissions.allPermissionsGranted } returns false
        gate(allGranted.copy(isLocationAlarmEnabled = true))

        compose.onNodeWithText(string(R.string.try_again)).performClick()

        verify { locationPermissions.launchMultiplePermissionRequest() }
    }

    @Test
    fun `location permission is not needed while the location alarm is off`() {
        every { locationPermissions.allPermissionsGranted } returns false
        gate(allGranted.copy(isLocationAlarmEnabled = false))

        compose.onNodeWithText(APP_CONTENT).assertExists()
    }

    @Test
    fun `exact alarm permission can be granted, confirmed or skipped`() {
        gate(allGranted.copy(hasExactAlarmPermission = false, hasFullScreenIntentPermission = false))

        compose.onNodeWithText(string(R.string.exact_alarm_permission)).assertExists()
        compose.onNodeWithText(string(R.string.provide_permission)).performClick()
        compose.onNodeWithText(string(R.string.already_authorized)).performClick()
        compose.onNodeWithText(string(R.string.skip)).performClick()

        assertEquals(listOf("exactAlarmSettings", "check", "skipExact"), calls)
    }

    @Test
    fun `full screen permission is asked after exact alarms`() {
        gate(allGranted.copy(hasFullScreenIntentPermission = false))

        compose.onNodeWithText(string(R.string.full_screen_permission)).assertExists()
        compose.onNodeWithText(string(R.string.open_settings)).performClick()
        compose.onNodeWithText(string(R.string.already_authorized)).performClick()
        compose.onNodeWithText(string(R.string.skip)).performClick()

        assertEquals(listOf("fullScreenSettings", "check", "skipFullScreen"), calls)
    }

    private fun gate(settings: SettingsUiState) {
        compose.setContent {
            PermissionGate(
                settingsUiState = settings,
                onCheckPermissions = { calls += "check" },
                onSkipExactAlarmPermission = { calls += "skipExact" },
                onSkipFullScreenIntentPermission = { calls += "skipFullScreen" },
                standardPermissionState = standardPermissions,
                locationPermissionState = locationPermissions,
                openAppSettings = { calls += "appSettings" },
                openExactAlarmSettings = { calls += "exactAlarmSettings" },
                openFullScreenIntentSettings = { calls += "fullScreenSettings" },
            ) { Text(APP_CONTENT) }
        }
    }

    // endregion

    @Test
    fun `autostart suggestion opens settings or is dismissed`() {
        compose.setContent {
            AutostartSuggestionCard(onOpenSettings = { calls += "open" }, onDismiss = { calls += "dismiss" })
        }

        compose.onNodeWithText(string(R.string.autostart_suggestion_title)).assertExists()
        compose.onNodeWithText(string(R.string.open_settings)).performClick()
        compose.onNodeWithText(string(R.string.dismiss)).performClick()

        assertEquals(listOf("open", "dismiss"), calls)
    }

    private fun string(resId: Int) = app.getString(resId)

    private companion object {
        const val APP_CONTENT = "app content"
    }
}
