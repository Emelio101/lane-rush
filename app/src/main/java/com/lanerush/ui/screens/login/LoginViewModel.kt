package com.lanerush.ui.screens.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lanerush.BuildConfig
import com.lanerush.domain.repository.AuthRepository
import com.lanerush.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    // One-shot navigation event — emitted only on confirmed auth success
    private val _navigateToHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToHome = _navigateToHome.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (serverClientId.isBlank()) {
                    throw Exception("Web Client ID is missing from local.properties")
                }

                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken

                authRepository.signInWithGoogle(idToken)
                    .onSuccess { user ->
                        _navigateToHome.emit(Unit) // Firebase confirmed — navigate immediately
                        leaderboardRepository.createOrUpdateUser(user) // background write
                    }
                    .onFailure { e ->
                        Log.e("LoginViewModel", "Firebase Auth failed", e)
                        _errorMessage.value = "Auth failed: ${e.localizedMessage}"
                    }

            } catch (_: GetCredentialCancellationException) {
                // User dismissed the sheet — not an error, do nothing
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Google Sign-In Exception: ${e.message}", e)
                val devMsg = when {
                    e.message?.contains("10") == true || e.message?.contains("DEVELOPER_ERROR") == true ->
                        "Misconfigured: Check SHA-1 and Web Client ID in Firebase Console."
                    e.message?.contains("7") == true || e.message?.contains("NETWORK_ERROR") == true ->
                        "Network Error: Please check your internet connection."
                    else -> e.localizedMessage ?: "Google Sign-In failed"
                }
                _errorMessage.value = "Google Sign-In failed: $devMsg"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    _navigateToHome.emit(Unit) // Firebase confirmed — navigate immediately
                    leaderboardRepository.createOrUpdateUser(user) // background write
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Login failed"
                }
            _isLoading.value = false
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _errorMessage.value = "All fields are required"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.signUpWithEmail(email, password, displayName)
                .onSuccess { user ->
                    _navigateToHome.emit(Unit) // Firebase confirmed — navigate immediately
                    leaderboardRepository.createOrUpdateUser(user) // background write
                }
                .onFailure { e ->
                    _errorMessage.value = e.localizedMessage ?: "Signup failed"
                }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}