package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

data class ExpenseCategory(
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String
)

data class ExpenseItem(
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("description")
    val description: String? = null
)

data class ExpenseBatch(
    @SerializedName("batch_id")
    val batchId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("payment_method")
    val paymentMethod: PaymentMethod,
    @SerializedName("expense_date_millis")
    val expenseDateMillis: Long,
    @SerializedName("date_label")
    val dateLabel: String,
    @SerializedName("time_label")
    val timeLabel: String,
    @SerializedName("items")
    val items: List<ExpenseItem>,
    @SerializedName("status")
    val status: String = "SAVED"
) {
    val totalAmount: Long
        get() = items.sumOf { it.amount }
}

data class CreateExpenseItemRequest(
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("description")
    val description: String? = null
)

data class CreateExpensesRequest(
    @SerializedName("payment_method")
    val paymentMethod: String,
    @SerializedName("expense_date")
    val expenseDate: String,
    @SerializedName("items")
    val items: List<CreateExpenseItemRequest>
)
