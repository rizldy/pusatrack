package com.pusacoffee.pusatrack

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.ExpenseItem
import com.pusacoffee.pusatrack.data.model.FinancePeriod
import com.pusacoffee.pusatrack.data.model.LeadTimeFormatter
import com.pusacoffee.pusatrack.data.model.OrderStatus
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.repository.ActivityHistoryRepository
import com.pusacoffee.pusatrack.data.repository.ExpenseRepository
import com.pusacoffee.pusatrack.data.repository.FinanceRepository
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import com.pusacoffee.pusatrack.utils.DateFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class OrdersAndFinanceTest {

    @Before
    fun setUp() {
        InventoryRepository.resetForTesting()
        ProductRepository.resetForTesting()
        ActivityHistoryRepository.resetForTesting()
        SalesRepository.resetForTesting()
        PurchaseOrderRepository.resetForTesting()
        ExpenseRepository.resetForTesting()
        FinanceRepository.resetForTesting()
    }

    @Test
    fun tc06_createOrderPsn015_computesTotalBuildsWhatsAppWithoutPriceAndRejectsDirectIngredient() {
        // Bubuk Kopi (id=2) 10 pak x 120.000 + Bubuk Strawberry (id=1) 6 bungkus x 85.000 + Cup (id=4) 10 pak x 25.000
        val result = PurchaseOrderRepository.createOrder(
            supplierName = "Pusat Pusa Coffee",
            orderDateMillis = System.currentTimeMillis(),
            items = linkedMapOf(2 to 10, 1 to 6, 4 to 10),
            userId = 1,
            userName = "Natashya Rosario"
        )

        assertTrue(result is RepoResult.Success)
        val order = (result as RepoResult.Success).data
        assertEquals("PSN-015", order.orderNumber)
        assertEquals(1_960_000L, order.totalAmount)
        assertEquals(3, order.details.size)
        assertEquals(26, order.totalPackages)
        assertEquals(OrderStatus.ORDERED, order.status)

        val waMessage = LeadTimeFormatter.buildWhatsAppMessage(order, "Natashya Rosario")
        assertTrue(waMessage.contains("PSN-015"))
        assertTrue(waMessage.contains("Bubuk Kopi: 10 pak"))
        assertTrue(waMessage.contains("Bubuk Strawberry: 6 bungkus"))
        assertTrue(waMessage.contains("Cup Plastik 12 oz: 10 pak"))
        // Strictly NO price or "Rp" in WhatsApp template
        assertFalse(waMessage.contains("Rp"))
        assertFalse(waMessage.contains("1.960.000"))
        assertFalse(waMessage.contains("120.000"))

        // Reject DIRECT ingredient (Susu UHT, id=3) in central purchase order
        val directResult = PurchaseOrderRepository.createOrder(
            supplierName = "Pusat Pusa Coffee",
            orderDateMillis = System.currentTimeMillis(),
            items = mapOf(3 to 2),
            userId = 1,
            userName = "Natashya Rosario"
        )
        assertTrue(directResult is RepoResult.Error)
    }

    @Test
    fun tc07_receiveOrderPsn014_computesLeadTimeAddsStockAndRejectsDuplicateConfirmation() {
        val beforeKopi = InventoryRepository.getIngredientById(2)!!.currentStock
        val beforeStraw = InventoryRepository.getIngredientById(1)!!.currentStock
        val beforeCup = InventoryRepository.getIngredientById(4)!!.currentStock

        // Received: Wed 18 Nov 2026 09:41 WITA (ordered Mon 16 Nov 2026 09:15 WITA -> 2 hari 26 menit)
        val receivedMillis = Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 18)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 41)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val receiveResult = PurchaseOrderRepository.receiveOrder(
            orderId = 14,
            receivedAtMillis = receivedMillis,
            confirmedMatch = true,
            userId = 2,
            userName = "Afdal Rizaldy"
        )

        assertTrue(receiveResult is RepoResult.Success)
        val receivedOrder = (receiveResult as RepoResult.Success).data
        assertEquals(OrderStatus.RECEIVED, receivedOrder.status)
        assertEquals("2 hari 26 menit", LeadTimeFormatter.formatLong(receivedOrder.leadTimeSeconds!!))

        // Stock increased: +8.000 g Kopi, +3.000 g Strawberry, +500 pcs Cup
        assertEquals(beforeKopi + 8_000, InventoryRepository.getIngredientById(2)!!.currentStock)
        assertEquals(beforeStraw + 3_000, InventoryRepository.getIngredientById(1)!!.currentStock)
        assertEquals(beforeCup + 500, InventoryRepository.getIngredientById(4)!!.currentStock)

        // Duplicate confirmation rejected (409 rule)
        val duplicateConfirm = PurchaseOrderRepository.receiveOrder(
            orderId = 14,
            receivedAtMillis = receivedMillis,
            confirmedMatch = true,
            userId = 2,
            userName = "Afdal Rizaldy"
        )
        assertTrue(duplicateConfirm is RepoResult.Error)
    }

    @Test
    fun tc08_monthlyFinanceSummary_computesProfitAndSeparatesCashBalance() {
        val summary = FinanceRepository.getSummary(FinancePeriod.MONTHLY)
        assertEquals(7_200_000L, summary.totalSales)
        assertEquals(4_250_000L, summary.totalHpp)
        assertEquals(2_950_000L, summary.grossProfit)
        assertEquals(1_350_000L, summary.totalExpenses)
        assertEquals(1_600_000L, summary.netProfit)
        assertEquals("22,2%", summary.formatNetMarginPercent())

        // Changing opening cash balance does NOT affect net profit (R-05)
        FinanceRepository.setOpeningCashBalance(500_000L)
        val afterCashChange = FinanceRepository.getSummary(FinancePeriod.MONTHLY)
        assertEquals(500_000L, afterCashChange.openingCashBalance)
        assertEquals(1_600_000L, afterCashChange.netProfit)
    }

    @Test
    fun tc09_recordExpenses_computesTotalLogsHistoryAndRejectsZeroAmount() {
        val historyBefore = ActivityHistoryRepository.query(type = ActivityType.EXPENSE).size

        val result = ExpenseRepository.recordExpenses(
            paymentMethod = PaymentMethod.CASH,
            expenseDateMillis = System.currentTimeMillis(),
            items = listOf(
                ExpenseItem(1, "Es Batu", 25_000L, "2 kantong"),
                ExpenseItem(2, "Konsumsi", 15_000L, null)
            ),
            userId = 1,
            userName = "Natashya Rosario"
        )

        assertTrue(result is RepoResult.Success)
        val batch = (result as RepoResult.Success).data
        assertEquals(40_000L, batch.totalAmount)
        assertEquals(historyBefore + 1, ActivityHistoryRepository.query(type = ActivityType.EXPENSE).size)

        // Reject amount <= 0
        val invalidResult = ExpenseRepository.recordExpenses(
            paymentMethod = PaymentMethod.CASH,
            expenseDateMillis = System.currentTimeMillis(),
            items = listOf(ExpenseItem(1, "Es Batu", 0L, null)),
            userId = 1,
            userName = "Natashya Rosario"
        )
        assertTrue(invalidResult is RepoResult.Error)
    }
}
