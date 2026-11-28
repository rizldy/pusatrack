package com.pusacoffee.pusatrack

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.model.StockMovementType
import com.pusacoffee.pusatrack.data.repository.ActivityHistoryRepository
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SalesAndHistoryTest {

    @Before
    fun setUp() {
        InventoryRepository.resetForTesting()
        ProductRepository.resetForTesting()
        ActivityHistoryRepository.resetForTesting()
        SalesRepository.resetForTesting()
    }

    @Test
    fun tc02_cashSale_computesTotalChangeAndDeductsExactRecipeStock() {
        val beforeKopi = InventoryRepository.getIngredientById(2)!!.currentStock
        val beforeStraw = InventoryRepository.getIngredientById(1)!!.currentStock
        val beforeSusu = InventoryRepository.getIngredientById(3)!!.currentStock
        val beforeCup = InventoryRepository.getIngredientById(4)!!.currentStock

        // Kopi Susu (id=1) x2 + Strawberry (id=2) x1, Cash received Rp 50.000
        val result = SalesRepository.confirmSale(
            cartItems = mapOf(1 to 2, 2 to 1),
            paymentMethod = PaymentMethod.CASH,
            amountReceived = 50_000L,
            userId = 1,
            userName = "Natashya Rosario"
        )

        assertTrue(result is RepoResult.Success)
        val sale = (result as RepoResult.Success).data
        assertEquals(30_000L, sale.totalAmount)
        assertEquals(50_000L, sale.amountReceived)
        assertEquals(20_000L, sale.changeAmount)
        assertEquals(PaymentMethod.CASH, sale.paymentMethod)

        // Exact recipe stock deduction: Kopi -36 g, Strawberry -15 g, Susu -450 ml, Cup -3 pcs
        assertEquals(beforeKopi - 36, InventoryRepository.getIngredientById(2)!!.currentStock)
        assertEquals(beforeStraw - 15, InventoryRepository.getIngredientById(1)!!.currentStock)
        assertEquals(beforeSusu - 450, InventoryRepository.getIngredientById(3)!!.currentStock)
        assertEquals(beforeCup - 3, InventoryRepository.getIngredientById(4)!!.currentStock)

        val effectsById = sale.stockEffects.associateBy { it.ingredientId }
        assertEquals(36, effectsById[2]?.usedQuantity)
        assertEquals(15, effectsById[1]?.usedQuantity)
        assertEquals(450, effectsById[3]?.usedQuantity)
        assertEquals(3, effectsById[4]?.usedQuantity)

        // Verify SALE stock movement written
        val latestSusuMovement = InventoryRepository.getMovementsForIngredient(3).first()
        assertEquals(StockMovementType.SALE, latestSusuMovement.movementType)
        assertEquals(-450, latestSusuMovement.quantity)
        assertEquals(sale.saleNumber, latestSusuMovement.referenceLabel)
    }

    @Test
    fun cashierValidation_rejectsEmptyCartInsufficientCashAndInactiveProduct() {
        // Empty cart
        val emptyResult = SalesRepository.confirmSale(
            cartItems = emptyMap(),
            paymentMethod = PaymentMethod.CASH,
            amountReceived = 50_000L,
            userId = 1,
            userName = "Natashya Rosario"
        )
        assertTrue(emptyResult is RepoResult.Error)

        // Cash received < total (20.000 < 30.000)
        val lowCashResult = SalesRepository.confirmSale(
            cartItems = mapOf(1 to 2, 2 to 1),
            paymentMethod = PaymentMethod.CASH,
            amountReceived = 20_000L,
            userId = 1,
            userName = "Natashya Rosario"
        )
        assertTrue(lowCashResult is RepoResult.Error)

        // Deactivate product 2, then attempt sale
        val product2 = ProductRepository.getProductById(2)!!
        ProductRepository.updateProduct(
            productId = 2,
            name = product2.productName,
            sellingPrice = product2.sellingPrice,
            isActive = false,
            recipes = product2.recipes
        )
        val inactiveResult = SalesRepository.confirmSale(
            cartItems = mapOf(2 to 1),
            paymentMethod = PaymentMethod.QRIS,
            amountReceived = 10_000L,
            userId = 1,
            userName = "Natashya Rosario"
        )
        assertTrue(inactiveResult is RepoResult.Error)
    }

    @Test
    fun r03_priceChangeAfterSaleDoesNotAlterConfirmedSaleSnapshot() {
        val saleResult = SalesRepository.confirmSale(
            cartItems = mapOf(1 to 2),
            paymentMethod = PaymentMethod.QRIS,
            amountReceived = 20_000L,
            userId = 2,
            userName = "Afdal Rizaldy"
        )
        assertTrue(saleResult is RepoResult.Success)
        val saleId = (saleResult as RepoResult.Success).data.saleId

        // Change Kopi Susu selling price from 10.000 -> 15.000
        val kopiSusu = ProductRepository.getProductById(1)!!
        ProductRepository.updateProduct(
            productId = 1,
            name = kopiSusu.productName,
            sellingPrice = 15_000L,
            isActive = true,
            recipes = kopiSusu.recipes
        )

        val storedSale = SalesRepository.getSaleById(saleId)
        assertNotNull(storedSale)
        assertEquals(20_000L, storedSale!!.totalAmount)
        assertEquals(10_000L, storedSale.details.first().sellingPriceSnapshot)
        assertEquals(5_676L, storedSale.details.first().hppSnapshot)
    }

    @Test
    fun tc11_activityHistoryRecordedOnSaleAndSupportsFilterAndSorting() {
        val countBefore = ActivityHistoryRepository.getAll().size

        val saleResult = SalesRepository.confirmSale(
            cartItems = mapOf(1 to 1),
            paymentMethod = PaymentMethod.QRIS,
            amountReceived = 10_000L,
            userId = 3,
            userName = "Raidah Syahirah"
        )
        assertTrue(saleResult is RepoResult.Success)
        val createdSale = (saleResult as RepoResult.Success).data

        assertEquals(countBefore + 1, ActivityHistoryRepository.getAll().size)

        // Filter by SALE and user Raidah Syahirah
        val raidahSales = ActivityHistoryRepository.query(
            type = ActivityType.SALE,
            userName = "Raidah Syahirah",
            newestFirst = true
        )
        assertEquals(1, raidahSales.size)
        assertEquals(createdSale.saleId, raidahSales.first().referenceSaleId)

        // Verify newest-first vs oldest-first sorting
        val newest = ActivityHistoryRepository.query(newestFirst = true)
        val oldest = ActivityHistoryRepository.query(newestFirst = false)
        assertTrue(newest.first().timestampMillis >= newest.last().timestampMillis)
        assertTrue(oldest.first().timestampMillis <= oldest.last().timestampMillis)
        assertEquals(newest.first().activityId, oldest.last().activityId)
    }
}
