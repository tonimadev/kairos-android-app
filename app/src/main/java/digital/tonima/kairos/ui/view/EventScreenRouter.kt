package digital.tonima.kairos.ui.view

import androidx.compose.runtime.Composable
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.SettingsUiState

@Composable
fun EventScreenRouter(
    uiState: EventScreenUiState,
    settingsUiState: SettingsUiState,
    aiUiState: AiUiState,
    chatDetailScreen: @Composable () -> Unit,
    chatHistoryScreen: @Composable () -> Unit,
    importCalendarScreen: @Composable () -> Unit,
    manageCalendarsScreen: @Composable () -> Unit,
    settingsScreen: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
) {
    when {
        settingsUiState.showSettingsScreen -> settingsScreen()
        aiUiState.selectedConversationId != null -> chatDetailScreen()
        aiUiState.showChatHistoryScreen -> chatHistoryScreen()
        uiState.showImportCalendarScreen -> importCalendarScreen()
        uiState.showManageCalendarsScreen -> manageCalendarsScreen()
        else -> mainContent()
    }
}
