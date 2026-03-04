package digital.tonima.kairos.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.kairos.wear.ui.WearApp
import digital.tonima.kairos.wear.ui.theme.KairosTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KairosTheme {
                WearApp(permissionManager = permissionManager)
            }
        }
    }
}
