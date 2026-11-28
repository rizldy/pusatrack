package com.pusacoffee.pusatrack.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.data.model.ProductCostSummary
import com.pusacoffee.pusatrack.databinding.ItemProductBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

data class ProductListItem(
    val product: Product,
    val costSummary: ProductCostSummary
)

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<ProductListItem, ProductAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductListItem) {
            val product = item.product
            val cost = item.costSummary

            binding.tvProductName.text = product.productName
            binding.tvInactiveBadge.isVisible = !product.isActive
            binding.tvSellingPrice.text = CurrencyFormatter.format(product.sellingPrice)
            binding.tvHppAndMargin.text =
                "HPP ${CurrencyFormatter.format(cost.estimatedHpp)} · Margin ${cost.formatMarginPercent()}"
            binding.root.alpha = if (product.isActive) 1.0f else 0.72f

            binding.root.setOnClickListener { onItemClick(product) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductListItem>() {
        override fun areItemsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean =
            oldItem.product.productId == newItem.product.productId

        override fun areContentsTheSame(oldItem: ProductListItem, newItem: ProductListItem): Boolean =
            oldItem == newItem
    }
}
