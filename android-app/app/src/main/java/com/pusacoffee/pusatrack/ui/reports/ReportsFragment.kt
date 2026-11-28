package com.pusacoffee.pusatrack.ui.reports

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.checkbox.MaterialCheckBox
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentReportsBinding
import com.pusacoffee.pusatrack.utils.DateFormatter
import com.pusacoffee.pusatrack.utils.ReportSheetOption
import com.pusacoffee.pusatrack.utils.XlsxReportGenerator
import java.io.File
import java.util.Calendar
import java.util.Date

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private enum class PeriodPreset { TODAY, LAST_7_DAYS, THIS_MONTH, CUSTOM }

    private var activePreset: PeriodPreset = PeriodPreset.THIS_MONTH
    private val startCal: Calendar = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)
    private val endCal: Calendar = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)
    private val selectedOptions = linkedSetOf<ReportSheetOption>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        selectedOptions.clear()
        ReportSheetOption.entries.filter { it.defaultSelected }.forEach { selectedOptions.add(it) }

        setupPresetChips()
        setupDatePickers()
        buildSheetCheckboxes()
        applyPreset(PeriodPreset.THIS_MONTH)

        binding.btnExportXlsx.setOnClickListener { exportReport() }
    }

    private fun setupPresetChips() {
        binding.chipToday.setOnClickListener { applyPreset(PeriodPreset.TODAY) }
        binding.chip7Days.setOnClickListener { applyPreset(PeriodPreset.LAST_7_DAYS) }
        binding.chipThisMonth.setOnClickListener { applyPreset(PeriodPreset.THIS_MONTH) }
        binding.chipCustom.setOnClickListener { applyPreset(PeriodPreset.CUSTOM) }
    }

    private fun applyPreset(preset: PeriodPreset) {
        activePreset = preset
        val now = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)
        endCal.timeInMillis = now.timeInMillis

        when (preset) {
            PeriodPreset.TODAY -> {
                startCal.timeInMillis = now.timeInMillis
            }

            PeriodPreset.LAST_7_DAYS -> {
                startCal.timeInMillis = now.timeInMillis
                startCal.add(Calendar.DAY_OF_YEAR, -6)
            }

            PeriodPreset.THIS_MONTH -> {
                startCal.timeInMillis = now.timeInMillis
                startCal.set(Calendar.DAY_OF_MONTH, 1)
            }

            PeriodPreset.CUSTOM -> Unit
        }

        updatePresetChipStyles()
        updateDateLabels()
        binding.layoutErrorBanner.isVisible = false
    }

    private fun updatePresetChipStyles() {
        styleChip(binding.chipToday, activePreset == PeriodPreset.TODAY)
        styleChip(binding.chip7Days, activePreset == PeriodPreset.LAST_7_DAYS)
        styleChip(binding.chipThisMonth, activePreset == PeriodPreset.THIS_MONTH)
        styleChip(binding.chipCustom, activePreset == PeriodPreset.CUSTOM)
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        val ctx = requireContext()
        chip.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chip.setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.bg_white else R.color.text_muted))
    }

    private fun setupDatePickers() {
        binding.btnStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    startCal.set(y, m, d, 0, 0, 0)
                    activePreset = PeriodPreset.CUSTOM
                    updatePresetChipStyles()
                    updateDateLabels()
                    binding.layoutErrorBanner.isVisible = false
                },
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnEndDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    endCal.set(y, m, d, 23, 59, 59)
                    activePreset = PeriodPreset.CUSTOM
                    updatePresetChipStyles()
                    updateDateLabels()
                    binding.layoutErrorBanner.isVisible = false
                },
                endCal.get(Calendar.YEAR),
                endCal.get(Calendar.MONTH),
                endCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabels() {
        binding.tvStartDate.text = DateFormatter.formatDate(Date(startCal.timeInMillis))
        binding.tvEndDate.text = DateFormatter.formatDate(Date(endCal.timeInMillis))
    }

    private fun buildSheetCheckboxes() {
        val ctx = requireContext()
        binding.containerSheetCheckboxes.removeAllViews()
        for (option in ReportSheetOption.entries) {
            val cb = MaterialCheckBox(ctx).apply {
                text = option.sheetTitle
                isChecked = option in selectedOptions
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedOptions.add(option) else selectedOptions.remove(option)
                    binding.layoutErrorBanner.isVisible = false
                }
            }
            binding.containerSheetCheckboxes.addView(cb)
        }
    }

    private fun exportReport() {
        val startMillis = (startCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endMillis = (endCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val slug = buildRangeSlug()
        val exportDir = File(requireContext().cacheDir, "exports")

        when (
            val result = XlsxReportGenerator.generate(
                outputDir = exportDir,
                startDateMillis = startMillis,
                endDateMillis = endMillis,
                rangeSlug = slug,
                selectedSheets = selectedOptions.toList()
            )
        ) {
            is RepoResult.Success -> {
                binding.layoutErrorBanner.isVisible = false
                ExportReadyBottomSheet.newInstance(
                    filePath = result.data.file.absolutePath,
                    fileName = result.data.fileName,
                    sheetCount = result.data.sheetCount,
                    fileSizeLabel = result.data.formatFileSize()
                ).show(childFragmentManager, ExportReadyBottomSheet.TAG)
            }

            is RepoResult.Error -> {
                binding.tvErrorMessage.text = result.message
                binding.layoutErrorBanner.isVisible = true
            }
        }
    }

    private fun buildRangeSlug(): String {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        val sDay = startCal.get(Calendar.DAY_OF_MONTH)
        val eDay = endCal.get(Calendar.DAY_OF_MONTH)
        val eMonth = months[endCal.get(Calendar.MONTH)]
        val eYear = endCal.get(Calendar.YEAR)
        return "$sDay-${eDay}${eMonth}$eYear"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
