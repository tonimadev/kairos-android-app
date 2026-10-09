package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ui.theme.Dimensions
import java.time.LocalTime

/**
 * What sits above the day's events. The compact variant (phones in portrait) folds the weather,
 * the global alarms switch and the day picker into the header, so no collapsible panel is needed.
 */
@Composable
internal fun EventListHeader(
    uiState: EventScreenUiState,
    settingsUiState: SettingsUiState,
    settingsActions: SettingsActions?,
    eventActions: EventActions,
    aiActions: AiActions,
    compact: Boolean,
) {
    Column {
        val hour = remember { LocalTime.now().hour }
        val greeting =
            stringResource(
                when {
                    hour < 12 -> R.string.greeting_morning
                    hour < 18 -> R.string.greeting_afternoon
                    else -> R.string.greeting_evening
                },
            )
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimensions.PaddingTiny),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                WeatherCard(
                    weather = uiState.weather,
                    weatherError = uiState.weatherError,
                    isTemperatureInCelsius = settingsUiState.isTemperatureInCelsius,
                    onFetchWeather = eventActions.onFetchWeather,
                    compact = true,
                )
            }
        } else {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Dimensions.SpacingSmall, top = Dimensions.SpacingSmall),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Dimensions.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
        ) {
            SearchField(
                query = uiState.searchQuery,
                onQueryChange = eventActions.onSearchQueryChanged,
                modifier = Modifier.weight(1f),
            )
            if (compact && settingsActions != null) {
                CompactAlarmsToggle(
                    alarmsEnabled = settingsUiState.isGlobalAlarmEnabled,
                    onToggle = settingsActions.onToggle,
                )
            }
        }

        if (compact && settingsActions != null) {
            ControlPanel(
                settingsUiState = settingsUiState,
                settingsActions = settingsActions,
                showAlarmsToggle = false,
            )
        }

        if (!uiState.isAiUser) {
            ProUpgradeCard(onUpgradeClick = aiActions.onSubscriptionRequest)
        }

        if (compact) {
            DayStrip(
                selectedDate = uiState.selectedDate,
                currentMonth = uiState.currentMonth,
                onMonthChanged = eventActions.onMonthChanged,
                onDateSelected = eventActions.onDateSelected,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(painterResource(R.drawable.date_range), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_k_monochrome),
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        },
        singleLine = true,
    )
}
