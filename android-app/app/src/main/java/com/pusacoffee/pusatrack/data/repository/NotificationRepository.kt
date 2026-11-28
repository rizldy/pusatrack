package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.AppNotification
import com.pusacoffee.pusatrack.data.model.LeadTimeFormatter
import com.pusacoffee.pusatrack.data.model.NotificationType
import com.pusacoffee.pusatrack.data.model.OrderStatus
import com.pusacoffee.pusatrack.data.model.StockStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Operational Notification repository (M5, L09).
 * Dynamically evaluates low/out-of-stock ingredients (TC-12) and purchase order statuses.
 */
object NotificationRepository {

    private val readIds = mutableSetOf<String>()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val list = mutableListOf<AppNotification>()

        // 1. Evaluate ingredient stock alerts (TC-12: currentStock <= minimumStock)
        for (ing in InventoryRepository.getAllIngredients().filter { it.isActive }) {
            when (ing.stockStatus) {
                StockStatus.OUT -> {
                    val id = "stock_out_${ing.ingredientId}"
                    list.add(
                        AppNotification(
                            id = id,
                            type = NotificationType.OUT_OF_STOCK,
                            title = "${ing.ingredientName} habis",
                            message = "Stok 0 ${ing.usageUnit} (batas min. ${ing.minimumStockPackages} ${ing.purchaseUnit}). Segera lakukan pengadaan.",
                            timeLabel = "Stok Kritis",
                            isRead = id in readIds,
                            targetIngredientId = ing.ingredientId
                        )
                    )
                }

                StockStatus.LOW -> {
                    val id = "stock_low_${ing.ingredientId}"
                    list.add(
                        AppNotification(
                            id = id,
                            type = NotificationType.LOW_STOCK,
                            title = "Stok ${ing.ingredientName} menipis",
                            message = "Sisa ${ing.formatCurrentStockUsage()} (${ing.formatPackageBreakdown()}), batas min. ${ing.minimumStockPackages} ${ing.purchaseUnit}.",
                            timeLabel = "Perlu Distok",
                            isRead = id in readIds,
                            targetIngredientId = ing.ingredientId
                        )
                    )
                }

                StockStatus.SAFE -> {
                    // Clear read state if ingredient recovered to SAFE so future low-stock triggers as unread!
                    readIds.remove("stock_low_${ing.ingredientId}")
                    readIds.remove("stock_out_${ing.ingredientId}")
                }
            }
        }

        // 2. Evaluate active purchase orders
        for (order in PurchaseOrderRepository.getActiveOrders()) {
            val id = "order_pending_${order.orderId}"
            val elapsedSec = ((System.currentTimeMillis() - order.orderDateMillis) / 1000L)
                .coerceAtLeast(60L)
            list.add(
                AppNotification(
                    id = id,
                    type = NotificationType.ORDER_PENDING,
                    title = "Pesanan ${order.orderNumber} sedang berjalan",
                    message = "${order.supplierName} · berjalan ${LeadTimeFormatter.formatShort(elapsedSec)}. Konfirmasi saat barang tiba.",
                    timeLabel = order.orderDateLabel,
                    isRead = id in readIds,
                    targetOrderId = order.orderId
                )
            )
        }

        // 3. Most recent received order (informational)
        PurchaseOrderRepository.getReceivedOrders().firstOrNull()?.let { recent ->
            val id = "order_received_${recent.orderId}"
            list.add(
                AppNotification(
                    id = id,
                    type = NotificationType.ORDER_RECEIVED,
                    title = "Pesanan ${recent.orderNumber} telah diterima",
                    message = "Lead time ${LeadTimeFormatter.formatLong(recent.leadTimeSeconds ?: 0L)}. Stok telah diperbarui.",
                    timeLabel = recent.receivedDateLabel ?: recent.orderDateLabel,
                    isRead = true, // Historical received orders default to read
                    targetOrderId = recent.orderId
                )
            )
        }

        _notifications.value = list
        _unreadCount.value = list.count { !it.isRead }
    }

    fun markAsRead(notificationId: String) {
        readIds.add(notificationId)
        refresh()
    }

    fun markAllAsRead() {
        _notifications.value.forEach { readIds.add(it.id) }
        refresh()
    }

    fun resetForTesting() {
        readIds.clear()
        refresh()
    }
}
