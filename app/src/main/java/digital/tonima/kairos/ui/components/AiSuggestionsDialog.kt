package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun AiSuggestionsDialog(
    onDismiss: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onVoiceClick: () -> Unit,
) {
    val suggestions =
        listOf(
            stringResource(R.string.ai_suggestion_sleep),
            stringResource(R.string.ai_suggestion_create_event),
            stringResource(R.string.ai_suggestion_reminder),
            stringResource(R.string.ai_suggestion_agenda),
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.PaddingSmall),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_k_monochrome),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.ai_suggestions_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingSmall),
            ) {
                Text(
                    text = stringResource(R.string.daily_briefing_trial_invite),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.PaddingSmall),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingSmall),
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionItem(
                            text = suggestion,
                            onClick = {
                                onSuggestionClick(suggestion)
                                onDismiss()
                            },
                        )
                    }
                }

                Surface(
                    onClick = onVoiceClick,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = Dimensions.PaddingNormal),
                ) {
                    Row(
                        modifier = Modifier.padding(Dimensions.PaddingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mic),
                            contentDescription = stringResource(R.string.cd_voice_capture),
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.cd_voice_capture),
                            modifier = Modifier.padding(start = Dimensions.PaddingSmall),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SuggestionItem(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.PaddingNormal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_k_monochrome),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = Dimensions.PaddingSmall),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiSuggestionItemPreview() {
    SuggestionItem(
        text = "Create a meeting",
        onClick = {},
    )
}
