package com.pusacoffee.pusatrack.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.ActivityHistoryItem
import com.pusacoffee.pusatrack.data.model.ActivityType
import com.pusacoffee.pusatrack.databinding.ItemActivityHistoryBinding

class ActivityHistoryAdapter(
    private val onSaleItemClick: (Int) -> Unit
) : ListAdapter<ActivityHistoryItem, ActivityHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActivityHistoryBinding.inflate(
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
        private val binding: ItemActivityHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActivityHistoryItem) {
            val ctx = binding.root.context
            binding.tvTypeChip.text = item.activityType.displayLabel
            binding.tvDateTime.text = "${item.dateLabel} · ${item.timeLabel}"
            binding.tvTitle.text = "${item.userName} · ${item.title}"
            binding.tvSubtitle.text = item.subtitle
            binding.tvAmountOrDelta.text = item.amountOrDelta

            when (item.activityType) {
                ActivityType.SALE -> {
                    binding.tvTypeChip.setBackgroundResource(R.drawable.bg_status_safe)
                    binding.tvTypeChip.setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
                }

                ActivityType.STOCK -> {
                    binding.tvTypeChip.setBackgroundResource(R.drawable.bg_status_low)
                    binding.tvTypeChip.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_text))
                }

                ActivityType.ORDER -> {
                    binding.tvTypeChip.setBackgroundResource(R.drawable.bg_status_neutral)
                    binding.tvTypeChip.setTextColor(ContextCompat.getColor(ctx, R.color.text_ink))
                }

                ActivityType.EXPENSE -> {
                    binding.tvTypeChip.setBackgroundResource(R.drawable.bg_status_out)
                    binding.tvTypeChip.setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
                }
            }

            binding.tvAmountOrDelta.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (item.isNegativeDelta) R.color.tertiary_text else R.color.primary_forest
                )
            )

            val saleId = item.referenceSaleId
            val isClickableSale = item.activityType == ActivityType.SALE && saleId != null
            binding.ivChevron.isVisible = isClickableSale
            binding.root.isClickable = isClickableSale
            binding.root.isFocusable = isClickableSale
            binding.root.setOnClickListener(
                if (isClickableSale) {
                    { onSaleItemClick(saleId!!) }
                } else {
                    null
                }
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ActivityHistoryItem>() {
        override fun areItemsTheSame(
            oldItem: ActivityHistoryItem,
            newItem: ActivityHistoryItem
        ): Boolean = oldItem.activityId == newItem.activityId

        override fun areContentsTheSame(
            oldItem: ActivityHistoryItem,
            newItem: ActivityHistoryItem
        ): Boolean = oldItem == newItem
    }
}
