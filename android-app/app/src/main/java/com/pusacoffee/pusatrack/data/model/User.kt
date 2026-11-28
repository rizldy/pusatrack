package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("is_active")
    val isActive: Boolean = true
)

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponseData(
    @SerializedName("user")
    val user: User
)
