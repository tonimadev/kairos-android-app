package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import digital.tonima.kairos.R
import digital.tonima.kairos.core.R as coreR

@Composable
fun OpenOnPhoneChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        label = { Text(stringResource(coreR.string.open_on_phone)) },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.smartphone),
                contentDescription = null,
            )
        },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    )
}
