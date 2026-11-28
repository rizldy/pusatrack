package com.pusacoffee.pusatrack.ui.cashier

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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import com.pusacoffee.pusatrack.databinding.DialogConfirmSaleBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

class ConfirmSaleDialogFragment : DialogFragment() {

    private var _binding: DialogConfirmSaleBinding? = null
    private val binding get() = _binding!!

    private val cashierViewModel: CashierViewModel by activityViewModels()

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
        _binding = DialogConfirmSaleBinding.inflate(inflater, container, false)
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

        val lines = cashierViewModel.getCartLines()
        val total = lines.sumOf { it.subtotal }
        val method = cashierViewModel.selectedPaymentMethod
        val received = cashierViewModel.cashAmountReceived

        val ctx = requireContext()
        binding.containerConfirmItems.removeAllViews()
        for (line in lines) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val left = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = "${line.product.productName} ×${line.quantity}"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            val right = TextView(ctx).apply {
                text = CurrencyFormatter.format(line.subtotal)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            row.addView(left)
            row.addView(right)
            binding.containerConfirmItems.addView(row)
        }

        binding.tvConfirmTotal.text = CurrencyFormatter.format(total)
        binding.tvConfirmMethod.text = method.displayLabel

        val isCash = method == PaymentMethod.CASH
        binding.rowConfirmReceived.isVisible = isCash
        binding.rowConfirmChange.isVisible = isCash
        if (isCash) {
            binding.tvConfirmReceived.text = CurrencyFormatter.format(received)
            binding.tvConfirmChange.text = CurrencyFormatter.format((received - total).coerceAtLeast(0L))
        }

        binding.btnBackToCheck.setOnClickListener { dismiss() }

        binding.btnConfirmSale.setOnClickListener {
            confirmAtomicSale()
        }
    }

    private fun confirmAtomicSale() {
        val session = SessionManager(requireContext())
        val userId = session.getUserId().takeIf { it > 0 } ?: 1
        val userName = session.getFullName().orEmpty().ifBlank { "Karyawan" }

        when (
            val result = SalesRepository.confirmSale(
                cartItems = cashierViewModel.cart.value,
                paymentMethod = cashierViewModel.selectedPaymentMethod,
                amountReceived = cashierViewModel.cashAmountReceived,
                userId = userId,
                userName = userName
            )
        ) {
            is RepoResult.Success -> {
                cashierViewModel.clearCart()
                setFragmentResult(
                    RESULT_KEY,
                    bundleOf(KEY_SALE_ID to result.data.saleId)
                )
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
        const val TAG = "ConfirmSaleDialogFragment"
        const val RESULT_KEY = "confirm_sale_result"
        const val KEY_SALE_ID = "sale_id"
    }
}
