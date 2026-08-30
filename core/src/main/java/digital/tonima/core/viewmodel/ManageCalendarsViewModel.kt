package digital.tonima.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.core.usecases.DeleteCalendarUseCase
import digital.tonima.core.usecases.GetAvailableCalendarsUseCase
import digital.tonima.core.usecases.UpdateCalendarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCalendarsViewModel
    @Inject
    constructor(
        private val getAvailableCalendarsUseCase: GetAvailableCalendarsUseCase,
        private val updateCalendarUseCase: UpdateCalendarUseCase,
        private val deleteCalendarUseCase: DeleteCalendarUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ManageCalendarsUiState())
        val uiState = _uiState.asStateFlow()

        val effect = uiState.map { it.effect }.distinctUntilChanged()

        init {
            handleIntent(ManageCalendarsIntent.LoadCalendars)
        }

        fun handleIntent(intent: ManageCalendarsIntent) {
            when (intent) {
                is ManageCalendarsIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
                ManageCalendarsIntent.LoadCalendars -> loadCalendars()
                is ManageCalendarsIntent.OpenEditDialog -> {
                    _uiState.update {
                        it.copy(
                            showEditDialog = true,
                            selectedCalendarId = intent.calendar.id,
                            editName = intent.calendar.displayName,
                            editColor = intent.calendar.color,
                        )
                    }
                }
                ManageCalendarsIntent.CloseEditDialog -> {
                    _uiState.update { it.copy(showEditDialog = false, selectedCalendarId = null) }
                }
                is ManageCalendarsIntent.UpdateEditName -> {
                    _uiState.update { it.copy(editName = intent.name) }
                }
                is ManageCalendarsIntent.UpdateEditColor -> {
                    _uiState.update { it.copy(editColor = intent.color) }
                }
                ManageCalendarsIntent.SaveEdit -> saveEdit()
                is ManageCalendarsIntent.DeleteCalendar -> deleteCalendar(intent.calendarId)
            }
        }

        private fun loadCalendars() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val calendars = getAvailableCalendarsUseCase()
                // Filtra apenas os calendários locais que criamos,
                // ou mostra todos os do usuário que ele tem permissão de Owner.
                // Filtra apenas os calendários locais que criamos
                val kairosCalendars =
                    calendars.filter {
                        it.accountName == "Kairos Imports"
                    }
                _uiState.update { it.copy(calendars = kairosCalendars, isLoading = false) }
            }
        }

        private fun saveEdit() {
            val state = _uiState.value
            val id = state.selectedCalendarId ?: return
            viewModelScope.launch {
                val success = updateCalendarUseCase(id, state.editName, state.editColor)
                if (success) {
                    handleIntent(ManageCalendarsIntent.CloseEditDialog)
                    loadCalendars()
                }
            }
        }

        private fun deleteCalendar(calendarId: Long) {
            viewModelScope.launch {
                val success = deleteCalendarUseCase(calendarId)
                if (success) {
                    loadCalendars()
                }
            }
        }
    }
