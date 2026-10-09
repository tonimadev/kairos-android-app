package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ui.theme.Dimensions

/** Compact banner-style entry point to the AI plan; meant to sit above the day's events. */
@Composable
fun ProUpgradeCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PaddingSmall),
        shape = RoundedCornerShape(Dimensions.RadiusMedium),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = Dimensions.PaddingMedium, vertical = Dimensions.PaddingSmall)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.pro_label),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(Dimensions.PaddingSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pro_ia_upgrade_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.pro_ia_upgrade_desc),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(Dimensions.PaddingSmall))
            Button(
                onClick = onUpgradeClick,
                shape = RoundedCornerShape(Dimensions.RadiusSmall),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text(text = stringResource(R.string.try_pro_plan), maxLines = 1)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProUpgradeCardPreview() {
    ProUpgradeCard(
        onUpgradeClick = {},
    )
}
