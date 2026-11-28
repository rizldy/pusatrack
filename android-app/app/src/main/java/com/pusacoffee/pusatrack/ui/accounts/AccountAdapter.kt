package com.pusacoffee.pusatrack.ui.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pusacoffee.pusatrack.R
import com.pusacoffee.pusatrack.data.model.User
import com.pusacoffee.pusatrack.databinding.ItemAccountBinding

class AccountAdapter(
    private val currentUserId: Int,
    private val onItemClick: (User) -> Unit
) : ListAdapter<User, AccountAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccountBinding.inflate(
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
        private val binding: ItemAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            val ctx = binding.root.context
            binding.tvInitials.text = buildInitials(user.fullName)
            binding.tvFullName.text = user.fullName
            binding.tvUsername.text = "@${user.username}"
            binding.tvYouBadge.isVisible = user.userId == currentUserId

            if (user.isActive) {
                binding.tvStatusBadge.text = "Aktif"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_safe)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
                binding.avatarCircle.setBackgroundResource(R.drawable.bg_success_circle)
                binding.tvInitials.setTextColor(ContextCompat.getColor(ctx, R.color.primary_forest))
            } else {
                binding.tvStatusBadge.text = "Nonaktif"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_out)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
                binding.avatarCircle.setBackgroundResource(R.drawable.bg_berry_circle)
                binding.tvInitials.setTextColor(ContextCompat.getColor(ctx, R.color.tertiary_text))
            }

            binding.root.setOnClickListener { onItemClick(user) }
        }

        private fun buildInitials(name: String): String {
            val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                words.size >= 2 -> "${words[0][0]}${words[1][0]}".uppercase()
                words.size == 1 -> words[0].take(2).uppercase()
                else -> "KR"
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem == newItem
    }
}
