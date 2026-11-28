package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.model.Sale
import com.pusacoffee.pusatrack.data.model.SaleDetail
import com.pusacoffee.pusatrack.data.model.SaleStatus
import com.pusacoffee.pusatrack.data.model.StockEffect
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date

/**
 * Shared singleton repository for Cashier Sales (L03-L06 & L08).
 * Confirmed sales are strictly immutable (R-06): no edit, cancel, or delete methods.
 */
object SalesRepository {

    private val _sales = MutableStateFlow(seedSales())
    val sales: StateFlow<List<Sale>> = _sales.asStateFlow()

    private var nextSaleId = 16

    fun getAllSales(): List<Sale> = _sales.value

    fun getSaleById(saleId: Int): Sale? =
        _sales.value.find { it.saleId == saleId }

    fun getLatestSale(): Sale? = _sales.value.firstOrNull()

    fun getTodaySummary(): TodaySalesSummary {
        val todayLabel = DateFormatter.formatDate(Date())
        val todaySales = _sales.value.filter { it.confirmedDateLabel == todayLabel }
        val cashTotal = todaySales
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.totalAmount }
        val qrisTotal = todaySales
            .filter { it.paymentMethod == PaymentMethod.QRIS }
            .sumOf { it.totalAmount }
        return TodaySalesSummary(
            totalAmount = cashTotal + qrisTotal,
            transactionCount = todaySales.size,
            cashAmount = cashTotal,
            qrisAmount = qrisTotal,
            latestSale = todaySales.firstOrNull() ?: _sales.value.firstOrNull()
        )
    }

    /**
     * Atomically validates and confirms a sale:
     * 1. Validates non-empty cart, qty >= 1, active products, and cash amountReceived >= total.
     * 2. Captures sellingPriceSnapshot & hppSnapshot (R-03).
     * 3. Deducts ingredients in InventoryRepository & writes SALE StockMovements (R-02).
     * 4. Records immutable Sale & ActivityHistory entry (R-06, R-07).
     */
    fun confirmSale(
        cartItems: Map<Int, Int>,
        paymentMethod: PaymentMethod,
        amountReceived: Long,
        userId: Int,
        userName: String
    ): RepoResult<Sale> {
        if (cartItems.isEmpty()) {
            return RepoResult.Error("Keranjang masih kosong. Pilih minimal 1 produk.")
        }

        val details = mutableListOf<SaleDetail>()
        val ingredientDeductions = linkedMapOf<Int, Int>()

        for ((productId, qty) in cartItems) {
            if (qty < 1) {
                return RepoResult.Error("Jumlah produk minimal 1.")
            }
            val product = ProductRepository.getProductById(productId)
                ?: return RepoResult.Error("Produk tidak ditemukan.")
            if (!product.isActive) {
                return RepoResult.Error("Produk \"${product.productName}\" sedang nonaktif dan tidak dapat dijual.")
            }

            val costSummary = ProductRepository.calculateProductCost(product)
            val subtotal = product.sellingPrice * qty
            details.add(
                SaleDetail(
                    productId = product.productId,
                    productName = product.productName,
                    quantity = qty,
                    sellingPriceSnapshot = product.sellingPrice,
                    hppSnapshot = costSummary.estimatedHpp,
                    subtotal = subtotal
                )
            )

            for (recipe in product.recipes) {
                val totalNeeded = recipe.quantityRequired * qty
                if (totalNeeded > 0) {
                    ingredientDeductions[recipe.ingredientId] =
                        (ingredientDeductions[recipe.ingredientId] ?: 0) + totalNeeded
                }
            }
        }

        val totalAmount = details.sumOf { it.subtotal }
        val effectiveReceived: Long
        val changeAmount: Long

        if (paymentMethod == PaymentMethod.CASH) {
            if (amountReceived < totalAmount) {
                return RepoResult.Error("Uang diterima kurang dari total pembayaran.")
            }
            effectiveReceived = amountReceived
            changeAmount = amountReceived - totalAmount
        } else {
            effectiveReceived = totalAmount
            changeAmount = 0L
        }

        val saleId = nextSaleId++
        val saleNumber = String.format("TRX-%04d", saleId)
        val nowMillis = System.currentTimeMillis()
        val nowDate = Date(nowMillis)
        val dateLabel = DateFormatter.formatDate(nowDate)
        val timeLabel = DateFormatter.formatTime(nowDate)
        val dateTimeLabel = "$dateLabel · $timeLabel"

        // Atomic stock deduction & StockMovement recording
        val stockEffects = InventoryRepository.deductStockForSale(
            deductions = ingredientDeductions,
            saleNumber = saleNumber,
            userName = userName,
            dateTimeLabel = dateTimeLabel
        )

        val sale = Sale(
            saleId = saleId,
            saleNumber = saleNumber,
            userId = userId,
            userName = userName,
            paymentMethod = paymentMethod,
            totalAmount = totalAmount,
            amountReceived = effectiveReceived,
            changeAmount = changeAmount,
            status = SaleStatus.CONFIRMED,
            confirmedAtMillis = nowMillis,
            confirmedDateLabel = dateLabel,
            confirmedTimeLabel = timeLabel,
            details = details,
            stockEffects = stockEffects
        )

        _sales.value = listOf(sale) + _sales.value

        val itemsSummary = details.joinToString(", ") { "${it.productName} ×${it.quantity}" }
        ActivityHistoryRepository.record(
            userId = userId,
            userName = userName,
            activityType = ActivityType.SALE,
            title = "Transaksi dicatat",
            subtitle = "$saleNumber · ${paymentMethod.displayLabel} · $itemsSummary",
            amountOrDelta = CurrencyFormatter.format(totalAmount),
            isNegativeDelta = false,
            timestampMillis = nowMillis,
            referenceSaleId = saleId
        )

        return RepoResult.Success(sale)
    }

    fun resetForTesting() {
        _sales.value = seedSales()
        nextSaleId = 16
    }

    data class TodaySalesSummary(
        val totalAmount: Long,
        val transactionCount: Int,
        val cashAmount: Long,
        val qrisAmount: Long,
        val latestSale: Sale?
    )

    private fun seedSales(): List<Sale> {
        val todayBase = Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun millisToday(hour: Int, minute: Int): Long {
            return (todayBase.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }.timeInMillis
        }

        val trx15Millis = millisToday(9, 38)
        val trx14Millis = millisToday(9, 12)
        val todayDate = DateFormatter.formatDate(Date(trx15Millis))

        return listOf(
            Sale(
                saleId = 15,
                saleNumber = "TRX-0015",
                userId = 1,
                userName = "Natashya Rosario",
                paymentMethod = PaymentMethod.CASH,
                totalAmount = 30_000L,
                amountReceived = 50_000L,
                changeAmount = 20_000L,
                status = SaleStatus.CONFIRMED,
                confirmedAtMillis = trx15Millis,
                confirmedDateLabel = todayDate,
                confirmedTimeLabel = "09:38",
                details = listOf(
                    SaleDetail(
                        productId = 1,
                        productName = "Kopi Susu",
                        quantity = 2,
                        sellingPriceSnapshot = 10_000L,
                        hppSnapshot = 5_676L,
                        subtotal = 20_000L
                    ),
                    SaleDetail(
                        productId = 2,
                        productName = "Strawberry",
                        quantity = 1,
                        sellingPriceSnapshot = 10_000L,
                        hppSnapshot = 6_066L,
                        subtotal = 10_000L
                    )
                ),
                stockEffects = listOf(
                    StockEffect(2, "Bubuk Kopi", 36, "g", 17964),
                    StockEffect(1, "Bubuk Strawberry", 15, "g", 2985),
                    StockEffect(3, "Susu UHT Full Cream", 450, "ml", 2140),
                    StockEffect(4, "Cup Plastik 12 oz", 3, "pcs", 57)
                )
            ),
            Sale(
                saleId = 14,
                saleNumber = "TRX-0014",
                userId = 1,
                userName = "Natashya Rosario",
                paymentMethod = PaymentMethod.QRIS,
                totalAmount = 20_000L,
                amountReceived = 20_000L,
                changeAmount = 0L,
                status = SaleStatus.CONFIRMED,
                confirmedAtMillis = trx14Millis,
                confirmedDateLabel = todayDate,
                confirmedTimeLabel = "09:12",
                details = listOf(
                    SaleDetail(
                        productId = 1,
                        productName = "Kopi Susu",
                        quantity = 1,
                        sellingPriceSnapshot = 10_000L,
                        hppSnapshot = 5_676L,
                        subtotal = 10_000L
                    ),
                    SaleDetail(
                        productId = 2,
                        productName = "Strawberry",
                        quantity = 1,
                        sellingPriceSnapshot = 10_000L,
                        hppSnapshot = 6_066L,
                        subtotal = 10_000L
                    )
                ),
                stockEffects = listOf(
                    StockEffect(2, "Bubuk Kopi", 18, "g", 18000),
                    StockEffect(1, "Bubuk Strawberry", 15, "g", 3000),
                    StockEffect(3, "Susu UHT Full Cream", 300, "ml", 2590),
                    StockEffect(4, "Cup Plastik 12 oz", 2, "pcs", 60)
                )
            )
        )
    }
}
