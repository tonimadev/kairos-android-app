package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import digital.tonima.core.model.Event
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions
import java.time.LocalDate

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EventList(
    modifier: Modifier = Modifier,
    uiState: EventScreenUiState,
    eventsByDate: Map<LocalDate, List<Event>>,
    onRefresh: () -> Unit,
    onEventToggle: (event: Event, isEnabled: Boolean, disableAllOccurrences: Boolean) -> Unit,
    onEventVibrateToggle: (event: Event, vibrateOnly: Boolean) -> Unit,
    onEventClick: (Event) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    headerContent: (@Composable () -> Unit)? = null,
) {
    val pullRefreshState = rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = onRefresh)
    val eventsInDay =
        remember(uiState.selectedDate, eventsByDate, uiState.searchQuery) {
            val allInDay = eventsByDate[uiState.selectedDate] ?: emptyList()
            if (uiState.searchQuery.isBlank()) {
                allInDay
            } else {
                allInDay.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
            }
        }

    val pendingToggle = remember { mutableStateOf<Pair<Event, Boolean>?>(null) }

    Column(modifier = modifier) {
        Box(Modifier.weight(1f).pullRefresh(pullRefreshState)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
            ) {
                headerContent?.let {
                    item {
                        it()
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = Dimensions.PaddingSmall),
                        placeholder = { Text(stringResource(R.string.search)) },
                        leadingIcon = { Icon(painterResource(R.drawable.date_range), contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        painterResource(digital.tonima.kairos.core.R.drawable.ic_k_monochrome),
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        singleLine = true,
                    )
                }

                if (eventsInDay.isEmpty() && !uiState.isRefreshing) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillParentMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(R.string.no_events_found_for_this_day)) }
                    }
                } else {
                    items(eventsInDay, key = { it.uniqueIntentId }) { event ->
                        EventCard(
                            event = event,
                            isGloballyEnabled = uiState.isGlobalAlarmEnabled,
                            onToggle = { isEnabled ->
                                if (event.isRecurring) {
                                    pendingToggle.value = event to isEnabled
                                } else {
                                    onEventToggle(event, isEnabled, false)
                                }
                            },
                            onVibrateToggle = { onEventVibrateToggle(event, it) },
                            onEventClick = { onEventClick(event) },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimensions.ListBottomSpacer))
                }
            }
            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            pendingToggle.value?.let { (pendingEvent, pendingEnabled) ->
                AlertDialog(
                    onDismissRequest = { pendingToggle.value = null },
                    title = { Text(stringResource(R.string.update_alarm_title)) },
                    text = { Text(stringResource(R.string.update_alarm_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            onEventToggle(pendingEvent, pendingEnabled, true)
                            pendingToggle.value = null
                        }) { Text(stringResource(R.string.recurring_option)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            onEventToggle(pendingEvent, pendingEnabled, false)
                            pendingToggle.value = null
                        }) { Text(stringResource(R.string.only_this_option)) }
                    },
                )
            }
        }
    }
}
