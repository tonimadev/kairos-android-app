package digital.tonima.core.utils

import android.app.Application
import android.os.Build
import android.os.UserManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DeviceInfoUtilsTest {
    private fun managedProfileContext(): Application {
        val app = RuntimeEnvironment.getApplication()
        // MANAGE_USERS lets the shadow answer below API 30, so the test sees whether the app asks at all.
        shadowOf(app).grantPermissions("android.permission.MANAGE_USERS")
        shadowOf(app.getSystemService(UserManager::class.java)).setManagedProfile(true)
        return app
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `below Android 11 the hidden isManagedProfile API is not called`() {
        // On a real Android 9/10 device UserManager.isManagedProfile is a non-SDK API and the call fails
        // at app start, so the app must report a personal profile without asking.
        assertFalse(DeviceInfoUtils.isWorkProfile(managedProfileContext()))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `from Android 11 a managed profile is detected`() {
        assertTrue(DeviceInfoUtils.isWorkProfile(managedProfileContext()))
    }
}
