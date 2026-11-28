package com.pusacoffee.pusatrack.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentIngredientFormBinding
import java.math.BigDecimal
import java.math.RoundingMode

class EditIngredientFragment : Fragment() {

    private var _binding: FragmentIngredientFormBinding? = null
    private val binding get() = _binding!!

    private var ingredient: Ingredient? = null
    private var selectedProcurement: ProcurementType = ProcurementType.SUPPLIED

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ingredientId = arguments?.getInt("ingredientId", -1) ?: -1
        ingredient = InventoryRepository.getIngredientById(ingredientId)
        val item = ingredient
        if (item == null) {
            findNavController().navigateUp()
            return
        }

        binding.tvScreenTitle.text = "Edit Bahan"
        binding.btnSubmit.text = "Simpan Perubahan"
        binding.layoutLockedStock.isVisible = true
        binding.layoutOpeningStock.isVisible = false
        binding.bannerOpeningStockInfo.isVisible = false

        binding.tvLockedStockValue.text =
            "${item.formatCurrentStockUsage()} (${item.formatPackageBreakdown()})"

        binding.etName.setText(item.ingredientName)
        binding.etPurchaseUnit.setText(item.purchaseUnit)
        binding.etUsageUnit.setText(item.usageUnit)
        binding.etPackageSize.setText(item.packageSize.toString())
        binding.etPackagePrice.setText(item.packagePrice.toString())
        binding.etMinPackages.setText(item.minimumStockPackages.toString())
        selectedProcurement = item.procurementType

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnProcurementSupplied.setOnClickListener {
            selectedProcurement = ProcurementType.SUPPLIED
            updateProcurementUi()
        }
        binding.btnProcurementDirect.setOnClickListener {
            selectedProcurement = ProcurementType.DIRECT
            updateProcurementUi()
        }

        val watcher = {
            binding.layoutErrorBanner.isVisible = false
            updateLivePreviews()
        }
        binding.etUsageUnit.doAfterTextChanged { watcher() }
        binding.etPackageSize.doAfterTextChanged { watcher() }
        binding.etPackagePrice.doAfterTextChanged { watcher() }
        binding.etMinPackages.doAfterTextChanged { watcher() }

        binding.btnSubmit.setOnClickListener { submitForm() }

        updateProcurementUi()
        updateLivePreviews()
    }

    private fun updateProcurementUi() {
        val ctx = requireContext()
        val suppliedActive = selectedProcurement == ProcurementType.SUPPLIED
        binding.btnProcurementSupplied.setBackgroundResource(
            if (suppliedActive) R.drawable.bg_chip_selected else 0
        )
        binding.btnProcurementSupplied.setTextColor(
            ContextCompat.getColor(ctx, if (suppliedActive) R.color.bg_white else R.color.text_muted)
        )
        binding.btnProcurementDirect.setBackgroundResource(
            if (!suppliedActive) R.drawable.bg_chip_selected else 0
        )
        binding.btnProcurementDirect.setTextColor(
            ContextCompat.getColor(ctx, if (!suppliedActive) R.color.bg_white else R.color.text_muted)
        )
    }

    private fun updateLivePreviews() {
        val item = ingredient ?: return
        val unit = binding.etUsageUnit.text?.toString()?.trim().orEmpty().ifEmpty { item.usageUnit }
        val pkgSize = binding.etPackageSize.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val pkgPrice = binding.etPackagePrice.text?.toString()?.trim()?.toLongOrNull() ?: 0L
        val minPkg = binding.etMinPackages.text?.toString()?.trim()?.toIntOrNull() ?: 0

        val minUsage = minPkg * pkgSize
        binding.tvMinConversion.text = "= ${Ingredient.formatNumber(minUsage)} $unit"

        binding.tvPreviewPrimary.text =
            "Stok tetap: ${Ingredient.formatNumber(item.currentStock)} $unit"

        val unitCost = if (pkgSize > 0) {
            BigDecimal.valueOf(pkgPrice).divide(
                BigDecimal.valueOf(pkgSize.toLong()),
                8,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }
        binding.tvPreviewUnitCost.text =
            "Harga satuan pakai: ${Ingredient.formatUnitCostValue(unitCost)} per $unit"
    }

    private fun submitForm() {
        val item = ingredient ?: return
        val name = binding.etName.text?.toString().orEmpty()
        val purchaseUnit = binding.etPurchaseUnit.text?.toString().orEmpty()
        val usageUnit = binding.etUsageUnit.text?.toString().orEmpty()
        val pkgSize = binding.etPackageSize.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val pkgPrice = binding.etPackagePrice.text?.toString()?.trim()?.toLongOrNull() ?: -1L
        val minPkg = binding.etMinPackages.text?.toString()?.trim()?.toIntOrNull() ?: -1

        when (
            val result = InventoryRepository.updateIngredient(
                ingredientId = item.ingredientId,
                name = name,
                purchaseUnit = purchaseUnit,
                usageUnit = usageUnit,
                packageSize = pkgSize,
                packagePrice = pkgPrice,
                minimumStockPackages = minPkg,
                procurementType = selectedProcurement
            )
        ) {
            is RepoResult.Success -> findNavController().navigateUp()
            is RepoResult.Error -> {
                binding.tvErrorMessage.text = result.message
                binding.layoutErrorBanner.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
