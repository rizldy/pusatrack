package com.pusacoffee.pusatrack.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.StockStatus
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.NotificationRepository
import com.pusacoffee.pusatrack.databinding.FragmentInventoryBinding
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IngredientAdapter
    private var selectedFilter: StockStatus? = null
    private var searchQuery: String = ""
    private var allIngredients: List<Ingredient> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupControls()
        observeIngredients()
    }

    private fun setupRecyclerView() {
        adapter = IngredientAdapter { ingredient ->
            findNavController().navigate(
                R.id.ingredientDetailFragment,
                bundleOf("ingredientId" to ingredient.ingredientId)
            )
        }
        binding.rvIngredients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIngredients.adapter = adapter
    }

    private fun setupControls() {
        binding.btnStockCheck.setOnClickListener {
            findNavController().navigate(R.id.stockCheckFragment)
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.fabAddIngredient.setOnClickListener {
            findNavController().navigate(R.id.addIngredientFragment)
        }

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            binding.containerSearch.isActivated = hasFocus
        }

        binding.etSearch.doAfterTextChanged { editable ->
            searchQuery = editable?.toString().orEmpty().trim()
            applyFilterAndRender()
        }

        binding.chipAll.setOnClickListener { selectFilter(null) }
        binding.chipSafe.setOnClickListener { selectFilter(StockStatus.SAFE) }
        binding.chipLow.setOnClickListener { selectFilter(StockStatus.LOW) }
        binding.chipOut.setOnClickListener { selectFilter(StockStatus.OUT) }
    }

    private fun selectFilter(status: StockStatus?) {
        selectedFilter = status
        updateChipStyles()
        applyFilterAndRender()
    }

    private fun updateChipStyles() {
        styleChip(binding.chipAll, selectedFilter == null)
        styleChip(binding.chipSafe, selectedFilter == StockStatus.SAFE)
        styleChip(binding.chipLow, selectedFilter == StockStatus.LOW)
        styleChip(binding.chipOut, selectedFilter == StockStatus.OUT)
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

    private fun observeIngredients() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    InventoryRepository.ingredients.collect { list ->
                        allIngredients = list.filter { it.isActive }
                        updateCountsAndBanner()
                        updateChipStyles()
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

    private fun updateCountsAndBanner() {
        val total = allIngredients.size
        val safeCount = allIngredients.count { it.stockStatus == StockStatus.SAFE }
        val lowCount = allIngredients.count { it.stockStatus == StockStatus.LOW }
        val outCount = allIngredients.count { it.stockStatus == StockStatus.OUT }

        binding.chipAll.text = "Semua ($total)"
        binding.chipSafe.text = "Aman ($safeCount)"
        binding.chipLow.text = "Menipis ($lowCount)"
        binding.chipOut.text = "Habis ($outCount)"

        val attentionCount = lowCount + outCount
        binding.bannerLowStock.isVisible = attentionCount > 0
        if (attentionCount > 0) {
            binding.tvLowStockBanner.text = when {
                outCount > 0 && lowCount > 0 ->
                    "$outCount bahan habis dan $lowCount bahan menipis — segera lakukan pengadaan."
                outCount > 0 ->
                    "$outCount bahan baku habis — segera lakukan pengadaan."
                else ->
                    "$lowCount bahan baku berada di bawah batas minimum."
            }
        }
    }

    private fun applyFilterAndRender() {
        val filtered = allIngredients.filter { item ->
            val matchesStatus = selectedFilter == null || item.stockStatus == selectedFilter
            val matchesQuery = searchQuery.isEmpty() ||
                item.ingredientName.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
        adapter.submitList(filtered)
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.rvIngredients.isVisible = filtered.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
