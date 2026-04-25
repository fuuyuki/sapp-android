package com.example.sapp.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sapp.data.model.*
import com.example.sapp.data.repository.AppRepository
import com.example.sapp.data.local.dataStore
import com.example.sapp.ui.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: AppRepository,
    private val context: Context
) : ViewModel() {

    val devices = MutableStateFlow<List<DeviceOut>>(emptyList())
    val schedules = MutableStateFlow<List<ScheduleOut>>(emptyList())
    val medlogs = MutableStateFlow<List<MedlogOut>>(emptyList())
    val userProfile = MutableStateFlow<UserOut?>(null)
    val adherenceSummary = MutableStateFlow<AdherenceSummaryResponse?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun setAuthError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun setSessionReady() {
        _authState.value = AuthState.SessionReady
    }

    private var currentUserId: UUID? = null
    private val tokenKey = stringPreferencesKey("jwt_token")

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val meResponse = repository.getMe()
                currentUserId = meResponse.user_id
                refreshDashboard()
                _authState.value = AuthState.SessionReady
            } catch (e: Exception) {
                _authState.value = AuthState.Idle
            }
        }
    }


    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.LoggingIn
            try {
                val response = repository.login(email, pass)
                if (response.isSuccessful) {
                    response.body()?.access_token?.let { token ->
                        context.dataStore.edit { it[tokenKey] = token }
                        _authState.value = AuthState.Success

                        try {
                            val meResponse = repository.getMe()
                            currentUserId = meResponse.user_id
                            refreshDashboard()
                            _authState.value = AuthState.SessionReady
                        } catch (e: Exception) {
                            setAuthError("Login successful but failed to get user ID: ${e.message}")
                        }
                    } ?: setAuthError("No token received")
                } else {
                    setAuthError("Login failed. Check credentials.")
                }
            } catch (e: Exception) {
                setAuthError(e.localizedMessage ?: "Unknown error")
            }
        }
    }


    fun register(name: String, email: String, pass: String, role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.register(name, email, pass, role)
                if (response.isSuccessful) {
                    _authState.value = AuthState.RegistrationSuccess
                } else {
                    _authState.value = AuthState.Error("Registration failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }


    fun registerFcmToken(token: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val response = repository.registerToken(userId, token)
                if (response.isSuccessful) {
                    // Token registered successfully → session is ready
                    setSessionReady()
                } else {
                    setAuthError("Failed to register FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                setAuthError("Error registering FCM token: ${e.localizedMessage}")
            }
        }
    }



    fun loadUserProfile(userId: UUID) {
        viewModelScope.launch {
            try {
                userProfile.value = repository.getUser(userId)
            } catch (e: Exception) {
                errorMessage.value = "Failed to load profile: ${e.localizedMessage}"
            }
        }
    }

    fun loadAdherenceSummary(userId: UUID) {
        viewModelScope.launch {
            try {
                adherenceSummary.value = repository.getAdherenceSummary(userId)
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    fun loadDevices(userId: UUID) {
        viewModelScope.launch {
            try {
                val device = repository.getDevice(userId)
                devices.value = listOf(device)
            } catch (e: Exception) {
                devices.value = emptyList()
            }
        }
    }

    fun loadSchedules(userId: UUID) {
        viewModelScope.launch {
            try {
                schedules.value = repository.getSchedules(userId)
            } catch (e: Exception) {
                schedules.value = emptyList()
            }
        }
    }

    fun loadMedlogs(userId: UUID) {
        viewModelScope.launch {
            try {
                medlogs.value = repository.getMedlogs(userId)
            } catch (e: Exception) {
                medlogs.value = emptyList()
            }
        }
    }

    fun createSchedule(pillname: String, doseTime: String, repeatDays: Int, onSuccess: () -> Unit) {
        val userId = currentUserId ?: return
        val deviceId = devices.value.firstOrNull()?.chip_id ?: "DEFAULT_CHIP"

        viewModelScope.launch {
            try {
                val request = ScheduleRequest(
                    pillname = pillname,
                    dose_time = doseTime,
                    repeat_days = repeatDays,
                    user_id = userId,
                    device_id = deviceId
                )
                repository.createSchedule(userId, request)
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = "Failed to create schedule: ${e.localizedMessage}"
            }
        }
    }

    fun updateSchedule(scheduleId: UUID, pillname: String, doseTime: String, repeatDays: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val update = mutableMapOf<String, Any?>()
                update["pillname"] = pillname
                update["dose_time"] = doseTime
                update["repeat_days"] = repeatDays

                repository.updateSchedule(scheduleId, update)
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = "Failed to update schedule: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSchedule(scheduleId: UUID) {
        viewModelScope.launch {
            try {
                val response = repository.deleteSchedule(scheduleId)
                if (response.isSuccessful) {
                    refreshDashboard()
                } else {
                    errorMessage.value = "Failed to delete schedule: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage.value = "Error deleting schedule: ${e.localizedMessage}"
            }
        }
    }

    fun refreshDashboard() {
        currentUserId?.let {
            loadUserProfile(it)
            loadDevices(it)
            loadAdherenceSummary(it)
            loadSchedules(it)
            loadMedlogs(it)
        }
    }

    fun logout() {
        currentUserId = null
        _authState.value = AuthState.Idle
        userProfile.value = null
        devices.value = emptyList()
        schedules.value = emptyList()
        medlogs.value = emptyList()
        adherenceSummary.value = null
    }
}
