package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.LeadTimeFormatter
import com.pusacoffee.pusatrack.data.model.LeadTimeStats
import com.pusacoffee.pusatrack.data.model.OrderStatus
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.model.PurchaseOrder
import com.pusacoffee.pusatrack.data.model.PurchaseOrderDetail
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

/**
 * Shared singleton repository for Purchase Orders & Lead Time Monitoring (M7, L17-L22).
 */
object PurchaseOrderRepository {

    private val _orders = MutableStateFlow(seedOrders())
    val orders: StateFlow<List<PurchaseOrder>> = _orders.asStateFlow()

    private var nextOrderId = 15

    fun getAllOrders(): List<PurchaseOrder> = _orders.value

    fun getOrderById(orderId: Int): PurchaseOrder? =
        _orders.value.find { it.orderId == orderId }

    fun getActiveOrders(): List<PurchaseOrder> =
        _orders.value.filter { it.status == OrderStatus.ORDERED }

    fun getReceivedOrders(): List<PurchaseOrder> =
        _orders.value.filter { it.status == OrderStatus.RECEIVED }

    fun getLeadTimeStats(): LeadTimeStats {
        val completed = _orders.value.mapNotNull {
            if (it.status == OrderStatus.RECEIVED) it.leadTimeSeconds else null
        }
        if (completed.isEmpty()) {
            return LeadTimeStats(0L, 0L, 0L, 0)
        }
        return LeadTimeStats(
            avgSeconds = completed.sum() / completed.size,
            fastestSeconds = completed.min(),
            slowestSeconds = completed.max(),
            completedCount = completed.size
        )
    }

    /**
     * Creates a new central purchase order.
     * Validates: >= 1 item, qty >= 1, and ONLY SUPPLIED ingredients allowed.
     * Stock is NOT increased yet until receiveOrder() is confirmed.
     */
    fun createOrder(
        supplierName: String,
        orderDateMillis: Long,
        items: Map<Int, Int>,
        userId: Int,
        userName: String
    ): RepoResult<PurchaseOrder> {
        val trimmedSupplier = supplierName.trim()
        if (trimmedSupplier.isEmpty()) {
            return RepoResult.Error("Nama pemasok wajib diisi.")
        }
        if (items.isEmpty()) {
            return RepoResult.Error("Pilih minimal 1 bahan baku untuk dipesan.")
        }

        val details = mutableListOf<PurchaseOrderDetail>()
        for ((ingredientId, qty) in items) {
            if (qty < 1) {
                return RepoResult.Error("Jumlah kemasan tiap bahan minimal 1.")
            }
            val ingredient = InventoryRepository.getIngredientById(ingredientId)
                ?: return RepoResult.Error("Bahan baku tidak ditemukan.")
            if (ingredient.procurementType != ProcurementType.SUPPLIED) {
                return RepoResult.Error(
                    "\"${ingredient.ingredientName}\" adalah bahan Beli langsung. Gunakan Tambah stok di Detail Bahan."
                )
            }
            details.add(
                PurchaseOrderDetail(
                    ingredientId = ingredient.ingredientId,
                    ingredientName = ingredient.ingredientName,
                    purchaseUnit = ingredient.purchaseUnit,
                    usageUnit = ingredient.usageUnit,
                    quantity = qty,
                    packageSizeSnapshot = ingredient.packageSize,
                    packagePriceSnapshot = ingredient.packagePrice
                )
            )
        }

        val orderId = nextOrderId++
        val orderNumber = String.format("PSN-%03d", orderId)
        val orderDate = Date(orderDateMillis)
        val dateLabel = DateFormatter.formatDate(orderDate)
        val timeLabel = DateFormatter.formatTime(orderDate)

        val order = PurchaseOrder(
            orderId = orderId,
            orderNumber = orderNumber,
            supplierName = trimmedSupplier,
            userId = userId,
            userName = userName,
            orderDateMillis = orderDateMillis,
            orderDateLabel = dateLabel,
            orderTimeLabel = timeLabel,
            receivedAtMillis = null,
            receivedDateLabel = null,
            status = OrderStatus.ORDERED,
            leadTimeSeconds = null,
            details = details
        )

        _orders.value = listOf(order) + _orders.value

        ActivityHistoryRepository.record(
            userId = userId,
            userName = userName,
            activityType = ActivityType.ORDER,
            title = "Pesanan $orderNumber dibuat",
            subtitle = "$trimmedSupplier · ${details.size} bahan baku (${order.totalPackages} kemasan)",
            amountOrDelta = orderNumber,
            isNegativeDelta = false,
            timestampMillis = orderDateMillis
        )

        return RepoResult.Success(order)
    }

    /**
     * Confirms receipt of an ordered purchase order:
     * - Rejects already-RECEIVED orders (409 conflict rule)
     * - Validates confirmedMatch == true and receivedAtMillis >= orderDateMillis
     * - Computes leadTimeSeconds, increases stock & updates HPP in InventoryRepository
     * - Records PURCHASE stock movements and ActivityHistory entry
     */
    fun receiveOrder(
        orderId: Int,
        receivedAtMillis: Long,
        confirmedMatch: Boolean,
        userId: Int,
        userName: String
    ): RepoResult<PurchaseOrder> {
        val existing = getOrderById(orderId)
            ?: return RepoResult.Error("Pesanan tidak ditemukan.")

        if (existing.status == OrderStatus.RECEIVED) {
            return RepoResult.Error("Pesanan ${existing.orderNumber} sudah dikonfirmasi diterima.")
        }
        if (!confirmedMatch) {
            return RepoResult.Error("Centang konfirmasi kesesuaian jumlah dan ukuran kemasan terlebih dahulu.")
        }
        if (receivedAtMillis < existing.orderDateMillis) {
            return RepoResult.Error("Waktu terima tidak boleh sebelum waktu pemesanan.")
        }

        val leadTimeSeconds = (receivedAtMillis - existing.orderDateMillis) / 1000L
        val receivedDate = Date(receivedAtMillis)
        val receivedDateLabel = DateFormatter.formatDate(receivedDate)
        val receivedTimeLabel = DateFormatter.formatTime(receivedDate)
        val dateTimeLabel = "$receivedDateLabel · $receivedTimeLabel"

        // Update stock & HPP atomically
        InventoryRepository.receivePurchaseOrderStock(
            details = existing.details,
            orderNumber = existing.orderNumber,
            userName = userName,
            dateTimeLabel = dateTimeLabel
        )

        val updated = existing.copy(
            receivedAtMillis = receivedAtMillis,
            receivedDateLabel = receivedDateLabel,
            status = OrderStatus.RECEIVED,
            leadTimeSeconds = leadTimeSeconds
        )

        _orders.value = _orders.value.map {
            if (it.orderId == orderId) updated else it
        }

        ActivityHistoryRepository.record(
            userId = userId,
            userName = userName,
            activityType = ActivityType.ORDER,
            title = "Penerimaan ${existing.orderNumber} dikonfirmasi",
            subtitle = "Lead time ${LeadTimeFormatter.formatLong(leadTimeSeconds)} · ${existing.details.size} bahan diterima",
            amountOrDelta = LeadTimeFormatter.formatShort(leadTimeSeconds),
            isNegativeDelta = false,
            timestampMillis = receivedAtMillis
        )

        return RepoResult.Success(updated)
    }

    fun resetForTesting() {
        _orders.value = seedOrders()
        nextOrderId = 15
    }

    private fun seedOrders(): List<PurchaseOrder> {
        // Exact TC-07 order date: Mon 16 Nov 2026 09:15 WITA
        fun witaMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int, minute: Int): Long {
            return Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroBased)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val psn14OrderMillis = witaMillis(2026, Calendar.NOVEMBER, 16, 9, 15)
        val psn14Date = Date(psn14OrderMillis)

        val psn14 = PurchaseOrder(
            orderId = 14,
            orderNumber = "PSN-014",
            supplierName = "Pusat Pusa Coffee",
            userId = 2,
            userName = "Afdal Rizaldy",
            orderDateMillis = psn14OrderMillis,
            orderDateLabel = DateFormatter.formatDate(psn14Date),
            orderTimeLabel = DateFormatter.formatTime(psn14Date),
            receivedAtMillis = null,
            receivedDateLabel = null,
            status = OrderStatus.ORDERED,
            leadTimeSeconds = null,
            details = listOf(
                PurchaseOrderDetail(
                    ingredientId = 2,
                    ingredientName = "Bubuk Kopi",
                    purchaseUnit = "pak",
                    usageUnit = "g",
                    quantity = 8,
                    packageSizeSnapshot = 1000,
                    packagePriceSnapshot = 120_000L
                ),
                PurchaseOrderDetail(
                    ingredientId = 1,
                    ingredientName = "Bubuk Strawberry",
                    purchaseUnit = "bungkus",
                    usageUnit = "g",
                    quantity = 6,
                    packageSizeSnapshot = 500,
                    packagePriceSnapshot = 85_000L
                ),
                PurchaseOrderDetail(
                    ingredientId = 4,
                    ingredientName = "Cup Plastik 12 oz",
                    purchaseUnit = "pak",
                    usageUnit = "pcs",
                    quantity = 10,
                    packageSizeSnapshot = 50,
                    packagePriceSnapshot = 25_000L
                )
            )
        )

        fun receivedOrder(
            id: Int,
            receivedDay: Int,
            leadSeconds: Long,
            userName: String
        ): PurchaseOrder {
            val recMillis = witaMillis(2026, Calendar.NOVEMBER, receivedDay, 11, 15)
            val ordMillis = recMillis - (leadSeconds * 1000L)
            return PurchaseOrder(
                orderId = id,
                orderNumber = String.format("PSN-%03d", id),
                supplierName = "Pusat Pusa Coffee",
                userId = 3,
                userName = userName,
                orderDateMillis = ordMillis,
                orderDateLabel = DateFormatter.formatDate(Date(ordMillis)),
                orderTimeLabel = DateFormatter.formatTime(Date(ordMillis)),
                receivedAtMillis = recMillis,
                receivedDateLabel = DateFormatter.formatDate(Date(recMillis)),
                status = OrderStatus.RECEIVED,
                leadTimeSeconds = leadSeconds,
                details = listOf(
                    PurchaseOrderDetail(2, "Bubuk Kopi", "pak", "g", 8, 1000, 120_000L),
                    PurchaseOrderDetail(1, "Bubuk Strawberry", "bungkus", "g", 6, 500, 85_000L)
                )
            )
        }

        // 6 completed orders -> avg = 2 hari 4 jam (187200s), fastest = 1 hr 6 jam (108000s), slowest = 3 hr 4 jam (273600s)
        return listOf(
            psn14,
            receivedOrder(13, 14, 273_600L, "Raidah Syahirah"), // 3 hari 4 jam
            receivedOrder(12, 10, 194_400L, "Natashya Rosario"), // 2 hari 6 jam
            receivedOrder(11, 7, 187_200L, "Afdal Rizaldy"),     // 2 hari 4 jam
            receivedOrder(10, 4, 187_200L, "Raidah Syahirah"),   // 2 hari 4 jam
            receivedOrder(9, 2, 172_800L, "Natashya Rosario"),   // 2 hari
            receivedOrder(8, 1, 108_000L, "Afdal Rizaldy")       // 1 hari 6 jam
        )
    }
}
