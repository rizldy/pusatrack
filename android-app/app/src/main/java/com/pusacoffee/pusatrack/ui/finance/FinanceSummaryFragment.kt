package com.pusacoffee.pusatrack.ui.finance

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.DailySalesBar
import com.pusacoffee.pusatrack.data.model.FinancePeriod
import com.pusacoffee.pusatrack.data.model.FinanceSummary
import com.pusacoffee.pusatrack.data.repository.ExpenseRepository
import com.pusacoffee.pusatrack.data.repository.FinanceRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.data.repository.SalesRepository
import com.pusacoffee.pusatrack.databinding.FragmentFinanceSummaryBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FinanceSummaryFragment : Fragment() {

    private var _binding: FragmentFinanceSummaryBinding? = null
    private val binding get() = _binding!!

    private var selectedPeriod: FinancePeriod = FinancePeriod.MONTHLY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinanceSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.fabRecordExpense.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }

        binding.btnPeriodDaily.setOnClickListener { selectPeriod(FinancePeriod.DAILY) }
        binding.btnPeriodWeekly.setOnClickListener { selectPeriod(FinancePeriod.WEEKLY) }
        binding.btnPeriodMonthly.setOnClickListener { selectPeriod(FinancePeriod.MONTHLY) }
        binding.btnSetOpeningCash.setOnClickListener { showOpeningCashDialog() }

        observeFinanceData()
    }

    private fun selectPeriod(period: FinancePeriod) {
        selectedPeriod = period
        updatePeriodButtonsUi()
        renderSummary(FinanceRepository.getSummary(selectedPeriod))
    }

    private fun updatePeriodButtonsUi() {
        stylePeriodTab(binding.btnPeriodDaily, selectedPeriod == FinancePeriod.DAILY)
        stylePeriodTab(binding.btnPeriodWeekly, selectedPeriod == FinancePeriod.WEEKLY)
        stylePeriodTab(binding.btnPeriodMonthly, selectedPeriod == FinancePeriod.MONTHLY)
    }

    private fun stylePeriodTab(view: TextView, active: Boolean) {
        val ctx = requireContext()
        view.setBackgroundResource(if (active) R.drawable.bg_chip_selected else 0)
        view.setTextColor(ContextCompat.getColor(ctx, if (active) R.color.bg_white else R.color.text_muted))
    }

    private fun observeFinanceData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    SalesRepository.sales,
                    ExpenseRepository.batches,
                    FinanceRepository.openingCashBalance
                ) { _, _, _ ->
                    FinanceRepository.getSummary(selectedPeriod)
                }.collect { summary ->
                    updatePeriodButtonsUi()
                    renderSummary(summary)
                }
            }
        }
    }

    private fun renderSummary(summary: FinanceSummary) {
        binding.tvPeriodLabel.text = summary.periodLabel
        binding.tvNetProfit.text = CurrencyFormatter.format(summary.netProfit)
        binding.tvNetMarginBadge.text = "${summary.formatNetMarginPercent()} dari penjualan"

        renderBarChart(summary.dailyBars)
        binding.tvAvgSales.text = "Rata-rata: ${CurrencyFormatter.format(summary.averageDailySales)}"
        binding.tvHighestSales.text =
            "Tertinggi: ${summary.highestDayLabel} (${CurrencyFormatter.format(summary.highestDayAmount)})"

        binding.tvBreakdownSales.text = CurrencyFormatter.format(summary.totalSales)
        binding.tvBreakdownHpp.text = "-${CurrencyFormatter.format(summary.totalHpp)}"
        binding.tvBreakdownGross.text = CurrencyFormatter.format(summary.grossProfit)
        binding.tvBreakdownExpenses.text = "-${CurrencyFormatter.format(summary.totalExpenses)}"

        val denom = summary.totalSales.coerceAtLeast(1L)
        binding.barSales.progress = if (summary.totalSales > 0) 100 else 0
        binding.barHpp.progress = ((summary.totalHpp * 100L) / denom).toInt().coerceIn(0, 100)
        binding.barExpenses.progress = ((summary.totalExpenses * 100L) / denom).toInt().coerceIn(0, 100)

        // Stacked payment bar
        binding.tvCashSalesValue.text = CurrencyFormatter.format(summary.cashSales)
        binding.tvQrisSalesValue.text = CurrencyFormatter.format(summary.qrisSales)
        val cashWeight = summary.cashSales.coerceAtLeast(0L).toFloat()
        val qrisWeight = summary.qrisSales.coerceAtLeast(0L).toFloat()
        val totalWeight = cashWeight + qrisWeight
        (binding.viewCashSegment.layoutParams as LinearLayout.LayoutParams).weight =
            if (totalWeight > 0f) cashWeight else 1f
        (binding.viewQrisSegment.layoutParams as LinearLayout.LayoutParams).weight =
            if (totalWeight > 0f) qrisWeight else 1f
        binding.viewCashSegment.requestLayout()
        binding.viewQrisSegment.requestLayout()

        // Opening cash & cash change (separate from profit)
        binding.tvOpeningCash.text = CurrencyFormatter.format(summary.openingCashBalance)
        val prefix = if (summary.cashChange >= 0L) "+" else ""
        binding.tvCashChange.text = "$prefix${CurrencyFormatter.format(summary.cashChange)}"
        binding.tvCashChange.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (summary.cashChange >= 0L) R.color.primary_forest else R.color.tertiary_text
            )
        )
    }

    private fun renderBarChart(bars: List<DailySalesBar>) {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val maxAmount = bars.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
        val maxBarHeightPx = (86 * density).toInt()

        binding.containerBarChart.removeAllViews()
        for (bar in bars) {
            val col = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            val barHeightPx = ((bar.amount.toDouble() / maxAmount.toDouble()) * maxBarHeightPx)
                .toInt()
                .coerceAtLeast((8 * density).toInt())

            val barView = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams((22 * density).toInt(), barHeightPx)
                setBackgroundResource(R.drawable.bg_bar_rounded)
                alpha = if (bar.amount == maxAmount && bar.amount > 0L) 1.0f else 0.45f
            }

            val labelTv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (6 * density).toInt()
                }
                text = bar.dayLabel
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                textSize = 11f
            }

            col.addView(barView)
            col.addView(labelTv)
            binding.containerBarChart.addView(col)
        }
    }

    private fun showOpeningCashDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Contoh: 200000"
            setText(FinanceRepository.openingCashBalance.value.toString())
        }
        val container = FrameLayout(ctx).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Atur Saldo Kas Awal")
            .setMessage("Saldo kas awal ditampilkan terpisah dari perhitungan laba.")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val value = input.text?.toString()?.trim()?.toLongOrNull() ?: -1L
                when (val result = FinanceRepository.setOpeningCashBalance(value)) {
                    is RepoResult.Success -> Unit
                    is RepoResult.Error -> Toast.makeText(ctx, result.message, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
