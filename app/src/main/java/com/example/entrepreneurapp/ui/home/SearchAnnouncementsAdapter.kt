package com.example.entrepreneurapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.databinding.ItemSearchAnnouncementBinding
import com.example.entrepreneurapp.models.SearchResult

class SearchAnnouncementsAdapter(
    private val onAnnouncementClick: (SearchResult.AnnouncementResult) -> Unit
) : RecyclerView.Adapter<SearchAnnouncementsAdapter.ViewHolder>() {

    private var announcements = listOf<SearchResult.AnnouncementResult>()

    fun submitList(newAnnouncements: List<SearchResult.AnnouncementResult>) {
        announcements = newAnnouncements
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(announcements[position])
    }

    override fun getItemCount() = announcements.size

    inner class ViewHolder(
        private val binding: ItemSearchAnnouncementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(announcementResult: SearchResult.AnnouncementResult) {
            val announcement = announcementResult.announcement

            binding.tvAvatar.text = announcement.userAvatar
            binding.tvUserName.text = announcement.userName
            binding.tvCategory.text = "[${announcement.category}]"
            binding.tvContent.text = announcement.content
            binding.tvTime.text = announcement.getTimeAgo()

            binding.root.setOnClickListener {
                onAnnouncementClick(announcementResult)
            }
        }
    }
}