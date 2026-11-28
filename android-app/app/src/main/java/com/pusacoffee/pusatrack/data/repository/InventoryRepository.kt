package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
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
        dateTimeLabel: String
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
            isActive = true
        )

        _ingredients.value = _ingredients.value + newIngredient

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
        procurementType: ProcurementType
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
            procurementType = procurementType
        )

        _ingredients.value = _ingredients.value.map {
            if (it.ingredientId == ingredientId) updated else it
        }
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

        return RepoResult.Success(updated)
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
