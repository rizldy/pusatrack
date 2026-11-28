package com.pusacoffee.pusatrack.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.RecipeItem
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.databinding.DialogAddRecipeIngredientBinding
import com.pusacoffee.pusatrack.databinding.FragmentProductFormBinding
import com.pusacoffee.pusatrack.databinding.ItemRecipeRowBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BaseProductFormFragment : Fragment() {

    private var _binding: FragmentProductFormBinding? = null
    protected val binding get() = _binding!!

    protected val recipeItems = mutableListOf<RecipeItem>()

    protected abstract val screenTitle: String
    protected abstract val submitButtonLabel: String
    protected abstract fun initializeFormState()
    protected abstract fun onSubmitForm(
        name: String,
        sellingPrice: Long,
        isActive: Boolean,
        recipes: List<RecipeItem>
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvScreenTitle.text = screenTitle
        binding.btnSubmit.text = submitButtonLabel
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        initializeFormState()

        binding.etProductName.doAfterTextChanged { binding.layoutErrorBanner.isVisible = false }
        binding.etSellingPrice.doAfterTextChanged {
            binding.layoutErrorBanner.isVisible = false
            updateLiveCostPreview()
        }

        binding.btnAddIngredientRow.setOnClickListener { showAddIngredientDialog() }

        binding.btnSubmit.setOnClickListener {
            val name = binding.etProductName.text?.toString().orEmpty()
            val price = binding.etSellingPrice.text?.toString()?.trim()?.toLongOrNull() ?: 0L
            val isActive = binding.switchActive.isChecked
            onSubmitForm(name, price, isActive, recipeItems.toList())
        }

        renderRecipeRows()
        updateLiveCostPreview()
    }

    protected fun showError(message: String) {
        binding.tvErrorMessage.text = message
        binding.layoutErrorBanner.isVisible = true
    }

    private fun showAddIngredientDialog() {
        val usedIds = recipeItems.map { it.ingredientId }.toSet()
        val available = InventoryRepository.getAllIngredients()
            .filter { it.isActive && it.ingredientId !in usedIds }

        if (available.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Semua bahan baku aktif sudah ada di resep ini.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogBinding = DialogAddRecipeIngredientBinding.inflate(layoutInflater)
        val labels = available.map { "${it.ingredientName} (${it.usageUnit})" }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
        dialogBinding.spinnerIngredient.adapter = spinnerAdapter

        var selectedIngredient: Ingredient = available.first()
        dialogBinding.tvQuantityLabel.text = "Jumlah per 1 porsi (${selectedIngredient.usageUnit})"

        dialogBinding.spinnerIngredient.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedIngredient = available[position]
                    dialogBinding.tvQuantityLabel.text =
                        "Jumlah per 1 porsi (${selectedIngredient.usageUnit})"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tambah bahan resep")
            .setView(dialogBinding.root)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Tambahkan") { _, _ ->
                val qty = dialogBinding.etQuantity.text?.toString()?.trim()?.toIntOrNull() ?: 0
                if (qty <= 0) {
                    showError("Jumlah bahan resep harus lebih dari 0.")
                    return@setPositiveButton
                }
                if (recipeItems.any { it.ingredientId == selectedIngredient.ingredientId }) {
                    showError("Bahan yang sama tidak boleh duplikat dalam satu resep.")
                    return@setPositiveButton
                }
                binding.layoutErrorBanner.isVisible = false
                recipeItems.add(
                    RecipeItem(
                        ingredientId = selectedIngredient.ingredientId,
                        quantityRequired = qty
                    )
                )
                renderRecipeRows()
                updateLiveCostPreview()
            }
            .show()
    }

    protected fun renderRecipeRows() {
        val ingredientsMap = InventoryRepository.getIngredientsMap()
        binding.containerRecipeRows.removeAllViews()
        binding.layoutEmptyRecipes.isVisible = recipeItems.isEmpty()

        recipeItems.forEachIndexed { index, item ->
            val ingredient = ingredientsMap[item.ingredientId] ?: return@forEachIndexed
            val rowBinding = ItemRecipeRowBinding.inflate(
                layoutInflater,
                binding.containerRecipeRows,
                false
            )
            val itemCost = ingredient.unitCostDecimal
                .multiply(BigDecimal.valueOf(item.quantityRequired.toLong()))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()

            rowBinding.tvIngredientName.text = ingredient.ingredientName
            rowBinding.tvItemCost.text = "Biaya bahan: ${CurrencyFormatter.format(itemCost)}"
            rowBinding.tvQuantity.text =
                "${Ingredient.formatNumber(item.quantityRequired)} ${ingredient.usageUnit}"
            rowBinding.btnDeleteRow.isVisible = true
            rowBinding.btnDeleteRow.setOnClickListener {
                recipeItems.removeAt(index)
                renderRecipeRows()
                updateLiveCostPreview()
            }
            binding.containerRecipeRows.addView(rowBinding.root)
        }
    }

    protected fun updateLiveCostPreview() {
        val sellingPrice = binding.etSellingPrice.text?.toString()?.trim()?.toLongOrNull() ?: 0L
        val summary = ProductRepository.calculatePreviewCost(sellingPrice, recipeItems)
        binding.tvPreviewHpp.text = CurrencyFormatter.format(summary.estimatedHpp)
        binding.tvPreviewMargin.text = if (sellingPrice > 0L) {
            "${CurrencyFormatter.format(summary.marginAmount)} (${summary.formatMarginPercent()})"
        } else {
            "Rp 0 (0,0%)"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
