package digital.tonima.kairos.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.google.common.collect.ImmutableList // Adicionado para manter a imutabilidade
import com.google.common.collect.ImmutableMap
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.kairos.core.ui.theme.Dimensions
import java.time.Instant
import java.time.ZoneId

@Composable
fun MainContent(
    uiState: EventScreenUiState,
    settingsUiState: SettingsUiState,
    aiUiState: AiUiState,
    eventActions: EventActions,
    settingsActions: SettingsActions,
    aiActions: AiActions,
    windowSizeClass: WindowSizeClass? = null,
    isProUser: Boolean = true,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium

    val showSideBySide = isLandscape || isExpanded || isMedium

    val eventsByDate =
        remember(uiState.events) {
            val groupedMap =
                uiState.events.groupBy { event ->
                    Instant.ofEpochMilli(event.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .toEpochDay()
                }.mapValues { (_, eventsList) ->
                    ImmutableList.copyOf(eventsList)
                }

            ImmutableMap.copyOf(groupedMap)
        }

    if (showSideBySide) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.PaddingNormal),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(end = Dimensions.PaddingSmall),
            ) {
                WeatherCard(
                    weather = uiState.weather,
                    weatherError = uiState.weatherError,
                    isTemperatureInCelsius = settingsUiState.isTemperatureInCelsius,
                    onFetchWeather = eventActions.onFetchWeather,
                    modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                )
                ControlPanel(
                    settingsUiState = settingsUiState,
                    settingsActions = settingsActions,
                )
                CalendarView(
                    modifier = Modifier.padding(top = Dimensions.PaddingSmall),
                    currentMonth = uiState.currentMonth,
                    selectedDate = uiState.selectedDate,
                    eventsByDate = eventsByDate,
                    onMonthChanged = eventActions.onMonthChanged,
                    onDateSelected = eventActions.onDateSelected,
                    onReturnToToday = eventActions.onReturnToToday,
                )
            }
            EventList(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = Dimensions.PaddingSmall, top = Dimensions.PaddingNormal),
                uiState = uiState,
                settingsUiState = settingsUiState,
                eventsByDate = eventsByDate,
                eventActions = eventActions,
                aiActions = aiActions,
                aiUiState = aiUiState,
                isProUser = isProUser,
            )
        }
    } else {
        EventList(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimensions.PaddingNormal),
            uiState = uiState,
            settingsUiState = settingsUiState,
            eventsByDate = eventsByDate,
            eventActions = eventActions,
            aiActions = aiActions,
            aiUiState = aiUiState,
            isProUser = isProUser,
            compactHeader = true,
            settingsActions = settingsActions,
        )
    }
}
