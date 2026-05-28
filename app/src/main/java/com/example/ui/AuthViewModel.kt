package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("todo_auth_prefs", Context.MODE_PRIVATE)

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userDisplayName = MutableStateFlow(prefs.getString("user_display_name", null))
    val userDisplayName: StateFlow<String?> = _userDisplayName.asStateFlow()

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _forgotPasswordSent = MutableStateFlow(false)
    val forgotPasswordSent: StateFlow<Boolean> = _forgotPasswordSent.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _onboardingCompleted.value = true
    }

    fun loginWithEmail(email: String, password: String): Boolean {
        if (email.isBlank() || !email.contains("@")) {
            _authError.value = "Please enter a valid email address."
            return false
        }
        if (password.length < 6) {
            _authError.value = "Password must be at least 6 characters."
            return false
        }

        // True-working simulated authenticating loop
        _authError.value = null
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putBoolean("is_guest_mode", false)
            .apply()

        _userEmail.value = email
        _isLoggedIn.value = true
        _isGuestMode.value = false
        return true
    }

    fun registerWithEmail(email: String, password: String): Boolean {
        return loginWithEmail(email, password) // Simulation registers and logs in immediately
    }

    fun loginWithGoogle(mockEmail: String = "google.user@gmail.com", displayName: String? = null) {
        _authError.value = null
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", mockEmail)
            .putString("user_display_name", displayName)
            .putBoolean("is_guest_mode", false)
            .apply()

        _userEmail.value = mockEmail
        _userDisplayName.value = displayName
        _isLoggedIn.value = true
        _isGuestMode.value = false
    }

    fun continueAsGuest() {
        _authError.value = null
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_email", null)
            .putBoolean("is_guest_mode", true)
            .apply()

        _userEmail.value = null
        _isLoggedIn.value = false
        _isGuestMode.value = true
    }

    fun sendForgotPasswordLink(email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _authError.value = "Please enter a valid email address."
            return
        }
        _authError.value = null
        _forgotPasswordSent.value = true
    }

    fun resetForgotPassword() {
        _forgotPasswordSent.value = false
        _authError.value = null
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_email", null)
            .putString("user_display_name", null)
            .putBoolean("is_guest_mode", false)
            .apply()

        _userEmail.value = null
        _userDisplayName.value = null
        _isLoggedIn.value = false
        _isGuestMode.value = false
    }

    fun resetAllStates() {
        prefs.edit().clear().apply()
        _onboardingCompleted.value = false
        _isLoggedIn.value = false
        _userEmail.value = null
        _isGuestMode.value = false
        _forgotPasswordSent.value = false
        _authError.value = null
    }
}

class AuthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
