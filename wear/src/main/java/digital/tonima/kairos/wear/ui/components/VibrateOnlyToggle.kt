package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import digital.tonima.kairos.core.R as coreR

@Composable
fun VibrateOnlyToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(stringResource(coreR.string.vibrate_only)) },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    )
}
