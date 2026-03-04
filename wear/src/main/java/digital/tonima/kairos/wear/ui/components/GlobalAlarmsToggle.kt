package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material3.Text
import digital.tonima.kairos.core.R as coreR

@Composable
fun GlobalAlarmsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleChip(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(stringResource(coreR.string.activate_event_alarms)) },
        toggleControl = { Switch(checked = checked) },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    )
}
