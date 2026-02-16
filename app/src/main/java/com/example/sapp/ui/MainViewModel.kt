package com.example.sapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sapp.data.model.*
import com.example.sapp.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(private val repository: AppRepository) : ViewModel() {
    val devices = MutableStateFlow<List<DeviceOut>>(emptyList())
    val schedules = MutableStateFlow<List<ScheduleOut>>(emptyList())
    val medlogs = MutableStateFlow<List<MedlogOut>>(emptyList())
    val loginSuccess = MutableStateFlow(false)
    val userProfile = MutableStateFlow<UserOut?>(null)
    val adherenceSummary = MutableStateFlow<AdherenceSummaryResponse?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    private var currentUserId: UUID? = null

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            try {
                val meResponse = repository.getMe()
                val userId = meResponse.user_id
                currentUserId = userId
                loginSuccess.value = true
                refreshDashboard()
            } catch (e: Exception) {
                // Session not valid or not present
                loginSuccess.value = false
            }
        }
    }

    fun register(name: String, email: String, pass: String, role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.register(name, email, pass, role)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    errorMessage.value = "Registration failed: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage.value = e.localizedMessage
            }
        }
    }

    fun login(email: String, pass: String, onTokenReceived: suspend (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.login(email, pass)
                if (response.isSuccessful) {
                    response.body()?.access_token?.let { token ->
                        onTokenReceived(token)
                        
                        try {
                            val meResponse = repository.getMe()
                            val userId = meResponse.user_id
                            currentUserId = userId
                            loginSuccess.value = true
                            
                            refreshDashboard()
                        } catch (e: Exception) {
                            errorMessage.value = "Login successful but failed to get user ID: ${e.message}"
                        }
                    }
                } else {
                    errorMessage.value = "Login failed. Check credentials."
                }
            } catch (e: Exception) {
                errorMessage.value = e.localizedMessage
            }
        }
    }

    fun registerFcmToken(token: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                repository.registerToken(userId, token)
            } catch (e: Exception) {
                // Silently fail or log
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
        loginSuccess.value = false
        userProfile.value = null
        devices.value = emptyList()
        schedules.value = emptyList()
        medlogs.value = emptyList()
        adherenceSummary.value = null
    }
}
