package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material3.Text
import digital.tonima.core.model.DeviceCalendar
import digital.tonima.kairos.core.R as coreR

/**
 * Cabeçalho da seção de filtro de calendários.
 * Deve ser usado como um `item {}` separado na ScalingLazyColumn.
 */
@Composable
fun CalendarFilterHeader(modifier: Modifier = Modifier) {
    SectionLabel(
        text = stringResource(coreR.string.calendar_filter_title),
        modifier = modifier,
    )
}

/**
 * Um toggle chip para um único [DeviceCalendar].
 * Deve ser usado como um `item {}` separado na ScalingLazyColumn.
 *
 * @param calendar           O calendário a exibir.
 * @param enabledCalendarIds IDs atualmente selecionados. Vazio = todos ativos.
 * @param onToggle           Chamado com (calendarId, isEnabled) ao tocar.
 */
@Composable
fun CalendarFilterChip(
    calendar: DeviceCalendar,
    enabledCalendarIds: Set<Long>,
    onToggle: (calendarId: Long, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isChecked = enabledCalendarIds.isEmpty() || enabledCalendarIds.contains(calendar.id)
    ToggleChip(
        checked = isChecked,
        onCheckedChange = { checked -> onToggle(calendar.id, checked) },
        label = {
            Text(
                text = calendar.displayName.ifBlank { calendar.accountName },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            Text(
                text = calendar.accountName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        toggleControl = { Switch(checked = isChecked) },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    )
}
