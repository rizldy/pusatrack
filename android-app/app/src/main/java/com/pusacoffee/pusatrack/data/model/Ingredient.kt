package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class ProcurementType(val apiValue: String, val displayLabel: String) {
    @SerializedName("SUPPLIED")
    SUPPLIED("SUPPLIED", "Dipesan ke pusat"),

    @SerializedName("DIRECT")
    DIRECT("DIRECT", "Beli langsung")
}

enum class StockStatus(val label: String) {
    SAFE("Aman"),
    LOW("Menipis"),
    OUT("Habis")
}

enum class StockMovementType(val apiValue: String, val displayLabel: String) {
    @SerializedName("SALE")
    SALE("SALE", "Penjualan"),

    @SerializedName("PURCHASE")
    PURCHASE("PURCHASE", "Penerimaan pesanan"),

    @SerializedName("DIRECT_PURCHASE")
    DIRECT_PURCHASE("DIRECT_PURCHASE", "Beli langsung"),

    @SerializedName("OPENING_STOCK")
    OPENING_STOCK("OPENING_STOCK", "Stok awal"),

    @SerializedName("ADJUSTMENT")
    ADJUSTMENT("ADJUSTMENT", "Pemeriksaan fisik")
}

data class Ingredient(
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("ingredient_name")
    val ingredientName: String,
    @SerializedName("purchase_unit")
    val purchaseUnit: String,
    @SerializedName("package_size")
    val packageSize: Int,
    @SerializedName("usage_unit")
    val usageUnit: String,
    @SerializedName("current_stock")
    val currentStock: Int,
    @SerializedName("minimum_stock_packages")
    val minimumStockPackages: Int,
    @SerializedName("package_price")
    val packagePrice: Long,
    @SerializedName("procurement_type")
    val procurementType: ProcurementType,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("image_path")
    val imagePath: String? = null
) {
    /** Minimum stock expressed in usage unit (minimum_stock_packages * package_size). */
    val minimumStockUsage: Int
        get() = minimumStockPackages * packageSize

    /** R-01: Full packages = currentStock / packageSize. */
    val fullPackages: Int
        get() = if (packageSize > 0) currentStock / packageSize else 0

    /** R-01: Open remainder = currentStock % packageSize. */
    val remainderUsage: Int
        get() = if (packageSize > 0) currentStock % packageSize else 0

    /** Status: Habis (0), Menipis (0 < stock <= min), Aman (> min). */
    val stockStatus: StockStatus
        get() = when {
            currentStock <= 0 -> StockStatus.OUT
            currentStock <= minimumStockUsage -> StockStatus.LOW
            else -> StockStatus.SAFE
        }

    /** Exact BigDecimal cost per 1 usage unit = packagePrice / packageSize. */
    val unitCostDecimal: BigDecimal
        get() = if (packageSize > 0) {
            BigDecimal.valueOf(packagePrice).divide(
                BigDecimal.valueOf(packageSize.toLong()),
                8,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

    /** Formats stock as usage unit, e.g., "2.140 ml". */
    fun formatCurrentStockUsage(): String = "${formatNumber(currentStock)} $usageUnit"

    /** Formats stock in packages + remainder per R-01, e.g., "2 kotak + 300 ml". */
    fun formatPackageBreakdown(): String {
        return when {
            currentStock <= 0 -> "0 $purchaseUnit"
            fullPackages > 0 && remainderUsage > 0 ->
                "${formatNumber(fullPackages)} $purchaseUnit + ${formatNumber(remainderUsage)} $usageUnit"
            fullPackages > 0 ->
                "${formatNumber(fullPackages)} $purchaseUnit"
            else ->
                "${formatNumber(remainderUsage)} $usageUnit"
        }
    }

    /** Formats minimum stock, e.g., "min. 3 kotak". */
    fun formatMinimumLabel(): String = "min. ${formatNumber(minimumStockPackages)} $purchaseUnit"

    /** Formats unit cost, e.g., "Rp 20,11" or "Rp 120". */
    fun formatUnitCost(): String = formatUnitCostValue(unitCostDecimal)

    /** Progress percentage relative to 2x minimum stock (capped 0..100) for visual bar. */
    fun stockProgressPercent(): Int {
        val referenceMax = (minimumStockUsage * 2).coerceAtLeast(packageSize).coerceAtLeast(1)
        return ((currentStock.toDouble() / referenceMax.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    companion object {
        private val idSymbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }

        private val intFormatter = DecimalFormat("#,##0", idSymbols)
        private val decFormatter = DecimalFormat("#,##0.##", idSymbols)

        fun formatNumber(value: Int): String = intFormatter.format(value)

        fun formatUnitCostValue(cost: BigDecimal): String {
            val rounded = cost.setScale(2, RoundingMode.HALF_UP)
            val stripped = rounded.stripTrailingZeros()
            return if (stripped.scale() <= 0) {
                "Rp ${intFormatter.format(rounded.toLong())}"
            } else {
                "Rp ${decFormatter.format(rounded.toDouble())}"
            }
        }
    }
}

data class StockMovement(
    @SerializedName("movement_id")
    val movementId: Int,
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("movement_type")
    val movementType: StockMovementType,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("stock_before")
    val stockBefore: Int,
    @SerializedName("stock_after")
    val stockAfter: Int,
    @SerializedName("reference_label")
    val referenceLabel: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("date_time_label")
    val dateTimeLabel: String,
    @SerializedName("description")
    val description: String? = null
)

data class StockInRequest(
    @SerializedName("purchase_place")
    val purchasePlace: String?,
    @SerializedName("date_time")
    val dateTime: String,
    @SerializedName("package_quantity")
    val packageQuantity: Int,
    @SerializedName("package_price")
    val packagePrice: Long
)

data class IngredientUpsertRequest(
    @SerializedName("ingredient_name")
    val ingredientName: String,
    @SerializedName("purchase_unit")
    val purchaseUnit: String,
    @SerializedName("usage_unit")
    val usageUnit: String,
    @SerializedName("package_size")
    val packageSize: Int,
    @SerializedName("package_price")
    val packagePrice: Long,
    @SerializedName("minimum_stock_packages")
    val minimumStockPackages: Int,
    @SerializedName("procurement_type")
    val procurementType: ProcurementType,
    @SerializedName("opening_stock_packages")
    val openingStockPackages: Int = 0
)
