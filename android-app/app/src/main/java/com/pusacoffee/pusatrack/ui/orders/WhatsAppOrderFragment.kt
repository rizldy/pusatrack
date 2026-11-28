package com.pusacoffee.pusatrack.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.LeadTimeFormatter
import com.pusacoffee.pusatrack.data.repository.PurchaseOrderRepository
import com.pusacoffee.pusatrack.databinding.FragmentWhatsappOrderBinding
import com.pusacoffee.pusatrack.utils.IntentHelper

class WhatsAppOrderFragment : Fragment() {

    private var _binding: FragmentWhatsappOrderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhatsappOrderBinding.inflate(inflater, container, false)
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

        val senderName = SessionManager(requireContext()).getFullName().orEmpty().ifBlank { order.userName }
        val messageText = LeadTimeFormatter.buildWhatsAppMessage(order, senderName)

        binding.tvOrderSubtitle.text = "${order.orderNumber} · ${order.supplierName}"
        binding.tvMessagePreview.text = messageText

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnOpenWhatsApp.setOnClickListener {
            IntentHelper.sendWhatsAppText(requireContext(), messageText)
        }

        binding.btnLater.setOnClickListener {
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
