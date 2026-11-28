package com.pusacoffee.pusatrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.MainActivity
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import com.pusacoffee.pusatrack.databinding.FragmentTransactionDetailBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
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

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnBackToHistory.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.historyFragment)
                ?: findNavController().navigateUp()
        }

        binding.tvSaleNumber.text = sale.saleNumber
        binding.tvSaleMeta.text =
            "${sale.confirmedDateLabel} · ${sale.confirmedTimeLabel} · ${sale.userName}"

        val ctx = requireContext()
        binding.containerOrderDetails.removeAllViews()
        for (detail in sale.details) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }
            val leftCol = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
            }
            val nameTv = TextView(ctx).apply {
                text = detail.productName
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 14f
            }
            val qtyPriceTv = TextView(ctx).apply {
                text = "${detail.quantity} × ${CurrencyFormatter.format(detail.sellingPriceSnapshot)}"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                textSize = 12f
            }
            leftCol.addView(nameTv)
            leftCol.addView(qtyPriceTv)

            val subtotalTv = TextView(ctx).apply {
                text = CurrencyFormatter.format(detail.subtotal)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 14f
            }
            row.addView(leftCol)
            row.addView(subtotalTv)
            binding.containerOrderDetails.addView(row)
        }

        binding.tvOrderTotal.text = CurrencyFormatter.format(sale.totalAmount)
        binding.tvPaymentMethod.text = sale.paymentMethod.displayLabel

        val isCash = sale.paymentMethod == PaymentMethod.CASH
        binding.rowReceived.isVisible = isCash
        binding.rowChange.isVisible = isCash
        if (isCash) {
            binding.tvAmountReceived.text = CurrencyFormatter.format(sale.amountReceived)
            binding.tvChangeAmount.text = CurrencyFormatter.format(sale.changeAmount)
        }

        binding.containerIngredientsUsed.removeAllViews()
        if (sale.stockEffects.isEmpty()) {
            val emptyTv = TextView(ctx).apply {
                text = "Tidak ada bahan baku terpakai."
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                textSize = 13f
            }
            binding.containerIngredientsUsed.addView(emptyTv)
        } else {
            for (effect in sale.stockEffects) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 6, 0, 6)
                }
                val nameTv = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    text = effect.ingredientName
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                    textSize = 14f
                }
                val usedTv = TextView(ctx).apply {
                    text = "-${Ingredient.formatNumber(effect.usedQuantity)} ${effect.usageUnit}"
                    setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
                    textSize = 14f
                }
                row.addView(nameTv)
                row.addView(usedTv)
                binding.containerIngredientsUsed.addView(row)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
