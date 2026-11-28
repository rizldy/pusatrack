package com.pusacoffee.pusatrack.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.AppNotification
import com.pusacoffee.pusatrack.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
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
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppNotification) {
            val ctx = binding.root.context
            binding.tvTitle.text = item.title
            binding.tvMessage.text = item.message
            binding.tvTimeLabel.text = item.timeLabel
            binding.viewUnreadDot.isVisible = !item.isRead

            binding.cardNotification.setCardBackgroundColor(
                ContextCompat.getColor(
                    ctx,
                    if (item.isRead) R.color.surface_card else R.color.secondary_surface_soft
                )
            )

            binding.cardNotification.setOnClickListener { onClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean =
            oldItem == newItem
    }
}
