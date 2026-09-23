package digital.tonima.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import android.widget.Toast
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild

/** Runs on Robolectric so Intent and ComponentName are real and the OEM lookup can be checked. */
@RunWith(RobolectricTestRunner::class)
class AutoStartHelperOemTest {
    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk()
    private val started = mutableListOf<Intent>()

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "digital.tonima.kairos"
        every { context.getString(any()) } returns "Abra as configurações"
        every { context.startActivity(any()) } answers { started += firstArg<Intent>() }
        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<String>(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Toast::class)
    }

    @Test
    fun `the first autostart screen the phone has is opened`() {
        givenResolvable("com.huawei.systemmanager")

        openAutostartSettings(context)

        assertEquals("com.huawei.systemmanager", started.single().component?.packageName)
        verify(exactly = 0) { Toast.makeText(any(), any<String>(), any()) }
    }

    @Test
    fun `a screen that fails to open is skipped for the next one`() {
        givenResolvable("com.miui.securitycenter", "com.samsung.android.lool")
        every { context.startActivity(match { it.component?.packageName == "com.miui.securitycenter" }) } throws
            ActivityNotFoundException()

        openAutostartSettings(context)

        assertEquals("com.samsung.android.lool", started.single().component?.packageName)
    }

    @Test
    fun `without any OEM screen the app details are opened with a hint`() {
        every { packageManager.resolveActivity(any<Intent>(), any<Int>()) } returns null

        openAutostartSettings(context)

        val intent = started.single()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:digital.tonima.kairos", intent.data.toString())
        verify { Toast.makeText(context, "Abra as configurações", Toast.LENGTH_LONG) }
    }

    @Test
    fun `a broken package manager still falls back to the app details`() {
        every { packageManager.resolveActivity(any<Intent>(), any<Int>()) } throws SecurityException()

        openAutostartSettings(context)

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.single().action)
    }

    @Test
    fun `only manufacturers known to kill background apps need autostart`() {
        ShadowBuild.setManufacturer("Xiaomi")
        assertTrue(needsAutostartPermission())

        ShadowBuild.setManufacturer("Google")
        assertFalse(needsAutostartPermission())
    }

    private fun givenResolvable(vararg packages: String) {
        every { packageManager.resolveActivity(any<Intent>(), any<Int>()) } answers {
            if (firstArg<Intent>().component?.packageName in packages) ResolveInfo() else null
        }
    }
}
