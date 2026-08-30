package digital.tonima.kairos.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.common.collect.ImmutableList // Adicionado para manter a imutabilidade
import com.google.common.collect.ImmutableMap
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.kairos.core.R.string.hide_dashboard
import digital.tonima.kairos.core.R.string.show_dashboard
import digital.tonima.kairos.ui.theme.Dimensions
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

    var showDashboard by remember { mutableStateOf(false) }

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
            headerContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showDashboard = !showDashboard },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB0B0C0)),
                    ) {
                        Text(stringResource(if (showDashboard) hide_dashboard else show_dashboard))
                        Icon(
                            imageVector =
                                if (showDashboard) {
                                    Icons.Rounded.KeyboardArrowUp
                                } else {
                                    Icons.Rounded.KeyboardArrowDown
                                },
                            contentDescription =
                                stringResource(
                                    if (showDashboard) hide_dashboard else show_dashboard,
                                ),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = showDashboard,
                        enter = expandVertically(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(300)),
                    ) {
                        Column {
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
                            Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
                        }
                    }
                }
            },
        )
    }
}
