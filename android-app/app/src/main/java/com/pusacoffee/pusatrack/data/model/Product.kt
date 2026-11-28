package com.pusacoffee.pusatrack.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class RecipeItem(
    @SerializedName("ingredient_id")
    val ingredientId: Int,
    @SerializedName("quantity_required")
    val quantityRequired: Int
)

data class Product(
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("selling_price")
    val sellingPrice: Long,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("recipes")
    val recipes: List<RecipeItem> = emptyList()
)

data class ProductCostSummary(
    val estimatedHpp: Long,
    val marginAmount: Long,
    val marginPercent: Double
) {
    fun formatMarginPercent(): String {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            decimalSeparator = ','
        }
        val df = DecimalFormat("0.0", symbols)
        return "${df.format(marginPercent)}%"
    }
}

data class ProductUpsertRequest(
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("selling_price")
    val sellingPrice: Long,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("recipes")
    val recipes: List<RecipeItem>
)

object RecipeCalculator {

    /**
     * Calculates HPP = Σ (quantity_required * (package_price / package_size)) using BigDecimal,
     * rounded HALF_UP to nearest Rupiah, plus margin Rp and margin %.
     */
    fun calculate(
        sellingPrice: Long,
        recipes: List<RecipeItem>,
        ingredientsById: Map<Int, Ingredient>
    ): ProductCostSummary {
        var totalHppDecimal = BigDecimal.ZERO
        for (item in recipes) {
            val ingredient = ingredientsById[item.ingredientId] ?: continue
            if (item.quantityRequired <= 0 || ingredient.packageSize <= 0) continue
            val itemCost = ingredient.unitCostDecimal.multiply(
                BigDecimal.valueOf(item.quantityRequired.toLong())
            )
            totalHppDecimal = totalHppDecimal.add(itemCost)
        }

        val hpp = totalHppDecimal.setScale(0, RoundingMode.HALF_UP).toLong()
        val margin = sellingPrice - hpp
        val marginPercent = if (sellingPrice > 0L) {
            BigDecimal.valueOf(margin * 100L)
                .divide(BigDecimal.valueOf(sellingPrice), 2, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }

        return ProductCostSummary(
            estimatedHpp = hpp,
            marginAmount = margin,
            marginPercent = marginPercent
        )
    }
}
