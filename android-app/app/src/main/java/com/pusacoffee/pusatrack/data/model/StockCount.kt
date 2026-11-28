package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName

data class StockCountEntry(
    val ingredient: Ingredient,
    val countedWholePackages: Int,
    val hasOpenPackage: Boolean
) {
    val systemWholePackages: Int
        get() = ingredient.fullPackages

    val systemRemainder: Int
        get() = ingredient.remainderUsage

    /**
     * Physical stock calculation (PRD M6d):
     * - If hasOpenPackage == true: keep system remainder -> countedWhole * packageSize + systemRemainder
     * - If hasOpenPackage == false: open remainder treated as 0 -> countedWhole * packageSize
     */
    val physicalStock: Int
        get() = if (hasOpenPackage) {
            (countedWholePackages * ingredient.packageSize) + systemRemainder
        } else {
            countedWholePackages * ingredient.packageSize
        }

    val delta: Int
        get() = physicalStock - ingredient.currentStock

    val isDiscrepancy: Boolean
        get() = delta != 0
}

data class StockCountAdjustment(
    val ingredientId: Int,
    val ingredientName: String,
    val usageUnit: String,
    val stockBefore: Int,
    val stockAfter: Int,
    val delta: Int
)

data class StockCountRecord(
    val countId: Int,
    val userName: String,
    val dateTimeLabel: String,
    val reason: String,
    val description: String?,
    val adjustments: List<StockCountAdjustment>
)

data class StockCountItemRequest(
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("counted_whole_packages")
    val countedWholePackages: Int,
    @SerializedName("has_open_package")
    val hasOpenPackage: Boolean
)

data class CreateStockCountRequest(
    @SerializedName("reason")
    val reason: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("items")
    val items: List<StockCountItemRequest>
)
