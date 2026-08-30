package digital.tonima.core.viewmodel

import androidx.compose.runtime.Immutable

@Immutable
data class AuthUiState(
    val isGoogleConnected: Boolean = false,
    val effect: AuthSideEffect? = null,
)
