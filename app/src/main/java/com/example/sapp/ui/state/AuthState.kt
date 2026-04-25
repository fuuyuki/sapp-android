package com.example.sapp.ui.state

// app/ui/state/AuthState.kt
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object LoggingIn : AuthState()
    data class Error(val message: String) : AuthState()
    object Success : AuthState()        // credentials accepted
    object SessionReady : AuthState()   // FCM token registered, user data loaded
}