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
}
