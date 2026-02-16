// com.example.sapp.data.model.Models.kt
package com.example.sapp.data.model

import java.util.UUID

// 1. Users
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "patient"
)

data class RegisterResponse(
    val id: String,
    val email: String,
    val message: String
)

data class UserOut(
    val id: UUID,
    val name: String,
    val email: String,
    val role: String
)

data class MeResponse(
    val user_id: UUID
)

data class LoginResponse(val access_token: String, val token_type: String)

// Adherence Summary
data class NextDoseInfo(
    val next_dose: String?
)

data class AdherenceSummaryResponse(
    val user_id: String,
    val adherence_streak: Int,
    val next_dose: NextDoseInfo?,
    val weekly_adherence: Double
)

// 2. Devices
data class DeviceRequest(
    val name: String,
    val chip_id: String,
    val user_id: UUID,
    val api_key: String,
    val status: String = "offline"
)

data class DeviceOut(
    val name: String,
    val chip_id: String,
    val status: String,
    val user_id: UUID,
    val last_seen: String?,
    val api_key: String
)

// 3. Schedules
data class ScheduleRequest(
    val pillname: String,
    val dose_time: String,
    val repeat_days: Int,
    val user_id: UUID,
    val device_id: String
)

data class ScheduleOut(
    val id: UUID,
    val pillname: String,
    val dose_time: String,
    val repeat_days: Int,
    val user_id: UUID,
    val device_id: String
)

// 4. Medlogs
data class MedlogRequest(
    val pillname: String,
    val scheduled_time: String,
    val status: String,
    val user_id: UUID,
    val device_id: String
)

data class MedlogOut(
    val id: UUID,
    val pillname: String,
    val scheduled_time: String,
    val status: String,
    val user_id: UUID,
    val device_id: String
)

// 5. Notifications
data class NotificationOut(
    val id: UUID,
    val message: String,
    val device_id: String,
    val user_id: UUID,
    val created_at: String
)

data class MessageResponse(val message: String)

// FCM Token registration
data class TokenRegisterRequest(
    val user_id: UUID,
    val token: String
)
