package com.pusacoffee.pusatrack.ui.inventory

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.StockStatus
import com.pusacoffee.pusatrack.databinding.ItemIngredientBinding
import com.pusacoffee.pusatrack.utils.CurrencyFormatter
import java.io.File

class IngredientAdapter(
    private val onItemClick: (Ingredient) -> Unit
) : ListAdapter<Ingredient, IngredientAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIngredientBinding.inflate(
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
        private val binding: ItemIngredientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Ingredient) {
            val ctx = binding.root.context
            binding.tvInitial.text = buildInitials(item.ingredientName)

            val photoFile = item.imagePath?.let { File(it) }?.takeIf { it.exists() }
            if (photoFile != null) {
                binding.ivIngredientPhoto.isVisible = true
                binding.tvInitial.isVisible = false
                binding.ivIngredientPhoto.load(photoFile) { crossfade(true) }
            } else {
                binding.ivIngredientPhoto.isVisible = false
                binding.tvInitial.isVisible = true
            }

            binding.tvIngredientName.text = item.ingredientName
            binding.tvStockPrimary.text =
                "${item.formatCurrentStockUsage()} (${item.formatPackageBreakdown()})"
            binding.tvProcurementLabel.text =
                "${item.procurementType.displayLabel} · ${CurrencyFormatter.format(item.packagePrice)}/${item.purchaseUnit}"
            binding.tvMinStockLabel.text = item.formatMinimumLabel()
            binding.progressStock.progress = item.stockProgressPercent()

            when (item.stockStatus) {
                StockStatus.SAFE -> {
                    binding.tvStatusChip.text = StockStatus.SAFE.label
                    binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_safe)
                    binding.tvStatusChip.setTextColor(
                        ContextCompat.getColor(ctx, R.color.primary_forest)
                    )
                    binding.progressStock.progressTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.primary_forest)
                    )
                }

                StockStatus.LOW -> {
                    binding.tvStatusChip.text = StockStatus.LOW.label
                    binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_low)
                    binding.tvStatusChip.setTextColor(
                        ContextCompat.getColor(ctx, R.color.secondary_text)
                    )
                    binding.progressStock.progressTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.secondary_caramel)
                    )
                }

                StockStatus.OUT -> {
                    binding.tvStatusChip.text = StockStatus.OUT.label
                    binding.tvStatusChip.setBackgroundResource(R.drawable.bg_status_out)
                    binding.tvStatusChip.setTextColor(
                        ContextCompat.getColor(ctx, R.color.tertiary_text)
                    )
                    binding.progressStock.progressTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.tertiary_berry)
                    )
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun buildInitials(name: String): String {
            val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                words.size >= 2 -> "${words[0][0]}${words[1][0]}".uppercase()
                words.size == 1 -> words[0].take(2).uppercase()
                else -> "BH"
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Ingredient>() {
        override fun areItemsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean =
            oldItem.ingredientId == newItem.ingredientId

        override fun areContentsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean =
            oldItem == newItem
    }
}
