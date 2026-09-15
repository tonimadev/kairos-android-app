package digital.tonima.core.viewmodel

sealed class SettingsSideEffect {
    data class ShowSnackbar(val message: UiText) : SettingsSideEffect()
}
