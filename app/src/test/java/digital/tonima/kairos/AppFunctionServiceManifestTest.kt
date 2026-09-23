package digital.tonima.kairos

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The system only indexes and runs KairosAppFunctions through a service that answers this action
 * and is guarded by BIND_APP_FUNCTION_SERVICE. androidx.appfunctions stopped declaring it in
 * 1.0.0-alpha10, so the app manifest has to; without it every call fails with "App function not
 * found".
 */
@RunWith(RobolectricTestRunner::class)
class AppFunctionServiceManifestTest {
    @Test
    fun `the app function service is declared for the system to bind`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = Intent(APP_FUNCTION_SERVICE_ACTION).setPackage(context.packageName)

        val service = context.packageManager.queryIntentServices(intent, 0).single().serviceInfo

        assertEquals("androidx.appfunctions.PlatformAppFunctionService", service.name)
        assertEquals("android.permission.BIND_APP_FUNCTION_SERVICE", service.permission)
        assertTrue(service.exported)
    }

    private companion object {
        const val APP_FUNCTION_SERVICE_ACTION = "android.app.appfunctions.AppFunctionService"
    }
}
