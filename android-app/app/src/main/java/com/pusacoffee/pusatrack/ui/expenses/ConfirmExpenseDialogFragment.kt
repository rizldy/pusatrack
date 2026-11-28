package com.pusacoffee.pusatrack.ui.expenses

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
import com.pusacoffee.pusatrack.data.model.ExpenseItem
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.repository.ExpenseRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.DialogConfirmExpenseBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import java.util.Date

class ConfirmExpenseDialogFragment : DialogFragment() {

    private var _binding: DialogConfirmExpenseBinding? = null
    private val binding get() = _binding!!

    private var paymentMethod: PaymentMethod = PaymentMethod.CASH
    private var expenseDateMillis: Long = System.currentTimeMillis()
    private val items = mutableListOf<ExpenseItem>()

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
        _binding = DialogConfirmExpenseBinding.inflate(inflater, container, false)
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

        val methodName = arguments?.getString(ARG_METHOD) ?: PaymentMethod.CASH.name
        paymentMethod = PaymentMethod.valueOf(methodName)
        expenseDateMillis = arguments?.getLong(ARG_DATE_MILLIS, System.currentTimeMillis())
            ?: System.currentTimeMillis()

        val catIds = arguments?.getIntArray(ARG_CAT_IDS) ?: IntArray(0)
        val catNames = arguments?.getStringArray(ARG_CAT_NAMES) ?: emptyArray()
        val amounts = arguments?.getLongArray(ARG_AMOUNTS) ?: LongArray(0)
        val descs = arguments?.getStringArray(ARG_DESCS) ?: emptyArray()

        items.clear()
        for (i in catIds.indices) {
            items.add(
                ExpenseItem(
                    categoryId = catIds[i],
                    categoryName = catNames.getOrNull(i).orEmpty(),
                    amount = amounts.getOrNull(i) ?: 0L,
                    description = descs.getOrNull(i)?.ifEmpty { null }
                )
            )
        }

        val date = Date(expenseDateMillis)
        binding.tvConfirmMeta.text =
            "${paymentMethod.displayLabel} · ${DateFormatter.formatDate(date)} · ${DateFormatter.formatTime(date)}"

        val ctx = requireContext()
        binding.containerConfirmLines.removeAllViews()
        for (item in items) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val label = if (!item.description.isNullOrBlank()) {
                "${item.categoryName} (${item.description})"
            } else {
                item.categoryName
            }
            val left = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = label
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            val right = TextView(ctx).apply {
                text = CurrencyFormatter.format(item.amount)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            row.addView(left)
            row.addView(right)
            binding.containerConfirmLines.addView(row)
        }

        binding.tvConfirmTotal.text = CurrencyFormatter.format(items.sumOf { it.amount })

        binding.btnBackCheck.setOnClickListener { dismiss() }
        binding.btnConfirmSaveExpense.setOnClickListener { saveExpenses() }
    }

    private fun saveExpenses() {
        val session = SessionManager(requireContext())
        val userId = session.getUserId().takeIf { it > 0 } ?: 1
        val userName = session.getFullName().orEmpty().ifBlank { "Karyawan" }

        when (
            val result = ExpenseRepository.recordExpenses(
                paymentMethod = paymentMethod,
                expenseDateMillis = expenseDateMillis,
                items = items,
                userId = userId,
                userName = userName
            )
        ) {
            is RepoResult.Success -> {
                val bundle = Bundle().apply { putBoolean(KEY_SAVED, true) }
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
        const val TAG = "ConfirmExpenseDialogFragment"
        const val RESULT_KEY = "confirm_expense_result"
        const val KEY_SAVED = "saved"

        private const val ARG_METHOD = "arg_method"
        private const val ARG_DATE_MILLIS = "arg_date_millis"
        private const val ARG_CAT_IDS = "arg_cat_ids"
        private const val ARG_CAT_NAMES = "arg_cat_names"
        private const val ARG_AMOUNTS = "arg_amounts"
        private const val ARG_DESCS = "arg_descs"

        fun newInstance(
            paymentMethod: PaymentMethod,
            expenseDateMillis: Long,
            items: List<ExpenseItem>
        ): ConfirmExpenseDialogFragment {
            return ConfirmExpenseDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_METHOD, paymentMethod.name)
                    putLong(ARG_DATE_MILLIS, expenseDateMillis)
                    putIntArray(ARG_CAT_IDS, items.map { it.categoryId }.toIntArray())
                    putStringArray(ARG_CAT_NAMES, items.map { it.categoryName }.toTypedArray())
                    putLongArray(ARG_AMOUNTS, items.map { it.amount }.toLongArray())
                    putStringArray(ARG_DESCS, items.map { it.description.orEmpty() }.toTypedArray())
                }
            }
        }
    }
}
