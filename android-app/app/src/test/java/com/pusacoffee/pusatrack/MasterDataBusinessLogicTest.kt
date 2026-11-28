package com.pusacoffee.pusatrack

import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.model.RecipeItem
import com.pusacoffee.pusatrack.data.model.StockMovementType
import com.pusacoffee.pusatrack.data.model.StockStatus
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MasterDataBusinessLogicTest {

    @Before
    fun setUp() {
        InventoryRepository.resetForTesting()
        ProductRepository.resetForTesting()
    }

    @Test
    fun r01_packageConversionAndStockStatus() {
        val susu = InventoryRepository.getIngredientById(3)
        assertNotNull(susu)
        susu!!
        // 2140 ml / 920 ml = 2 kotak + 300 ml
        assertEquals(2, susu.fullPackages)
        assertEquals(300, susu.remainderUsage)
        assertEquals("2 kotak + 300 ml", susu.formatPackageBreakdown())
        // 2140 <= 3 * 920 (2760) -> Menipis
        assertEquals(StockStatus.LOW, susu.stockStatus)

        val kopi = InventoryRepository.getIngredientById(2)!!
        assertEquals(17, kopi.fullPackages)
        assertEquals(964, kopi.remainderUsage)
        assertEquals(StockStatus.SAFE, kopi.stockStatus)

        val outOfStock = susu.copy(currentStock = 0)
        assertEquals(StockStatus.OUT, outOfStock.stockStatus)
        assertEquals("0 kotak", outOfStock.formatPackageBreakdown())
    }

    @Test
    fun tc03_hppAndMarginCalculation_KopiSusuAndStrawberry() {
        val kopiSusu = ProductRepository.getProductById(1)!!
        val kopiCost = ProductRepository.calculateProductCost(kopiSusu)
        assertEquals(5676L, kopiCost.estimatedHpp)
        assertEquals(4324L, kopiCost.marginAmount)
        assertEquals("43,2%", kopiCost.formatMarginPercent())

        val strawberry = ProductRepository.getProductById(2)!!
        val strawCost = ProductRepository.calculateProductCost(strawberry)
        assertEquals(6066L, strawCost.estimatedHpp)
        assertEquals(3934L, strawCost.marginAmount)
        assertEquals("39,3%", strawCost.formatMarginPercent())

        val emptyRecipeCost = ProductRepository.calculatePreviewCost(10_000L, emptyList())
        assertEquals(0L, emptyRecipeCost.estimatedHpp)
        assertEquals(10_000L, emptyRecipeCost.marginAmount)
    }

    @Test
    fun tc04_addDirectStock_SusuUhtIncreasesTo4900mlAndUpdatesStatus() {
        val result = InventoryRepository.addDirectStock(
            ingredientId = 3,
            packageQuantity = 3,
            packagePrice = 18_500L,
            purchasePlace = "Indomaret BTP",
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 10:00",
            isFutureDate = false
        )

        assertTrue(result is RepoResult.Success)
        val updated = (result as RepoResult.Success).data
        assertEquals(4900, updated.currentStock)
        assertEquals(5, updated.fullPackages)
        assertEquals(300, updated.remainderUsage)
        assertEquals(StockStatus.SAFE, updated.stockStatus)

        val latestMovement = InventoryRepository.getMovementsForIngredient(3).first()
        assertEquals(StockMovementType.DIRECT_PURCHASE, latestMovement.movementType)
        assertEquals(2760, latestMovement.quantity)
        assertEquals(2140, latestMovement.stockBefore)
        assertEquals(4900, latestMovement.stockAfter)
    }

    @Test
    fun validation_rejectsDuplicateRecipeIngredientsAndInvalidQuantities() {
        val dupResult = ProductRepository.addProduct(
            name = "Es Kopi Dobel",
            sellingPrice = 12_000L,
            isActive = true,
            recipes = listOf(
                RecipeItem(ingredientId = 2, quantityRequired = 18),
                RecipeItem(ingredientId = 2, quantityRequired = 10)
            )
        )
        assertTrue(dupResult is RepoResult.Error)

        val zeroQtyRecipe = ProductRepository.addProduct(
            name = "Es Kopi Nol",
            sellingPrice = 12_000L,
            isActive = true,
            recipes = listOf(RecipeItem(ingredientId = 2, quantityRequired = 0))
        )
        assertTrue(zeroQtyRecipe is RepoResult.Error)

        val zeroStockIn = InventoryRepository.addDirectStock(
            ingredientId = 3,
            packageQuantity = 0,
            packagePrice = 18_500L,
            purchasePlace = null,
            userName = "Afdal Rizaldy",
            dateTimeLabel = "Rab, 18 Nov 2026 · 10:05"
        )
        assertTrue(zeroStockIn is RepoResult.Error)

        val futureStockIn = InventoryRepository.addDirectStock(
            ingredientId = 3,
            packageQuantity = 2,
            packagePrice = 18_500L,
            purchasePlace = null,
            userName = "Afdal Rizaldy",
            dateTimeLabel = "Kam, 19 Nov 2026 · 10:05",
            isFutureDate = true
        )
        assertTrue(futureStockIn is RepoResult.Error)
    }

    @Test
    fun editIngredient_keepsCurrentStockLockedAndUnchanged() {
        val beforeStock = InventoryRepository.getIngredientById(3)!!.currentStock
        val updateResult = InventoryRepository.updateIngredient(
            ingredientId = 3,
            name = "Susu UHT Full Cream",
            purchaseUnit = "kotak",
            usageUnit = "ml",
            packageSize = 920,
            packagePrice = 19_000L,
            minimumStockPackages = 3,
            procurementType = ProcurementType.DIRECT
        )
        assertTrue(updateResult is RepoResult.Success)
        val after = (updateResult as RepoResult.Success).data
        assertEquals(beforeStock, after.currentStock)
        assertEquals(19_000L, after.packagePrice)
    }
}
