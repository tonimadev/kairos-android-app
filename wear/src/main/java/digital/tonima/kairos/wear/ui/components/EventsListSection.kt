package digital.tonima.kairos.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R as coreR

@Composable
fun EventsListSection(
    events: List<Event>,
    isRefreshing: Boolean,
    isGlobalAlarmEnabled: Boolean,
    onEventToggle: (event: Event, isEnabled: Boolean, applyToSeries: Boolean) -> Unit,
) {
    if (events.isEmpty() && !isRefreshing) {
        Text(text = stringResource(coreR.string.no_events_found_for_this_day))
        return
    }

    val sorted = events.sortedBy { it.startTime }

    for (event in sorted) {
        val pendingToggle = remember(event.id, event.startTime) { mutableStateOf<Pair<Event, Boolean>?>(null) }

        EventCard(
            event = event,
            isGloballyEnabled = isGlobalAlarmEnabled,
            onToggle = { isEnabled ->
                if (event.isRecurring) {
                    pendingToggle.value = event to isEnabled
                } else {
                    onEventToggle(event, isEnabled, false)
                }
            },
        )

        if (pendingToggle.value != null) {
            pendingToggle.value?.let { (pendingEvent, pendingEnabled) ->
                if (pendingEvent.id == event.id && pendingEvent.startTime == event.startTime) {
                    Dialog(
                        visible = true,
                        onDismissRequest = { pendingToggle.value = null },
                    ) {
                        val scrollState = rememberScrollState()

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(coreR.string.update_alarm_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(coreR.string.update_alarm_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        onEventToggle(pendingEvent, pendingEnabled, true)
                                        pendingToggle.value = null
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(coreR.string.recurring_option),
                                        textAlign = TextAlign.Center,
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        onEventToggle(pendingEvent, pendingEnabled, false)
                                        pendingToggle.value = null
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(coreR.string.only_this_option),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
