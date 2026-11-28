package com.pusacoffee.pusatrack.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pusacoffee.pusatrack.MainActivity
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.repository.AuthRepository
import com.pusacoffee.pusatrack.databinding.FragmentHomeBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        authRepository = AuthRepository(requireContext())

        bindUserAndSummary()
        setupClickListeners()
    }

    private fun bindUserAndSummary() {
        val fullName = sessionManager.getFullName().orEmpty().ifBlank { "Karyawan" }

        binding.tvGreeting.text = DateFormatter.getGreeting()
        binding.tvUserName.text = fullName
        binding.tvHeroDate.text = DateFormatter.formatDate()

        // Initial dummy operational summary matching PRD & mockup v7
        val totalSales = 360_000L
        val cashSales = 210_000L
        val qrisSales = 150_000L
        val transactionCount = 18

        binding.tvTodaySales.text = CurrencyFormatter.format(totalSales)
        binding.tvSalesBreakdown.text =
            "$transactionCount transaksi · Tunai ${CurrencyFormatter.format(cashSales)} · QRIS ${CurrencyFormatter.format(qrisSales)}"

        binding.tvExpenseTodayValue.text = CurrencyFormatter.format(40_000L)
        binding.tvExpenseTodayMeta.text = "2 catatan · Tunai"

        binding.tvLastTrxCode.text = "TRX-0015"
        binding.tvLastTrxMeta.text = "09:38 · ${CurrencyFormatter.format(30_000L)}"

        binding.tvLowStockCount.text = "2 bahan"
        binding.tvLowStockNames.text = "Susu UHT, Cup 12 oz"

        binding.tvActiveOrderCode.text = "PSN-014"
        binding.tvActiveOrderMeta.text = "2 hari · Perjalanan"
    }

    private fun setupClickListeners() {
        val mainActivity = activity as? MainActivity

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.btnOpenCashier.setOnClickListener {
            mainActivity?.navigateToTab(R.id.cashierFragment)
        }

        binding.btnViewLowStock.setOnClickListener {
            mainActivity?.navigateToTab(R.id.inventoryFragment)
        }

        binding.cardExpenseToday.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }

        binding.cardLastTransaction.setOnClickListener {
            mainActivity?.navigateToTab(R.id.historyFragment)
        }

        binding.cardLowStockTile.setOnClickListener {
            mainActivity?.navigateToTab(R.id.inventoryFragment)
        }

        binding.cardActiveOrderTile.setOnClickListener {
            mainActivity?.navigateToTab(R.id.ordersFragment)
        }

        // Menu Lainnya
        binding.menuFinance.setOnClickListener {
            findNavController().navigate(R.id.financeFragment)
        }

        binding.menuReports.setOnClickListener {
            findNavController().navigate(R.id.reportsFragment)
        }

        binding.menuExpenses.setOnClickListener {
            findNavController().navigate(R.id.expensesFragment)
        }

        binding.menuProducts.setOnClickListener {
            findNavController().navigate(R.id.productsFragment)
        }

        binding.menuStockCheck.setOnClickListener {
            findNavController().navigate(R.id.stockCheckFragment)
        }

        binding.menuAccounts.setOnClickListener {
            findNavController().navigate(R.id.accountsFragment)
        }

        binding.menuLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.logout_confirm) { _, _ ->
                performLogout()
            }
            .show()
    }

    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.logout()
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
