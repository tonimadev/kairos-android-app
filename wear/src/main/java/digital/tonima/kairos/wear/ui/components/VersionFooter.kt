package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun VersionFooter() {
    Spacer(Modifier.height(12.dp))
    val context = LocalContext.current
    val versionName =
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    Text(
        text = "v$versionName",
        style = MaterialTheme.typography.labelSmall,
    )
    Spacer(Modifier.height(8.dp))
}
