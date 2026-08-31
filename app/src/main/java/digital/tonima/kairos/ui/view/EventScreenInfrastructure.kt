package digital.tonima.kairos.ui.view

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.review.ReviewManagerFactory.create
import digital.tonima.core.viewmodel.AiIntent
import digital.tonima.core.viewmodel.AiSideEffect
import digital.tonima.core.viewmodel.AiSideEffect.RequireUserConfirmation
import digital.tonima.core.viewmodel.AiViewModel
import digital.tonima.core.viewmodel.AuthIntent
import digital.tonima.core.viewmodel.AuthSideEffect
import digital.tonima.core.viewmodel.AuthSideEffect.LaunchGoogleSignIn
import digital.tonima.core.viewmodel.AuthViewModel
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventSideEffect.AIToolError
import digital.tonima.core.viewmodel.EventSideEffect.CopyToClipboard
import digital.tonima.core.viewmodel.EventSideEffect.OpenMeetingUrl
import digital.tonima.core.viewmodel.EventSideEffect.RequestAppReview
import digital.tonima.core.viewmodel.EventSideEffect.RequestPurchase
import digital.tonima.core.viewmodel.EventSideEffect.RequestSubscription
import digital.tonima.core.viewmodel.EventSideEffect.ShowSnackbar
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.core.viewmodel.SettingsIntent
import digital.tonima.core.viewmodel.SettingsViewModel
import logcat.logcat

@Composable
fun EventScreenInfrastructure(
    eventViewModel: EventViewModel,
    aiViewModel: AiViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState,
    onSubscriptionRequest: () -> Unit,
    onPurchaseRequest: () -> Unit,
    onSetAiConfirmationData: (RequireUserConfirmation) -> Unit,
) {
    val googleSignInLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            authViewModel.handleIntent(AuthIntent.HandleGoogleSignInResult(result.data))
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    settingsViewModel.handleIntent(SettingsIntent.CheckPermissions)
                    eventViewModel.handleIntent(EventIntent.RefreshEvents)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HandleSideEffects(
        eventViewModel = eventViewModel,
        aiViewModel = aiViewModel,
        authViewModel = authViewModel,
        settingsViewModel = settingsViewModel,
        snackbarHostState = snackbarHostState,
        googleSignInLauncher = googleSignInLauncher,
        onSetAiConfirmationData = onSetAiConfirmationData,
        onSubscriptionRequest = onSubscriptionRequest,
        onPurchaseRequest = onPurchaseRequest,
    )
}

@Composable
private fun HandleSideEffects(
    eventViewModel: EventViewModel,
    aiViewModel: AiViewModel,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onSetAiConfirmationData: (RequireUserConfirmation) -> Unit,
    onSubscriptionRequest: () -> Unit,
    onPurchaseRequest: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(eventViewModel.effect) {
        eventViewModel.effect.collect { effect ->
            if (effect != null) {
                when (effect) {
                    is ShowSnackbar ->
                        snackbarHostState.showSnackbar(
                            effect.message.asString(context),
                        )
                    is AIToolError ->
                        snackbarHostState.showSnackbar(
                            effect.message.asString(context),
                        )
                    is OpenMeetingUrl -> {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                        } catch (e: Exception) {
                            logcat("EventScreen") { "Failed to open meeting URL: ${e.message}" }
                        }
                    }
                    is CopyToClipboard -> {
                        val clipboard =
                            context.getSystemService(
                                Context.CLIPBOARD_SERVICE,
                            ) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Meeting Link", effect.text)
                        clipboard.setPrimaryClip(clip)
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                    RequestAppReview -> {
                        val activity = context.findActivity()
                        if (activity != null) {
                            val reviewManager = create(context)
                            reviewManager.requestReviewFlow().addOnCompleteListener { request ->
                                if (request.isSuccessful) {
                                    val reviewInfo = request.result
                                    reviewManager.launchReviewFlow(activity, reviewInfo)
                                } else {
                                    openPlayStoreFallback(context)
                                }
                            }
                        } else {
                            openPlayStoreFallback(context)
                        }
                    }
                    RequestSubscription -> onSubscriptionRequest()
                    RequestPurchase -> onPurchaseRequest()
                }
                eventViewModel.handleIntent(EventIntent.ConsumeEffect)
            }
        }
    }

    LaunchedEffect(aiViewModel.effect) {
        aiViewModel.effect.collect { effect ->
            if (effect != null) {
                when (effect) {
                    is RequireUserConfirmation -> onSetAiConfirmationData(effect)
                    is AiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message.asString(context))
                    is AiSideEffect.AIToolError -> snackbarHostState.showSnackbar(effect.message.asString(context))
                }
                aiViewModel.handleIntent(AiIntent.ConsumeEffect)
            }
        }
    }

    LaunchedEffect(authViewModel.effect) {
        authViewModel.effect.collect { effect ->
            if (effect != null) {
                when (effect) {
                    is LaunchGoogleSignIn -> googleSignInLauncher.launch(effect.intent)
                    is AuthSideEffect.ShowSnackbar ->
                        snackbarHostState.showSnackbar(
                            effect.message.asString(context),
                        )
                }
                authViewModel.handleIntent(AuthIntent.ConsumeEffect)
            }
        }
    }

    LaunchedEffect(settingsViewModel.effect) {
        settingsViewModel.effect.collect { effect ->
            if (effect != null) {
                when (effect) {
                    is digital.tonima.core.viewmodel.SettingsSideEffect.ShowSnackbar ->
                        snackbarHostState.showSnackbar(
                            effect.message.asString(context),
                        )
                }
                settingsViewModel.handleIntent(SettingsIntent.ConsumeEffect)
            }
        }
    }
}

private fun openPlayStoreFallback(context: Context) {
    try {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                "market://details?id=${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } catch (_: Exception) {
        val webIntent =
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(webIntent)
    }
}
