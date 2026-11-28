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
    val isActive: Boolean = true,
    @SerializedName("created_at")
    val createdAtLabel: String = "1 Nov 2026"
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

data class CreateUserRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class UpdateUserRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("username")
    val username: String
)

data class ResetPasswordRequest(
    @SerializedName("new_password")
    val newPassword: String,
    @SerializedName("current_user_password")
    val currentUserPassword: String
)

data class UpdateUserStatusRequest(
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("current_user_password")
    val currentUserPassword: String? = null
)
