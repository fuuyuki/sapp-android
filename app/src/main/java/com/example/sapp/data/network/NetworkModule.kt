package com.example.sapp.data.network

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sapp.ApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.sapp.data.local.dataStore

class AuthInterceptor(
    private val tokenProvider: suspend () -> String?
) : Interceptor {

    constructor(context: Context) : this({
        val tokenKey = stringPreferencesKey("jwt_token")
        context.applicationContext.dataStore.data.first()[tokenKey]
    })

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking { tokenProvider() }

        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrEmpty()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

/**
 * Logs request/response headers only, never bodies. Bodies can contain the
 * patient JWT (login response) or raw JPEG bytes (evidence upload), so they
 * must not reach logcat. Authorization is redacted as well.
 */
private class SafeHttpLoggingInterceptor : Interceptor {
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
        redactHeader("Authorization")
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response =
        logger.intercept(chain)
}

object RetrofitClient {
    fun getApiService(context: Context): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(SafeHttpLoggingInterceptor())
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://sap.protofylabs.web.id/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
