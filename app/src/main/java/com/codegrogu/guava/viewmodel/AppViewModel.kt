package com.codegrogu.guava.viewmodel

import androidx.lifecycle.ViewModel
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppState(
    val currentUser: User? = null,
    val userRole: UserRole = UserRole.UNKNOWN,
    val isLoading: Boolean = false,
)

class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

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
