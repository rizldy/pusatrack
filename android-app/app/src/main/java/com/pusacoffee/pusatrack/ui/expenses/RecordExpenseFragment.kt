package com.pusacoffee.pusatrack.ui.expenses

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.ExpenseItem
import com.pusacoffee.pusatrack.data.model.PaymentMethod
import com.pusacoffee.pusatrack.data.repository.ExpenseRepository
import com.pusacoffee.pusatrack.databinding.FragmentRecordExpenseBinding
import com.pusacoffee.pusatrack.databinding.ItemExpenseRowBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import java.util.Calendar

class RecordExpenseFragment : Fragment() {

    private var _binding: FragmentRecordExpenseBinding? = null
    private val binding get() = _binding!!

    private var selectedMethod: PaymentMethod = PaymentMethod.CASH
    private val expenseCalendar: Calendar = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)

    private data class DraftRow(
        var categoryIndex: Int = 0,
        var amountText: String = "",
        var description: String = ""
    )

    private val draftRows = mutableListOf(DraftRow())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnMethodCash.setOnClickListener {
            selectedMethod = PaymentMethod.CASH
            updateMethodUi()
        }
        binding.btnMethodQris.setOnClickListener {
            selectedMethod = PaymentMethod.QRIS
            updateMethodUi()
        }

        binding.btnPickDateTime.setOnClickListener { pickDateTime() }
        binding.btnAddExpenseRow.setOnClickListener {
            draftRows.add(DraftRow())
            renderRows()
        }
        binding.btnReviewExpense.setOnClickListener { openConfirmDialog() }

        childFragmentManager.setFragmentResultListener(
            ConfirmExpenseDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(ConfirmExpenseDialogFragment.KEY_SAVED, false)) {
                findNavController().navigateUp()
            }
        }

        updateMethodUi()
        updateDateTimeLabel()
        renderRows()
    }

    private fun updateMethodUi() {
        val ctx = requireContext()
        val isCash = selectedMethod == PaymentMethod.CASH
        binding.btnMethodCash.setBackgroundResource(if (isCash) R.drawable.bg_chip_selected else 0)
        binding.btnMethodCash.setTextColor(
            ContextCompat.getColor(ctx, if (isCash) R.color.bg_white else R.color.text_muted)
        )
        binding.btnMethodQris.setBackgroundResource(if (!isCash) R.drawable.bg_chip_selected else 0)
        binding.btnMethodQris.setTextColor(
            ContextCompat.getColor(ctx, if (!isCash) R.color.bg_white else R.color.text_muted)
        )
    }

    private fun pickDateTime() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                expenseCalendar.set(Calendar.YEAR, year)
                expenseCalendar.set(Calendar.MONTH, month)
                expenseCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        expenseCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        expenseCalendar.set(Calendar.MINUTE, minute)
                        updateDateTimeLabel()
                    },
                    expenseCalendar.get(Calendar.HOUR_OF_DAY),
                    expenseCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            expenseCalendar.get(Calendar.YEAR),
            expenseCalendar.get(Calendar.MONTH),
            expenseCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeLabel() {
        val date = expenseCalendar.time
        binding.tvExpenseDateTime.text = "${DateFormatter.formatDate(date)} · ${DateFormatter.formatTime(date)}"
    }

    private fun renderRows() {
        val categories = ExpenseRepository.categories
        val categoryNames = categories.map { it.categoryName }

        binding.containerExpenseRows.removeAllViews()
        draftRows.forEachIndexed { index, row ->
            val rowBinding = ItemExpenseRowBinding.inflate(
                layoutInflater,
                binding.containerExpenseRows,
                false
            )
            rowBinding.tvRowTitle.text = "Baris #${index + 1}"
            rowBinding.btnRemoveRow.isVisible = draftRows.size > 1
            rowBinding.btnRemoveRow.setOnClickListener {
                draftRows.removeAt(index)
                renderRows()
            }

            val spinnerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categoryNames
            )
            rowBinding.spinnerCategory.adapter = spinnerAdapter
            rowBinding.spinnerCategory.setSelection(row.categoryIndex.coerceIn(0, categories.lastIndex))
            rowBinding.spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        row.categoryIndex = position
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }

            rowBinding.etAmount.setText(row.amountText)
            rowBinding.etDescription.setText(row.description)

            rowBinding.etAmount.doAfterTextChanged { editable ->
                row.amountText = editable?.toString().orEmpty()
                binding.layoutErrorBanner.isVisible = false
                updateTotal()
            }
            rowBinding.etDescription.doAfterTextChanged { editable ->
                row.description = editable?.toString().orEmpty()
            }

            binding.containerExpenseRows.addView(rowBinding.root)
        }

        updateTotal()
    }

    private fun updateTotal() {
        val total = draftRows.sumOf { it.amountText.trim().toLongOrNull() ?: 0L }
        binding.tvExpenseTotal.text = CurrencyFormatter.format(total)
    }

    private fun openConfirmDialog() {
        val categories = ExpenseRepository.categories
        val validatedItems = mutableListOf<ExpenseItem>()

        for ((index, row) in draftRows.withIndex()) {
            val amount = row.amountText.trim().toLongOrNull() ?: 0L
            if (amount <= 0L) {
                showError("Nominal pada baris #${index + 1} harus lebih dari Rp 0.")
                return
            }
            val cat = categories[row.categoryIndex.coerceIn(0, categories.lastIndex)]
            validatedItems.add(
                ExpenseItem(
                    categoryId = cat.categoryId,
                    categoryName = cat.categoryName,
                    amount = amount,
                    description = row.description.trim().ifEmpty { null }
                )
            )
        }

        val dialog = ConfirmExpenseDialogFragment.newInstance(
            paymentMethod = selectedMethod,
            expenseDateMillis = expenseCalendar.timeInMillis,
            items = validatedItems
        )
        dialog.show(childFragmentManager, ConfirmExpenseDialogFragment.TAG)
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
