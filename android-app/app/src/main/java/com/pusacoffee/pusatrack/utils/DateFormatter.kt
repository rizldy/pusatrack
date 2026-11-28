package com.pusacoffee.pusatrack.utils

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object DateFormatter {

    val WITA_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Makassar")

    private val SHORT_DAYS = arrayOf(
        "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"
    )

    private val SHORT_MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    /**
     * Formats date in WITA (Asia/Makassar) as "Rab, 18 Nov 2026".
     */
    fun formatDate(date: Date = Date()): String {
        val cal = Calendar.getInstance(WITA_TIMEZONE).apply { time = date }
        val dayName = SHORT_DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = SHORT_MONTHS[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$dayName, $dayOfMonth $monthName $year"
    }

    /**
     * Formats time in WITA as "09:38".
     */
    fun formatTime(date: Date = Date()): String {
        val cal = Calendar.getInstance(WITA_TIMEZONE).apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Returns Indonesian time-of-day greeting in WITA.
     */
    fun getGreeting(date: Date = Date()): String {
        val cal = Calendar.getInstance(WITA_TIMEZONE).apply { time = date }
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Selamat pagi,"
            in 11..14 -> "Selamat siang,"
            in 15..17 -> "Selamat sore,"
            else -> "Selamat malam,"
        }
    }
}
