package com.pusacoffee.pusatrack.ui.orders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.ProcurementType
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.databinding.FragmentCreateOrderBinding
import com.pusacoffee.pusatrack.databinding.ItemCartRowBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import java.util.Calendar

class CreateOrderFragment : Fragment() {

    private var _binding: FragmentCreateOrderBinding? = null
    private val binding get() = _binding!!

    private val orderCalendar: Calendar = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)
    private val selectedQuantities = linkedMapOf<Int, Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.etSupplier.setText("Pusat Pusa Coffee")
        updateDateTimeLabel()

        // Pre-populate with first supplied ingredient if empty
        if (selectedQuantities.isEmpty()) {
            val firstSupplied = InventoryRepository.getAllIngredients()
                .firstOrNull { it.isActive && it.procurementType == ProcurementType.SUPPLIED }
            if (firstSupplied != null) {
                selectedQuantities[firstSupplied.ingredientId] = 1
            }
        }

        binding.btnPickOrderDateTime.setOnClickListener { pickOrderDateTime() }
        binding.btnAddOrderItem.setOnClickListener { showAddSuppliedIngredientDialog() }

        binding.btnReviewOrder.setOnClickListener { openConfirmDialog() }

        childFragmentManager.setFragmentResultListener(
            ConfirmOrderDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val createdOrderId = bundle.getInt(ConfirmOrderDialogFragment.KEY_ORDER_ID, -1)
            if (createdOrderId != -1) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.createOrderFragment, true)
                    .build()
                val args = Bundle().apply { putInt("orderId", createdOrderId) }
                findNavController().navigate(R.id.orderCreatedFragment, args, navOptions)
            }
        }

        renderOrderItems()
    }

    private fun pickOrderDateTime() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                orderCalendar.set(Calendar.YEAR, year)
                orderCalendar.set(Calendar.MONTH, month)
                orderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        orderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        orderCalendar.set(Calendar.MINUTE, minute)
                        updateDateTimeLabel()
                    },
                    orderCalendar.get(Calendar.HOUR_OF_DAY),
                    orderCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            orderCalendar.get(Calendar.YEAR),
            orderCalendar.get(Calendar.MONTH),
            orderCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeLabel() {
        val date = orderCalendar.time
        binding.tvOrderDateTime.text = "${DateFormatter.formatDate(date)} · ${DateFormatter.formatTime(date)}"
    }

    private fun showAddSuppliedIngredientDialog() {
        val available = InventoryRepository.getAllIngredients().filter {
            it.isActive &&
                it.procurementType == ProcurementType.SUPPLIED &&
                it.ingredientId !in selectedQuantities
        }

        if (available.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Semua bahan dipasok pusat sudah ada di daftar pesanan.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val labels = available.map {
            "${it.ingredientName} (${CurrencyFormatter.format(it.packagePrice)}/${it.purchaseUnit})"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tambah bahan dipasok pusat")
            .setItems(labels) { _, which ->
                val chosen = available[which]
                selectedQuantities[chosen.ingredientId] = 1
                binding.layoutErrorBanner.isVisible = false
                renderOrderItems()
            }
            .show()
    }

    private fun renderOrderItems() {
        val ingredientsMap = InventoryRepository.getIngredientsMap()
        binding.containerOrderItems.removeAllViews()
        binding.tvEmptyOrderItems.isVisible = selectedQuantities.isEmpty()

        var totalAmount = 0L
        var totalPackages = 0

        for ((ingredientId, qty) in selectedQuantities.toMap()) {
            val ingredient: Ingredient = ingredientsMap[ingredientId] ?: continue
            val subtotal = ingredient.packagePrice * qty
            totalAmount += subtotal
            totalPackages += qty

            val rowBinding = ItemCartRowBinding.inflate(
                layoutInflater,
                binding.containerOrderItems,
                false
            )
            rowBinding.tvItemName.text = ingredient.ingredientName
            rowBinding.tvItemSubtotal.text =
                "${CurrencyFormatter.format(ingredient.packagePrice)}/${ingredient.purchaseUnit} · ${CurrencyFormatter.format(subtotal)}"
            rowBinding.tvQty.text = qty.toString()

            rowBinding.btnMinus.setOnClickListener {
                val next = qty - 1
                if (next <= 0) {
                    selectedQuantities.remove(ingredientId)
                } else {
                    selectedQuantities[ingredientId] = next
                }
                renderOrderItems()
            }
            rowBinding.btnPlus.setOnClickListener {
                selectedQuantities[ingredientId] = qty + 1
                renderOrderItems()
            }
            binding.containerOrderItems.addView(rowBinding.root)
        }

        binding.tvOrderCounts.text = "${selectedQuantities.size} bahan · $totalPackages kemasan"
        binding.tvOrderTotal.text = CurrencyFormatter.format(totalAmount)
    }

    private fun openConfirmDialog() {
        val supplier = binding.etSupplier.text?.toString()?.trim().orEmpty()
        if (supplier.isEmpty()) {
            showError("Nama pemasok wajib diisi.")
            return
        }
        if (selectedQuantities.isEmpty()) {
            showError("Pilih minimal 1 bahan baku untuk dipesan.")
            return
        }

        val dialog = ConfirmOrderDialogFragment.newInstance(
            supplierName = supplier,
            orderDateMillis = orderCalendar.timeInMillis,
            items = selectedQuantities
        )
        dialog.show(childFragmentManager, ConfirmOrderDialogFragment.TAG)
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
