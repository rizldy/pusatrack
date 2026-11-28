package com.pusacoffee.pusatrack.data.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("meta")
    val meta: ApiMeta? = null,
    @SerializedName("errors")
    val errors: Map<String, String>? = null
) {
    val isSuccess: Boolean
        get() = status.equals("success", ignoreCase = true)
}

data class ApiMeta(
    @SerializedName("page")
    val page: Int? = null,
    @SerializedName("per_page")
    val perPage: Int? = null,
    @SerializedName("total")
    val total: Int? = null
)
