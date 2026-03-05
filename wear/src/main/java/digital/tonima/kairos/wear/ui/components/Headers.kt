package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import digital.tonima.kairos.core.R as coreR

@Composable
fun AppHeaderTitle() {
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(coreR.string.app_name),
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
fun EventsSectionHeader() {
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(coreR.string.events_for_today),
        style = MaterialTheme.typography.labelMedium,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier,
    )
    Spacer(Modifier.height(4.dp))
}
