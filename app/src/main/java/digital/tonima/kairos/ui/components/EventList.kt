package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
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

    // Instead of filtering by day, let's just show all events as "alarms" for this mockup.
    // The design shows multiple alarms.
    val allEvents =
        remember(uiState.events, uiState.searchQuery) {
            val base = uiState.events
            if (uiState.searchQuery.isBlank()) {
                base
            } else {
                base.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
            }
        }

    val pendingToggle = remember { mutableStateOf<Pair<Event, Boolean>?>(null) }

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    headerContent?.invoke()

                    val hour = remember { java.time.LocalTime.now().hour }
                    val greeting =
                        when {
                            hour < 12 -> "Good Morning! ☀️"
                            hour < 18 -> "Good Afternoon! ☕"
                            else -> "Good Evening! 🌙"
                        }
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Dimensions.SpacingSmall, top = Dimensions.SpacingSmall),
                    )

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

                    val showBriefingCard = uiState.selectedDate == today && uiState.searchQuery.isBlank()
                    if (!uiState.isAiUser) {
                        ProUpgradeCard(
                            onUpgradeClick = aiActions.onSubscriptionRequest,
                        )
                    } else if (showBriefingCard && (uiState.isGeneratingBriefing || uiState.dailyBriefing != null)) {
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
            }

            if (allEvents.isEmpty() && !uiState.isRefreshing) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.PaddingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TipsAndUpdates,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "No Alarms Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                items(allEvents, key = { it.uniqueIntentId }) { event ->
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

            item(span = { GridItemSpan(maxLineSpan) }) {
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
