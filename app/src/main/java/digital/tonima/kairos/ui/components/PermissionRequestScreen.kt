package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun StandardPermissionsScreen(
    onSettingsClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PaddingNormal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.initial_permissions_required),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
        Text(
            stringResource(R.string.permissions_disclaimer),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingDefault)) {
            Button(onClick = onSettingsClick) { Text(stringResource(R.string.open_settings)) }
            Button(onClick = onRetryClick) { Text(stringResource(R.string.try_again)) }
        }
    }
}

@Composable
fun ExactAlarmPermissionScreen(
    onAlreadyAuthorizedClick: () -> Unit,
    onProvidePermissionClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PaddingNormal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.exact_alarm_permission),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
        Text(
            stringResource(R.string.exact_alarm_permission_disclaimer),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        Button(onClick = onProvidePermissionClick) { Text(stringResource(R.string.provide_permission)) }
        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
        TextButton(onClick = onAlreadyAuthorizedClick) { Text(stringResource(R.string.already_authorized)) }
        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
        TextButton(onClick = onSkipClick) { Text(stringResource(R.string.skip)) }
    }
}

@Composable
fun FullScreenIntentPermissionScreen(
    onAlreadyAuthorizedClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.PaddingNormal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.full_screen_permission),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
        Text(
            stringResource(R.string.full_screen_permission_disclaimer),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        Button(onClick = onOpenSettingsClick) { Text(stringResource(R.string.open_settings)) }
        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
        TextButton(onClick = onAlreadyAuthorizedClick) { Text(stringResource(R.string.already_authorized)) }
        Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
        TextButton(onClick = onSkipClick) { Text(stringResource(R.string.skip)) }
    }
}

@Preview(showBackground = true)
@Composable
fun StandardPermissionsScreenPreview() {
    StandardPermissionsScreen(
        onSettingsClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
fun ExactAlarmPermissionScreenPreview() {
    ExactAlarmPermissionScreen(
        onAlreadyAuthorizedClick = {},
        onProvidePermissionClick = {},
        onSkipClick = {},
    )
}

@Preview(showBackground = true)
@Composable
fun FullScreenIntentPermissionScreenPreview() {
    FullScreenIntentPermissionScreen(
        onAlreadyAuthorizedClick = {},
        onOpenSettingsClick = {},
        onSkipClick = {},
    )
}
