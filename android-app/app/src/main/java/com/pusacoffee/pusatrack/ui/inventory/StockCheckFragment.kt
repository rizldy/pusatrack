package com.pusacoffee.pusatrack.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.StockCountEntry
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.databinding.FragmentStockCheckBinding
import com.pusacoffee.pusatrack.databinding.ItemStockCheckBinding
import com.pusacoffee.pusatrack.utils.DateFormatter

class StockCheckFragment : Fragment() {

    private var _binding: FragmentStockCheckBinding? = null
    private val binding get() = _binding!!

    private data class CountDraft(
        val ingredient: Ingredient,
        var countedWhole: Int,
        var hasOpenPackage: Boolean
    ) {
        fun toEntry(): StockCountEntry = StockCountEntry(
            ingredient = ingredient,
            countedWholePackages = countedWhole,
            hasOpenPackage = hasOpenPackage
        )
    }

    private val drafts = mutableListOf<CountDraft>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fullName = SessionManager(requireContext()).getFullName().orEmpty().ifBlank { "Karyawan" }
        binding.tvSubtitle.text = "${DateFormatter.formatDate()} · $fullName"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Initialize drafts from current active ingredients
        drafts.clear()
        InventoryRepository.getAllIngredients()
            .filter { it.isActive }
            .forEach { ing ->
                drafts.add(
                    CountDraft(
                        ingredient = ing,
                        countedWhole = ing.fullPackages,
                        hasOpenPackage = ing.remainderUsage > 0
                    )
                )
            }

        renderCards()

        binding.btnAdjustStock.setOnClickListener {
            val entries = drafts.map { it.toEntry() }
            val diffCount = entries.count { it.isDiscrepancy }
            if (diffCount == 0) {
                Toast.makeText(
                    requireContext(),
                    "Semua bahan sudah sesuai stok sistem. Tidak ada selisih yang perlu disesuaikan.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            ConfirmStockCheckDialogFragment.pendingEntries = entries
            ConfirmStockCheckDialogFragment().show(
                childFragmentManager,
                ConfirmStockCheckDialogFragment.TAG
            )
        }

        childFragmentManager.setFragmentResultListener(
            ConfirmStockCheckDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(ConfirmStockCheckDialogFragment.KEY_ADJUSTED, false)) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.inventoryFragment, false)
                    .setLaunchSingleTop(true)
                    .build()
                findNavController().navigate(R.id.inventoryFragment, null, navOptions)
            }
        }
    }

    private fun renderCards() {
        val ctx = requireContext()
        binding.containerStockCheckItems.removeAllViews()

        for (draft in drafts) {
            val rowBinding = ItemStockCheckBinding.inflate(
                layoutInflater,
                binding.containerStockCheckItems,
                false
            )
            val ing = draft.ingredient
            rowBinding.tvIngredientName.text = ing.ingredientName
            rowBinding.tvSystemStock.text =
                "Stok sistem: ${ing.formatPackageBreakdown()} (${ing.formatCurrentStockUsage()})"
            rowBinding.tvWholePackageLabel.text = "Kemasan utuh (${ing.purchaseUnit})"

            fun updateCardUi() {
                val entry = draft.toEntry()
                rowBinding.tvCountedPackages.text = draft.countedWhole.toString()

                if (entry.isDiscrepancy) {
                    rowBinding.tvMatchChip.text = "Selisih"
                    rowBinding.tvMatchChip.setBackgroundResource(R.drawable.bg_status_low)
                    rowBinding.tvMatchChip.setTextColor(
                        ContextCompat.getColor(ctx, R.color.secondary_text)
                    )
                    val sign = if (entry.delta > 0) "+" else "-"
                    val absDelta = kotlin.math.abs(entry.delta)
                    rowBinding.tvLiveNote.text =
                        "Selisih $sign${Ingredient.formatNumber(absDelta)} ${ing.usageUnit} (fisik ${Ingredient.formatNumber(entry.physicalStock)} ${ing.usageUnit})"
                    rowBinding.tvLiveNote.setTextColor(
                        ContextCompat.getColor(ctx, R.color.tertiary_text)
                    )
                } else {
                    rowBinding.tvMatchChip.text = "Sesuai"
                    rowBinding.tvMatchChip.setBackgroundResource(R.drawable.bg_status_safe)
                    rowBinding.tvMatchChip.setTextColor(
                        ContextCompat.getColor(ctx, R.color.primary_forest)
                    )
                    rowBinding.tvLiveNote.text = if (draft.hasOpenPackage && ing.remainderUsage > 0) {
                        "Sisa kemasan terbuka ${Ingredient.formatNumber(ing.remainderUsage)} ${ing.usageUnit} dipertahankan · Sesuai sistem"
                    } else {
                        "Sesuai dengan stok sistem"
                    }
                    rowBinding.tvLiveNote.setTextColor(
                        ContextCompat.getColor(ctx, R.color.primary_forest)
                    )
                }
                updateFooterSummary()
            }

            rowBinding.switchOpenPackage.isChecked = draft.hasOpenPackage
            rowBinding.switchOpenPackage.setOnCheckedChangeListener { _, isChecked ->
                draft.hasOpenPackage = isChecked
                updateCardUi()
            }

            rowBinding.btnMinus.setOnClickListener {
                if (draft.countedWhole > 0) {
                    draft.countedWhole--
                    updateCardUi()
                }
            }
            rowBinding.btnPlus.setOnClickListener {
                draft.countedWhole++
                updateCardUi()
            }

            updateCardUi()
            binding.containerStockCheckItems.addView(rowBinding.root)
        }
        updateFooterSummary()
    }

    private fun updateFooterSummary() {
        val entries = drafts.map { it.toEntry() }
        val diffCount = entries.count { it.isDiscrepancy }
        val matchCount = entries.size - diffCount
        binding.tvSummaryFooter.text =
            "$diffCount bahan berselisih, $matchCount sesuai. Stok berubah setelah Anda menekan Sesuaikan Stok."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
