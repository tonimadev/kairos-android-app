package digital.tonima.kairos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.billing.BillingManager
import digital.tonima.core.billing.SubscriptionManager
import digital.tonima.core.inappupdate.InAppUpdateManager
import digital.tonima.kairos.inappupdate.InAppUpdateDelegate
import digital.tonima.kairos.ui.theme.KairosTheme
import digital.tonima.kairos.ui.view.EventScreen
import logcat.logcat
import javax.inject.Inject
import digital.tonima.kairos.core.R as CoreR

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager
    private lateinit var inAppUpdateDelegate: InAppUpdateDelegate

    private val updateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                logcat {
                    "App update flow failed or was canceled. Result code: ${result.resultCode}"
                }
                Toast.makeText(this, CoreR.string.update_fail_or_canceled, Toast.LENGTH_LONG).show()
            } else {
                logcat {
                    "App update flow completed successfully."
                }
            }
        }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        billingManager.connect()
        subscriptionManager.connect()
        setContent {
            KairosTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val scope = rememberCoroutineScope()
                    val rememberSnackbarHostState = remember { SnackbarHostState() }

                    inAppUpdateDelegate =
                        remember(rememberSnackbarHostState, scope, inAppUpdateManager, this) {
                            InAppUpdateDelegate(
                                activity = this,
                                inAppUpdateManager = inAppUpdateManager,
                                snackbarHostState = rememberSnackbarHostState,
                                coroutineScope = scope,
                                updateLauncher = updateLauncher,
                            )
                        }

                    LaunchedEffect(inAppUpdateDelegate) {
                        inAppUpdateDelegate.onCreate()
                    }

                    EventScreen(
                        snackbarHostState = rememberSnackbarHostState,
                        onPurchaseRequest = { billingManager.launchPurchaseFlow(this) },
                        onSubscriptionRequest = { subscriptionManager.launchSubscriptionFlow(this) },
                        windowSizeClass = windowSizeClass,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::inAppUpdateDelegate.isInitialized) {
            inAppUpdateDelegate.onResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inAppUpdateDelegate.isInitialized) {
            inAppUpdateDelegate.onDestroy()
        }
    }
}
