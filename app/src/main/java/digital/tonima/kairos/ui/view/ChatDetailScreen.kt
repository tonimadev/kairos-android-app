package digital.tonima.kairos.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.core.ai.model.ChatMessage
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.parseMarkdownToAnnotatedString
import digital.tonima.kairos.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    messages: List<ChatMessage>,
    isAsking: Boolean,
    isSpeaking: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(digital.tonima.kairos.core.R.string.drawer_ai_assistant)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_close))
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.SpacingNormal)
                            .navigationBarsPadding()
                            .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Digite sua mensagem...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                    )
                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSendMessage(textInput)
                                textInput = ""
                            },
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar")
                        }
                    } else {
                        IconButton(
                            onClick = onSpeakToggle,
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (isSpeaking) {
                                            digital.tonima.kairos.R.drawable.volume_off
                                        } else {
                                            digital.tonima.kairos.core.R.drawable.ic_mic
                                        },
                                    ),
                                contentDescription = "Falar",
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(Dimensions.SpacingNormal),
        ) {
            items(messages) { msg ->
                ChatMessageItem(msg)
                Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
            }
            if (isAsking) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimensions.SpacingNormal),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(msg: ChatMessage) {
    val isUser = msg.role == ChatMessage.Role.USER

    // We only display Text messages or FunctionResponses visually. We hide FunctionCalls or format them differently.
    val textContent =
        when (msg) {
            is ChatMessage.Text -> msg.content
            is ChatMessage.FunctionCall -> "⏳ ${msg.name}..." // Hide or show small indicator
            is ChatMessage.FunctionResponse -> "✅ Ação concluída" // Hide or show small indicator
        }

    if (msg is ChatMessage.FunctionCall || msg is ChatMessage.FunctionResponse) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = textContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 16.dp,
                        ),
                    )
                    .background(
                        if (isUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .padding(Dimensions.SpacingNormal),
        ) {
            SelectionContainer {
                Text(
                    text = parseMarkdownToAnnotatedString(textContent),
                    color =
                        if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
