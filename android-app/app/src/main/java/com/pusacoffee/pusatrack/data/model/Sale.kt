package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

enum class PaymentMethod(val apiValue: String, val displayLabel: String) {
    @SerializedName("CASH")
    CASH("CASH", "Tunai"),

    @SerializedName("QRIS")
    QRIS("QRIS", "QRIS")
}

enum class SaleStatus(val apiValue: String, val displayLabel: String) {
    @SerializedName("CONFIRMED")
    CONFIRMED("CONFIRMED", "Terkonfirmasi")
}

data class SaleDetail(
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("selling_price_snapshot")
    val sellingPriceSnapshot: Long,
    @SerializedName("hpp_snapshot")
    val hppSnapshot: Long,
    @SerializedName("subtotal")
    val subtotal: Long
)

data class StockEffect(
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("ingredient")
    val ingredientName: String,
    @SerializedName("used")
    val usedQuantity: Int,
    @SerializedName("unit")
    val usageUnit: String,
    @SerializedName("stock_after")
    val stockAfter: Int
)

data class Sale(
    @SerializedName("sale_id")
    val saleId: Int,
    @SerializedName("sale_number")
    val saleNumber: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("payment_method")
    val paymentMethod: PaymentMethod,
    @SerializedName("total_amount")
    val totalAmount: Long,
    @SerializedName("amount_received")
    val amountReceived: Long,
    @SerializedName("change_amount")
    val changeAmount: Long,
    @SerializedName("status")
    val status: SaleStatus = SaleStatus.CONFIRMED,
    @SerializedName("confirmed_at_millis")
    val confirmedAtMillis: Long,
    @SerializedName("confirmed_date_label")
    val confirmedDateLabel: String,
    @SerializedName("confirmed_time_label")
    val confirmedTimeLabel: String,
    @SerializedName("details")
    val details: List<SaleDetail>,
    @SerializedName("stock_effects")
    val stockEffects: List<StockEffect>
)

data class CreateSaleItemRequest(
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("quantity")
    val quantity: Int
)

data class CreateSaleRequest(
    @SerializedName("payment_method")
    val paymentMethod: String,
    @SerializedName("amount_received")
    val amountReceived: Long,
    @SerializedName("items")
    val items: List<CreateSaleItemRequest>
)

data class DashboardSummary(
    @SerializedName("total_sales")
    val totalSales: Long,
    @SerializedName("transaction_count")
    val transactionCount: Int,
    @SerializedName("cash_sales")
    val cashSales: Long,
    @SerializedName("qris_sales")
    val qrisSales: Long
)
