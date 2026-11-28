package com.pusacoffee.pusatrack.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.repository.InventoryRepository
import com.pusacoffee.pusatrack.data.repository.ProductRepository
import com.pusacoffee.pusatrack.databinding.FragmentProductsBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.addProductFragment)
        }

        adapter = ProductAdapter { product ->
            findNavController().navigate(
                R.id.productDetailFragment,
                bundleOf("productId" to product.productId)
            )
        }
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    ProductRepository.products,
                    InventoryRepository.ingredients
                ) { products, _ ->
                    products.map { product ->
                        ProductListItem(
                            product = product,
                            costSummary = ProductRepository.calculateProductCost(product)
                        )
                    }
                }.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmptyProducts.isVisible = list.isEmpty()
                    binding.rvProducts.isVisible = list.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
