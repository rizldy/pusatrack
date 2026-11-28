package com.pusacoffee.pusatrack.ui.cashier

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.databinding.ItemCashierProductBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter

data class CashierMenuItem(
    val product: Product,
    val selectedQty: Int
)

class CashierProductAdapter(
    private val onProductTap: (Product) -> Unit
) : ListAdapter<CashierMenuItem, CashierProductAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCashierProductBinding.inflate(
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
        private val binding: ItemCashierProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CashierMenuItem) {
            val ctx = binding.root.context
            val isSelected = item.selectedQty > 0

            binding.tvProductName.text = item.product.productName
            binding.tvProductPrice.text = CurrencyFormatter.format(item.product.sellingPrice)

            binding.tvQtyBadge.isVisible = isSelected
            binding.tvQtyBadge.text = "×${item.selectedQty}"

            binding.cardProduct.setCardBackgroundColor(
                ContextCompat.getColor(
                    ctx,
                    if (isSelected) R.color.primary_surface_soft else R.color.surface_card
                )
            )

            binding.cardProduct.setOnClickListener {
                onProductTap(item.product)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CashierMenuItem>() {
        override fun areItemsTheSame(oldItem: CashierMenuItem, newItem: CashierMenuItem): Boolean =
            oldItem.product.productId == newItem.product.productId

        override fun areContentsTheSame(oldItem: CashierMenuItem, newItem: CashierMenuItem): Boolean =
            oldItem == newItem
    }
}
