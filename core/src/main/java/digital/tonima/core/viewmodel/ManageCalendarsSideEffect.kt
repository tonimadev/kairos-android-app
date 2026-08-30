package digital.tonima.core.viewmodel

sealed class ManageCalendarsSideEffect {
    data class ShowSnackbar(val message: UiText) : ManageCalendarsSideEffect()
}
