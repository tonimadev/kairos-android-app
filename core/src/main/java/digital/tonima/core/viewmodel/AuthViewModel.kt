package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.usecases.GetGoogleSignInIntentUseCase
import digital.tonima.core.usecases.HandleGoogleSignInResultUseCase
import digital.tonima.core.usecases.IsGoogleSignedInUseCase
import digital.tonima.core.usecases.SignOutFromGoogleUseCase
import digital.tonima.core.viewmodel.AuthIntent.ConsumeEffect
import digital.tonima.core.viewmodel.AuthIntent.HandleGoogleSignInResult
import digital.tonima.core.viewmodel.AuthIntent.SignInWithGoogle
import digital.tonima.core.viewmodel.AuthIntent.SignOutFromGoogle
import digital.tonima.core.viewmodel.AuthSideEffect.LaunchGoogleSignIn
import digital.tonima.core.viewmodel.AuthSideEffect.ShowSnackbar
import digital.tonima.core.viewmodel.UiText.DynamicString
import digital.tonima.core.viewmodel.UiText.StringResource
import digital.tonima.kairos.core.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val isGoogleSignedIn: IsGoogleSignedInUseCase,
        private val getGoogleSignInIntent: GetGoogleSignInIntentUseCase,
        private val handleGoogleSignInResultUseCase: HandleGoogleSignInResultUseCase,
        private val signOutFromGoogle: SignOutFromGoogleUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState = _uiState.asStateFlow()

        val effect = uiState.map { it.effect }.distinctUntilChanged()

        init {
            _uiState.update { it.copy(isGoogleConnected = isGoogleSignedIn()) }
        }

        fun handleIntent(intent: AuthIntent) {
            viewModelScope.launch {
                when (intent) {
                    is ConsumeEffect -> _uiState.update { it.copy(effect = null) }
                    SignInWithGoogle -> {
                        val signInIntent = getGoogleSignInIntent()
                        _uiState.update {
                            it.copy(effect = LaunchGoogleSignIn(signInIntent))
                        }
                    }
                    SignOutFromGoogle -> {
                        signOutFromGoogle()
                        _uiState.update { it.copy(isGoogleConnected = false) }
                    }
                    is HandleGoogleSignInResult -> {
                        val result = handleGoogleSignInResultUseCase(intent.resultData)
                        if (result.isSuccess) {
                            _uiState.update {
                                it.copy(
                                    isGoogleConnected = true,
                                    effect =
                                        ShowSnackbar(
                                            StringResource(R.string.google_logout_title),
                                        ),
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(effect = ShowSnackbar(DynamicString("Login failed")))
                            }
                        }
                    }
                }
            }
        }
    }
