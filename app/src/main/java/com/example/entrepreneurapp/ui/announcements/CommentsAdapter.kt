package com.example.entrepreneurapp.ui.announcements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.models.Comment
import com.google.android.material.button.MaterialButton

class CommentsAdapter(
    private var comments: List<Comment>,
    private val currentUserId: String,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserAvatar: TextView = itemView.findViewById(R.id.tvUserAvatar)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val btnDeleteComment: MaterialButton = itemView.findViewById(R.id.btnDeleteComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        // Set data
        holder.tvUserAvatar.text = comment.userAvatar
        holder.tvUserName.text = comment.userName
        holder.tvTimeAgo.text = comment.getTimeAgo()
        holder.tvContent.text = comment.content

        // Show delete button only for own comments
        if (comment.userId == currentUserId) {
            holder.btnDeleteComment.visibility = View.VISIBLE
            holder.btnDeleteComment.setOnClickListener {
                onDeleteClick(comment)
            }
        } else {
            holder.btnDeleteComment.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }
}