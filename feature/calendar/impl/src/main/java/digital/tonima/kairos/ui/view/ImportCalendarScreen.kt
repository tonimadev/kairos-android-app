package digital.tonima.kairos.ui.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import digital.tonima.core.viewmodel.ImportCalendarIntent
import digital.tonima.core.viewmodel.ImportCalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCalendarScreen(
    viewModel: ImportCalendarViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    val presetColors =
        listOf(
            Color(0xFFF44336),
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF673AB7),
            Color(0xFF3F51B5),
            Color(0xFF2196F3),
            Color(0xFF03A9F4),
            Color(0xFF00BCD4),
            Color(0xFF009688),
            Color(0xFF4CAF50),
            Color(0xFF8BC34A),
            Color(0xFFCDDC39),
            Color(0xFFFFEB3B),
            Color(0xFFFFC107),
            Color(0xFFFF9800),
            Color(0xFFFF5722),
        )

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                viewModel.handleIntent(ImportCalendarIntent.FileSelected(uri.toString()))
            }
        }

    if (state.isSuccess) {
        AlertDialog(
            onDismissRequest = {
                viewModel.handleIntent(ImportCalendarIntent.ResetSuccess)
                onNavigateBack()
            },
            title = { Text("Sucesso") },
            text = { Text("Calendário importado com sucesso!") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(ImportCalendarIntent.ResetSuccess)
                    onNavigateBack()
                }) {
                    Text("OK")
                }
            },
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(ImportCalendarIntent.DismissError) },
            title = { Text("Erro") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.handleIntent(ImportCalendarIntent.DismissError) }) {
                    Text("OK")
                }
            },
        )
    }

    androidx.activity.compose.BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Calendário ICS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.calendarName,
                onValueChange = { viewModel.handleIntent(ImportCalendarIntent.UpdateName(it)) },
                label = { Text("Nome do Calendário") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Origem do Arquivo", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.url,
                onValueChange = { viewModel.handleIntent(ImportCalendarIntent.UpdateUrl(it)) },
                label = { Text("URL do .ics") },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.fileUri == null,
            )

            Text("OU", modifier = Modifier.align(Alignment.CenterHorizontally))

            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    if (state.fileUri != null) {
                        ButtonDefaults.filledTonalButtonColors()
                    } else {
                        ButtonDefaults.buttonColors()
                    },
            ) {
                Text(if (state.fileUri != null) "Arquivo Selecionado" else "Selecionar Arquivo Local")
            }

            Text("Cor do Calendário", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetColors) { color ->
                    val isSelected = state.calendarColor == color.toArgb()
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.handleIntent(ImportCalendarIntent.UpdateColor(color.toArgb())) }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape,
                                ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Ativar Alarmes Automaticamente")
                Switch(
                    checked = state.alarmsEnabled,
                    onCheckedChange = { viewModel.handleIntent(ImportCalendarIntent.ToggleAlarms(it)) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.handleIntent(ImportCalendarIntent.SubmitImport) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Importar")
                }
            }
        }
    }
}
