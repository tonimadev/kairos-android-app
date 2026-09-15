package digital.tonima.core.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.usecases.ImportIcsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class ImportCalendarViewModel
    @Inject
    constructor(
        private val importIcsUseCase: ImportIcsUseCase,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ImportCalendarUiState())
        val uiState = _uiState.asStateFlow()

        val effect = uiState.map { it.effect }.distinctUntilChanged()

        fun handleIntent(intent: ImportCalendarIntent) {
            when (intent) {
                is ImportCalendarIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
                is ImportCalendarIntent.UpdateUrl -> {
                    _uiState.update { it.copy(url = intent.url, fileUri = null) }
                }
                is ImportCalendarIntent.FileSelected -> {
                    _uiState.update { it.copy(fileUri = intent.uri, url = "") }
                }
                is ImportCalendarIntent.UpdateName -> {
                    _uiState.update { it.copy(calendarName = intent.name) }
                }
                is ImportCalendarIntent.UpdateColor -> {
                    _uiState.update { it.copy(calendarColor = intent.color) }
                }
                is ImportCalendarIntent.ToggleAlarms -> {
                    _uiState.update { it.copy(alarmsEnabled = intent.enabled) }
                }
                is ImportCalendarIntent.SubmitImport -> {
                    importCalendar()
                }
                ImportCalendarIntent.DismissError -> {
                    _uiState.update { it.copy(error = null) }
                }
                ImportCalendarIntent.ResetSuccess -> {
                    _uiState.update { it.copy(isSuccess = false) }
                }
            }
        }

        private fun importCalendar() {
            val state = _uiState.value
            if (state.calendarName.isBlank()) {
                _uiState.update { it.copy(error = "O nome do calendário é obrigatório") }
                return
            }

            if (state.url.isBlank() && state.fileUri == null) {
                _uiState.update { it.copy(error = "Forneça uma URL ou selecione um arquivo") }
                return
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            viewModelScope.launch {
                try {
                    val content =
                        withContext(Dispatchers.IO) {
                            if (state.fileUri != null) {
                                val uri = state.fileUri.toUri()
                                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                                    it.readText()
                                } ?: throw IllegalStateException("Não foi possível ler o arquivo selecionado.")
                            } else {
                                URL(state.url).readText()
                            }
                        }

                    val result =
                        importIcsUseCase(
                            content = content,
                            calendarName = state.calendarName,
                            color = state.calendarColor,
                            alarmsEnabled = state.alarmsEnabled,
                        )

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                        _uiState.update { it.copy(isLoading = false, error = msg) }
                    }
                } catch (e: Exception) {
                    val eMsg = e.message ?: "Erro ao importar calendário"
                    _uiState.update { it.copy(isLoading = false, error = eMsg) }
                }
            }
        }
    }
