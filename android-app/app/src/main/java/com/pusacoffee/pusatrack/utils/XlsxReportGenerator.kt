package com.pusacoffee.pusatrack.utils

import com.pusacoffee.pusatrack.data.model.FinancePeriod
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.StockMovementType
import com.pusacoffee.pusatrack.data.repository.ExpenseRepository
import com.pusacoffee.pusatrack.data.repository.FinanceRepository
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ReportSheetOption(
    val apiKey: String,
    val sheetTitle: String,
    val defaultSelected: Boolean
) {
    SALES_PRODUCTS("sales_products", "Penjualan Produk", true),
    INGREDIENT_USAGE("ingredient_usage", "Penggunaan Bahan Baku", true),
    STOCK_MOVEMENTS("stock_movements", "Pergerakan Stok", false),
    INGREDIENT_PURCHASES("ingredient_purchases", "Pembelian Bahan", false),
    PROCUREMENT_LEAD_TIME("procurement_lead_time", "Lead Time Pengadaan", true),
    STOCK_ADJUSTMENTS("stock_adjustments", "Penyesuaian Stok", false),
    OPERATIONAL_EXPENSES("operational_expenses", "Pengeluaran Operasional", false),
    FINANCE_SUMMARY("finance_summary", "Rekap Keuangan", true)
}

data class GeneratedReport(
    val file: File,
    val fileName: String,
    val sheetCount: Int,
    val fileSizeBytes: Long
) {
    fun formatFileSize(): String {
        val kb = (fileSizeBytes / 1024.0).coerceAtLeast(1.0)
        return String.format("%.1f KB", kb)
    }
}

/**
 * Pure Kotlin/Java OOXML (.xlsx) report generator using ZipOutputStream.
 * Generates valid Microsoft Excel workbooks with 1 worksheet per selected option
 * without requiring Apache POI (lightweight & fully compatible with minSdk 24).
 */
object XlsxReportGenerator {

    fun generate(
        outputDir: File,
        startDateMillis: Long,
        endDateMillis: Long,
        rangeSlug: String,
        selectedSheets: List<ReportSheetOption>
    ): RepoResult<GeneratedReport> {
        if (selectedSheets.isEmpty()) {
            return RepoResult.Error("Pilih minimal 1 isi laporan untuk diekspor.")
        }
        if (startDateMillis > endDateMillis) {
            return RepoResult.Error("Tanggal awal tidak boleh setelah tanggal akhir.")
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val safeSlug = rangeSlug.replace(Regex("[^A-Za-z0-9\\-_]"), "")
            .ifEmpty { "Export" }
        val fileName = "Laporan_PUSATRACK_$safeSlug.xlsx"
        val outFile = File(outputDir, fileName)

        return try {
            val sheetsData = selectedSheets.map { option ->
                option.sheetTitle to buildRowsForSheet(option)
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zip ->
                writeEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheetsData.size))
                writeEntry(zip, "_rels/.rels", ROOT_RELS_XML)
                writeEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheetsData.map { it.first }))
                writeEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheetsData.size))
                writeEntry(zip, "xl/styles.xml", STYLES_XML)

                sheetsData.forEachIndexed { index, (_, rows) ->
                    val sheetPath = "xl/worksheets/sheet${index + 1}.xml"
                    writeEntry(zip, sheetPath, buildWorksheetXml(rows))
                }
            }

            RepoResult.Success(
                GeneratedReport(
                    file = outFile,
                    fileName = fileName,
                    sheetCount = sheetsData.size,
                    fileSizeBytes = outFile.length()
                )
            )
        } catch (e: Exception) {
            RepoResult.Error("Gagal membuat file laporan Excel: ${e.message ?: "kesalahan I/O"}")
        }
    }

    private fun buildRowsForSheet(option: ReportSheetOption): List<List<String>> {
        val generatedAt = "${DateFormatter.formatDate(Date())} ${DateFormatter.formatTime(Date())} WITA"
        return when (option) {
            ReportSheetOption.SALES_PRODUCTS -> {
                val rows = mutableListOf(
                    listOf("Kode Transaksi", "Tanggal", "Jam", "Kasir", "Metode", "Produk", "Qty", "Harga Satuan", "Subtotal")
                )
                for (sale in SalesRepository.getAllSales()) {
                    for (detail in sale.details) {
                        rows.add(
                            listOf(
                                sale.saleNumber,
                                sale.confirmedDateLabel,
                                sale.confirmedTimeLabel,
                                sale.userName,
                                sale.paymentMethod.displayLabel,
                                detail.productName,
                                detail.quantity.toString(),
                                CurrencyFormatter.format(detail.sellingPriceSnapshot),
                                CurrencyFormatter.format(detail.subtotal)
                            )
                        )
                    }
                }
                rows
            }

            ReportSheetOption.INGREDIENT_USAGE -> {
                val rows = mutableListOf(
                    listOf("Kode Transaksi", "Tanggal", "Bahan Baku", "Jumlah Terpakai", "Satuan", "Sisa Stok")
                )
                for (sale in SalesRepository.getAllSales()) {
                    for (eff in sale.stockEffects) {
                        rows.add(
                            listOf(
                                sale.saleNumber,
                                sale.confirmedDateLabel,
                                eff.ingredientName,
                                eff.usedQuantity.toString(),
                                eff.usageUnit,
                                eff.stockAfter.toString()
                            )
                        )
                    }
                }
                rows
            }

            ReportSheetOption.STOCK_MOVEMENTS -> {
                val ingMap = InventoryRepository.getIngredientsMap()
                val rows = mutableListOf(
                    listOf("Waktu", "Bahan Baku", "Jenis", "Referensi", "Pengguna", "Perubahan", "Stok Sebelum", "Stok Setelah")
                )
                for (m in InventoryRepository.movements.value) {
                    val ing = ingMap[m.ingredientId]
                    rows.add(
                        listOf(
                            m.dateTimeLabel,
                            ing?.ingredientName ?: "#${m.ingredientId}",
                            m.movementType.displayLabel,
                            m.referenceLabel,
                            m.userName,
                            "${m.quantity} ${ing?.usageUnit.orEmpty()}",
                            m.stockBefore.toString(),
                            m.stockAfter.toString()
                        )
                    )
                }
                rows
            }

            ReportSheetOption.INGREDIENT_PURCHASES -> {
                val rows = mutableListOf(
                    listOf("Kode Pesanan", "Tanggal Pesan", "Pemasok", "Status", "Bahan", "Jumlah Kemasan", "Harga Kemasan", "Subtotal")
                )
                for (order in PurchaseOrderRepository.getAllOrders()) {
                    for (d in order.details) {
                        rows.add(
                            listOf(
                                order.orderNumber,
                                order.orderDateLabel,
                                order.supplierName,
                                order.status.displayLabel,
                                d.ingredientName,
                                "${d.quantity} ${d.purchaseUnit}",
                                CurrencyFormatter.format(d.packagePriceSnapshot),
                                CurrencyFormatter.format(d.subtotal)
                            )
                        )
                    }
                }
                rows
            }

            ReportSheetOption.PROCUREMENT_LEAD_TIME -> {
                val rows = mutableListOf(
                    listOf("Kode Pesanan", "Pemasok", "Tanggal Pesan", "Tanggal Terima", "Status", "Lead Time")
                )
                for (order in PurchaseOrderRepository.getAllOrders()) {
                    rows.add(
                        listOf(
                            order.orderNumber,
                            order.supplierName,
                            "${order.orderDateLabel} ${order.orderTimeLabel}",
                            order.receivedDateLabel ?: "-",
                            order.status.displayLabel,
                            order.leadTimeSeconds?.let {
                                com.pusacoffee.pusatrack.data.model.LeadTimeFormatter.formatLong(it)
                            } ?: "Dalam perjalanan"
                        )
                    )
                }
                rows
            }

            ReportSheetOption.STOCK_ADJUSTMENTS -> {
                val ingMap = InventoryRepository.getIngredientsMap()
                val rows = mutableListOf(
                    listOf("Waktu", "Bahan Baku", "Pengguna", "Selisih", "Stok Sebelum", "Stok Setelah", "Alasan")
                )
                InventoryRepository.movements.value
                    .filter { it.movementType == StockMovementType.ADJUSTMENT }
                    .forEach { m ->
                        val ing = ingMap[m.ingredientId]
                        rows.add(
                            listOf(
                                m.dateTimeLabel,
                                ing?.ingredientName ?: "#${m.ingredientId}",
                                m.userName,
                                "${m.quantity} ${ing?.usageUnit.orEmpty()}",
                                m.stockBefore.toString(),
                                m.stockAfter.toString(),
                                m.description ?: m.referenceLabel
                            )
                        )
                    }
                rows
            }

            ReportSheetOption.OPERATIONAL_EXPENSES -> {
                val rows = mutableListOf(
                    listOf("Tanggal", "Jam", "Pengguna", "Metode", "Kategori", "Nominal", "Keterangan")
                )
                for (batch in ExpenseRepository.getAllBatches()) {
                    for (item in batch.items) {
                        rows.add(
                            listOf(
                                batch.dateLabel,
                                batch.timeLabel,
                                batch.userName,
                                batch.paymentMethod.displayLabel,
                                item.categoryName,
                                CurrencyFormatter.format(item.amount),
                                item.description ?: "-"
                            )
                        )
                    }
                }
                rows
            }

            ReportSheetOption.FINANCE_SUMMARY -> {
                val summary = FinanceRepository.getSummary(FinancePeriod.MONTHLY)
                listOf(
                    listOf("Komponen Keuangan", "Nilai", "Catatan"),
                    listOf("Periode", summary.periodLabel, "Dibuat $generatedAt"),
                    listOf("Total Penjualan", CurrencyFormatter.format(summary.totalSales), "Tunai + QRIS"),
                    listOf("HPP (Bahan Baku)", CurrencyFormatter.format(summary.totalHpp), "Snapshot resep"),
                    listOf("Laba Kotor", CurrencyFormatter.format(summary.grossProfit), "Penjualan - HPP"),
                    listOf("Pengeluaran Operasional", CurrencyFormatter.format(summary.totalExpenses), "Non-pembelian bahan"),
                    listOf("Estimasi Laba Bersih", CurrencyFormatter.format(summary.netProfit), summary.formatNetMarginPercent()),
                    listOf("Penjualan Tunai", CurrencyFormatter.format(summary.cashSales), ""),
                    listOf("Penjualan QRIS", CurrencyFormatter.format(summary.qrisSales), ""),
                    listOf("Saldo Kas Awal", CurrencyFormatter.format(summary.openingCashBalance), "Terpisah dari laba"),
                    listOf("Perubahan Kas", CurrencyFormatter.format(summary.cashChange), "Tunai masuk - keluar")
                )
            }
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val overrides = (1..sheetCount).joinToString("\n") { i ->
            """  <Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$overrides
</Types>"""
    }

    private fun buildWorkbookXml(sheetNames: List<String>): String {
        val sheetsXml = sheetNames.mapIndexed { idx, name ->
            val safeName = xmlEscape(name.take(31))
            """    <sheet name="$safeName" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>"""
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
$sheetsXml
  </sheets>
</workbook>"""
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val rels = (1..sheetCount).joinToString("\n") { i ->
            """  <Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
  <Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildWorksheetXml(rows: List<List<String>>): String {
        val rowsXml = rows.mapIndexed { rowIdx, cells ->
            val rowNum = rowIdx + 1
            val cellsXml = cells.mapIndexed { colIdx, value ->
                val colRef = columnLetter(colIdx) + rowNum
                val styleAttr = if (rowIdx == 0) """ s="1"""" else ""
                """<c r="$colRef" t="inlineStr"$styleAttr><is><t>${xmlEscape(value)}</t></is></c>"""
            }.joinToString("")
            """    <row r="$rowNum">$cellsXml</row>"""
        }.joinToString("\n")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
$rowsXml
  </sheetData>
</worksheet>"""
    }

    private fun columnLetter(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun xmlEscape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
}
