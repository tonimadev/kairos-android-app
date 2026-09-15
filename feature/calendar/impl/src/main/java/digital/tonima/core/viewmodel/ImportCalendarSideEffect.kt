package digital.tonima.core.viewmodel

sealed class ImportCalendarSideEffect {
    data class ShowSnackbar(val message: UiText) : ImportCalendarSideEffect()

    data object NavigateBack : ImportCalendarSideEffect()
}
