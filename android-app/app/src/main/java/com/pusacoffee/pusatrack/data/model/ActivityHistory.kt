package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

enum class ActivityType(val apiValue: String, val displayLabel: String) {
    @SerializedName("SALE")
    SALE("SALE", "Penjualan"),

    @SerializedName("STOCK")
    STOCK("STOCK", "Stok"),

    @SerializedName("ORDER")
    ORDER("ORDER", "Pesanan"),

    @SerializedName("EXPENSE")
    EXPENSE("EXPENSE", "Biaya")
}

data class ActivityHistoryItem(
    @SerializedName("activity_id")
    val activityId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("activity_type")
    val activityType: ActivityType,
    @SerializedName("title")
    val title: String,
    @SerializedName("subtitle")
    val subtitle: String,
    @SerializedName("amount_or_delta")
    val amountOrDelta: String,
    @SerializedName("is_negative_delta")
    val isNegativeDelta: Boolean = false,
    @SerializedName("timestamp_millis")
    val timestampMillis: Long,
    @SerializedName("date_label")
    val dateLabel: String,
    @SerializedName("time_label")
    val timeLabel: String,
    @SerializedName("reference_sale_id")
    val referenceSaleId: Int? = null
)
