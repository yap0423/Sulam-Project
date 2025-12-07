package com.example.entrepreneurapp.ui.announcements

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.models.Announcement
import com.google.android.material.button.MaterialButton

class AnnouncementsAdapter(
    private var announcements: List<Announcement>,
    private val currentUserId: String,
    private val onAnnouncementClick: (Announcement) -> Unit,
    private val onLikeClick: (Announcement) -> Unit,
    private val onCommentClick: (Announcement) -> Unit,
    private val onMoreClick: (Announcement) -> Unit
) : RecyclerView.Adapter<AnnouncementsAdapter.AnnouncementViewHolder>() {

    inner class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserAvatar: TextView = itemView.findViewById(R.id.tvUserAvatar)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        val categoryBadge: LinearLayout = itemView.findViewById(R.id.categoryBadge)
        val tvCategoryEmoji: TextView = itemView.findViewById(R.id.tvCategoryEmoji)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val ivAnnouncementImage: ImageView = itemView.findViewById(R.id.ivAnnouncementImage)
        val btnLike: LinearLayout = itemView.findViewById(R.id.btnLike)
        val tvLikeIcon: TextView = itemView.findViewById(R.id.tvLikeIcon)
        val tvLikesCount: TextView = itemView.findViewById(R.id.tvLikesCount)
        val btnComment: LinearLayout = itemView.findViewById(R.id.btnComment)
        val tvCommentsCount: TextView = itemView.findViewById(R.id.tvCommentsCount)
        val btnMore: MaterialButton = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement_card, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]

        // Set user info
        holder.tvUserAvatar.text = announcement.userAvatar
        holder.tvUserName.text = announcement.userName
        holder.tvTimeAgo.text = announcement.getTimeAgo()

        // Set category with color
        holder.tvCategoryEmoji.text = announcement.getCategoryEmoji()
        holder.tvCategory.text = announcement.category
        try {
            val categoryColor = Color.parseColor(announcement.getCategoryColor())
            holder.categoryBadge.setBackgroundColor(categoryColor)
        } catch (e: Exception) {
            // Fallback to default color
        }

        // Set content
        holder.tvTitle.text = announcement.title
        holder.tvContent.text = announcement.content

        // Set image (if available)
        if (announcement.imageUrl.isNotEmpty()) {
            holder.ivAnnouncementImage.visibility = View.VISIBLE
            // TODO: Load image with Glide/Picasso when implementing image upload
        } else {
            holder.ivAnnouncementImage.visibility = View.GONE
        }

        // Set like status
        val isLiked = announcement.isLikedBy(currentUserId)
        holder.tvLikeIcon.text = if (isLiked) "❤️" else "🤍"
        holder.tvLikesCount.text = announcement.likesCount.toString()

        // Set comments count
        holder.tvCommentsCount.text = announcement.commentsCount.toString()

        // Show more button only for own posts
        if (announcement.userId == currentUserId) {
            holder.btnMore.visibility = View.VISIBLE
        } else {
            holder.btnMore.visibility = View.GONE
        }

        // Click listeners
        holder.itemView.setOnClickListener {
            onAnnouncementClick(announcement)
        }

        holder.btnLike.setOnClickListener {
            onLikeClick(announcement)
        }

        holder.btnComment.setOnClickListener {
            onCommentClick(announcement)
        }

        holder.btnMore.setOnClickListener {
            onMoreClick(announcement)
        }
    }

    override fun getItemCount(): Int = announcements.size

    fun updateAnnouncements(newAnnouncements: List<Announcement>) {
        announcements = newAnnouncements
        notifyDataSetChanged()
    }
}