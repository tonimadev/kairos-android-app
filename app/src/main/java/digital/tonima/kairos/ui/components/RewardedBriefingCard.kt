package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ads.components.RewardedAdStatus
import digital.tonima.kairos.core.ui.theme.Dimensions

/**
 * Free users' way to the daily briefing: watching a rewarded video generates today's briefing.
 * Once it exists (the briefing is generated at most once a day) the text is shown instead.
 */
@Composable
fun RewardedBriefingCard(
    briefing: String?,
    isGenerating: Boolean,
    adStatus: RewardedAdStatus,
    onWatchAdClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(Dimensions.PaddingNormal)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_k_monochrome),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier.size(Dimensions.IconSizeSmall),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                Text(
                    text = stringResource(R.string.daily_briefing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
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
            } else if (!isGenerating) {
                Text(
                    text = stringResource(R.string.briefing_rewarded_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                Button(
                    onClick = onWatchAdClick,
                    enabled = adStatus == RewardedAdStatus.Ready,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        stringResource(
                            when (adStatus) {
                                RewardedAdStatus.Ready -> R.string.briefing_rewarded_watch
                                RewardedAdStatus.Unavailable -> R.string.briefing_rewarded_unavailable
                                RewardedAdStatus.Loading,
                                RewardedAdStatus.Disabled,
                                -> R.string.briefing_rewarded_loading
                            },
                        ),
                    )
                }
            }
        }
    }
}
