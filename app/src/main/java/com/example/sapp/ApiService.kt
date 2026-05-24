// com.example.sapp.data.api.ApiService.kt
package com.example.sapp

import com.example.sapp.data.model.*
import retrofit2.Response
import retrofit2.http.*
import java.util.UUID

interface ApiService {
    // --- Auth & Registration ---
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("me")
    suspend fun getMe(): MeResponse

    // --- Users ---
    @GET("users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: UUID): UserOut

    @PUT("users/{user_id}")
    suspend fun updateUser(@Path("user_id") userId: UUID, @Body update: Map<String, @JvmSuppressWildcards Any?>): UserOut

    // --- Adherence Summary ---
    @GET("users/{user_id}/adherence-summary")
    suspend fun getAdherenceSummary(@Path("user_id") userId: UUID): AdherenceSummaryResponse

    @GET("caretakers/{caretaker_id}/patients")
    suspend fun getPatientsByCaretaker(@Path("caretaker_id") caretakerId: UUID): List<UserOut>


    // --- Devices ---
    @GET("devices")
    suspend fun getDevices(@Query("user_id") userId: UUID): List<DeviceOut>

    @GET("devices")
    suspend fun getDevicesByPatient(@Query("patient_id") patientId: UUID): List<DeviceOut>

    @GET("devices")
    suspend fun getDevicesByCaretaker(@Query("caretaker_id") caretakerId: UUID): List<DeviceOut>

    // --- Schedules ---
    @GET("schedules/{user_id}")
    suspend fun getSchedules(@Path("user_id") userId: UUID): List<ScheduleOut>

    @GET("schedules/{caretaker_id}/{patient_id}")
    suspend fun getSchedulesForPatient(
        @Path("caretaker_id") caretakerId: UUID,
        @Path("patient_id") patientId: UUID
    ): List<ScheduleOut>

    @POST("schedules/{user_id}")
    suspend fun createSchedule(@Path("user_id") userId: UUID, @Body schedule: ScheduleRequest): ScheduleOut

    @PUT("schedules/{schedule_id}")
    suspend fun updateSchedule(@Path("schedule_id") scheduleId: UUID, @Body update: Map<String, @JvmSuppressWildcards Any?>): ScheduleOut

    @DELETE("schedules/{schedule_id}")
    suspend fun deleteSchedule(@Path("schedule_id") scheduleId: UUID): Response<MessageResponse>

    // --- Medlogs ---
    @GET("medlogs/{user_id}")
    suspend fun getMedlogs(@Path("user_id") userId: UUID): List<MedlogOut>

    // --- FCM Tokens ---
    @POST("register_token")
    suspend fun registerToken(@Body request: TokenRegisterRequest): Response<MessageResponse>
}
