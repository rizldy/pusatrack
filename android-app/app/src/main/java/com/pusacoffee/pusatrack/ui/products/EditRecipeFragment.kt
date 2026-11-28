package com.pusacoffee.pusatrack.ui.products

import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.data.model.RecipeItem
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult

class EditRecipeFragment : BaseProductFormFragment() {

    private var productId: Int = -1

    override val screenTitle: String = "Edit Resep"
    override val submitButtonLabel: String = "Simpan Perubahan"

    override fun initializeFormState() {
        productId = arguments?.getInt("productId", -1) ?: -1
        val product = ProductRepository.getProductById(productId)
        if (product == null) {
            findNavController().navigateUp()
            return
        }

        binding.etProductName.setText(product.productName)
        binding.etSellingPrice.setText(product.sellingPrice.toString())
        binding.switchActive.isChecked = product.isActive
        recipeItems.clear()
        recipeItems.addAll(product.recipes)
    }

    override fun onSubmitForm(
        name: String,
        sellingPrice: Long,
        isActive: Boolean,
        recipes: List<RecipeItem>
    ) {
        when (
            val result = ProductRepository.updateProduct(
                productId = productId,
                name = name,
                sellingPrice = sellingPrice,
                isActive = isActive,
                recipes = recipes
            )
        ) {
            is RepoResult.Success -> findNavController().navigateUp()
            is RepoResult.Error -> showError(result.message)
        }
    }
}
