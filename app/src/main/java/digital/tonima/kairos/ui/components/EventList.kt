package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TipsAndUpdates
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
import androidx.compose.ui.unit.dp
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
    eventActions: EventActions,
    aiActions: AiActions,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val pullRefreshState =
        rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = eventActions.onRefresh)
    val today = remember { LocalDate.now() }
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
                    val hour = remember { java.time.LocalTime.now().hour }
                    val greeting =
                        when {
                            hour < 12 -> "Good Morning! ☀️"
                            hour < 18 -> "Good Afternoon! ☕"
                            else -> "Good Evening! 🌙"
                        }
                    Text(
                        text = greeting,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Dimensions.SpacingSmall, top = Dimensions.SpacingSmall),
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = eventActions.onSearchQueryChanged,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = Dimensions.PaddingSmall),
                        placeholder = { Text(stringResource(R.string.search)) },
                        leadingIcon = { Icon(painterResource(R.drawable.date_range), contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { eventActions.onSearchQueryChanged("") }) {
                                    Icon(
                                        painterResource(R.drawable.ic_k_monochrome),
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        singleLine = true,
                    )
                }

                val showBriefingCard = uiState.selectedDate == today && uiState.searchQuery.isBlank()
                if (!uiState.isAiUser) {
                    item {
                        ProUpgradeCard(
                            onUpgradeClick = aiActions.onSubscriptionRequest,
                        )
                    }
                }
                if (showBriefingCard) {
                    item {
                        DailyBriefingCard(
                            briefing = uiState.dailyBriefing,
                            isGenerating = uiState.isGeneratingBriefing,
                            isAiUser = uiState.isAiUser,
                            onGenerateClick = aiActions.onGenerateBriefing,
                            onInteractClick = aiActions.onReply,
                            onUpgradeClick = aiActions.onSubscriptionRequest,
                            modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                        )
                    }
                }

                if (eventsInDay.isEmpty() && !uiState.isRefreshing) {
                    item {
                        Column(
                            modifier =
                                Modifier
                                    .then(
                                        if (showBriefingCard) {
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = Dimensions.PaddingLarge)
                                        } else {
                                            Modifier.fillParentMaxSize()
                                        },
                                    ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TipsAndUpdates,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            )
                            Text(
                                text = "Your day is clear! 🌟",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.no_events_found_for_this_day),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
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
                                    eventActions.onEventToggle(event, isEnabled, false)
                                }
                            },
                            onVibrateToggle = { eventActions.onEventVibrateToggle(event, it) },
                            onEventClick = { eventActions.onEventClick(event) },
                            onJoinMeeting = eventActions.onJoinMeeting,
                            onCopyMeetingUrl = eventActions.onCopyMeetingUrl,
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Dimensions.ListBottomSpacer))
                }
            }

            AiVoiceInteractionCard(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Dimensions.PaddingNormal)
                        .padding(bottom = Dimensions.ListBottomSpacer),
                question = uiState.lastAiQuestion,
                response = uiState.aiResponse,
                isAsking = uiState.isAskingAi,
                isSpeaking = uiState.isSpeaking,
                onSpeak = aiActions.onSpeakAiResponse,
                onStopSpeaking = aiActions.onStopSpeaking,
                onDismiss = aiActions.onClearAiResponse,
                onReply = aiActions.onReply,
            )

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
                            eventActions.onEventToggle(pendingEvent, pendingEnabled, true)
                            pendingToggle.value = null
                        }) { Text(stringResource(R.string.recurring_option)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            eventActions.onEventToggle(pendingEvent, pendingEnabled, false)
                            pendingToggle.value = null
                        }) { Text(stringResource(R.string.only_this_option)) }
                    },
                )
            }
        }
    }
}
