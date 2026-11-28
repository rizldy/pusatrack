package com.pusacoffee.pusatrack.ui.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.data.repository.ActivityHistoryRepository
import com.pusacoffee.pusatrack.data.repository.NotificationRepository
import com.pusacoffee.pusatrack.databinding.FragmentHistoryBinding
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ActivityHistoryAdapter

    private var selectedType: ActivityType? = null
    private var selectedUser: String? = null
    private var fromMillis: Long? = null
    private var toMillis: Long? = null
    private var dateLabel: String = "Semua tanggal"
    private var newestFirst: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActivityHistoryAdapter { saleId ->
            findNavController().navigate(
                R.id.transactionDetailFragment,
                bundleOf("saleId" to saleId)
            )
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        setupControls()
        observeHistory()
    }

    private fun setupControls() {
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.btnFilterDate.setOnClickListener { showDateFilterDialog() }
        binding.btnFilterUser.setOnClickListener { showUserFilterDialog() }
        binding.btnToggleSort.setOnClickListener {
            newestFirst = !newestFirst
            binding.tvSortOrder.text = if (newestFirst) "Terbaru" else "Terlama"
            applyFilterAndRender()
        }

        binding.chipTypeAll.setOnClickListener { selectType(null) }
        binding.chipTypeSale.setOnClickListener { selectType(ActivityType.SALE) }
        binding.chipTypeStock.setOnClickListener { selectType(ActivityType.STOCK) }
        binding.chipTypeOrder.setOnClickListener { selectType(ActivityType.ORDER) }
        binding.chipTypeExpense.setOnClickListener { selectType(ActivityType.EXPENSE) }
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("Semua tanggal", "Hari ini", "7 hari terakhir", "Pilih tanggal...")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter tanggal")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        fromMillis = null
                        toMillis = null
                        dateLabel = "Semua tanggal"
                        binding.tvFilterDate.text = dateLabel
                        applyFilterAndRender()
                    }

                    1 -> {
                        val cal = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)
                        setSingleDayRange(cal)
                        dateLabel = "Hari ini"
                        binding.tvFilterDate.text = dateLabel
                        applyFilterAndRender()
                    }

                    2 -> {
                        val endCal = Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        val startCal = (endCal.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, -6)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        fromMillis = startCal.timeInMillis
                        toMillis = endCal.timeInMillis
                        dateLabel = "7 hari terakhir"
                        binding.tvFilterDate.text = dateLabel
                        applyFilterAndRender()
                    }

                    3 -> openSingleDatePicker()
                }
            }
            .show()
    }

    private fun openSingleDatePicker() {
        val now = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance(DateFormatter.WITA_TIMEZONE).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                setSingleDayRange(chosen)
                dateLabel = DateFormatter.formatDate(Date(chosen.timeInMillis))
                binding.tvFilterDate.text = dateLabel
                applyFilterAndRender()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setSingleDayRange(cal: Calendar) {
        val start = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        fromMillis = start.timeInMillis
        toMillis = end.timeInMillis
    }

    private fun showUserFilterDialog() {
        val users = ActivityHistoryRepository.getDistinctUserNames()
        val options = (listOf("Semua user") + users).toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter karyawan")
            .setItems(options) { _, which ->
                selectedUser = if (which == 0) null else users[which - 1]
                binding.tvFilterUser.text = selectedUser ?: "Semua user"
                applyFilterAndRender()
            }
            .show()
    }

    private fun selectType(type: ActivityType?) {
        selectedType = type
        updateTypeChipStyles()
        applyFilterAndRender()
    }

    private fun updateTypeChipStyles() {
        styleChip(binding.chipTypeAll, selectedType == null)
        styleChip(binding.chipTypeSale, selectedType == ActivityType.SALE)
        styleChip(binding.chipTypeStock, selectedType == ActivityType.STOCK)
        styleChip(binding.chipTypeOrder, selectedType == ActivityType.ORDER)
        styleChip(binding.chipTypeExpense, selectedType == ActivityType.EXPENSE)
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        val ctx = requireContext()
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected)
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.bg_white))
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected)
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ActivityHistoryRepository.items.collect {
                        updateTypeChipStyles()
                        applyFilterAndRender()
                    }
                }
                launch {
                    NotificationRepository.unreadCount.collect { unread ->
                        binding.tvNotificationBadge.isVisible = unread > 0
                        binding.tvNotificationBadge.text = unread.toString()
                    }
                }
            }
        }
    }

    private fun applyFilterAndRender() {
        val filtered = ActivityHistoryRepository.query(
            type = selectedType,
            userName = selectedUser,
            fromMillis = fromMillis,
            toMillis = toMillis,
            newestFirst = newestFirst
        )
        adapter.submitList(filtered)
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.rvHistory.isVisible = filtered.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
