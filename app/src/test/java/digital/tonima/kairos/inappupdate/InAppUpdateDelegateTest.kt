package digital.tonima.kairos.inappupdate

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import digital.tonima.core.inappupdate.InAppUpdateManager
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class InAppUpdateDelegateTest {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var fakeManager: FakeInAppUpdateManager
    private lateinit var launcher: DummyLauncher
    private lateinit var snackbarHostState: androidx.compose.material3.SnackbarHostState
    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        fakeManager = FakeInAppUpdateManager()
        launcher = DummyLauncher()
        snackbarHostState = androidx.compose.material3.SnackbarHostState()
        scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
    }

    @Test
    fun onCreate_callsCheckForUpdate_withFlexible_and_invokesCallback() {
        var callbackInvoked = false
        fakeManager.onCheckForUpdateCallback = { callbackInvoked = true }

        val delegate =
            InAppUpdateDelegate(
                activity = activity,
                inAppUpdateManager = fakeManager,
                snackbarHostState = snackbarHostState,
                coroutineScope = scope,
                updateLauncher = launcher,
            )

        delegate.onCreate()

        assertTrue("checkForUpdate should have been called", fakeManager.checkForUpdateCalled)
        assertEquals(InstallStatus.UNKNOWN, 0)
        assertSame("Launcher passed should be the same instance", launcher, fakeManager.lastLauncher)
        assertEquals(
            "Update type should be FLEXIBLE (0)",
            com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE,
            fakeManager.lastUpdateType,
        )
        assertTrue("onUpdateAvailable callback should be invoked", callbackInvoked)
    }

    @Test
    fun onResume_delegatesToHandleImmediateUpdateOnResume() {
        val delegate =
            InAppUpdateDelegate(
                activity = activity,
                inAppUpdateManager = fakeManager,
                snackbarHostState = snackbarHostState,
                coroutineScope = scope,
                updateLauncher = launcher,
            )

        delegate.onResume()

        assertTrue("handleImmediateUpdateOnResume should be called", fakeManager.handleImmediateCalled)
        assertSame(launcher, fakeManager.lastLauncher)
    }

    @Test
    fun whenDownloaded_showsSnackbar_andActionCallsCompleteFlexibleUpdate() {
        val delegate =
            InAppUpdateDelegate(
                activity = activity,
                inAppUpdateManager = fakeManager,
                snackbarHostState = snackbarHostState,
                coroutineScope = scope,
                updateLauncher = launcher,
            )

        val field = InAppUpdateDelegate::class.java.getDeclaredField("installStateUpdatedListener")
        field.isAccessible = true
        val listener = field.get(delegate) as com.google.android.play.core.install.InstallStateUpdatedListener

        listener.onStateUpdate(FakeInstallState(InstallStatus.DOWNLOADED))

        val current = snackbarHostState.currentSnackbarData
        current?.performAction()

        assertTrue("completeFlexibleUpdate should be called after action", fakeManager.completeFlexibleUpdateCalled)
    }

    @Test
    fun theListenerLivesFromOnCreateUntilOnDestroy() {
        val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val registered = slot<InstallStateUpdatedListener>()
        val delegate = delegateWith(appUpdateManager)

        delegate.onCreate()
        verify { appUpdateManager.registerListener(capture(registered)) }

        delegate.onDestroy()
        verify { appUpdateManager.unregisterListener(registered.captured) }
    }

    @Test
    fun anInstalledUpdateStopsListening() {
        val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val registered = slot<InstallStateUpdatedListener>()
        delegateWith(appUpdateManager).onCreate()
        verify { appUpdateManager.registerListener(capture(registered)) }

        listOf(InstallStatus.DOWNLOADING, InstallStatus.INSTALLING, InstallStatus.PENDING).forEach {
            registered.captured.onStateUpdate(FakeInstallState(it))
        }
        verify(exactly = 0) { appUpdateManager.unregisterListener(any()) }

        registered.captured.onStateUpdate(FakeInstallState(InstallStatus.INSTALLED))
        verify { appUpdateManager.unregisterListener(registered.captured) }
    }

    @Test
    fun dismissingTheDownloadedSnackbarDoesNotRestart() {
        val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val registered = slot<InstallStateUpdatedListener>()
        delegateWith(appUpdateManager).onCreate()
        verify { appUpdateManager.registerListener(capture(registered)) }

        registered.captured.onStateUpdate(FakeInstallState(InstallStatus.DOWNLOADED))
        snackbarHostState.currentSnackbarData?.dismiss()

        assertFalse(fakeManager.completeFlexibleUpdateCalled)
    }

    private fun delegateWith(appUpdateManager: AppUpdateManager) =
        InAppUpdateDelegate(
            activity = activity,
            inAppUpdateManager = fakeManager,
            snackbarHostState = snackbarHostState,
            coroutineScope = scope,
            updateLauncher = launcher,
            appUpdateManager = appUpdateManager,
        )

    private class DummyLauncher : ActivityResultLauncher<IntentSenderRequest>() {
        override val contract: ActivityResultContract<IntentSenderRequest, Any?> =
            object : ActivityResultContract<IntentSenderRequest, Any?>() {
                override fun createIntent(
                    context: Context,
                    input: IntentSenderRequest,
                ): Intent = Intent("dummy-action")

                override fun parseResult(
                    resultCode: Int,
                    intent: Intent?,
                ): Any? = null
            }

        override fun launch(
            input: IntentSenderRequest,
            options: ActivityOptionsCompat?,
        ) { /* no-op */ }

        override fun unregister() { /* no-op */ }
    }

    private class FakeInstallState(
        private val status: Int,
    ) : InstallState() {
        override fun bytesDownloaded(): Long = 0L

        override fun totalBytesToDownload(): Long = 0L

        override fun installErrorCode(): Int = 0

        override fun installStatus(): Int = status

        override fun packageName(): String = RuntimeEnvironment.getApplication().packageName
    }

    private class FakeInAppUpdateManager : InAppUpdateManager {
        var checkForUpdateCalled = false
        var handleImmediateCalled = false
        var completeFlexibleUpdateCalled = false
        var lastLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
        var lastUpdateType: Int? = null
        var onCheckForUpdateCallback: (() -> Unit)? = null

        override fun checkForUpdate(
            updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
            updateType: Int,
            onUpdateAvailable: (() -> Unit)?,
            onNoUpdateAvailable: (() -> Unit)?,
            onUpdateInProgress: (() -> Unit)?,
            onError: ((Exception) -> Unit)?,
        ) {
            checkForUpdateCalled = true
            lastLauncher = updateLauncher
            lastUpdateType = updateType
            onCheckForUpdateCallback?.invoke()
            onUpdateAvailable?.invoke()
        }

        override fun handleImmediateUpdateOnResume(updateLauncher: ActivityResultLauncher<IntentSenderRequest>) {
            handleImmediateCalled = true
            lastLauncher = updateLauncher
        }

        override fun completeFlexibleUpdate() {
            completeFlexibleUpdateCalled = true
        }
    }
}
