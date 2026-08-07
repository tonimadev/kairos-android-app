package digital.tonima.kairos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.util.openAutostartSettings
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanel(
    uiState: EventScreenUiState,
    settingsActions: SettingsActions,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
    ) {
        AlarmsToggleRow(
            modifier = Modifier.padding(top = Dimensions.PaddingSmall),
            alarmsEnabled = uiState.isGlobalAlarmEnabled,
            onToggle = settingsActions.onToggle,
        )

        if (uiState.audioWarning != AudioWarningState.NORMAL) {
            RingerModeWarningCard(ringerMode = uiState.audioWarning)
        }

        if (uiState.showAutostartSuggestion) {
            AutostartSuggestionCard(
                onOpenSettings = { openAutostartSettings(context) },
                onDismiss = settingsActions.onDismissAutostart,
            )
        }
    }
}
