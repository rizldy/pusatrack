package com.pusacoffee.pusatrack.ui.inventory

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
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.StockCountEntry
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.DialogConfirmStockCheckBinding
import com.pusacoffee.pusatrack.utils.DateFormatter

class ConfirmStockCheckDialogFragment : DialogFragment() {

    private var _binding: DialogConfirmStockCheckBinding? = null
    private val binding get() = _binding!!

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
        _binding = DialogConfirmStockCheckBinding.inflate(inflater, container, false)
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

        val entries = pendingEntries
        val discrepancies = entries.filter { it.isDiscrepancy }
        val matchCount = entries.size - discrepancies.size

        binding.tvConfirmSubtitle.text =
            "${discrepancies.size} bahan berubah, $matchCount bahan sesuai."
        binding.etReason.setText("Selisih pemeriksaan fisik")

        val ctx = requireContext()
        binding.containerDiscrepancies.removeAllViews()
        for (entry in discrepancies) {
            val ing = entry.ingredient
            val sign = if (entry.delta > 0) "+" else "-"
            val absDelta = kotlin.math.abs(entry.delta)

            val itemBlock = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 6)
            }
            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val nameTv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = ing.ingredientName
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            val deltaTv = TextView(ctx).apply {
                text = "$sign${Ingredient.formatNumber(absDelta)} ${ing.usageUnit}"
                setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
                textSize = 13f
            }
            topRow.addView(nameTv)
            topRow.addView(deltaTv)

            val changeTv = TextView(ctx).apply {
                text = "dari ${Ingredient.formatNumber(ing.currentStock)} ${ing.usageUnit} menjadi ${Ingredient.formatNumber(entry.physicalStock)} ${ing.usageUnit}"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                textSize = 11f
            }

            itemBlock.addView(topRow)
            itemBlock.addView(changeTv)
            binding.containerDiscrepancies.addView(itemBlock)
        }

        binding.btnBackCheck.setOnClickListener { dismiss() }
        binding.btnConfirmAdjust.setOnClickListener {
            val session = SessionManager(requireContext())
            val userId = session.getUserId().takeIf { it > 0 } ?: 1
            val userName = session.getFullName().orEmpty().ifBlank { "Karyawan" }
            val dateTimeLabel = "${DateFormatter.formatDate()} · ${DateFormatter.formatTime()}"
            val reason = binding.etReason.text?.toString().orEmpty().ifBlank { "Selisih pemeriksaan fisik" }
            val description = binding.etDescription.text?.toString()

            when (
                val result = InventoryRepository.applyStockCount(
                    entries = entries,
                    reason = reason,
                    description = description,
                    userId = userId,
                    userName = userName,
                    dateTimeLabel = dateTimeLabel
                )
            ) {
                is RepoResult.Success -> {
                    pendingEntries = emptyList()
                    val bundle = Bundle().apply { putBoolean(KEY_ADJUSTED, true) }
                    setFragmentResult(RESULT_KEY, bundle)
                    dismiss()
                }

                is RepoResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConfirmStockCheckDialogFragment"
        const val RESULT_KEY = "confirm_stock_check_result"
        const val KEY_ADJUSTED = "adjusted"

        var pendingEntries: List<StockCountEntry> = emptyList()
    }
}
