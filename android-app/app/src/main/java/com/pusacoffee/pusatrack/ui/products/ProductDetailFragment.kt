package com.pusacoffee.pusatrack.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.databinding.FragmentProductDetailBinding
import com.pusacoffee.pusatrack.databinding.ItemRecipeRowBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private var productId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = arguments?.getInt("productId", -1) ?: -1

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnEditRecipe.setOnClickListener {
            findNavController().navigate(
                R.id.editRecipeFragment,
                bundleOf("productId" to productId)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    ProductRepository.products,
                    InventoryRepository.ingredients
                ) { products, ingredients ->
                    val product = products.find { it.productId == productId }
                    val map = ingredients.associateBy { it.ingredientId }
                    product to map
                }.collect { (product, ingredientsMap) ->
                    if (product == null) {
                        findNavController().navigateUp()
                        return@collect
                    }
                    bindProduct(product, ingredientsMap)
                }
            }
        }
    }

    private fun bindProduct(product: Product, ingredientsMap: Map<Int, Ingredient>) {
        val ctx = requireContext()
        binding.tvProductName.text = product.productName
        binding.tvSellingPrice.text = CurrencyFormatter.format(product.sellingPrice)

        if (product.isActive) {
            binding.tvStatusChip.text = "Aktif"
            binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_safe)
            binding.tvStatusChip.setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
        } else {
            binding.tvStatusChip.text = "Nonaktif"
            binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_neutral)
            binding.tvStatusChip.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }

        val cost = ProductRepository.calculateProductCost(product)
        binding.tvEstimatedHpp.text = CurrencyFormatter.format(cost.estimatedHpp)
        binding.tvMarginSummary.text =
            "${CurrencyFormatter.format(cost.marginAmount)} (${cost.formatMarginPercent()})"

        binding.containerRecipeRows.removeAllViews()
        binding.tvEmptyRecipes.isVisible = product.recipes.isEmpty()

        for (recipe in product.recipes) {
            val ingredient = ingredientsMap[recipe.ingredientId] ?: continue
            val rowBinding = ItemRecipeRowBinding.inflate(
                layoutInflater,
                binding.containerRecipeRows,
                false
            )
            val itemCost = ingredient.unitCostDecimal
                .multiply(BigDecimal.valueOf(recipe.quantityRequired.toLong()))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()

            rowBinding.tvIngredientName.text = ingredient.ingredientName
            rowBinding.tvItemCost.text = "Biaya bahan: ${CurrencyFormatter.format(itemCost)}"
            rowBinding.tvQuantity.text =
                "${Ingredient.formatNumber(recipe.quantityRequired)} ${ingredient.usageUnit}"
            rowBinding.btnDeleteRow.isVisible = false
            binding.containerRecipeRows.addView(rowBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
