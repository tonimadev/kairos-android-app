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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState
import digital.tonima.core.viewmodel.uimodel.EventUiModel
import digital.tonima.kairos.BuildConfig
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.ads.components.NativeAdCard
import digital.tonima.kairos.core.ads.components.rememberNativeAds
import digital.tonima.kairos.core.ads.components.rememberRewardedAd
import digital.tonima.kairos.core.ui.theme.Dimensions
import java.time.LocalDate

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EventList(
    modifier: Modifier = Modifier,
    uiState: EventScreenUiState,
    aiUiState: AiUiState,
    settingsUiState: SettingsUiState,
    eventsByDate: ImmutableMap<Long, ImmutableList<EventUiModel>>,
    eventActions: EventActions,
    aiActions: AiActions,
    isProUser: Boolean = true,
    compactHeader: Boolean = false,
    settingsActions: SettingsActions? = null,
) {
    val pullRefreshState =
        rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = eventActions.onRefresh)
    val today = remember { LocalDate.now().toEpochDay() }

    val allEvents =
        remember(eventsByDate, uiState.selectedDate, uiState.searchQuery) {
            val base = eventsByDate[uiState.selectedDate] ?: emptyList()
            if (uiState.searchQuery.isBlank()) {
                base
            } else {
                base.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
            }
        }

    val pendingToggle = remember { mutableStateOf<Pair<EventUiModel, Boolean>?>(null) }

    val showBriefingCard = uiState.selectedDate == today && uiState.searchQuery.isBlank()
    // Free users (no AI plan) can unlock today's briefing by watching a rewarded video; Pro users
    // paid to remove ads, so they are never offered one.
    val offersRewardedBriefing = !uiState.isAiUser && !isProUser && showBriefingCard
    val rewardedBriefingAd =
        rememberRewardedAd(
            adUnitId = BuildConfig.ADMOB_REWARDED_AD_UNIT_BRIEFING,
            enabled = offersRewardedBriefing && aiUiState.dailyBriefing == null,
        )
    // Ads are hoisted out of the lazy grid: an item that loaded its own would re-request one
    // every time it scrolled back into view.
    val nativeAds =
        rememberNativeAds(
            adUnitId = BuildConfig.ADMOB_NATIVE_AD_UNIT_EVENT_LIST,
            count = MAX_NATIVE_ADS,
            enabled = !isProUser && allEvents.isNotEmpty(),
        )
    val feed = remember(allEvents, nativeAds.size) { buildEventFeed(allEvents, nativeAds.size) }
    val adLabel = stringResource(R.string.ad_label)

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = if (compactHeader) 4.dp else 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EventListHeader(
                    uiState = uiState,
                    settingsUiState = settingsUiState,
                    settingsActions = settingsActions,
                    eventActions = eventActions,
                    aiActions = aiActions,
                    compact = compactHeader,
                )
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
                            contentDescription = stringResource(R.string.no_events),
                            modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        )
                        Text(
                            text = stringResource(R.string.no_alarms_found),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                items(
                    feed,
                    key = { item ->
                        when (item) {
                            is EventFeedItem.Event -> item.event.uniqueIntentId
                            is EventFeedItem.NativeAd -> "native_ad_${item.index}"
                        }
                    },
                    span = { item ->
                        when (item) {
                            is EventFeedItem.Event -> GridItemSpan(1)
                            is EventFeedItem.NativeAd -> GridItemSpan(maxLineSpan)
                        }
                    },
                ) { item ->
                    when (item) {
                        is EventFeedItem.Event -> {
                            val event = item.event
                            EventCard(
                                event = event,
                                isGloballyEnabled = settingsUiState.isGlobalAlarmEnabled,
                                onToggle = { isEnabled ->
                                    if (event.isRecurring) {
                                        pendingToggle.value = event to isEnabled
                                    } else {
                                        eventActions.onEventToggle(event, isEnabled, false)
                                    }
                                },
                                onEventClick = { eventActions.onEventClick(event) },
                                onJoinMeeting = eventActions.onJoinMeeting,
                                onCopyMeetingUrl = eventActions.onCopyMeetingUrl,
                            )
                        }
                        is EventFeedItem.NativeAd -> NativeAdCard(nativeAds[item.index], adLabel)
                    }
                }
            }

            // The day's events come first; everything collapsible or promotional sits below them.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    if (!uiState.isAiUser && offersRewardedBriefing) {
                        RewardedBriefingCard(
                            briefing = aiUiState.dailyBriefing,
                            isGenerating = aiUiState.isGeneratingBriefing,
                            adStatus = rewardedBriefingAd.status,
                            onWatchAdClick = { rewardedBriefingAd.show(onReward = aiActions.onGenerateBriefing) },
                            modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                        )
                    } else if (uiState.isAiUser && showBriefingCard) {
                        // Always shown (not just after a briefing is generated) so the AI
                        // entry point is discoverable without depending on the bottom bar.
                        DailyBriefingCard(
                            briefing = aiUiState.dailyBriefing,
                            isGenerating = aiUiState.isGeneratingBriefing,
                            onGenerateClick = aiActions.onGenerateBriefing,
                            onInteractClick = aiActions.onOpenChat,
                            modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
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
