package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun AutostartSuggestionCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PaddingSmall),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.ElevationSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(Dimensions.PaddingNormal)) {
            Text(
                text = stringResource(R.string.autostart_suggestion_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
            Text(
                text = stringResource(R.string.autostart_suggestion_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dismiss))
                }
                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
    }
}
