package com.pusacoffee.pusatrack.ui.inventory

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentAddStockBinding
import com.pusacoffee.pusatrack.utils.DateFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

class AddStockFragment : Fragment() {

    private var _binding: FragmentAddStockBinding? = null
    private val binding get() = _binding!!

    private var ingredient: Ingredient? = null
    private val selectedCalendar: Calendar = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStockBinding.inflate(inflater, container, false)
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

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvIngredientSubtitle.text =
            "${item.ingredientName} · 1 ${item.purchaseUnit} = ${Ingredient.formatNumber(item.packageSize)} ${item.usageUnit}"
        binding.tvPackageQtyLabel.text = "Jumlah kemasan (${item.purchaseUnit})"
        binding.tvPackagePriceLabel.text = "Harga per ${item.purchaseUnit} (Rp)"
        binding.etPackagePrice.setText(item.packagePrice.toString())

        updateDateTimeLabel()
        updateLivePreview()

        binding.btnPickDateTime.setOnClickListener { openDateTimePicker() }

        binding.etPackageQty.doAfterTextChanged {
            binding.layoutErrorBanner.isVisible = false
            updateLivePreview()
        }
        binding.etPackagePrice.doAfterTextChanged {
            binding.layoutErrorBanner.isVisible = false
            updateLivePreview()
        }

        binding.btnSaveStock.setOnClickListener { saveStock() }
    }

    private fun openDateTimePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedCalendar.set(Calendar.MINUTE, minute)
                        binding.layoutErrorBanner.isVisible = false
                        updateDateTimeLabel()
                    },
                    selectedCalendar.get(Calendar.HOUR_OF_DAY),
                    selectedCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun updateDateTimeLabel() {
        val date = selectedCalendar.time
        binding.tvDateTime.text = "${DateFormatter.formatDate(date)} · ${DateFormatter.formatTime(date)}"
    }

    private fun updateLivePreview() {
        val item = ingredient ?: return
        val qty = binding.etPackageQty.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val price = binding.etPackagePrice.text?.toString()?.trim()?.toLongOrNull() ?: item.packagePrice
        val addedUsage = qty * item.packageSize
        val resultingStock = item.currentStock + addedUsage

        val unitCost = if (item.packageSize > 0) {
            BigDecimal.valueOf(price).divide(
                BigDecimal.valueOf(item.packageSize.toLong()),
                8,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

        binding.tvPreviewDelta.text =
            "+${Ingredient.formatNumber(qty)} × ${Ingredient.formatNumber(item.packageSize)} ${item.usageUnit} = +${Ingredient.formatNumber(addedUsage)} ${item.usageUnit}"
        binding.tvPreviewResult.text =
            "Stok jadi ${Ingredient.formatNumber(resultingStock)} ${item.usageUnit} · harga bahan ${Ingredient.formatUnitCostValue(unitCost)} per ${item.usageUnit}"
    }

    private fun saveStock() {
        val item = ingredient ?: return
        val qty = binding.etPackageQty.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val priceText = binding.etPackagePrice.text?.toString()?.trim().orEmpty()
        val price = priceText.toLongOrNull()
        if (priceText.isEmpty() || price == null) {
            showError("Harga per kemasan wajib diisi dengan angka valid.")
            return
        }

        val nowMillis = System.currentTimeMillis() + 60_000L // 1 min tolerance
        val isFuture = selectedCalendar.timeInMillis > nowMillis
        val userName = SessionManager(requireContext()).getFullName().orEmpty().ifBlank { "Karyawan" }
        val dateTimeLabel = binding.tvDateTime.text.toString()

        when (
            val result = InventoryRepository.addDirectStock(
                ingredientId = item.ingredientId,
                packageQuantity = qty,
                packagePrice = price,
                purchasePlace = binding.etPurchasePlace.text?.toString(),
                userName = userName,
                dateTimeLabel = dateTimeLabel,
                isFutureDate = isFuture
            )
        ) {
            is RepoResult.Success -> findNavController().navigateUp()
            is RepoResult.Error -> showError(result.message)
        }
    }

    private fun showError(message: String) {
        binding.tvErrorMessage.text = message
        binding.layoutErrorBanner.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
