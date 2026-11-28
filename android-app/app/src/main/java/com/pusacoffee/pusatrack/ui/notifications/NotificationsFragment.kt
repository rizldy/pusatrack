package com.pusacoffee.pusatrack.ui.notifications

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusacoffee.pusatrack.MainActivity
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.AppNotification
import com.pusacoffee.pusatrack.data.repository.NotificationRepository
import com.pusacoffee.pusatrack.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NotificationRepository.refresh()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnMarkAllRead.setOnClickListener {
            NotificationRepository.markAllAsRead()
        }

        adapter = NotificationAdapter { item ->
            handleNotificationClick(item)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationRepository.notifications.collect { list ->
                    adapter.submitList(list)
                    binding.layoutEmpty.isVisible = list.isEmpty()
                    binding.rvNotifications.isVisible = list.isNotEmpty()
                }
            }
        }
    }

    private fun handleNotificationClick(item: AppNotification) {
        NotificationRepository.markAsRead(item.id)
        val ingredientId = item.targetIngredientId
        val orderId = item.targetOrderId

        when {
            ingredientId != null -> {
                val args = Bundle().apply { putInt("ingredientId", ingredientId) }
                findNavController().navigate(R.id.ingredientDetailFragment, args)
            }

            orderId != null -> {
                (activity as? MainActivity)?.navigateToTab(R.id.ordersFragment)
                    ?: findNavController().navigate(R.id.ordersFragment)
            }

            else -> {
                (activity as? MainActivity)?.navigateToTab(R.id.inventoryFragment)
                    ?: findNavController().navigate(R.id.inventoryFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
