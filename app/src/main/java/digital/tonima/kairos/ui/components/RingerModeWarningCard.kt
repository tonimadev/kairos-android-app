package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.kairos.R.drawable.vibration
import digital.tonima.kairos.R.drawable.volume_off
import digital.tonima.kairos.core.R.string.ringer_mode_silent_warning
import digital.tonima.kairos.core.R.string.ringer_mode_vibrate_warning
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun RingerModeWarningCard(ringerMode: AudioWarningState) {
    val warningText =
        when (ringerMode) {
            AudioWarningState.VIBRATE -> stringResource(ringer_mode_vibrate_warning)
            AudioWarningState.SILENT, AudioWarningState.ALARM_MUTED -> stringResource(ringer_mode_silent_warning)
            AudioWarningState.NORMAL -> ""
        }
    val icon =
        when (ringerMode) {
            AudioWarningState.VIBRATE -> vibration
            AudioWarningState.SILENT, AudioWarningState.ALARM_MUTED -> volume_off
            AudioWarningState.NORMAL -> null
        }

    if (warningText.isNotEmpty() && icon != null) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.PaddingSmall),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.ElevationSmall),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Row(
                modifier = Modifier.padding(Dimensions.PaddingNormal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = warningText,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.width(Dimensions.PaddingNormal))
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
