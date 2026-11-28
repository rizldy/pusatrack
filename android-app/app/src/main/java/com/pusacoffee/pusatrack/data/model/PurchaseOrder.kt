package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

enum class OrderStatus(val apiValue: String, val displayLabel: String) {
    @SerializedName("ORDERED")
    ORDERED("ORDERED", "Berjalan"),

    @SerializedName("RECEIVED")
    RECEIVED("RECEIVED", "Diterima")
}

data class PurchaseOrderDetail(
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("ingredient_name")
    val ingredientName: String,
    @SerializedName("purchase_unit")
    val purchaseUnit: String,
    @SerializedName("usage_unit")
    val usageUnit: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("package_size_snapshot")
    val packageSizeSnapshot: Int,
    @SerializedName("package_price_snapshot")
    val packagePriceSnapshot: Long
) {
    val subtotal: Long
        get() = quantity * packagePriceSnapshot

    val totalUsageQuantity: Int
        get() = quantity * packageSizeSnapshot
}

data class PurchaseOrder(
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("order_number")
    val orderNumber: String,
    @SerializedName("supplier_name")
    val supplierName: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("order_date_millis")
    val orderDateMillis: Long,
    @SerializedName("order_date_label")
    val orderDateLabel: String,
    @SerializedName("order_time_label")
    val orderTimeLabel: String,
    @SerializedName("received_at_millis")
    val receivedAtMillis: Long? = null,
    @SerializedName("received_date_label")
    val receivedDateLabel: String? = null,
    @SerializedName("status")
    val status: OrderStatus,
    @SerializedName("lead_time_seconds")
    val leadTimeSeconds: Long? = null,
    @SerializedName("details")
    val details: List<PurchaseOrderDetail>
) {
    val totalAmount: Long
        get() = details.sumOf { it.subtotal }

    val totalPackages: Int
        get() = details.sumOf { it.quantity }
}

data class LeadTimeStats(
    @SerializedName("avg_seconds")
    val avgSeconds: Long,
    @SerializedName("fastest_seconds")
    val fastestSeconds: Long,
    @SerializedName("slowest_seconds")
    val slowestSeconds: Long,
    @SerializedName("completed_count")
    val completedCount: Int
)

data class CreatePurchaseOrderItemRequest(
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("quantity")
    val quantity: Int
)

data class CreatePurchaseOrderRequest(
    @SerializedName("supplier_name")
    val supplierName: String,
    @SerializedName("order_date")
    val orderDate: String,
    @SerializedName("items")
    val items: List<CreatePurchaseOrderItemRequest>
)

data class ReceiveOrderRequest(
    @SerializedName("received_at")
    val receivedAt: String,
    @SerializedName("confirmed_match")
    val confirmedMatch: Boolean
)

object LeadTimeFormatter {

    /**
     * Formats duration in Indonesian long form, e.g. "2 hari 26 menit" or "2 hari 4 jam".
     */
    fun formatLong(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val days = safeSeconds / 86_400L
        val hours = (safeSeconds % 86_400L) / 3_600L
        val minutes = (safeSeconds % 3_600L) / 60L

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days hari")
        if (hours > 0) parts.add("$hours jam")
        if (minutes > 0 || parts.isEmpty()) parts.add("$minutes menit")
        return parts.joinToString(" ")
    }

    /**
     * Formats duration in compact form, e.g. "3 hr 4 jam", "1 hr 6 jam", "2 hr 26 mnt".
     */
    fun formatShort(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val days = safeSeconds / 86_400L
        val hours = (safeSeconds % 86_400L) / 3_600L
        val minutes = (safeSeconds % 3_600L) / 60L

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days hr")
        if (hours > 0) parts.add("$hours jam")
        if (minutes > 0 && days == 0L || parts.isEmpty()) {
            parts.add("$minutes mnt")
        } else if (minutes > 0 && hours == 0L) {
            parts.add("$minutes mnt")
        }
        return parts.joinToString(" ")
    }

    /**
     * Builds WhatsApp order template strictly WITHOUT prices (per PRD & mockup v7).
     */
    fun buildWhatsAppMessage(order: PurchaseOrder, senderFullName: String): String {
        val lines = order.details.mapIndexed { index, item ->
            "${index + 1}. ${item.ingredientName}: ${item.quantity} ${item.purchaseUnit}"
        }.joinToString("\n")

        return buildString {
            append("Halo, saya $senderFullName dari Pusa Coffee BTP.\n\n")
            append("Pesanan stok ${order.orderNumber} (${order.orderDateLabel}):\n")
            append(lines)
            append("\n\nMohon konfirmasi ketersediaan dan estimasi pengiriman. Terima kasih.")
        }
    }
}
