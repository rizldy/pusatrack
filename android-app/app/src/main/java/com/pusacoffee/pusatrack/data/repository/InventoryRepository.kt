package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.model.PurchaseOrderDetail
import com.pusacoffee.pusatrack.data.model.StockCountAdjustment
import com.pusacoffee.pusatrack.data.model.StockCountEntry
import com.pusacoffee.pusatrack.data.model.StockCountRecord
import com.pusacoffee.pusatrack.data.model.StockEffect
import com.pusacoffee.pusatrack.data.model.StockMovement
import com.pusacoffee.pusatrack.data.model.StockMovementType
import com.pusacoffee.pusatrack.data.model.StockStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>()
    data class Error(val message: String) : RepoResult<Nothing>()
}

/**
 * Shared in-memory repository for MS-1 Inventory (L10-L14).
 * Uses singleton state so changes propagate immediately across Inventory, Products (HPP), and Home.
 */
object InventoryRepository {

    private val _ingredients = MutableStateFlow(seedIngredients())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients.asStateFlow()

    private val _movements = MutableStateFlow(seedMovements())
    val movements: StateFlow<List<StockMovement>> = _movements.asStateFlow()

    private var nextIngredientId = 5
    private var nextMovementId = 10
    private var nextStockCountId = 1

    fun getAllIngredients(): List<Ingredient> = _ingredients.value

    fun getIngredientById(id: Int): Ingredient? =
        _ingredients.value.find { it.ingredientId == id }

    fun getIngredientsMap(): Map<Int, Ingredient> =
        _ingredients.value.associateBy { it.ingredientId }

    fun getMovementsForIngredient(ingredientId: Int): List<StockMovement> =
        _movements.value.filter { it.ingredientId == ingredientId }

    fun getLowOrOutIngredients(): List<Ingredient> =
        _ingredients.value.filter {
            it.isActive && (it.stockStatus == StockStatus.LOW || it.stockStatus == StockStatus.OUT)
        }

    fun addIngredient(
        name: String,
        purchaseUnit: String,
        usageUnit: String,
        packageSize: Int,
        openingStockPackages: Int,
        packagePrice: Long,
        minimumStockPackages: Int,
        procurementType: ProcurementType,
        userName: String,
        dateTimeLabel: String,
        imagePath: String? = null
    ): RepoResult<Ingredient> {
        val trimmedName = name.trim()
        val trimmedPurchaseUnit = purchaseUnit.trim()
        val trimmedUsageUnit = usageUnit.trim()

        if (trimmedName.isEmpty()) {
            return RepoResult.Error("Nama bahan baku wajib diisi.")
        }
        val duplicate = _ingredients.value.any {
            it.isActive && it.ingredientName.equals(trimmedName, ignoreCase = true)
        }
        if (duplicate) {
            return RepoResult.Error("Nama bahan baku sudah digunakan.")
        }
        if (trimmedPurchaseUnit.isEmpty() || trimmedUsageUnit.isEmpty()) {
            return RepoResult.Error("Satuan pembelian dan satuan penggunaan wajib diisi.")
        }
        if (packageSize <= 0) {
            return RepoResult.Error("Isi per kemasan harus lebih dari 0.")
        }
        if (openingStockPackages < 0) {
            return RepoResult.Error("Jumlah stok awal tidak boleh negatif.")
        }
        if (packagePrice < 0L) {
            return RepoResult.Error("Harga per kemasan tidak boleh negatif.")
        }
        if (minimumStockPackages < 0) {
            return RepoResult.Error("Batas minimum tidak boleh negatif.")
        }

        val openingStockUsage = openingStockPackages * packageSize
        val newIngredient = Ingredient(
            ingredientId = nextIngredientId++,
            ingredientName = trimmedName,
            purchaseUnit = trimmedPurchaseUnit,
            packageSize = packageSize,
            usageUnit = trimmedUsageUnit,
            currentStock = openingStockUsage,
            minimumStockPackages = minimumStockPackages,
            packagePrice = packagePrice,
            procurementType = procurementType,
            isActive = true,
            imagePath = imagePath
        )

        _ingredients.value = _ingredients.value + newIngredient
        NotificationRepository.refresh()

        if (openingStockUsage > 0) {
            val openingMovement = StockMovement(
                movementId = nextMovementId++,
                ingredientId = newIngredient.ingredientId,
                movementType = StockMovementType.OPENING_STOCK,
                quantity = openingStockUsage,
                stockBefore = 0,
                stockAfter = openingStockUsage,
                referenceLabel = "Stok awal ($openingStockPackages $trimmedPurchaseUnit)",
                userName = userName,
                dateTimeLabel = dateTimeLabel,
                description = "Pencatatan stok awal bahan baru"
            )
            _movements.value = listOf(openingMovement) + _movements.value
            ActivityHistoryRepository.record(
                userId = 0,
                userName = userName,
                activityType = ActivityType.STOCK,
                title = "Bahan baru ditambahkan",
                subtitle = "$trimmedName · Stok awal $openingStockPackages $trimmedPurchaseUnit",
                amountOrDelta = "+${Ingredient.formatNumber(openingStockUsage)} $trimmedUsageUnit",
                isNegativeDelta = false
            )
        }

        return RepoResult.Success(newIngredient)
    }

    fun updateIngredient(
        ingredientId: Int,
        name: String,
        purchaseUnit: String,
        usageUnit: String,
        packageSize: Int,
        packagePrice: Long,
        minimumStockPackages: Int,
        procurementType: ProcurementType,
        imagePath: String? = null,
        updateImage: Boolean = false
    ): RepoResult<Ingredient> {
        val existing = getIngredientById(ingredientId)
            ?: return RepoResult.Error("Bahan baku tidak ditemukan.")

        val trimmedName = name.trim()
        val trimmedPurchaseUnit = purchaseUnit.trim()
        val trimmedUsageUnit = usageUnit.trim()

        if (trimmedName.isEmpty()) {
            return RepoResult.Error("Nama bahan baku wajib diisi.")
        }
        val duplicate = _ingredients.value.any {
            it.isActive &&
                it.ingredientId != ingredientId &&
                it.ingredientName.equals(trimmedName, ignoreCase = true)
        }
        if (duplicate) {
            return RepoResult.Error("Nama bahan baku sudah digunakan.")
        }
        if (trimmedPurchaseUnit.isEmpty() || trimmedUsageUnit.isEmpty()) {
            return RepoResult.Error("Satuan pembelian dan satuan penggunaan wajib diisi.")
        }
        if (packageSize <= 0) {
            return RepoResult.Error("Isi per kemasan harus lebih dari 0.")
        }
        if (packagePrice < 0L) {
            return RepoResult.Error("Harga per kemasan tidak boleh negatif.")
        }
        if (minimumStockPackages < 0) {
            return RepoResult.Error("Batas minimum tidak boleh negatif.")
        }

        // Current stock is NEVER changed in Edit Bahan (L14 rule).
        val updated = existing.copy(
            ingredientName = trimmedName,
            purchaseUnit = trimmedPurchaseUnit,
            usageUnit = trimmedUsageUnit,
            packageSize = packageSize,
            packagePrice = packagePrice,
            minimumStockPackages = minimumStockPackages,
            procurementType = procurementType,
            imagePath = if (updateImage) imagePath else existing.imagePath
        )

        _ingredients.value = _ingredients.value.map {
            if (it.ingredientId == ingredientId) updated else it
        }
        NotificationRepository.refresh()
        NotificationRepository.refresh()
        return RepoResult.Success(updated)
    }

    fun addDirectStock(
        ingredientId: Int,
        packageQuantity: Int,
        packagePrice: Long,
        purchasePlace: String?,
        userName: String,
        dateTimeLabel: String,
        isFutureDate: Boolean = false
    ): RepoResult<Ingredient> {
        val existing = getIngredientById(ingredientId)
            ?: return RepoResult.Error("Bahan baku tidak ditemukan.")

        if (isFutureDate) {
            return RepoResult.Error("Tanggal pembelian tidak boleh di masa depan.")
        }
        if (packageQuantity <= 0) {
            return RepoResult.Error("Jumlah kemasan harus lebih dari 0.")
        }
        if (packagePrice < 0L) {
            return RepoResult.Error("Harga per kemasan tidak boleh negatif.")
        }

        val addedUsage = packageQuantity * existing.packageSize
        val newStock = existing.currentStock + addedUsage
        val updated = existing.copy(
            currentStock = newStock,
            packagePrice = packagePrice
        )

        _ingredients.value = _ingredients.value.map {
            if (it.ingredientId == ingredientId) updated else it
        }
        NotificationRepository.refresh()

        val placeText = purchasePlace?.trim().takeUnless { it.isNullOrEmpty() }
            ?: "Beli langsung"
        val movement = StockMovement(
            movementId = nextMovementId++,
            ingredientId = ingredientId,
            movementType = StockMovementType.DIRECT_PURCHASE,
            quantity = addedUsage,
            stockBefore = existing.currentStock,
            stockAfter = newStock,
            referenceLabel = "$placeText (+$packageQuantity ${existing.purchaseUnit})",
            userName = userName,
            dateTimeLabel = dateTimeLabel,
            description = "Beli langsung $packageQuantity ${existing.purchaseUnit}"
        )
        _movements.value = listOf(movement) + _movements.value

        ActivityHistoryRepository.record(
            userId = 0,
            userName = userName,
            activityType = ActivityType.STOCK,
            title = "Stok ditambah",
            subtitle = "${existing.ingredientName} · $placeText (+$packageQuantity ${existing.purchaseUnit})",
            amountOrDelta = "+${Ingredient.formatNumber(addedUsage)} ${existing.usageUnit}",
            isNegativeDelta = false
        )

        return RepoResult.Success(updated)
    }

    /**
     * Atomically deducts ingredient stock for a confirmed sale (R-02) and writes SALE StockMovements.
     */
    fun deductStockForSale(
        deductions: Map<Int, Int>,
        saleNumber: String,
        userName: String,
        dateTimeLabel: String
    ): List<StockEffect> {
        if (deductions.isEmpty()) return emptyList()

        val effects = mutableListOf<StockEffect>()
        val newMovements = mutableListOf<StockMovement>()
        val updatedList = _ingredients.value.map { ingredient ->
            val used = deductions[ingredient.ingredientId] ?: 0
            if (used > 0) {
                val stockBefore = ingredient.currentStock
                val stockAfter = (stockBefore - used).coerceAtLeast(0)
                effects.add(
                    StockEffect(
                        ingredientId = ingredient.ingredientId,
                        ingredientName = ingredient.ingredientName,
                        usedQuantity = used,
                        usageUnit = ingredient.usageUnit,
                        stockAfter = stockAfter
                    )
                )
                newMovements.add(
                    StockMovement(
                        movementId = nextMovementId++,
                        ingredientId = ingredient.ingredientId,
                        movementType = StockMovementType.SALE,
                        quantity = -used,
                        stockBefore = stockBefore,
                        stockAfter = stockAfter,
                        referenceLabel = saleNumber,
                        userName = userName,
                        dateTimeLabel = dateTimeLabel,
                        description = "Pengurangan otomatis dari $saleNumber"
                    )
                )
                ingredient.copy(currentStock = stockAfter)
            } else {
                ingredient
            }
        }

        _ingredients.value = updatedList
        if (newMovements.isNotEmpty()) {
            _movements.value = newMovements + _movements.value
        }
        NotificationRepository.refresh()
        return effects
    }

    /**
     * Atomically receives a purchase order: increases currentStock by quantity * packageSizeSnapshot,
     * updates packagePrice (HPP) from snapshot, and records PURCHASE StockMovements.
     */
    fun receivePurchaseOrderStock(
        details: List<PurchaseOrderDetail>,
        orderNumber: String,
        userName: String,
        dateTimeLabel: String
    ) {
        if (details.isEmpty()) return
        val detailByIngredient = details.associateBy { it.ingredientId }
        val newMovements = mutableListOf<StockMovement>()

        val updatedList = _ingredients.value.map { ingredient ->
            val detail = detailByIngredient[ingredient.ingredientId]
            if (detail != null) {
                val addedUsage = detail.totalUsageQuantity
                val stockBefore = ingredient.currentStock
                val stockAfter = stockBefore + addedUsage
                newMovements.add(
                    StockMovement(
                        movementId = nextMovementId++,
                        ingredientId = ingredient.ingredientId,
                        movementType = StockMovementType.PURCHASE,
                        quantity = addedUsage,
                        stockBefore = stockBefore,
                        stockAfter = stockAfter,
                        referenceLabel = orderNumber,
                        userName = userName,
                        dateTimeLabel = dateTimeLabel,
                        description = "Penerimaan $orderNumber (+${detail.quantity} ${detail.purchaseUnit})"
                    )
                )
                ingredient.copy(
                    currentStock = stockAfter,
                    packagePrice = detail.packagePriceSnapshot
                )
            } else {
                ingredient
            }
        }

        _ingredients.value = updatedList
        if (newMovements.isNotEmpty()) {
            _movements.value = newMovements + _movements.value
        }
        NotificationRepository.refresh()
    }

    /**
     * Applies physical stock count (M6d, L15-L16).
     * Only ingredients with delta != 0 are updated, recorded as ADJUSTMENT movements, and logged to ActivityHistory.
     */
    fun applyStockCount(
        entries: List<StockCountEntry>,
        reason: String,
        description: String?,
        userId: Int,
        userName: String,
        dateTimeLabel: String
    ): RepoResult<StockCountRecord> {
        if (entries.any { it.countedWholePackages < 0 }) {
            return RepoResult.Error("Jumlah kemasan fisik tidak boleh negatif.")
        }

        val discrepancies = entries.filter { it.isDiscrepancy }
        if (discrepancies.isEmpty()) {
            return RepoResult.Error("Tidak ada selisih stok yang perlu disesuaikan.")
        }

        val cleanReason = reason.trim().ifEmpty { "Selisih pemeriksaan fisik" }
        val cleanDesc = description?.trim()?.ifEmpty { null }
        val discrepancyById = discrepancies.associateBy { it.ingredient.ingredientId }
        val adjustments = mutableListOf<StockCountAdjustment>()
        val newMovements = mutableListOf<StockMovement>()

        val updatedList = _ingredients.value.map { ing ->
            val entry = discrepancyById[ing.ingredientId]
            if (entry != null) {
                val stockBefore = ing.currentStock
                val stockAfter = entry.physicalStock.coerceAtLeast(0)
                val delta = stockAfter - stockBefore
                adjustments.add(
                    StockCountAdjustment(
                        ingredientId = ing.ingredientId,
                        ingredientName = ing.ingredientName,
                        usageUnit = ing.usageUnit,
                        stockBefore = stockBefore,
                        stockAfter = stockAfter,
                        delta = delta
                    )
                )
                newMovements.add(
                    StockMovement(
                        movementId = nextMovementId++,
                        ingredientId = ing.ingredientId,
                        movementType = StockMovementType.ADJUSTMENT,
                        quantity = delta,
                        stockBefore = stockBefore,
                        stockAfter = stockAfter,
                        referenceLabel = cleanReason,
                        userName = userName,
                        dateTimeLabel = dateTimeLabel,
                        description = cleanDesc ?: cleanReason
                    )
                )
                ing.copy(currentStock = stockAfter)
            } else {
                ing
            }
        }

        _ingredients.value = updatedList
        _movements.value = newMovements + _movements.value
        NotificationRepository.refresh()

        for (adj in adjustments) {
            val sign = if (adj.delta >= 0) "+" else "-"
            val absDelta = kotlin.math.abs(adj.delta)
            ActivityHistoryRepository.record(
                userId = userId,
                userName = userName,
                activityType = ActivityType.STOCK,
                title = "Stok disesuaikan",
                subtitle = "${adj.ingredientName} · $cleanReason",
                amountOrDelta = "$sign${Ingredient.formatNumber(absDelta)} ${adj.usageUnit}",
                isNegativeDelta = adj.delta < 0
            )
        }

        return RepoResult.Success(
            StockCountRecord(
                countId = nextStockCountId++,
                userName = userName,
                dateTimeLabel = dateTimeLabel,
                reason = cleanReason,
                description = cleanDesc,
                adjustments = adjustments
            )
        )
    }

    fun resetForTesting() {
        _ingredients.value = seedIngredients()
        _movements.value = seedMovements()
        nextIngredientId = 5
        nextMovementId = 10
    }

    private fun seedIngredients(): List<Ingredient> = listOf(
        Ingredient(
            ingredientId = 1,
            ingredientName = "Bubuk Strawberry",
            purchaseUnit = "bungkus",
            packageSize = 500,
            usageUnit = "g",
            currentStock = 2985,
            minimumStockPackages = 2,
            packagePrice = 85_000L,
            procurementType = ProcurementType.SUPPLIED
        ),
        Ingredient(
            ingredientId = 2,
            ingredientName = "Bubuk Kopi",
            purchaseUnit = "pak",
            packageSize = 1000,
            usageUnit = "g",
            currentStock = 17964,
            minimumStockPackages = 3,
            packagePrice = 120_000L,
            procurementType = ProcurementType.SUPPLIED
        ),
        Ingredient(
            ingredientId = 3,
            ingredientName = "Susu UHT Full Cream",
            purchaseUnit = "kotak",
            packageSize = 920,
            usageUnit = "ml",
            currentStock = 2140,
            minimumStockPackages = 3,
            packagePrice = 18_500L,
            procurementType = ProcurementType.DIRECT
        ),
        Ingredient(
            ingredientId = 4,
            ingredientName = "Cup Plastik 12 oz",
            purchaseUnit = "pak",
            packageSize = 50,
            usageUnit = "pcs",
            currentStock = 57,
            minimumStockPackages = 2,
            packagePrice = 25_000L,
            procurementType = ProcurementType.SUPPLIED
        )
    )

    private fun seedMovements(): List<StockMovement> = listOf(
        // Most recent first (TRX-0015 from PRD TC-02)
        StockMovement(
            movementId = 8,
            ingredientId = 3,
            movementType = StockMovementType.SALE,
            quantity = -450,
            stockBefore = 2590,
            stockAfter = 2140,
            referenceLabel = "TRX-0015",
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 09:38"
        ),
        StockMovement(
            movementId = 7,
            ingredientId = 4,
            movementType = StockMovementType.SALE,
            quantity = -3,
            stockBefore = 60,
            stockAfter = 57,
            referenceLabel = "TRX-0015",
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 09:38"
        ),
        StockMovement(
            movementId = 6,
            ingredientId = 2,
            movementType = StockMovementType.SALE,
            quantity = -36,
            stockBefore = 18000,
            stockAfter = 17964,
            referenceLabel = "TRX-0015",
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 09:38"
        ),
        StockMovement(
            movementId = 5,
            ingredientId = 1,
            movementType = StockMovementType.SALE,
            quantity = -15,
            stockBefore = 3000,
            stockAfter = 2985,
            referenceLabel = "TRX-0015",
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 09:38"
        ),
        StockMovement(
            movementId = 4,
            ingredientId = 3,
            movementType = StockMovementType.DIRECT_PURCHASE,
            quantity = 2760,
            stockBefore = 750,
            stockAfter = 3510,
            referenceLabel = "Indomaret BTP (+3 kotak)",
            userName = "Afdal Rizaldy",
            dateTimeLabel = "Sen, 16 Nov 2026 · 08:20"
        ),
        StockMovement(
            movementId = 3,
            ingredientId = 2,
            movementType = StockMovementType.PURCHASE,
            quantity = 8000,
            stockBefore = 10000,
            stockAfter = 18000,
            referenceLabel = "PSN-013",
            userName = "Raidah Syahirah",
            dateTimeLabel = "Sab, 14 Nov 2026 · 11:15"
        ),
        StockMovement(
            movementId = 2,
            ingredientId = 1,
            movementType = StockMovementType.PURCHASE,
            quantity = 3000,
            stockBefore = 0,
            stockAfter = 3000,
            referenceLabel = "PSN-013",
            userName = "Raidah Syahirah",
            dateTimeLabel = "Sab, 14 Nov 2026 · 11:15"
        ),
        StockMovement(
            movementId = 1,
            ingredientId = 4,
            movementType = StockMovementType.OPENING_STOCK,
            quantity = 100,
            stockBefore = 0,
            stockAfter = 100,
            referenceLabel = "Stok awal (2 pak)",
            userName = "Natashya Rosario",
            dateTimeLabel = "Jum, 13 Nov 2026 · 07:45"
        )
    )
}
