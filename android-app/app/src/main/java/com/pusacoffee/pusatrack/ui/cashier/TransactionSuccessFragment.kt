package com.pusacoffee.pusatrack.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import com.pusacoffee.pusatrack.databinding.FragmentTransactionSuccessBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

class TransactionSuccessFragment : Fragment() {

    private var _binding: FragmentTransactionSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saleId = arguments?.getInt("saleId", -1) ?: -1
        val sale = SalesRepository.getSaleById(saleId)
        if (sale == null) {
            findNavController().navigateUp()
            return
        }

        binding.tvSuccessSubtitle.text =
            "${sale.saleNumber} · ${CurrencyFormatter.format(sale.totalAmount)} · ${sale.paymentMethod.displayLabel}"

        val ctx = requireContext()
        binding.containerStockEffects.removeAllViews()
        if (sale.stockEffects.isEmpty()) {
            val emptyTv = TextView(ctx).apply {
                text = "Tidak ada bahan baku yang berkurang pada transaksi ini."
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                textSize = 13f
            }
            binding.containerStockEffects.addView(emptyTv)
        } else {
            for (effect in sale.stockEffects) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                }
                val nameTv = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    text = effect.ingredientName
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                    textSize = 14f
                }
                val deltaTv = TextView(ctx).apply {
                    text = "-${Ingredient.formatNumber(effect.usedQuantity)} ${effect.usageUnit} · sisa ${Ingredient.formatNumber(effect.stockAfter)} ${effect.usageUnit}"
                    setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
                    textSize = 12f
                }
                row.addView(nameTv)
                row.addView(deltaTv)
                binding.containerStockEffects.addView(row)
            }
        }

        binding.btnNewTransaction.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.cashierFragment, false)
                .setLaunchSingleTop(true)
                .build()
            findNavController().navigate(R.id.cashierFragment, null, navOptions)
        }

        binding.btnViewSaleDetail.setOnClickListener {
            findNavController().navigate(
                R.id.transactionDetailFragment,
                bundleOf("saleId" to sale.saleId)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
