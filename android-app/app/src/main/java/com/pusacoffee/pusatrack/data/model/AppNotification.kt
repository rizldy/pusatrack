package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

enum class NotificationType(val displayLabel: String) {
    OUT_OF_STOCK("Stok habis"),
    LOW_STOCK("Stok menipis"),
    ORDER_PENDING("Pesanan berjalan"),
    ORDER_RECEIVED("Pesanan diterima")
}

data class AppNotification(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: NotificationType,
    @SerializedName("title")
    val title: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("time_label")
    val timeLabel: String,
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("target_ingredient_id")
    val targetIngredientId: Int? = null,
    @SerializedName("target_order_id")
    val targetOrderId: Int? = null
)

data class DeviceTokenRequest(
    @SerializedName("fcm_token")
    val fcmToken: String,
    @SerializedName("platform")
    val platform: String = "android"
)

data class ReportExportRequest(
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    @SerializedName("sheets")
    val sheets: List<String>
)

data class ReportExportResponse(
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("download_url")
    val downloadUrl: String
)
