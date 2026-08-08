package digital.tonima.kairos.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import digital.tonima.core.viewmodel.AiIntent
import digital.tonima.core.viewmodel.AiSideEffect
import digital.tonima.core.viewmodel.AiUiState
import digital.tonima.core.viewmodel.AiViewModel
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.core.viewmodel.EventViewModel
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun EventScreenDialogs(
    uiState: EventScreenUiState,
    aiUiState: AiUiState,
    aiConfirmationData: AiSideEffect.RequireUserConfirmation?,
    onClearAiConfirmation: () -> Unit,
    eventViewModel: EventViewModel,
    aiViewModel: AiViewModel,
) {
    val context = LocalContext.current

    if (uiState.showRatingBottomSheet) {
        RatingBottomSheet(
            onDismissRequest = { eventViewModel.handleIntent(EventIntent.RateLater) },
            onRateNow = {
                eventViewModel.handleIntent(EventIntent.RateNow)
            },
            onRateLater = { eventViewModel.handleIntent(EventIntent.RateLater) },
            onRateNeverShow = { eventViewModel.handleIntent(EventIntent.RateNever) },
        )
    }

    aiConfirmationData?.let { data ->
        AlertDialog(
            onDismissRequest = {
                onClearAiConfirmation()
                aiViewModel.handleIntent(AiIntent.RejectPendingAction)
            },
            title = { Text(text = data.title.asString(context)) },
            text = { Text(text = data.message.asString(context)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearAiConfirmation()
                    aiViewModel.handleIntent(AiIntent.ApprovePendingAction)
                }) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onClearAiConfirmation()
                    aiViewModel.handleIntent(AiIntent.RejectPendingAction)
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingBottomSheet(
    onDismissRequest: () -> Unit,
    onRateNow: () -> Unit,
    onRateLater: () -> Unit,
    onRateNeverShow: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.rate_app_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
            Text(
                text = stringResource(R.string.rate_app_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            Button(onClick = onRateNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rate_now)) }
            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
            TextButton(
                onClick = onRateLater,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.rate_later)) }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimensions.SpacingSmall),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TextButton(onClick = onRateNeverShow, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.rate_never), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        }
    }
}
