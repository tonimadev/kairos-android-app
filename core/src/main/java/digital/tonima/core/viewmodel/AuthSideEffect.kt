package digital.tonima.core.viewmodel

sealed class AuthSideEffect {
    data class LaunchGoogleSignIn(val intent: android.content.Intent) : AuthSideEffect()

    data class ShowSnackbar(val message: UiText) : AuthSideEffect()
}
