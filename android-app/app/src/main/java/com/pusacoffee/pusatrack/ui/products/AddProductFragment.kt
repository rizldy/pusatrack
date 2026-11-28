package com.pusacoffee.pusatrack.ui.products

import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.data.model.RecipeItem
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult

class AddProductFragment : BaseProductFormFragment() {

    override val screenTitle: String = "Tambah Produk"
    override val submitButtonLabel: String = "Simpan Produk"

    override fun initializeFormState() {
        binding.switchActive.isChecked = true
        recipeItems.clear()
    }

    override fun onSubmitForm(
        name: String,
        sellingPrice: Long,
        isActive: Boolean,
        recipes: List<RecipeItem>
    ) {
        when (
            val result = ProductRepository.addProduct(
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
