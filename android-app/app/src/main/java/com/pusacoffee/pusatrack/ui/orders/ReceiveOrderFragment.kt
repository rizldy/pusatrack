package com.pusacoffee.pusatrack.ui.orders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.LeadTimeFormatter
import com.pusacoffee.pusatrack.data.model.PurchaseOrder
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.data.repository.RepoResult
import com.pusacoffee.pusatrack.databinding.FragmentReceiveOrderBinding
import com.pusacoffee.pusatrack.utils.DateFormatter
import java.util.Calendar

class ReceiveOrderFragment : Fragment() {

    private var _binding: FragmentReceiveOrderBinding? = null
    private val binding get() = _binding!!

    private var order: PurchaseOrder? = null
    private val receiveCalendar: Calendar = Calendar.getInstance(DateFormatter.WITA_TIMEZONE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiveOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = arguments?.getInt("orderId", -1) ?: -1
        order = PurchaseOrderRepository.getOrderById(orderId)
        val item = order
        if (item == null) {
            findNavController().navigateUp()
            return
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvOrderSubtitle.text =
            "${item.orderNumber} · Dipesan ${item.orderDateLabel} ${item.orderTimeLabel}"

        renderReceivedItems(item)
        updateDateTimeAndDuration()

        binding.btnPickDate.setOnClickListener { pickDate() }
        binding.btnPickTime.setOnClickListener { pickTime() }
        binding.cbConfirmMatch.setOnCheckedChangeListener { _, _ ->
            binding.layoutErrorBanner.isVisible = false
        }

        binding.btnSubmitReceive.setOnClickListener { submitReceipt() }
    }

    private fun renderReceivedItems(item: PurchaseOrder) {
        val ctx = requireContext()
        binding.containerReceiveItems.removeAllViews()
        for (detail in item.details) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 7, 0, 7)
            }
            val nameTv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = "${detail.ingredientName} (${detail.quantity} ${detail.purchaseUnit})"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 13f
            }
            val addTv = TextView(ctx).apply {
                text = "+${Ingredient.formatNumber(detail.totalUsageQuantity)} ${detail.usageUnit}"
                setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
                textSize = 13f
            }
            row.addView(nameTv)
            row.addView(addTv)
            binding.containerReceiveItems.addView(row)
        }
    }

    private fun pickDate() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                receiveCalendar.set(Calendar.YEAR, year)
                receiveCalendar.set(Calendar.MONTH, month)
                receiveCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.layoutErrorBanner.isVisible = false
                updateDateTimeAndDuration()
            },
            receiveCalendar.get(Calendar.YEAR),
            receiveCalendar.get(Calendar.MONTH),
            receiveCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickTime() {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                receiveCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                receiveCalendar.set(Calendar.MINUTE, minute)
                binding.layoutErrorBanner.isVisible = false
                updateDateTimeAndDuration()
            },
            receiveCalendar.get(Calendar.HOUR_OF_DAY),
            receiveCalendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeAndDuration() {
        val item = order ?: return
        val date = receiveCalendar.time
        binding.tvReceiveDate.text = DateFormatter.formatDate(date)
        binding.tvReceiveTime.text = DateFormatter.formatTime(date)

        val diffMillis = receiveCalendar.timeInMillis - item.orderDateMillis
        if (diffMillis < 0L) {
            binding.tvLeadTimePreview.text = "Waktu terima tidak valid"
            showError("Waktu terima tidak boleh sebelum waktu pemesanan.")
        } else {
            binding.tvLeadTimePreview.text = LeadTimeFormatter.formatLong(diffMillis / 1000L)
        }
    }

    private fun submitReceipt() {
        val item = order ?: return
        val session = SessionManager(requireContext())
        val userId = session.getUserId().takeIf { it > 0 } ?: 1
        val userName = session.getFullName().orEmpty().ifBlank { "Karyawan" }

        when (
            val result = PurchaseOrderRepository.receiveOrder(
                orderId = item.orderId,
                receivedAtMillis = receiveCalendar.timeInMillis,
                confirmedMatch = binding.cbConfirmMatch.isChecked,
                userId = userId,
                userName = userName
            )
        ) {
            is RepoResult.Success -> findNavController().navigateUp()
            is RepoResult.Error -> showError(result.message)
        }
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
