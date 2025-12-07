package com.example.entrepreneurapp.ui.planner

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemChatMessageBinding
import com.example.entrepreneurapp.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isCurrentUser = message.userId == currentUserId

            binding.apply {
                if (message.isResolution) {
                    // Resolution message style
                    messageCard.setCardBackgroundColor(
                        android.graphics.Color.parseColor("#E8F5E9")
                    )
                    tvAvatar.text = "✓"
                    tvName.text = "System"
                    tvMessage.text = message.message
                    tvTime.text = message.getTimeString()

                    (root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        leftMargin = 32
                        rightMargin = 32
                    }
                } else {
                    // Regular message style
                    tvAvatar.text = message.userAvatar
                    tvName.text = message.userName
                    tvMessage.text = message.message
                    tvTime.text = message.getTimeString()

                    if (isCurrentUser) {
                        messageCard.setCardBackgroundColor(
                            android.graphics.Color.parseColor("#E3F2FD")
                        )
                        (root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            leftMargin = 64
                            rightMargin = 16
                        }
                    } else {
                        messageCard.setCardBackgroundColor(
                            android.graphics.Color.parseColor("#FFFFFF")
                        )
                        (root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            leftMargin = 16
                            rightMargin = 64
                        }
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem
    }
}