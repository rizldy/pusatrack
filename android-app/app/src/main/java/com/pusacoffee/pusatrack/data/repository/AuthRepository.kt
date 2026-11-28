package com.pusacoffee.pusatrack.data.repository

import android.content.Context
import com.pusacoffee.pusatrack.data.api.ApiService
import com.pusacoffee.pusatrack.data.api.RetrofitClient
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.LoginRequest
import com.pusacoffee.pusatrack.data.model.User
import kotlinx.coroutines.delay

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    context: Context,
    private val useMockMode: Boolean = DEFAULT_USE_MOCK
) {

    private val sessionManager = SessionManager(context.applicationContext)
    private val apiService: ApiService by lazy {
        RetrofitClient.getApiService(context.applicationContext)
    }

    suspend fun login(usernameInput: String, passwordInput: String): AuthResult {
        val username = usernameInput.trim()
        val password = passwordInput

        if (username.isEmpty() || password.isEmpty()) {
            return AuthResult.Error("Masukkan username dan password terlebih dahulu.")
        }

        return if (useMockMode) {
            loginWithMock(username, password)
        } else {
            loginWithApi(username, password)
        }
    }

    private suspend fun loginWithMock(username: String, password: String): AuthResult {
        delay(350L)
        val user = AccountRepository.authenticate(username, password)
            ?: return AuthResult.Error(GENERIC_LOGIN_ERROR)

        sessionManager.saveSession(user)
        return AuthResult.Success(user)
    }

    private suspend fun loginWithApi(username: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            val body = response.body()
            val user = body?.data?.user

            if (response.isSuccessful && body?.isSuccess == true && user != null && user.isActive) {
                sessionManager.saveSession(user)
                AuthResult.Success(user)
            } else {
                AuthResult.Error(GENERIC_LOGIN_ERROR)
            }
        } catch (_: Exception) {
            AuthResult.Error("Tidak dapat terhubung ke server. Periksa koneksi jaringanmu.")
        }
    }

    suspend fun logout() {
        if (!useMockMode) {
            try {
                apiService.logout()
            } catch (_: Exception) {
                // Local session is still cleared even if server is unreachable.
            }
        }
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun getCurrentUser(): User? = sessionManager.getUser()

    companion object {
        const val DEFAULT_USE_MOCK = true
        const val GENERIC_LOGIN_ERROR =
            "Username atau password tidak sesuai, atau akun sedang tidak aktif."
    }
}
