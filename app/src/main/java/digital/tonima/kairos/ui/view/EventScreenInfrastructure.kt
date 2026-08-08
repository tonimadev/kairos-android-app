package digital.tonima.kairos.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import digital.tonima.core.viewmodel.AiIntent
import digital.tonima.core.viewmodel.AiSideEffect
import digital.tonima.core.viewmodel.AiViewModel
import digital.tonima.core.viewmodel.AuthIntent
import digital.tonima.core.viewmodel.AuthSideEffect
import digital.tonima.core.viewmodel.AuthViewModel
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventSideEffect
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.core.viewmodel.SettingsIntent
import digital.tonima.core.viewmodel.SettingsViewModel
import digital.tonima.kairos.core.R
import kotlinx.coroutines.launch
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
    onSetAiConfirmationData: (AiSideEffect.RequireUserConfirmation) -> Unit,
) {
    val aiInstruction = stringResource(R.string.ai_briefing_instruction)

    val googleSignInLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            authViewModel.handleIntent(AuthIntent.HandleGoogleSignInResult(result.data))
        }

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                results?.firstOrNull()?.let { spokenText ->
                    aiViewModel.handleIntent(AiIntent.AskAi(spokenText, aiInstruction))
                }
            }
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
        settingsViewModel = settingsViewModel,
        authViewModel = authViewModel,
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
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onSetAiConfirmationData: (AiSideEffect.RequireUserConfirmation) -> Unit,
    onSubscriptionRequest: () -> Unit,
    onPurchaseRequest: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                eventViewModel.sideEffect.collect { effect ->
                    when (effect) {
                        is EventSideEffect.ShowSnackbar ->
                            snackbarHostState.showSnackbar(
                                effect.message.asString(context),
                            )
                        is EventSideEffect.AIToolError ->
                            snackbarHostState.showSnackbar(
                                effect.message.asString(context),
                            )
                        is EventSideEffect.OpenMeetingUrl -> {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                            } catch (e: Exception) {
                                logcat { "Failed to open meeting URL: ${e.message}" }
                            }
                        }
                        is EventSideEffect.CopyToClipboard -> {
                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE,
                                ) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Meeting Link", effect.text)
                            clipboard.setPrimaryClip(clip)
                            snackbarHostState.showSnackbar(effect.message.asString(context))
                        }
                        EventSideEffect.RequestAppReview -> {
                            val activity = context.findActivity()
                            if (activity != null) {
                                val reviewManager =
                                    com.google.android.play.core.review.ReviewManagerFactory.create(
                                        context,
                                    )
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
                        EventSideEffect.RequestSubscription -> onSubscriptionRequest()
                        EventSideEffect.RequestPurchase -> onPurchaseRequest()
                    }
                }
            }
            launch {
                aiViewModel.sideEffect.collect { effect ->
                    when (effect) {
                        is AiSideEffect.RequireUserConfirmation -> onSetAiConfirmationData(effect)
                        is AiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message.asString(context))
                        is AiSideEffect.AIToolError -> snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
            launch {
                authViewModel.sideEffect.collect { effect ->
                    when (effect) {
                        is AuthSideEffect.LaunchGoogleSignIn -> googleSignInLauncher.launch(effect.intent)
                        is AuthSideEffect.ShowSnackbar ->
                            snackbarHostState.showSnackbar(
                                effect.message.asString(context),
                            )
                    }
                }
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
