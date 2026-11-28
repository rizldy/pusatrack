package com.pusacoffee.pusatrack.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.databinding.FragmentOrderCreatedBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

class OrderCreatedFragment : Fragment() {

    private var _binding: FragmentOrderCreatedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderCreatedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = arguments?.getInt("orderId", -1) ?: -1
        val order = PurchaseOrderRepository.getOrderById(orderId)
        if (order == null) {
            findNavController().navigateUp()
            return
        }

        binding.tvOrderCreatedSubtitle.text =
            "${order.orderNumber} · ${CurrencyFormatter.format(order.totalAmount)}"

        val ctx = requireContext()
        binding.containerOrderItems.removeAllViews()
        for (detail in order.details) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }
            val left = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = detail.ingredientName
                setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                textSize = 14f
            }
            val right = TextView(ctx).apply {
                text = "${detail.quantity} ${detail.purchaseUnit}"
                setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
                textSize = 14f
            }
            row.addView(left)
            row.addView(right)
            binding.containerOrderItems.addView(row)
        }

        binding.btnSendToWhatsApp.setOnClickListener {
            val args = Bundle().apply { putInt("orderId", order.orderId) }
            findNavController().navigate(R.id.whatsAppOrderFragment, args)
        }

        binding.btnViewOrders.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.ordersFragment, false)
                .setLaunchSingleTop(true)
                .build()
            findNavController().navigate(R.id.ordersFragment, null, navOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
