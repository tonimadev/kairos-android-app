package digital.tonima.kairos.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.analytics.CrashReporter
import digital.tonima.core.analytics.coroutineExceptionHandler
import digital.tonima.core.analytics.runOrReport
import digital.tonima.core.data.usecases.ObserveAppPreferencesUseCase
import digital.tonima.core.data.usecases.ToggleGlobalAlarmsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

/** Quick Settings tile that turns every event alarm on or off, mirroring the in-app global switch. */
@AndroidEntryPoint
class GlobalAlarmsTileService : TileService() {
    @Inject
    lateinit var toggleGlobalAlarmsUseCase: ToggleGlobalAlarmsUseCase

    @Inject
    lateinit var observeAppPreferencesUseCase: ObserveAppPreferencesUseCase

    @Inject
    lateinit var crashReporter: CrashReporter

    private val serviceScope by lazy {
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate +
                crashReporter.coroutineExceptionHandler("GlobalAlarmsTileService"),
        )
    }

    private var listeningJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob =
            serviceScope.launch {
                observeAppPreferencesUseCase()
                    .map { it.isGlobalAlarmEnabled }
                    .distinctUntilChanged()
                    .catch { e ->
                        logcat(LogPriority.ERROR) { "Tile: failed to read global alarm switch: ${e.message}" }
                        crashReporter.recordNonFatal(e, "GlobalAlarmsTileService: failed to read global alarm switch")
                        qsTile?.let { tile ->
                            tile.state = Tile.STATE_UNAVAILABLE
                            tile.updateTile()
                        }
                    }
                    .collect(::render)
            }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        // Turning every alarm off from a locked phone would let anyone silence the owner's alarms.
        if (isLocked) unlockAndRun(::toggle) else toggle()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun toggle() {
        serviceScope.launch {
            val enabled =
                crashReporter.runOrReport("GlobalAlarmsTileService: failed to toggle global alarms", fallback = null) {
                    toggleGlobalAlarmsUseCase()
                }
            if (enabled == null) {
                logcat(LogPriority.ERROR) { "Tile: failed to toggle global alarms" }
                return@launch
            }
            render(enabled)
        }
    }

    private fun render(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = globalAlarmsTileState(enabled)
        tile.updateTile()
    }
}

internal fun globalAlarmsTileState(enabled: Boolean): Int = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
