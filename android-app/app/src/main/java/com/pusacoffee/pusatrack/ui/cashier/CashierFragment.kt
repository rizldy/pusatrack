package com.pusacoffee.pusatrack.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.local.SessionManager
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.databinding.FragmentCashierBinding
import com.pusacoffee.pusatrack.databinding.ItemCartRowBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import com.pusacoffee.pusatrack.utils.DateFormatter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CashierFragment : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!

    private val cashierViewModel: CashierViewModel by activityViewModels()
    private lateinit var menuAdapter: CashierProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fullName = SessionManager(requireContext()).getFullName().orEmpty().ifBlank { "Karyawan" }
        binding.tvCashierSubtitle.text = "Kasir: $fullName · ${DateFormatter.formatDate()}"

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        menuAdapter = CashierProductAdapter { product ->
            cashierViewModel.addItem(product.productId)
        }
        binding.rvMenuProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMenuProducts.adapter = menuAdapter

        binding.btnClearCart.setOnClickListener {
            cashierViewModel.clearCart()
        }

        binding.btnProceedPayment.setOnClickListener {
            if (cashierViewModel.getTotalItemCount() > 0) {
                findNavController().navigate(R.id.paymentFragment)
            }
        }

        observeMenuAndCart()
    }

    private fun observeMenuAndCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    ProductRepository.products,
                    cashierViewModel.cart
                ) { products, cartMap ->
                    val activeProducts = products.filter { it.isActive }
                    val menuItems = activeProducts.map { p ->
                        CashierMenuItem(p, cartMap[p.productId] ?: 0)
                    }
                    menuItems to cartMap
                }.collect { (menuItems, _) ->
                    menuAdapter.submitList(menuItems)
                    binding.tvEmptyMenu.isVisible = menuItems.isEmpty()
                    binding.rvMenuProducts.isVisible = menuItems.isNotEmpty()
                    renderCartSection()
                }
            }
        }
    }

    private fun renderCartSection() {
        val lines = cashierViewModel.getCartLines()
        val hasItems = lines.isNotEmpty()

        binding.btnClearCart.isVisible = hasItems
        binding.tvEmptyCart.isVisible = !hasItems
        binding.btnProceedPayment.isEnabled = hasItems
        binding.btnProceedPayment.alpha = if (hasItems) 1.0f else 0.5f

        binding.containerCartItems.removeAllViews()
        for (line in lines) {
            val rowBinding = ItemCartRowBinding.inflate(
                layoutInflater,
                binding.containerCartItems,
                false
            )
            rowBinding.tvItemName.text = line.product.productName
            rowBinding.tvItemSubtotal.text =
                "${CurrencyFormatter.format(line.product.sellingPrice)} · Subtotal ${CurrencyFormatter.format(line.subtotal)}"
            rowBinding.tvQty.text = line.quantity.toString()

            rowBinding.btnMinus.setOnClickListener {
                cashierViewModel.decrementItem(line.product.productId)
            }
            rowBinding.btnPlus.setOnClickListener {
                cashierViewModel.addItem(line.product.productId)
            }
            binding.containerCartItems.addView(rowBinding.root)
        }

        val totalItems = lines.sumOf { it.quantity }
        val totalAmount = lines.sumOf { it.subtotal }
        binding.tvCartTotalLabel.text = "Total · $totalItems item"
        binding.tvCartTotalAmount.text = CurrencyFormatter.format(totalAmount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
