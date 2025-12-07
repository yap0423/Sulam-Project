package com.example.entrepreneurapp.ui.planner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemGroupMemberBinding
import com.example.entrepreneurapp.models.GroupMember
import com.google.firebase.auth.FirebaseAuth

class GroupMembersAdapter : ListAdapter<GroupMember, GroupMembersAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: GroupMember) {
            binding.apply {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isCurrentUser = member.userId == currentUserId

                tvAvatar.text = member.userAvatar
                tvName.text = if (isCurrentUser) "${member.userName} [You]" else member.userName
                tvRegion.text = member.region
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GroupMember>() {
        override fun areItemsTheSame(oldItem: GroupMember, newItem: GroupMember) =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: GroupMember, newItem: GroupMember) =
            oldItem == newItem
    }
}