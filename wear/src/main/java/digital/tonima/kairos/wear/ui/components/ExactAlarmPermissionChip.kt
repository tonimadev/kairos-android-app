package digital.tonima.kairos.wear.ui.components

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material3.Text
import digital.tonima.kairos.core.R as coreR

@Composable
fun ExactAlarmPermissionChip(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Chip(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val i =
                    android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                context.startActivity(i)
            }
        },
        label = {
            Text(
                text =
                    androidx.compose.ui.res
                        .stringResource(coreR.string.allow_exact_alarms),
            )
        },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    )
}
