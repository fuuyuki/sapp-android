package com.example.sapp.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sapp.AppRepository
import com.example.sapp.data.local.dataStore
import com.example.sapp.data.model.*
import com.example.sapp.ui.state.AuthState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: AppRepository,
    context: Context,
) : ViewModel() {

    // Use applicationContext to avoid leaking Activity context
    private val appContext = context.applicationContext

    val devices = MutableStateFlow<List<DeviceOut>>(emptyList())
    val schedules = MutableStateFlow<List<ScheduleOut>>(emptyList())
    val medlogs = MutableStateFlow<List<MedlogOut>>(emptyList())
    val userProfile = MutableStateFlow<UserOut?>(null)
    val adherenceSummary = MutableStateFlow<AdherenceSummaryResponse?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    private val languageKey = stringPreferencesKey("app_language")
    private val tokenKey = stringPreferencesKey("jwt_token")

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUserId = MutableStateFlow<UUID?>(null)
    val userId: StateFlow<UUID?> = _currentUserId.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    val selectedPatient = MutableStateFlow<UserOut?>(null)
    val patients = MutableStateFlow<List<UserOut>>(emptyList())

    // --- Language State ---
    val currentLanguage: StateFlow<String> = appContext.dataStore.data
        .map { it[languageKey] ?: "id" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "id")

    init {
        // Apply saved language on startup
        viewModelScope.launch {
            try {
                val savedLang = appContext.dataStore.data.first()[languageKey] ?: "id"
                val currentLocales = AppCompatDelegate.getApplicationLocales()

                if (currentLocales.toLanguageTags() != savedLang) {
                    val appLocale = LocaleListCompat.forLanguageTags(savedLang)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            } catch (_: Exception) {
                // Ignore initialization errors
            }
        }
        checkSession()
    }

    fun setAuthError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun setSessionReady() {
        _authState.value = AuthState.SessionReady
    }

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val meResponse = repository.getMe()
                _currentUserId.value = meResponse.user_id

                val user = repository.getUser(meResponse.user_id)
                _userRole.value = user.role
                userProfile.value = user

                refreshDashboard()
                _authState.value = AuthState.SessionReady
            } catch (_: Exception) {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun changeLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                // 1. Save to DataStore
                appContext.dataStore.edit { it[languageKey] = languageCode }
                // 2. Apply globally across the app
                val appLocale = LocaleListCompat.forLanguageTags(languageCode)
                AppCompatDelegate.setApplicationLocales(appLocale)
            } catch (_: Exception) {
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
                        appContext.dataStore.edit { it[tokenKey] = token }

                        try {
                            val meResponse = repository.getMe()
                            _currentUserId.value = meResponse.user_id
                            val user = repository.getUser(meResponse.user_id)
                            _userRole.value = user.role
                            userProfile.value = user

                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val token = task.result
                                        registerFcmToken(token)
                                    }
                                }
                            refreshDashboard()
                            _authState.value = AuthState.SessionReady
                        } catch (e: Exception) {
                            setAuthError("Login successful but failed to get user data: ${e.message}")
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

    fun registerFcmToken(fcmToken: String) {
        val uId = _currentUserId.value ?: return
        viewModelScope.launch {
            try {
                val response = repository.registerToken(uId, fcmToken)
                if (response.isSuccessful) {
                    setSessionReady()
                } else {
                    setAuthError("Failed to register FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                setAuthError("Error registering FCM token: ${e.localizedMessage}")
            }
        }
    }

    fun loadUserProfile(uId: UUID) {
        viewModelScope.launch {
            try {
                userProfile.value = repository.getUser(uId)
            } catch (e: Exception) {
                errorMessage.value = "Failed to load profile: ${e.localizedMessage}"
            }
        }
    }

    fun loadAdherenceSummary(uId: UUID) {
        viewModelScope.launch {
            try {
                adherenceSummary.value = repository.getAdherenceSummary(uId)
            } catch (_: Exception) {
            }
        }
    }

    fun loadDevices(uId: UUID, role: String) {
        viewModelScope.launch {
            try {
                val result: List<DeviceOut> = when (role) {
                    "patient" -> repository.getDevicesByPatient(uId)
                    "caretaker" -> repository.getDevicesByCaretaker(uId)
                    else -> repository.getDevices(uId)
                }
                devices.value = result
            } catch (_: Exception) {
                devices.value = emptyList()
            }
        }
    }

    fun loadPatients() {
        val caretakerId = _currentUserId.value ?: return
        if (_userRole.value != "caretaker") return
        viewModelScope.launch {
            try {
                patients.value = repository.getPatientsByCaretaker(caretakerId)
            } catch (_: Exception) {
                patients.value = emptyList()
            }
        }
    }

    fun loadPatientData(patientId: UUID) {
        val caretakerId = _currentUserId.value ?: return

        selectedPatient.value = null
        devices.value = emptyList()
        schedules.value = emptyList()
        medlogs.value = emptyList()
        adherenceSummary.value = null

        viewModelScope.launch {
            try {
                selectedPatient.value = repository.getUser(patientId)
                loadAdherenceSummary(patientId)
            } catch (_: Exception) {
                errorMessage.value = "Failed to load patient profile"
            }

            try {
                devices.value = repository.getDevicesByPatient(patientId)
            } catch (_: Exception) {
                devices.value = emptyList()
            }

            try {
                schedules.value = repository.getSchedulesForPatient(caretakerId, patientId)
            } catch (_: Exception) {
                schedules.value = emptyList()
            }

            try {
                medlogs.value = repository.getMedlogsByCaretakerForPatient(caretakerId, patientId)
            } catch (_: Exception) {
                medlogs.value = emptyList()
            }
        }
    }

    fun loadSchedules(uId: UUID) {
        viewModelScope.launch {
            try {
                schedules.value = repository.getSchedules(uId)
            } catch (_: Exception) {
                schedules.value = emptyList()
            }
        }
    }

    fun loadMedlogs(uId: UUID) {
        viewModelScope.launch {
            try {
                medlogs.value = repository.getMedlogs(uId)
            } catch (_: Exception) {
                medlogs.value = emptyList()
            }
        }
    }

    fun createSchedule(pillname: String, doseTime: String, repeatDays: Int, onSuccess: () -> Unit) {
        val uId = _currentUserId.value ?: return
        val deviceId = devices.value.firstOrNull()?.chip_id ?: "DEFAULT_CHIP"

        viewModelScope.launch {
            try {
                val request = ScheduleRequest(
                    pillname = pillname,
                    dose_time = doseTime,
                    repeat_days = repeatDays,
                    patient_id = uId,
                    device_id = deviceId
                )
                repository.createSchedule(uId, request)
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = "Failed to create schedule: ${e.localizedMessage}"
            }
        }
    }

    fun createScheduleForPatient(
        patientId: UUID,
        pillname: String,
        doseTime: String,
        repeatDays: Int,
        deviceId: String = "DEFAULT_CHIP",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = ScheduleRequest(pillname, doseTime, repeatDays, patientId, deviceId)
                repository.createSchedule(patientId, request)
                onSuccess()
            } catch (e: Exception) {
                errorMessage.value = "Failed to create schedule: ${e.message}"
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

                val pId = selectedPatient.value?.id
                if (pId != null) {
                    loadPatientData(pId)
                } else {
                    refreshDashboard()
                }

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
                    val pId = selectedPatient.value?.id
                    if (pId != null) {
                        loadPatientData(pId)
                    } else {
                        refreshDashboard()
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Delete failed: ${e.localizedMessage}"
            }
        }
    }

    fun refreshDashboard() {
        _currentUserId.value?.let { id ->
            val role = userRole.value ?: "patient"
            loadUserProfile(id)
            loadDevices(id, role)
            loadAdherenceSummary(id)
            loadSchedules(id)
            loadMedlogs(id)
            if (role == "caretaker") {
                loadPatients()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                appContext.dataStore.edit { it.remove(tokenKey) }
            } catch (_: Exception) {
            }
            _currentUserId.value = null
            _userRole.value = null
            _authState.value = AuthState.Idle
            userProfile.value = null
            devices.value = emptyList()
            schedules.value = emptyList()
            medlogs.value = emptyList()
            selectedPatient.value = null
            patients.value = emptyList()
        }
    }
}
