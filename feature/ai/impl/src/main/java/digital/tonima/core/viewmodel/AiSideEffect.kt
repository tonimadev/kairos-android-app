package digital.tonima.core.viewmodel

import androidx.compose.runtime.Immutable

@Immutable
sealed class AiSideEffect {
    @Immutable
    data class RequireUserConfirmation(
        val title: UiText,
        val message: UiText,
    ) : AiSideEffect()

    data class ShowSnackbar(val message: UiText) : AiSideEffect()

    data class AIToolError(val message: UiText) : AiSideEffect()

    /**
     * The AI wrote an event straight to the calendar provider; screens that cache
     * calendar events must reload them, then [message] can be shown to the user.
     */
    data class CalendarEventCreated(val message: UiText) : AiSideEffect()
}
