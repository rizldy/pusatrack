package com.pusacoffee.pusatrack.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.StockMovement
import com.pusacoffee.pusatrack.databinding.ItemStockMovementBinding

class StockMovementAdapter(
    private var usageUnit: String = ""
) : ListAdapter<StockMovement, StockMovementAdapter.ViewHolder>(DiffCallback) {

    fun updateUnit(unit: String) {
        usageUnit = unit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockMovementBinding.inflate(
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
        private val binding: ItemStockMovementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StockMovement) {
            val ctx = binding.root.context
            binding.tvMovementTitle.text =
                "${item.movementType.displayLabel} · ${item.referenceLabel}"
            binding.tvMovementMeta.text = "${item.dateTimeLabel} · ${item.userName}"

            val isPositive = item.quantity >= 0
            val sign = if (isPositive) "+" else "-"
            val absQty = kotlin.math.abs(item.quantity)
            binding.tvDeltaQty.text = "$sign${Ingredient.formatNumber(absQty)} $usageUnit"
            binding.tvDeltaQty.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (isPositive) R.color.primary_forest else R.color.tertiary_berry
                )
            )
            binding.tvStockAfter.text =
                "Stok: ${Ingredient.formatNumber(item.stockAfter)} $usageUnit"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<StockMovement>() {
        override fun areItemsTheSame(oldItem: StockMovement, newItem: StockMovement): Boolean =
            oldItem.movementId == newItem.movementId

        override fun areContentsTheSame(oldItem: StockMovement, newItem: StockMovement): Boolean =
            oldItem == newItem
    }
}
