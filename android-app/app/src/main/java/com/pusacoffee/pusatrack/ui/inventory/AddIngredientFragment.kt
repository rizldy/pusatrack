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
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentIngredientFormBinding
import com.pusacoffee.pusatrack.utils.DateFormatter
import java.math.BigDecimal
import java.math.RoundingMode

class AddIngredientFragment : Fragment() {

    private var _binding: FragmentIngredientFormBinding? = null
    private val binding get() = _binding!!

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

        binding.tvScreenTitle.text = "Tambah Bahan"
        binding.btnSubmit.text = "Simpan"
        binding.layoutLockedStock.isVisible = false
        binding.layoutOpeningStock.isVisible = true
        binding.bannerOpeningStockInfo.isVisible = true

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
        binding.etOpeningStock.doAfterTextChanged { watcher() }
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
        val unit = binding.etUsageUnit.text?.toString()?.trim().orEmpty().ifEmpty { "satuan" }
        val pkgSize = binding.etPackageSize.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val openingPkg = binding.etOpeningStock.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val pkgPrice = binding.etPackagePrice.text?.toString()?.trim()?.toLongOrNull() ?: 0L
        val minPkg = binding.etMinPackages.text?.toString()?.trim()?.toIntOrNull() ?: 0

        val minUsage = minPkg * pkgSize
        binding.tvMinConversion.text = "= ${Ingredient.formatNumber(minUsage)} $unit"

        val openingUsage = openingPkg * pkgSize
        binding.tvPreviewPrimary.text =
            "Stok awal: ${Ingredient.formatNumber(openingPkg)} × ${Ingredient.formatNumber(pkgSize)} $unit = ${Ingredient.formatNumber(openingUsage)} $unit"

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
        val name = binding.etName.text?.toString().orEmpty()
        val purchaseUnit = binding.etPurchaseUnit.text?.toString().orEmpty()
        val usageUnit = binding.etUsageUnit.text?.toString().orEmpty()
        val pkgSize = binding.etPackageSize.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val openingPkg = binding.etOpeningStock.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val pkgPrice = binding.etPackagePrice.text?.toString()?.trim()?.toLongOrNull() ?: -1L
        val minPkg = binding.etMinPackages.text?.toString()?.trim()?.toIntOrNull() ?: -1

        val userName = SessionManager(requireContext()).getFullName().orEmpty().ifBlank { "Karyawan" }
        val dateTimeLabel = "${DateFormatter.formatDate()} · ${DateFormatter.formatTime()}"

        when (
            val result = InventoryRepository.addIngredient(
                name = name,
                purchaseUnit = purchaseUnit,
                usageUnit = usageUnit,
                packageSize = pkgSize,
                openingStockPackages = openingPkg,
                packagePrice = pkgPrice,
                minimumStockPackages = minPkg,
                procurementType = selectedProcurement,
                userName = userName,
                dateTimeLabel = dateTimeLabel
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
