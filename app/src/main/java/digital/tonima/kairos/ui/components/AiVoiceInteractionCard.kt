package digital.tonima.kairos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}

@Composable
fun AiVoiceInteractionCard(
    question: String?,
    response: String?,
    isAsking: Boolean,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = question != null || isAsking,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.PaddingNormal),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_k_monochrome),
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.IconSizeSmall),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                        Text(
                            text = stringResource(R.string.ai_voice_interaction_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isAsking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            if (response != null) {
                                IconButton(
                                    onClick = { if (isSpeaking) onStopSpeaking() else onSpeak() },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isSpeaking) {
                                                    digital.tonima.kairos.R.drawable.volume_off
                                                } else {
                                                    digital.tonima.kairos.R.drawable.volume_up
                                                },
                                            ),
                                        contentDescription =
                                            stringResource(
                                                if (isSpeaking) R.string.cd_stop_speaking else R.string.cd_speak,
                                            ),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                                Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                            }

                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    painter = painterResource(digital.tonima.kairos.R.drawable.ic_expand_less),
                                    contentDescription = stringResource(R.string.cd_close),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

                    if (question != null) {
                        Text(
                            text = "\"$question\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(Dimensions.SpacingExtraSmall))
                    }

                    if (response != null) {
                        Text(
                            text = parseMarkdownToAnnotatedString(response),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    } else if (isAsking) {
                        Text(
                            text = stringResource(R.string.ai_answering),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
            // Bubble tail
            Box(
                modifier =
                    Modifier
                        .padding(end = 24.dp)
                        .size(20.dp, 12.dp)
                        .clip(BubbleTailShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
            )
        }
    }
}

private val BubbleTailShape =
    GenericShape { size: Size, _ ->
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
        addPath(path)
    }
