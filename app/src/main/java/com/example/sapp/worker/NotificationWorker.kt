package com.example.sapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.BackoffPolicy
import androidx.work.WorkRequest
import com.example.sapp.data.network.RetrofitClient
import java.util.UUID
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userIdString = inputData.getString("USER_ID") ?: return Result.failure()
        val userId = try {
            UUID.fromString(userIdString)
        } catch (e: Exception) {
            return Result.failure()
        }

        return try {
            // Fetch API service via RetrofitClient
            val apiService = RetrofitClient.getApiService(applicationContext)
            val notifications = apiService.getNotifications(userId)

            // Get shared preferences to track seen notifications
            val sharedPrefs = applicationContext.getSharedPreferences("notification_tracker", Context.MODE_PRIVATE)
            val seenIds = sharedPrefs.getStringSet("seen_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            var newCount = 0
            if (notifications.isNotEmpty()) {
                notifications.forEach { notification ->
                    val idStr = notification.id.toString()
                    
                    // Only show if we haven't seen this ID before
                    if (!seenIds.contains(idStr)) {
                        showNotification(
                            notification.id.hashCode(),
                            "Medication Alert",
                            notification.message
                        )
                        seenIds.add(idStr)
                        newCount++
                    }
                }
            }

            // Save updated seen IDs (keep only last 100 to prevent growth)
            if (newCount > 0) {
                val listToSave = if (seenIds.size > 100) seenIds.toList().takeLast(100).toSet() else seenIds
                sharedPrefs.edit().putStringSet("seen_ids", listToSave).apply()
            }

            Result.success()
        } catch (e: Exception) {
            // Retry later if network fails
            Result.retry()
        }
    }

    private fun showNotification(notifId: Int, title: String, message: String) {
        val channelId = "MED_REMINDER_CHANNEL"

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Medication Reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "Notifications for medication schedules"
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        try {
            // Check permission for Android 13+
            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                NotificationManagerCompat.from(applicationContext).notify(notifId, builder.build())
            }
        } catch (e: SecurityException) {
            // Log permission error if necessary
        }
    }

    companion object {
        fun startPeriodicWork(context: Context, userId: UUID) {
            val data = workDataOf("USER_ID" to userId.toString())

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "NotificationCheck",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
