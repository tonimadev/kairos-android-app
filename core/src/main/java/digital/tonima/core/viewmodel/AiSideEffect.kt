package digital.tonima.core.viewmodel

sealed class AiSideEffect {
    data class RequireUserConfirmation(
        val title: UiText,
        val message: UiText,
    ) : AiSideEffect()

    data class ShowSnackbar(val message: UiText) : AiSideEffect()

    data class AIToolError(val message: UiText) : AiSideEffect()
}
