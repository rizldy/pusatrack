package com.pusacoffee.pusatrack

import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.model.NotificationType
import com.pusacoffee.pusatrack.data.model.StockCountEntry
import com.pusacoffee.pusatrack.data.model.StockMovementType
import com.pusacoffee.pusatrack.data.repository.ActivityHistoryRepository
import com.pusacoffee.pusatrack.data.repository.ExpenseRepository
import com.pusacoffee.pusatrack.data.repository.FinanceRepository
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.NotificationRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import com.pusacoffee.pusatrack.utils.ReportSheetOption
import com.pusacoffee.pusatrack.utils.XlsxReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.util.zip.ZipFile

class StockCheckReportNotificationTest {

    @Before
    fun setUp() {
        InventoryRepository.resetForTesting()
        ProductRepository.resetForTesting()
        ActivityHistoryRepository.resetForTesting()
        SalesRepository.resetForTesting()
        PurchaseOrderRepository.resetForTesting()
        ExpenseRepository.resetForTesting()
        FinanceRepository.resetForTesting()
        NotificationRepository.resetForTesting()
    }

    @Test
    fun tc05_physicalStockCheck_adjustsOnlyDiscrepanciesAndLogsHistory() {
        val susu = InventoryRepository.getIngredientById(3)!! // 2140 ml = 2 kotak + 300 ml
        val kopi = InventoryRepository.getIngredientById(2)!! // 17964 g = 17 pak + 964 g

        // Susu: counted 2 kotak WITHOUT open package -> 1840 ml (delta -300 ml)
        val susuEntry = StockCountEntry(
            ingredient = susu,
            countedWholePackages = 2,
            hasOpenPackage = false
        )
        // Kopi: counted 17 pak WITH open package -> 17964 g (delta 0, Sesuai)
        val kopiEntry = StockCountEntry(
            ingredient = kopi,
            countedWholePackages = 17,
            hasOpenPackage = true
        )

        assertEquals(-300, susuEntry.delta)
        assertEquals(1840, susuEntry.physicalStock)
        assertTrue(susuEntry.isDiscrepancy)

        assertEquals(0, kopiEntry.delta)
        assertEquals(17964, kopiEntry.physicalStock)
        assertTrue(!kopiEntry.isDiscrepancy)

        val historyBefore = ActivityHistoryRepository.query(type = ActivityType.STOCK).size

        val result = InventoryRepository.applyStockCount(
            entries = listOf(susuEntry, kopiEntry),
            reason = "Selisih pemeriksaan fisik",
            description = "Sisa terbuka sudah habis",
            userId = 1,
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 17:30"
        )

        assertTrue(result is RepoResult.Success)
        val record = (result as RepoResult.Success).data
        assertEquals(1, record.adjustments.size)
        assertEquals(3, record.adjustments.first().ingredientId)
        assertEquals(-300, record.adjustments.first().delta)

        assertEquals(1840, InventoryRepository.getIngredientById(3)!!.currentStock)
        assertEquals(17964, InventoryRepository.getIngredientById(2)!!.currentStock)

        val latestMovement = InventoryRepository.getMovementsForIngredient(3).first()
        assertEquals(StockMovementType.ADJUSTMENT, latestMovement.movementType)
        assertEquals(-300, latestMovement.quantity)
        assertEquals(2140, latestMovement.stockBefore)
        assertEquals(1840, latestMovement.stockAfter)

        assertEquals(
            historyBefore + 1,
            ActivityHistoryRepository.query(type = ActivityType.STOCK).size
        )
    }

    @Test
    fun tc10_xlsxReportGenerator_createsValidZipWith4SheetsAndValidatesInputs() {
        val tempDir = Files.createTempDirectory("pusatrack_xlsx_test").toFile()
        try {
            val default4Sheets = ReportSheetOption.entries.filter { it.defaultSelected }
            assertEquals(4, default4Sheets.size)

            val startMillis = 1_793_491_200_000L
            val endMillis = 1_795_046_399_000L

            val result = XlsxReportGenerator.generate(
                outputDir = tempDir,
                startDateMillis = startMillis,
                endDateMillis = endMillis,
                rangeSlug = "1-18Nov2026",
                selectedSheets = default4Sheets
            )

            assertTrue(result is RepoResult.Success)
            val report = (result as RepoResult.Success).data
            assertEquals("Laporan_PUSATRACK_1-18Nov2026.xlsx", report.fileName)
            assertEquals(4, report.sheetCount)
            assertTrue(report.file.exists() && report.file.length() > 0L)

            // Verify valid OOXML ZIP structure with 4 worksheets
            ZipFile(report.file).use { zip ->
                assertNotNull(zip.getEntry("[Content_Types].xml"))
                assertNotNull(zip.getEntry("xl/workbook.xml"))
                assertNotNull(zip.getEntry("xl/worksheets/sheet1.xml"))
                assertNotNull(zip.getEntry("xl/worksheets/sheet2.xml"))
                assertNotNull(zip.getEntry("xl/worksheets/sheet3.xml"))
                assertNotNull(zip.getEntry("xl/worksheets/sheet4.xml"))
            }

            // Validation: 0 sheets rejected
            val emptySheetsResult = XlsxReportGenerator.generate(
                outputDir = tempDir,
                startDateMillis = startMillis,
                endDateMillis = endMillis,
                rangeSlug = "empty",
                selectedSheets = emptyList()
            )
            assertTrue(emptySheetsResult is RepoResult.Error)

            // Validation: startDate > endDate rejected
            val invalidRangeResult = XlsxReportGenerator.generate(
                outputDir = tempDir,
                startDateMillis = endMillis,
                endDateMillis = startMillis,
                rangeSlug = "bad",
                selectedSheets = default4Sheets
            )
            assertTrue(invalidRangeResult is RepoResult.Error)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun tc12_lowStockTriggersUnreadNotificationAndMarkAllReadClearsUnreadCount() {
        NotificationRepository.markAllAsRead()
        assertEquals(0, NotificationRepository.unreadCount.value)

        // Drop Bubuk Kopi (id=2, min=3 pak = 3000 g) down to 2 pak = 2000 g (LOW_STOCK)
        val kopi = InventoryRepository.getIngredientById(2)!!
        val dropEntry = StockCountEntry(
            ingredient = kopi,
            countedWholePackages = 2,
            hasOpenPackage = false
        )
        val applyResult = InventoryRepository.applyStockCount(
            entries = listOf(dropEntry),
            reason = "Uji stok menipis TC-12",
            description = null,
            userId = 1,
            userName = "Natashya Rosario",
            dateTimeLabel = "Rab, 18 Nov 2026 · 18:00"
        )
        assertTrue(applyResult is RepoResult.Success)

        val kopiNotif = NotificationRepository.notifications.value.find {
            it.targetIngredientId == 2 && it.type == NotificationType.LOW_STOCK
        }
        assertNotNull(kopiNotif)
        assertTrue(!kopiNotif!!.isRead)
        assertTrue(NotificationRepository.unreadCount.value >= 1)

        NotificationRepository.markAllAsRead()
        assertEquals(0, NotificationRepository.unreadCount.value)
    }
}
