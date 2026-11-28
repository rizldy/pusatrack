package com.pusacoffee.pusatrack.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.model.User
import com.pusacoffee.pusatrack.data.repository.AccountRepository
import com.pusacoffee.pusatrack.databinding.FragmentAccountsBinding
import kotlinx.coroutines.launch

class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!

    private enum class StatusFilter { ALL, ACTIVE, INACTIVE }

    private lateinit var adapter: AccountAdapter
    private var selectedFilter: StatusFilter = StatusFilter.ALL
    private var searchQuery: String = ""
    private var allUsers: List<User> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = SessionManager(requireContext()).getUserId()
        adapter = AccountAdapter(currentUserId) { user ->
            val args = Bundle().apply { putInt("userId", user.userId) }
            findNavController().navigate(R.id.accountDetailFragment, args)
        }

        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccounts.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.fabAddAccount.setOnClickListener {
            findNavController().navigate(R.id.addAccountFragment)
        }

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            binding.containerSearch.isActivated = hasFocus
        }
        binding.etSearch.doAfterTextChanged { editable ->
            searchQuery = editable?.toString().orEmpty().trim()
            applyFilterAndRender()
        }

        binding.chipAll.setOnClickListener { selectFilter(StatusFilter.ALL) }
        binding.chipActive.setOnClickListener { selectFilter(StatusFilter.ACTIVE) }
        binding.chipInactive.setOnClickListener { selectFilter(StatusFilter.INACTIVE) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AccountRepository.users.collect { list ->
                    allUsers = list
                    updateCountsAndSummary()
                    updateChipStyles()
                    applyFilterAndRender()
                }
            }
        }
    }

    private fun selectFilter(filter: StatusFilter) {
        selectedFilter = filter
        updateChipStyles()
        applyFilterAndRender()
    }

    private fun updateCountsAndSummary() {
        val total = allUsers.size
        val activeCount = allUsers.count { it.isActive }
        val inactiveCount = total - activeCount

        binding.tvSubtitleSummary.text = "$activeCount akun aktif · $inactiveCount nonaktif"
        binding.chipAll.text = "Semua ($total)"
        binding.chipActive.text = "Aktif ($activeCount)"
        binding.chipInactive.text = "Nonaktif ($inactiveCount)"
    }

    private fun updateChipStyles() {
        styleChip(binding.chipAll, selectedFilter == StatusFilter.ALL)
        styleChip(binding.chipActive, selectedFilter == StatusFilter.ACTIVE)
        styleChip(binding.chipInactive, selectedFilter == StatusFilter.INACTIVE)
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        val ctx = requireContext()
        chip.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
        chip.setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.bg_white else R.color.text_muted))
    }

    private fun applyFilterAndRender() {
        val filtered = allUsers.filter { user ->
            val matchesStatus = when (selectedFilter) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> user.isActive
                StatusFilter.INACTIVE -> !user.isActive
            }
            val matchesQuery = searchQuery.isEmpty() ||
                user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesQuery
        }
        adapter.submitList(filtered)
        binding.tvEmptyAccounts.isVisible = filtered.isEmpty()
        binding.rvAccounts.isVisible = filtered.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
