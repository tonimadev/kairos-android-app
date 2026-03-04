package digital.tonima.kairos.wear.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.wearable.Wearable.getMessageClient
import com.google.android.gms.wearable.Wearable.getNodeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.model.Event
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.sync.WearSyncSchema.PATH_REQUEST_SYNC
import digital.tonima.kairos.wear.WorkNames
import digital.tonima.kairos.wear.sync.CachedEventSchedulingWorker
import digital.tonima.kairos.wear.sync.SyncActions
import digital.tonima.kairos.wear.sync.WearEventCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

@HiltViewModel
class WearCalendarViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val appPreferencesRepository: AppPreferencesRepository,
    ) : ViewModel() {
        private val _next24hEvents = MutableStateFlow<List<Event>>(emptyList())
        val next24hEvents: StateFlow<List<Event>> = _next24hEvents.asStateFlow()

        private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
        val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

        private val eventsUpdatedReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    if (intent?.action == SyncActions.ACTION_EVENTS_UPDATED) {
                        reloadFromCache()
                    }
                }
            }

        init {
            reloadFromCache()
            if (Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(
                    eventsUpdatedReceiver,
                    IntentFilter(SyncActions.ACTION_EVENTS_UPDATED),
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(
                    eventsUpdatedReceiver,
                    IntentFilter(SyncActions.ACTION_EVENTS_UPDATED),
                )
            }

            viewModelScope.launch {
                combine(
                    appPreferencesRepository.isGlobalAlarmEnabled(),
                    appPreferencesRepository.getDisabledEventIds(),
                    appPreferencesRepository.getDisabledSeriesIds(),
                ) { _, _, _ -> }
                    .collect {
                        reloadFromCache()
                    }
            }
        }

        fun requestRescan() {
            viewModelScope.launch {
                try {
                    val nodeClient = getNodeClient(appContext)
                    nodeClient.connectedNodes
                        .addOnSuccessListener { nodes ->
                            val msgClient = getMessageClient(appContext)
                            for (node in nodes) {
                                msgClient
                                    .sendMessage(
                                        node.id,
                                        PATH_REQUEST_SYNC,
                                        ByteArray(0),
                                    ).addOnSuccessListener {
                                        logcat {
                                            "WearCalendarViewModel: Requested sync fro" +
                                                "m phone (node=${node.displayName})."
                                        }
                                    }.addOnFailureListener { t ->
                                        logcat(
                                            LogPriority.ERROR,
                                        ) { "WearCalendarViewModel: Failed to request sync: ${t.localizedMessage}" }
                                    }
                            }
                        }.addOnFailureListener { t ->
                            logcat(
                                LogPriority.ERROR,
                            ) { "WearCalendarViewModel: Failed to obtain connected nodes: ${t.localizedMessage}" }
                        }
                } catch (t: Throwable) {
                    logcat(
                        LogPriority.ERROR,
                    ) { "WearCalendarViewModel: Exception while requesting sync: ${t.localizedMessage}" }
                }
            }

            reloadFromCache()
            try {
                WorkManager
                    .getInstance(appContext)
                    .enqueueUniqueWork(
                        WorkNames.UNIQUE_SCHEDULE_NOW,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<CachedEventSchedulingWorker>().build(),
                    )
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR) {
                    "WearCalendarViewModel: Failed to enqueue CachedEventSchedulingWorker: ${e.localizedMessage}"
                }
            }
        }

        private fun reloadFromCache() {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val events =
                    WearEventCache
                        .load(appContext)
                        .filter { it.startTime >= now }
                        .sortedBy { it.startTime }
                val isGlobal = appPreferencesRepository.isGlobalAlarmEnabled().firstOrNull() ?: true
                val disabledInstanceIds = appPreferencesRepository.getDisabledEventIds().firstOrNull() ?: emptySet()
                val disabledSeriesIds = appPreferencesRepository.getDisabledSeriesIds().firstOrNull() ?: emptySet()

                val mapped =
                    events.map { e ->
                        val instanceDisabled = disabledInstanceIds.contains(e.uniqueIntentId.toString())
                        val seriesDisabled = disabledSeriesIds.contains(e.id.toString())
                        e.copy(isAlarmEnabled = isGlobal && !(instanceDisabled || seriesDisabled))
                    }
                _next24hEvents.value = mapped
                _lastUpdated.value = System.currentTimeMillis()
            }
        }

        override fun onCleared() {
            super.onCleared()
            try {
                appContext.unregisterReceiver(eventsUpdatedReceiver)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) {
                    "WearCalendarViewModel: Receiver not registered: ${e.localizedMessage}"
                }
            }
        }
    }
