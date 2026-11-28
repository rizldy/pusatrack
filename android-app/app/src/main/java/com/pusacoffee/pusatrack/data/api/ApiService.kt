package com.pusacoffee.pusatrack.data.api

import com.pusacoffee.pusatrack.data.model.LoginRequest
import com.pusacoffee.pusatrack.data.model.LoginResponseData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponseData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<LoginResponseData>>
}
