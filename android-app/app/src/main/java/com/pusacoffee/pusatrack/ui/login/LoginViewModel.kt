package com.pusacoffee.pusatrack.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pusacoffee.pusatrack.data.model.User
import com.pusacoffee.pusatrack.data.repository.AuthRepository
import com.pusacoffee.pusatrack.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun isAlreadyLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun login(username: String, password: String) {
        if (_uiState.value is LoginUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = authRepository.login(username, password)) {
                is AuthResult.Success -> _uiState.value = LoginUiState.Success(result.user)
                is AuthResult.Error -> _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
