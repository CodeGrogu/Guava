package com.codegrogu.guava.viewmodel

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppState(
    val currentUser: User? = null,
    val userRole: UserRole = UserRole.UNKNOWN,
    val isLoading: Boolean = false,
)

sealed class UiEvent {
    data class ShowSnackbar(
        override val message: String,
        val isError: Boolean = true
    ) : UiEvent(), SnackbarVisuals {
        override val actionLabel: String? = null
        override val duration: SnackbarDuration = SnackbarDuration.Short
        override val withDismissAction: Boolean = true
    }
}

class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    fun showSnackbar(message: String, isError: Boolean = true) {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.ShowSnackbar(message, isError))
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun updateUser(user: User?, role: UserRole) {
        _uiState.update {
            it.copy(
                currentUser = user,
                userRole = role,
                isLoading = false
            )
        }
    }

    fun clearState() {
        _uiState.update {
            AppState()
        }
    }
}
