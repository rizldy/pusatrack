package com.pusacoffee.pusatrack.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.LeadTimeFormatter
import com.pusacoffee.pusatrack.data.model.OrderStatus
import com.pusacoffee.pusatrack.data.model.PurchaseOrder
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.databinding.FragmentOrdersBinding
import com.pusacoffee.pusatrack.databinding.ItemActiveOrderBinding
import com.pusacoffee.pusatrack.databinding.ItemReceivedOrderBinding
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.fabCreateOrder.setOnClickListener {
            findNavController().navigate(R.id.createOrderFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                PurchaseOrderRepository.orders.collect { orders ->
                    renderOrdersScreen(orders)
                }
            }
        }
    }

    private fun renderOrdersScreen(orders: List<PurchaseOrder>) {
        val stats = PurchaseOrderRepository.getLeadTimeStats()
        binding.tvAvgLeadTime.text = if (stats.completedCount > 0) {
            LeadTimeFormatter.formatLong(stats.avgSeconds)
        } else {
            "-"
        }
        binding.tvCompletedCount.text = "dari ${stats.completedCount} pesanan diterima"
        binding.tvFastestLeadTime.text = if (stats.completedCount > 0) {
            LeadTimeFormatter.formatShort(stats.fastestSeconds)
        } else {
            "-"
        }
        binding.tvSlowestLeadTime.text = if (stats.completedCount > 0) {
            LeadTimeFormatter.formatShort(stats.slowestSeconds)
        } else {
            "-"
        }

        val activeOrders = orders.filter { it.status == OrderStatus.ORDERED }
        val receivedOrders = orders.filter { it.status == OrderStatus.RECEIVED }

        binding.containerActiveOrders.removeAllViews()
        binding.layoutEmptyActive.isVisible = activeOrders.isEmpty()

        val avgShort = if (stats.completedCount > 0) {
            LeadTimeFormatter.formatShort(stats.avgSeconds)
        } else {
            "-"
        }

        for (order in activeOrders) {
            val itemBinding = ItemActiveOrderBinding.inflate(
                layoutInflater,
                binding.containerActiveOrders,
                false
            )
            itemBinding.tvOrderNumber.text = order.orderNumber
            itemBinding.tvSupplier.text = "${order.supplierName} · oleh ${order.userName}"
            itemBinding.tvStepOrdered.text =
                "Dipesan · ${order.orderDateLabel} ${order.orderTimeLabel}"

            val elapsedSec = ((System.currentTimeMillis() - order.orderDateMillis) / 1000L)
                .coerceAtLeast(60L)
            itemBinding.tvStepInTransit.text =
                "Dalam perjalanan · ${LeadTimeFormatter.formatShort(elapsedSec)} (rata-rata $avgShort)"

            itemBinding.tvItemsSummary.text = order.details.joinToString(", ") {
                "${it.ingredientName} (${it.quantity} ${it.purchaseUnit})"
            }

            itemBinding.btnConfirmReceive.setOnClickListener {
                val args = Bundle().apply { putInt("orderId", order.orderId) }
                findNavController().navigate(R.id.receiveOrderFragment, args)
            }

            binding.containerActiveOrders.addView(itemBinding.root)
        }

        binding.containerReceivedOrders.removeAllViews()
        for (order in receivedOrders) {
            val recBinding = ItemReceivedOrderBinding.inflate(
                layoutInflater,
                binding.containerReceivedOrders,
                false
            )
            recBinding.tvOrderNumber.text = "${order.orderNumber} · ${order.supplierName}"
            recBinding.tvReceivedDate.text = "Diterima ${order.receivedDateLabel ?: "-"}"
            recBinding.tvLeadTimeChip.text = LeadTimeFormatter.formatShort(order.leadTimeSeconds ?: 0L)
            binding.containerReceivedOrders.addView(recBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
