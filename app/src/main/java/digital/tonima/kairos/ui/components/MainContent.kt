package digital.tonima.kairos.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.ui.theme.Dimensions
import java.time.Instant
import java.time.ZoneId

@Composable
fun MainContent(
    uiState: EventScreenUiState,
    eventActions: EventActions,
    settingsActions: SettingsActions,
    aiActions: AiActions,
    windowSizeClass: WindowSizeClass? = null,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium

    val showSideBySide = isLandscape || isExpanded || isMedium

    val eventsByDate =
        remember(uiState.events) {
            uiState.events.groupBy {
                Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
            }
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
                    isTemperatureInCelsius = uiState.isTemperatureInCelsius,
                    onFetchWeather = eventActions.onFetchWeather,
                    modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                )
                ControlPanel(
                    uiState = uiState,
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
                eventsByDate = eventsByDate,
                eventActions = eventActions,
                aiActions = aiActions,
            )
        }
    } else {
        EventList(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimensions.PaddingNormal),
            uiState = uiState,
            eventsByDate = eventsByDate,
            eventActions = eventActions,
            aiActions = aiActions,
            headerContent = {
                Column {
                    WeatherCard(
                        weather = uiState.weather,
                        isTemperatureInCelsius = uiState.isTemperatureInCelsius,
                        onFetchWeather = eventActions.onFetchWeather,
                        modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                    )
                    ControlPanel(
                        uiState = uiState,
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
                    Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
                }
            },
        )
    }
}
