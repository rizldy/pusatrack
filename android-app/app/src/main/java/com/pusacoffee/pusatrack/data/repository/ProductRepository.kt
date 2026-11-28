package com.pusacoffee.pusatrack.data.repository

import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.data.model.ProductCostSummary
import com.pusacoffee.pusatrack.data.model.RecipeCalculator
import com.pusacoffee.pusatrack.data.model.RecipeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared in-memory repository for MS-1 Products & Recipes (L29-L32).
 */
object ProductRepository {

    private val _products = MutableStateFlow(seedProducts())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private var nextProductId = 3

    fun getAllProducts(): List<Product> = _products.value

    fun getProductById(id: Int): Product? =
        _products.value.find { it.productId == id }

    fun calculateProductCost(product: Product): ProductCostSummary {
        return RecipeCalculator.calculate(
            sellingPrice = product.sellingPrice,
            recipes = product.recipes,
            ingredientsById = InventoryRepository.getIngredientsMap()
        )
    }

    fun calculatePreviewCost(sellingPrice: Long, recipes: List<RecipeItem>): ProductCostSummary {
        return RecipeCalculator.calculate(
            sellingPrice = sellingPrice,
            recipes = recipes,
            ingredientsById = InventoryRepository.getIngredientsMap()
        )
    }

    fun validateProductInput(
        name: String,
        sellingPrice: Long,
        recipes: List<RecipeItem>
    ): String? {
        if (name.trim().isEmpty()) {
            return "Nama produk wajib diisi."
        }
        if (sellingPrice <= 0L) {
            return "Harga jual harus lebih dari Rp 0."
        }
        val seenIds = mutableSetOf<Int>()
        for (item in recipes) {
            if (item.quantityRequired <= 0) {
                return "Jumlah bahan pada resep harus lebih dari 0."
            }
            if (!seenIds.add(item.ingredientId)) {
                return "Bahan yang sama tidak boleh duplikat dalam satu resep."
            }
        }
        return null
    }

    fun addProduct(
        name: String,
        sellingPrice: Long,
        isActive: Boolean,
        recipes: List<RecipeItem>
    ): RepoResult<Product> {
        val error = validateProductInput(name, sellingPrice, recipes)
        if (error != null) return RepoResult.Error(error)

        val newProduct = Product(
            productId = nextProductId++,
            productName = name.trim(),
            sellingPrice = sellingPrice,
            isActive = isActive,
            recipes = recipes.toList()
        )
        _products.value = _products.value + newProduct
        return RepoResult.Success(newProduct)
    }

    fun updateProduct(
        productId: Int,
        name: String,
        sellingPrice: Long,
        isActive: Boolean,
        recipes: List<RecipeItem>
    ): RepoResult<Product> {
        val existing = getProductById(productId)
            ?: return RepoResult.Error("Produk tidak ditemukan.")

        val error = validateProductInput(name, sellingPrice, recipes)
        if (error != null) return RepoResult.Error(error)

        val updated = existing.copy(
            productName = name.trim(),
            sellingPrice = sellingPrice,
            isActive = isActive,
            recipes = recipes.toList()
        )
        _products.value = _products.value.map {
            if (it.productId == productId) updated else it
        }
        return RepoResult.Success(updated)
    }

    fun resetForTesting() {
        _products.value = seedProducts()
        nextProductId = 3
    }

    private fun seedProducts(): List<Product> = listOf(
        Product(
            productId = 1,
            productName = "Kopi Susu",
            sellingPrice = 10_000L,
            isActive = true,
            recipes = listOf(
                RecipeItem(ingredientId = 2, quantityRequired = 18),  // 18 g Bubuk Kopi
                RecipeItem(ingredientId = 3, quantityRequired = 150), // 150 ml Susu UHT
                RecipeItem(ingredientId = 4, quantityRequired = 1)    // 1 pcs Cup Plastik 12 oz
            )
        ),
        Product(
            productId = 2,
            productName = "Strawberry",
            sellingPrice = 10_000L,
            isActive = true,
            recipes = listOf(
                RecipeItem(ingredientId = 1, quantityRequired = 15),  // 15 g Bubuk Strawberry
                RecipeItem(ingredientId = 3, quantityRequired = 150), // 150 ml Susu UHT
                RecipeItem(ingredientId = 4, quantityRequired = 1)    // 1 pcs Cup Plastik 12 oz
            )
        )
    )
}
