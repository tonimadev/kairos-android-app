package digital.tonima.kairos.ui.view

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ads.consent.AdsConsent
import digital.tonima.kairos.core.ui.theme.Dimensions

/**
 * Lets the user reopen the ads consent form. Google's UMP requires this entry whenever it reports
 * that privacy options are required (e.g. EEA/UK users); elsewhere nothing is shown.
 */
@Composable
internal fun AdPrivacyOptionsSection() {
    val isRequired by AdsConsent.isPrivacyOptionsRequired.collectAsState()
    if (!isRequired) return

    val activity = LocalActivity.current
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingDefault)) {
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.PaddingTiny))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = activity != null) { activity?.let(AdsConsent::showPrivacyOptions) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
        ) {
            Icon(
                imageVector = Icons.Rounded.AdsClick,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.IconSizeSmall),
            )
            Text(
                text = stringResource(R.string.ad_privacy_options),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
