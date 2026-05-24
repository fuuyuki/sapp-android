// com.example.sapp.data.repository.AppRepository.kt
package com.example.sapp

//import com.example.sapp.data.api.ApiService
import com.example.sapp.data.model.*
import java.util.UUID

class AppRepository(private val apiService: ApiService) {
    suspend fun login(e: String, p: String) =
        apiService.login(LoginRequest(email = e, password = p))

    suspend fun register(name: String, email: String, pass: String, role: String) =
        apiService.register(RegisterRequest(name = name, email = email, password = pass, role = role))

    suspend fun getMe() = apiService.getMe()

    suspend fun getUser(userId: UUID) = apiService.getUser(userId)

    suspend fun getPatientsByCaretaker(caretakerId: UUID): List<UserOut> {return apiService.getPatientsByCaretaker(caretakerId)}

    suspend fun getAdherenceSummary(userId: UUID) = apiService.getAdherenceSummary(userId)

    suspend fun getDevices(userId: UUID): List<DeviceOut> =
        apiService.getDevices(userId)


    suspend fun getDevicesByPatient(patientId: UUID): List<DeviceOut> =
        apiService.getDevicesByPatient(patientId)

    suspend fun getDevicesByCaretaker(caretakerId: UUID): List<DeviceOut> =
        apiService.getDevicesByCaretaker(caretakerId)

    suspend fun getSchedules(userId: UUID) = apiService.getSchedules(userId)

    suspend fun getSchedulesForPatient(caretakerId: UUID, patientId: UUID) =
        apiService.getSchedulesForPatient(caretakerId, patientId)

    suspend fun createSchedule(userId: UUID, schedule: ScheduleRequest) = 
        apiService.createSchedule(userId, schedule)

    suspend fun updateSchedule(scheduleId: UUID, update: Map<String, Any?>) =
        apiService.updateSchedule(scheduleId, update)

    suspend fun deleteSchedule(scheduleId: UUID) = apiService.deleteSchedule(scheduleId)

    suspend fun getMedlogs(userId: UUID) = apiService.getMedlogs(userId)

    suspend fun getMedlogsByCaretakerForPatient(caretakerId: UUID, patientId: UUID): List<MedlogOut> {
        return apiService.getMedlogsByCaretakerForPatient(caretakerId, patientId)
    }

    suspend fun registerToken(userId: UUID, token: String) =
        apiService.registerToken(TokenRegisterRequest(userId, token))
}
