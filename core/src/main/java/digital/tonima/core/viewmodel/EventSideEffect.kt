package digital.tonima.core.viewmodel

/**
 * One-shot side effects emitted by [EventViewModel] and consumed by the UI layer.
 *
 * Unlike [EventScreenUiState] (which is persistent), each effect is consumed
 * exactly once (via a `Channel` → `Flow`).
 *
 * All user-visible messages use [UiText] so they are resolved at the UI layer
 * with the correct locale / Context.
 */
sealed class EventSideEffect {
    /**
     * The AI agent attempted to execute a [digital.tonima.core.ai.RiskLevel.CRITICAL]
     * action. The UI must show a confirmation dialog with the given [title] and
     * [message]. If the user approves, dispatch [EventIntent.ApprovePendingAction];
     * if they reject, dispatch [EventIntent.RejectPendingAction].
     */
    data class RequireUserConfirmation(
        val title: UiText,
        val message: UiText,
    ) : EventSideEffect()

    /**
     * The AI agent executed a [digital.tonima.core.ai.RiskLevel.MODERATE] action.
     * The UI should show a non-blocking snackbar with the given [message].
     */
    data class ShowSnackbar(val message: UiText) : EventSideEffect()

    /**
     * The AI tool call failed (tool not found or bad arguments).
     * The UI should display an error message.
     */
    data class AIToolError(val message: UiText) : EventSideEffect()

    data class OpenMeetingUrl(val url: String) : EventSideEffect()

    data class CopyToClipboard(val text: String, val message: UiText) : EventSideEffect()

    data class LaunchGoogleSignIn(val intent: android.content.Intent) : EventSideEffect()
}
