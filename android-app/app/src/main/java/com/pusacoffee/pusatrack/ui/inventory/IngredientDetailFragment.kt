package com.pusacoffee.pusatrack.ui.inventory

import android.content.res.ColorStateList
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.model.StockStatus
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.databinding.FragmentIngredientDetailBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IngredientDetailFragment : Fragment() {

    private var _binding: FragmentIngredientDetailBinding? = null
    private val binding get() = _binding!!

    private var ingredientId: Int = -1
    private val movementAdapter = StockMovementAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ingredientId = arguments?.getInt("ingredientId", -1) ?: -1

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.rvMovements.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovements.adapter = movementAdapter

        binding.btnEditIngredient.setOnClickListener {
            findNavController().navigate(
                R.id.editIngredientFragment,
                bundleOf("ingredientId" to ingredientId)
            )
        }

        binding.btnAddStock.setOnClickListener {
            findNavController().navigate(
                R.id.addStockFragment,
                bundleOf("ingredientId" to ingredientId)
            )
        }

        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    InventoryRepository.ingredients,
                    InventoryRepository.movements
                ) { ingredients, movements ->
                    val ingredient = ingredients.find { it.ingredientId == ingredientId }
                    val history = movements.filter { it.ingredientId == ingredientId }
                    ingredient to history
                }.collect { (ingredient, history) ->
                    if (ingredient == null) {
                        findNavController().navigateUp()
                        return@collect
                    }
                    bindIngredient(ingredient)
                    movementAdapter.updateUnit(ingredient.usageUnit)
                    movementAdapter.submitList(history)
                    binding.tvEmptyMovements.isVisible = history.isEmpty()
                    binding.rvMovements.isVisible = history.isNotEmpty()
                }
            }
        }
    }

    private fun bindIngredient(item: Ingredient) {
        val ctx = requireContext()
        binding.tvTitle.text = item.ingredientName
        binding.tvSubtitle.text =
            "1 ${item.purchaseUnit} = ${Ingredient.formatNumber(item.packageSize)} ${item.usageUnit} · ${item.procurementType.displayLabel}"

        binding.tvStockUsage.text = item.formatCurrentStockUsage()
        binding.tvStockPackages.text = "Setara ${item.formatPackageBreakdown()}"
        binding.progressStock.progress = item.stockProgressPercent()

        binding.tvPackagePrice.text =
            "${CurrencyFormatter.format(item.packagePrice)} / ${item.purchaseUnit}"
        binding.tvUnitCost.text = "${item.formatUnitCost()} per ${item.usageUnit}"

        binding.tvMinPackages.text =
            "${Ingredient.formatNumber(item.minimumStockPackages)} ${item.purchaseUnit}"
        binding.tvMinUsage.text =
            "= ${Ingredient.formatNumber(item.minimumStockUsage)} ${item.usageUnit}"

        when (item.stockStatus) {
            StockStatus.SAFE -> {
                binding.tvStatusChip.text = StockStatus.SAFE.label
                binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_safe)
                binding.tvStatusChip.setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
                binding.progressStock.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.primary_forest)
                )
            }

            StockStatus.LOW -> {
                binding.tvStatusChip.text = StockStatus.LOW.label
                binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_low)
                binding.tvStatusChip.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_text))
                binding.progressStock.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.secondary_caramel)
                )
            }

            StockStatus.OUT -> {
                binding.tvStatusChip.text = StockStatus.OUT.label
                binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_out)
                binding.tvStatusChip.setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
                binding.progressStock.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.tertiary_berry)
                )
            }
        }

        // Tambah stok (Beli langsung) only for DIRECT procurement.
        val isDirect = item.procurementType == ProcurementType.DIRECT
        binding.btnAddStock.isVisible = isDirect
        binding.bannerSuppliedNote.isVisible = !isDirect
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
