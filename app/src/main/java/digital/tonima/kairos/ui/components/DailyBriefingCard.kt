package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun DailyBriefingCard(
    briefing: String?,
    isGenerating: Boolean,
    isAiUser: Boolean,
    onGenerateClick: () -> Unit,
    onInteractClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.PaddingNormal),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_k_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.IconSizeSmall),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                Text(
                    text = stringResource(R.string.daily_briefing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            if (briefing != null) {
                Text(
                    text = briefing,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                TextButton(
                    onClick = onInteractClick,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.ask_ai_label))
                }
            } else if (isAiUser) {
                if (!isGenerating) {
                    Text(
                        text = stringResource(R.string.daily_briefing_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    TextButton(
                        onClick = onGenerateClick,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.generate_briefing))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.daily_briefing_trial_invite),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                TextButton(
                    onClick = onUpgradeClick,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.subscribe_now))
                }
            }
        }
    }
}
