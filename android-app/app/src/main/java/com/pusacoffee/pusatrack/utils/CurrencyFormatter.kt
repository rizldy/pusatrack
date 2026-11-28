package com.pusacoffee.pusatrack.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,##0", symbols)

    fun format(amount: Long): String {
        val prefix = if (amount < 0) "-Rp " else "Rp "
        return prefix + formatter.format(abs(amount))
    }

    fun format(amount: Int): String = format(amount.toLong())

    fun format(amount: Double): String = format(amount.roundToLong())
}
