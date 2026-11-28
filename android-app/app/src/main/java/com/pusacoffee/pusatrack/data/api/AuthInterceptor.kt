package com.pusacoffee.pusatrack.data.api

import com.pusacoffee.pusatrack.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val username = sessionManager.getUsername()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (!username.isNullOrBlank()) {
            requestBuilder.header("Authorization", username)
        }

        return chain.proceed(requestBuilder.build())
    }
}
