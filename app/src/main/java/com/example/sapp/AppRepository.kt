// com.example.sapp.data.repository.AppRepository.kt
package com.example.sapp.data.repository

import com.example.sapp.data.api.ApiService
import com.example.sapp.data.model.*
import java.util.UUID

class AppRepository(private val apiService: ApiService) {
    suspend fun login(e: String, p: String) =
        apiService.login(LoginRequest(email = e, password = p))

    suspend fun register(name: String, email: String, pass: String, role: String) =
        apiService.register(RegisterRequest(name = name, email = email, password = pass, role = role))

    suspend fun getMe() = apiService.getMe()

    suspend fun getUser(userId: UUID) = apiService.getUser(userId)

    suspend fun getAdherenceSummary(userId: UUID) = apiService.getAdherenceSummary(userId)

    suspend fun getDevice(userId: UUID) = apiService.getDevice(userId)

    suspend fun getSchedules(userId: UUID) = apiService.getSchedules(userId)

    suspend fun createSchedule(userId: UUID, schedule: ScheduleRequest) = 
        apiService.createSchedule(userId, schedule)

    suspend fun updateSchedule(scheduleId: UUID, update: Map<String, Any?>) =
        apiService.updateSchedule(scheduleId, update)

    suspend fun deleteSchedule(scheduleId: UUID) = apiService.deleteSchedule(scheduleId)

    suspend fun getMedlogs(userId: UUID) = apiService.getMedlogs(userId)

    suspend fun getNotifications(userId: UUID): List<NotificationOut> {
        return apiService.getNotifications(userId)
    }

    suspend fun registerToken(userId: UUID, token: String) =
        apiService.registerToken(TokenRegisterRequest(userId, token))
}
