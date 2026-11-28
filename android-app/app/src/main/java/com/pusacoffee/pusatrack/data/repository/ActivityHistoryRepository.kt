package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.ActivityHistoryItem
import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

/**
 * Immutable (append-only) Activity History repository (R-06 / TC-11).
 * Only exposes record() and read/filter queries — no update or delete operations exist.
 */
object ActivityHistoryRepository {

    private val _items = MutableStateFlow(seedActivities())
    val items: StateFlow<List<ActivityHistoryItem>> = _items.asStateFlow()

    private var nextActivityId = 100

    fun getAll(): List<ActivityHistoryItem> = _items.value

    fun record(
        userId: Int,
        userName: String,
        activityType: ActivityType,
        title: String,
        subtitle: String,
        amountOrDelta: String,
        isNegativeDelta: Boolean = false,
        timestampMillis: Long = System.currentTimeMillis(),
        referenceSaleId: Int? = null
    ): ActivityHistoryItem {
        val date = Date(timestampMillis)
        val entry = ActivityHistoryItem(
            activityId = nextActivityId++,
            userId = userId,
            userName = userName,
            activityType = activityType,
            title = title,
            subtitle = subtitle,
            amountOrDelta = amountOrDelta,
            isNegativeDelta = isNegativeDelta,
            timestampMillis = timestampMillis,
            dateLabel = DateFormatter.formatDate(date),
            timeLabel = DateFormatter.formatTime(date),
            referenceSaleId = referenceSaleId
        )
        _items.value = listOf(entry) + _items.value
        return entry
    }

    fun query(
        type: ActivityType? = null,
        userName: String? = null,
        fromMillis: Long? = null,
        toMillis: Long? = null,
        newestFirst: Boolean = true
    ): List<ActivityHistoryItem> {
        val filtered = _items.value.filter { item ->
            val matchesType = type == null || item.activityType == type
            val matchesUser = userName.isNullOrBlank() ||
                item.userName.equals(userName, ignoreCase = true)
            val matchesFrom = fromMillis == null || item.timestampMillis >= fromMillis
            val matchesTo = toMillis == null || item.timestampMillis <= toMillis
            matchesType && matchesUser && matchesFrom && matchesTo
        }
        return if (newestFirst) {
            filtered.sortedByDescending { it.timestampMillis }
        } else {
            filtered.sortedBy { it.timestampMillis }
        }
    }

    fun getDistinctUserNames(): List<String> =
        _items.value.map { it.userName }.distinct().sorted()

    fun resetForTesting() {
        _items.value = seedActivities()
        nextActivityId = 100
    }

    private fun seedActivities(): List<ActivityHistoryItem> {
        val todayBase = Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun timeToday(hour: Int, minute: Int, daysAgo: Int = 0): Long {
            return (todayBase.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }.timeInMillis
        }

        fun makeItem(
            id: Int,
            userId: Int,
            userName: String,
            type: ActivityType,
            title: String,
            subtitle: String,
            amountOrDelta: String,
            isNegative: Boolean,
            millis: Long,
            saleId: Int? = null
        ): ActivityHistoryItem {
            val d = Date(millis)
            return ActivityHistoryItem(
                activityId = id,
                userId = userId,
                userName = userName,
                activityType = type,
                title = title,
                subtitle = subtitle,
                amountOrDelta = amountOrDelta,
                isNegativeDelta = isNegative,
                timestampMillis = millis,
                dateLabel = DateFormatter.formatDate(d),
                timeLabel = DateFormatter.formatTime(d),
                referenceSaleId = saleId
            )
        }

        return listOf(
            makeItem(
                id = 6,
                userId = 1,
                userName = "Natashya Rosario",
                type = ActivityType.SALE,
                title = "Transaksi dicatat",
                subtitle = "TRX-0015 · Tunai · Kopi Susu ×2, Strawberry ×1",
                amountOrDelta = "Rp 30.000",
                isNegative = false,
                millis = timeToday(9, 38, daysAgo = 0),
                saleId = 15
            ),
            makeItem(
                id = 5,
                userId = 1,
                userName = "Natashya Rosario",
                type = ActivityType.SALE,
                title = "Transaksi dicatat",
                subtitle = "TRX-0014 · QRIS · Kopi Susu ×1, Strawberry ×1",
                amountOrDelta = "Rp 20.000",
                isNegative = false,
                millis = timeToday(9, 12, daysAgo = 0),
                saleId = 14
            ),
            makeItem(
                id = 4,
                userId = 1,
                userName = "Natashya Rosario",
                type = ActivityType.EXPENSE,
                title = "Pengeluaran dicatat",
                subtitle = "Tunai · Es Batu & Konsumsi",
                amountOrDelta = "-Rp 40.000",
                isNegative = true,
                millis = timeToday(8, 50, daysAgo = 0)
            ),
            makeItem(
                id = 3,
                userId = 2,
                userName = "Afdal Rizaldy",
                type = ActivityType.STOCK,
                title = "Stok disesuaikan",
                subtitle = "Susu UHT Full Cream · Selisih pemeriksaan fisik",
                amountOrDelta = "-300 ml",
                isNegative = true,
                millis = timeToday(17, 30, daysAgo = 1)
            ),
            makeItem(
                id = 2,
                userId = 2,
                userName = "Afdal Rizaldy",
                type = ActivityType.ORDER,
                title = "Pesanan PSN-014 dibuat",
                subtitle = "Pusat Pusa Coffee · 3 bahan baku",
                amountOrDelta = "PSN-014",
                isNegative = false,
                millis = timeToday(9, 15, daysAgo = 2)
            ),
            makeItem(
                id = 1,
                userId = 2,
                userName = "Afdal Rizaldy",
                type = ActivityType.STOCK,
                title = "Stok ditambah",
                subtitle = "Susu UHT Full Cream · Indomaret BTP (+3 kotak)",
                amountOrDelta = "+2.760 ml",
                isNegative = false,
                millis = timeToday(8, 20, daysAgo = 2)
            )
        )
    }
}
