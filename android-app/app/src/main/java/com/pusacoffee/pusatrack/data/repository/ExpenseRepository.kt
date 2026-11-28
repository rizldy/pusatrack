package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.ExpenseBatch
import com.pusacoffee.pusatrack.data.model.ExpenseCategory
import com.pusacoffee.pusatrack.data.model.ExpenseItem
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

/**
 * Shared singleton immutable repository for Operational Expenses (M8, L24-L25).
 * No edit or delete operations exist (R-06).
 */
object ExpenseRepository {

    val categories: List<ExpenseCategory> = listOf(
        ExpenseCategory(1, "Es Batu"),
        ExpenseCategory(2, "Konsumsi"),
        ExpenseCategory(3, "Listrik & Air"),
        ExpenseCategory(4, "Perawatan Alat"),
        ExpenseCategory(5, "Operasional Lainnya")
    )

    private val _batches = MutableStateFlow(seedBatches())
    val batches: StateFlow<List<ExpenseBatch>> = _batches.asStateFlow()

    private var nextBatchId = 2

    fun getAllBatches(): List<ExpenseBatch> = _batches.value

    fun getTodaySummary(): TodayExpenseSummary {
        val todayLabel = DateFormatter.formatDate(Date())
        val todayBatches = _batches.value.filter { it.dateLabel == todayLabel }
        val total = todayBatches.sumOf { it.totalAmount }
        val cashTotal = todayBatches
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.totalAmount }
        val itemCount = todayBatches.sumOf { it.items.size }
        return TodayExpenseSummary(
            totalAmount = total,
            cashAmount = cashTotal,
            itemCount = itemCount
        )
    }

    /**
     * Records a batch of 1+ operational expense lines immutably and logs to ActivityHistory.
     */
    fun recordExpenses(
        paymentMethod: PaymentMethod,
        expenseDateMillis: Long,
        items: List<ExpenseItem>,
        userId: Int,
        userName: String
    ): RepoResult<ExpenseBatch> {
        if (items.isEmpty()) {
            return RepoResult.Error("Tambahkan minimal 1 baris pengeluaran.")
        }

        for (item in items) {
            if (item.categoryName.isBlank()) {
                return RepoResult.Error("Kategori pengeluaran wajib dipilih.")
            }
            if (item.amount <= 0L) {
                return RepoResult.Error("Nominal pengeluaran harus lebih dari Rp 0.")
            }
        }

        val date = Date(expenseDateMillis)
        val batch = ExpenseBatch(
            batchId = nextBatchId++,
            userId = userId,
            userName = userName,
            paymentMethod = paymentMethod,
            expenseDateMillis = expenseDateMillis,
            dateLabel = DateFormatter.formatDate(date),
            timeLabel = DateFormatter.formatTime(date),
            items = items.toList(),
            status = "SAVED"
        )

        _batches.value = listOf(batch) + _batches.value

        val categoriesText = items.map { it.categoryName }.distinct().joinToString(" & ")
        ActivityHistoryRepository.record(
            userId = userId,
            userName = userName,
            activityType = ActivityType.EXPENSE,
            title = "Pengeluaran dicatat",
            subtitle = "${paymentMethod.displayLabel} · $categoriesText",
            amountOrDelta = "-${CurrencyFormatter.format(batch.totalAmount)}",
            isNegativeDelta = true,
            timestampMillis = expenseDateMillis
        )

        return RepoResult.Success(batch)
    }

    fun resetForTesting() {
        _batches.value = seedBatches()
        nextBatchId = 2
    }

    data class TodayExpenseSummary(
        val totalAmount: Long,
        val cashAmount: Long,
        val itemCount: Int
    )

    private fun seedBatches(): List<ExpenseBatch> {
        val millis = Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 50)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val date = Date(millis)
        return listOf(
            ExpenseBatch(
                batchId = 1,
                userId = 1,
                userName = "Natashya Rosario",
                paymentMethod = PaymentMethod.CASH,
                expenseDateMillis = millis,
                dateLabel = DateFormatter.formatDate(date),
                timeLabel = "08:50",
                items = listOf(
                    ExpenseItem(1, "Es Batu", 25_000L, "2 kantong"),
                    ExpenseItem(2, "Konsumsi", 15_000L, "Air galon")
                ),
                status = "SAVED"
            )
        )
    }
}
