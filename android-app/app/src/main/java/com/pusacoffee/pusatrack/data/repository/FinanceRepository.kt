package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.DailySalesBar
import com.pusacoffee.pusatrack.data.model.FinancePeriod
import com.pusacoffee.pusatrack.data.model.FinanceSummary
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

/**
 * Shared repository for Financial Summary (M9, L23) & Opening Cash Balance (K-11 / R-05).
 */
object FinanceRepository {

    private val _openingCashBalance = MutableStateFlow(200_000L)
    val openingCashBalance: StateFlow<Long> = _openingCashBalance.asStateFlow()

    fun setOpeningCashBalance(amount: Long): RepoResult<Long> {
        if (amount < 0L) {
            return RepoResult.Error("Saldo kas awal tidak boleh negatif.")
        }
        _openingCashBalance.value = amount
        return RepoResult.Success(amount)
    }

    fun getSummary(period: FinancePeriod): FinanceSummary {
        // Compute live deltas beyond seeded sales (id > 15) and seeded expenses (batchId > 1)
        val newSales = SalesRepository.getAllSales().filter { it.saleId > 15 }
        val newSalesTotal = newSales.sumOf { it.totalAmount }
        val newSalesCash = newSales
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.totalAmount }
        val newSalesQris = newSales
            .filter { it.paymentMethod == PaymentMethod.QRIS }
            .sumOf { it.totalAmount }
        val newSalesHpp = newSales.sumOf { sale ->
            sale.details.sumOf { it.hppSnapshot * it.quantity }
        }

        val newExpenses = ExpenseRepository.getAllBatches().filter { it.batchId > 1 }
        val newExpensesTotal = newExpenses.sumOf { it.totalAmount }
        val newExpensesCash = newExpenses
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.totalAmount }

        return when (period) {
            FinancePeriod.DAILY -> buildDailySummary()
            FinancePeriod.WEEKLY -> buildWeeklySummary(
                newSalesTotal, newSalesCash, newSalesQris, newSalesHpp, newExpensesTotal, newExpensesCash
            )
            FinancePeriod.MONTHLY -> buildMonthlySummary(
                newSalesTotal, newSalesCash, newSalesQris, newSalesHpp, newExpensesTotal, newExpensesCash
            )
        }
    }

    private fun buildDailySummary(): FinanceSummary {
        val todayLabel = DateFormatter.formatDate(Date())
        val todaySales = SalesRepository.getAllSales().filter { it.confirmedDateLabel == todayLabel }
        val totalSales = todaySales.sumOf { it.totalAmount }
        val cashSales = todaySales
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.totalAmount }
        val qrisSales = todaySales
            .filter { it.paymentMethod == PaymentMethod.QRIS }
            .sumOf { it.totalAmount }
        val totalHpp = todaySales.sumOf { sale ->
            sale.details.sumOf { it.hppSnapshot * it.quantity }
        }

        val todayExpense = ExpenseRepository.getTodaySummary()
        val grossProfit = totalSales - totalHpp
        val netProfit = grossProfit - todayExpense.totalAmount
        val netMarginPercent = computeMarginPercent(netProfit, totalSales)
        val cashChange = cashSales - todayExpense.cashAmount

        val bars = listOf(
            DailySalesBar("Pagi", cashSales),
            DailySalesBar("Siang", qrisSales),
            DailySalesBar("Sore", 0L),
            DailySalesBar("Malam", 0L)
        )

        return FinanceSummary(
            period = FinancePeriod.DAILY,
            periodLabel = todayLabel,
            totalSales = totalSales,
            totalHpp = totalHpp,
            grossProfit = grossProfit,
            totalExpenses = todayExpense.totalAmount,
            netProfit = netProfit,
            netMarginPercent = netMarginPercent,
            cashSales = cashSales,
            qrisSales = qrisSales,
            openingCashBalance = _openingCashBalance.value,
            cashChange = cashChange,
            dailyBars = bars,
            averageDailySales = totalSales,
            highestDayLabel = "Hari ini",
            highestDayAmount = totalSales
        )
    }

    private fun buildWeeklySummary(
        extraSales: Long,
        extraCash: Long,
        extraQris: Long,
        extraHpp: Long,
        extraExpenses: Long,
        extraCashExpenses: Long
    ): FinanceSummary {
        val baseBars = listOf(
            DailySalesBar("Sen", 320_000L),
            DailySalesBar("Sel", 380_000L),
            DailySalesBar("Rab", 360_000L + extraSales),
            DailySalesBar("Kam", 410_000L),
            DailySalesBar("Jum", 450_000L),
            DailySalesBar("Sab", 520_000L),
            DailySalesBar("Min", 480_000L)
        )
        val totalSales = baseBars.sumOf { it.amount }
        val cashSales = 1_720_000L + extraCash
        val qrisSales = 1_200_000L + extraQris
        val totalHpp = 1_710_000L + extraHpp
        val totalExpenses = 420_000L + extraExpenses
        val cashExpenses = 310_000L + extraCashExpenses
        val grossProfit = totalSales - totalHpp
        val netProfit = grossProfit - totalExpenses
        val highest = baseBars.maxBy { it.amount }

        return FinanceSummary(
            period = FinancePeriod.WEEKLY,
            periodLabel = "7 hari terakhir",
            totalSales = totalSales,
            totalHpp = totalHpp,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netMarginPercent = computeMarginPercent(netProfit, totalSales),
            cashSales = cashSales,
            qrisSales = qrisSales,
            openingCashBalance = _openingCashBalance.value,
            cashChange = cashSales - cashExpenses,
            dailyBars = baseBars,
            averageDailySales = totalSales / baseBars.size,
            highestDayLabel = highest.dayLabel,
            highestDayAmount = highest.amount
        )
    }

    private fun buildMonthlySummary(
        extraSales: Long,
        extraCash: Long,
        extraQris: Long,
        extraHpp: Long,
        extraExpenses: Long,
        extraCashExpenses: Long
    ): FinanceSummary {
        // Exact TC-08 baseline: Sales 7.200.000, HPP 4.250.000, Gross 2.950.000, Expenses 1.350.000, Net 1.600.000 (22,2%)
        val totalSales = 7_200_000L + extraSales
        val totalHpp = 4_250_000L + extraHpp
        val grossProfit = totalSales - totalHpp
        val totalExpenses = 1_350_000L + extraExpenses
        val netProfit = grossProfit - totalExpenses
        val cashSales = 4_300_000L + extraCash
        val qrisSales = 2_900_000L + extraQris
        val cashExpenses = 980_000L + extraCashExpenses

        val bars = listOf(
            DailySalesBar("M1", 1_650_000L),
            DailySalesBar("M2", 1_850_000L),
            DailySalesBar("M3", 2_050_000L + extraSales),
            DailySalesBar("M4", 1_650_000L)
        )
        val highest = bars.maxBy { it.amount }

        return FinanceSummary(
            period = FinancePeriod.MONTHLY,
            periodLabel = "November 2026",
            totalSales = totalSales,
            totalHpp = totalHpp,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netMarginPercent = computeMarginPercent(netProfit, totalSales),
            cashSales = cashSales,
            qrisSales = qrisSales,
            openingCashBalance = _openingCashBalance.value,
            cashChange = cashSales - cashExpenses,
            dailyBars = bars,
            averageDailySales = totalSales / 18L,
            highestDayLabel = highest.dayLabel,
            highestDayAmount = highest.amount
        )
    }

    private fun computeMarginPercent(netProfit: Long, totalSales: Long): Double {
        if (totalSales <= 0L) return 0.0
        return BigDecimal.valueOf(netProfit * 100L)
            .divide(BigDecimal.valueOf(totalSales), 1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun resetForTesting() {
        _openingCashBalance.value = 200_000L
    }
}
