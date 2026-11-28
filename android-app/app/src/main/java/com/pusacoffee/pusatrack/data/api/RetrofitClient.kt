package com.pusacoffee.pusatrack.data.api

import android.content.Context
import com.pusacoffee.pusatrack.BuildConfig
import com.pusacoffee.pusatrack.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var apiServiceInstance: ApiService? = null

    fun getApiService(context: Context): ApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: buildRetrofit(context.applicationContext)
                .create(ApiService::class.java)
                .also { apiServiceInstance = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val sessionManager = SessionManager(context)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
