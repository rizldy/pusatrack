package com.pusacoffee.pusatrack.ui.orders

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.DialogConfirmOrderBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

class ConfirmOrderDialogFragment : DialogFragment() {

    private var _binding: DialogConfirmOrderBinding? = null
    private val binding get() = _binding!!

    private var supplierName: String = "Pusat Pusa Coffee"
    private var orderDateMillis: Long = System.currentTimeMillis()
    private val itemsMap = linkedMapOf<Int, Int>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogConfirmOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        supplierName = arguments?.getString(ARG_SUPPLIER).orEmpty().ifBlank { "Pusat Pusa Coffee" }
        orderDateMillis = arguments?.getLong(ARG_DATE_MILLIS, System.currentTimeMillis())
            ?: System.currentTimeMillis()
        val ids = arguments?.getIntArray(ARG_ITEM_IDS) ?: IntArray(0)
        val qtys = arguments?.getIntArray(ARG_ITEM_QTYS) ?: IntArray(0)
        itemsMap.clear()
        for (i in ids.indices) {
            if (i < qtys.size && qtys[i] > 0) {
                itemsMap[ids[i]] = qtys[i]
            }
        }

        binding.tvConfirmSupplier.text = "Pemasok: $supplierName"

        val ctx = requireContext()
        val ingredientsMap = InventoryRepository.getIngredientsMap()
        var totalAmount = 0L

        binding.containerConfirmItems.removeAllViews()
        for ((id, qty) in itemsMap) {
            val ing = ingredientsMap[id] ?: continue
            val subtotal = ing.packagePrice * qty
            totalAmount += subtotal

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val left = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = "${ing.ingredientName} ($qty ${ing.purchaseUnit})"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            val right = TextView(ctx).apply {
                text = CurrencyFormatter.format(subtotal)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            row.addView(left)
            row.addView(right)
            binding.containerConfirmItems.addView(row)
        }

        binding.tvConfirmTotal.text = CurrencyFormatter.format(totalAmount)

        binding.btnBackCheck.setOnClickListener { dismiss() }
        binding.btnConfirmSaveOrder.setOnClickListener { saveOrder() }
    }

    private fun saveOrder() {
        val session = SessionManager(requireContext())
        val userId = session.getUserId().takeIf { it > 0 } ?: 1
        val userName = session.getFullName().orEmpty().ifBlank { "Karyawan" }

        when (
            val result = PurchaseOrderRepository.createOrder(
                supplierName = supplierName,
                orderDateMillis = orderDateMillis,
                items = itemsMap,
                userId = userId,
                userName = userName
            )
        ) {
            is RepoResult.Success -> {
                val bundle = Bundle().apply { putInt(KEY_ORDER_ID, result.data.orderId) }
                setFragmentResult(RESULT_KEY, bundle)
                dismiss()
            }

            is RepoResult.Error -> {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConfirmOrderDialogFragment"
        const val RESULT_KEY = "confirm_order_result"
        const val KEY_ORDER_ID = "order_id"

        private const val ARG_SUPPLIER = "arg_supplier"
        private const val ARG_DATE_MILLIS = "arg_date_millis"
        private const val ARG_ITEM_IDS = "arg_item_ids"
        private const val ARG_ITEM_QTYS = "arg_item_qtys"

        fun newInstance(
            supplierName: String,
            orderDateMillis: Long,
            items: Map<Int, Int>
        ): ConfirmOrderDialogFragment {
            val entries = items.entries.toList()
            return ConfirmOrderDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUPPLIER, supplierName)
                    putLong(ARG_DATE_MILLIS, orderDateMillis)
                    putIntArray(ARG_ITEM_IDS, entries.map { it.key }.toIntArray())
                    putIntArray(ARG_ITEM_QTYS, entries.map { it.value }.toIntArray())
                }
            }
        }
    }
}
