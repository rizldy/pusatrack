package com.pusacoffee.pusatrack.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.databinding.FragmentPaymentBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val cashierViewModel: CashierViewModel by activityViewModels()
    private var totalAmount: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lines = cashierViewModel.getCartLines()
        if (lines.isEmpty()) {
            findNavController().navigateUp()
            return
        }

        val fullName = SessionManager(requireContext()).getFullName().orEmpty().ifBlank { "Karyawan" }
        binding.tvSubtitle.text = "Transaksi baru · $fullName"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        totalAmount = lines.sumOf { it.subtotal }
        binding.tvTotalAmount.text = CurrencyFormatter.format(totalAmount)
        renderOrderSummaryLines(lines)

        // Default to exact amount or previously typed cash amount
        if (cashierViewModel.cashAmountReceived <= 0L) {
            cashierViewModel.cashAmountReceived = totalAmount
        }
        binding.etAmountReceived.setText(cashierViewModel.cashAmountReceived.toString())

        setupPaymentMethodControls()
        setupQuickAmountChips()
        setupResultListener()

        binding.etAmountReceived.doAfterTextChanged { editable ->
            cashierViewModel.cashAmountReceived =
                editable?.toString()?.trim()?.toLongOrNull() ?: 0L
            updateCashValidationAndChange()
        }

        binding.btnReviewAndConfirm.setOnClickListener {
            if (cashierViewModel.selectedPaymentMethod == PaymentMethod.CASH) {
                if (cashierViewModel.cashAmountReceived < totalAmount) {
                    binding.tvCashError.isVisible = true
                    return@setOnClickListener
                }
            }
            ConfirmSaleDialogFragment().show(childFragmentManager, ConfirmSaleDialogFragment.TAG)
        }

        updateMethodUi()
        updateCashValidationAndChange()
    }

    private fun setupResultListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmSaleDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val confirmedSaleId = bundle.getInt(ConfirmSaleDialogFragment.KEY_SALE_ID, -1)
            if (confirmedSaleId != -1) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.paymentFragment, true)
                    .build()
                findNavController().navigate(
                    R.id.transactionSuccessFragment,
                    bundleOf("saleId" to confirmedSaleId),
                    navOptions
                )
            }
        }
    }

    private fun renderOrderSummaryLines(lines: List<CartLine>) {
        val ctx = requireContext()
        binding.containerOrderLines.removeAllViews()
        for (line in lines) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }
            val left = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = "${line.product.productName} ×${line.quantity}"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 14f
            }
            val right = TextView(ctx).apply {
                text = CurrencyFormatter.format(line.subtotal)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 14f
            }
            row.addView(left)
            row.addView(right)
            binding.containerOrderLines.addView(row)
        }
    }

    private fun setupPaymentMethodControls() {
        binding.btnMethodCash.setOnClickListener {
            cashierViewModel.selectedPaymentMethod = PaymentMethod.CASH
            updateMethodUi()
            updateCashValidationAndChange()
        }
        binding.btnMethodQris.setOnClickListener {
            cashierViewModel.selectedPaymentMethod = PaymentMethod.QRIS
            updateMethodUi()
        }
    }

    private fun setupQuickAmountChips() {
        binding.chipExactAmount.setOnClickListener {
            binding.etAmountReceived.setText(totalAmount.toString())
        }
        binding.chip50k.setOnClickListener {
            binding.etAmountReceived.setText("50000")
        }
        binding.chip100k.setOnClickListener {
            binding.etAmountReceived.setText("100000")
        }
    }

    private fun updateMethodUi() {
        val ctx = requireContext()
        val isCash = cashierViewModel.selectedPaymentMethod == PaymentMethod.CASH

        binding.btnMethodCash.setBackgroundResource(if (isCash) R.drawable.bg_chip_selected else 0)
        binding.btnMethodCash.setTextColor(
            ContextCompat.getColor(ctx, if (isCash) R.color.bg_white else R.color.text_muted)
        )
        binding.btnMethodQris.setBackgroundResource(if (!isCash) R.drawable.bg_chip_selected else 0)
        binding.btnMethodQris.setTextColor(
            ContextCompat.getColor(ctx, if (!isCash) R.color.bg_white else R.color.text_muted)
        )

        binding.layoutCashSection.isVisible = isCash
        binding.layoutQrisSection.isVisible = !isCash
        if (!isCash) {
            binding.btnReviewAndConfirm.isEnabled = true
            binding.btnReviewAndConfirm.alpha = 1.0f
        }
    }

    private fun updateCashValidationAndChange() {
        if (cashierViewModel.selectedPaymentMethod != PaymentMethod.CASH) return
        val received = cashierViewModel.cashAmountReceived
        val sufficient = received >= totalAmount
        binding.tvCashError.isVisible = !sufficient
        binding.cardChange.isVisible = sufficient
        if (sufficient) {
            binding.tvChangeAmount.text = CurrencyFormatter.format(received - totalAmount)
        }
        binding.btnReviewAndConfirm.isEnabled = sufficient
        binding.btnReviewAndConfirm.alpha = if (sufficient) 1.0f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
