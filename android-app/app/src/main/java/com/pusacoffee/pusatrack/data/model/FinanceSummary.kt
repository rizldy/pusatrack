package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class FinancePeriod(val apiValue: String, val displayLabel: String) {
    DAILY("DAILY", "Harian"),
    WEEKLY("WEEKLY", "Mingguan"),
    MONTHLY("MONTHLY", "Bulanan")
}

data class DailySalesBar(
    @SerializedName("day_label")
    val dayLabel: String,
    @SerializedName("amount")
    val amount: Long
)

data class FinanceSummary(
    @SerializedName("period")
    val period: FinancePeriod,
    @SerializedName("period_label")
    val periodLabel: String,
    @SerializedName("total_sales")
    val totalSales: Long,
    @SerializedName("total_hpp")
    val totalHpp: Long,
    @SerializedName("gross_profit")
    val grossProfit: Long,
    @SerializedName("total_expenses")
    val totalExpenses: Long,
    @SerializedName("net_profit")
    val netProfit: Long,
    @SerializedName("net_margin_percent")
    val netMarginPercent: Double,
    @SerializedName("cash_sales")
    val cashSales: Long,
    @SerializedName("qris_sales")
    val qrisSales: Long,
    @SerializedName("opening_cash_balance")
    val openingCashBalance: Long,
    @SerializedName("cash_change")
    val cashChange: Long,
    @SerializedName("daily_bars")
    val dailyBars: List<DailySalesBar>,
    @SerializedName("average_daily_sales")
    val averageDailySales: Long,
    @SerializedName("highest_day_label")
    val highestDayLabel: String,
    @SerializedName("highest_day_amount")
    val highestDayAmount: Long
) {
    fun formatNetMarginPercent(): String {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            decimalSeparator = ','
        }
        val df = DecimalFormat("0.0", symbols)
        return "${df.format(netMarginPercent)}%"
    }
}

data class CashBalanceRequest(
    @SerializedName("date")
    val date: String,
    @SerializedName("opening_balance")
    val openingBalance: Long
)

data class CashBalanceData(
    @SerializedName("date")
    val date: String,
    @SerializedName("opening_balance")
    val openingBalance: Long
)
