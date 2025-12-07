package com.example.entrepreneurapp.ui.announcements

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.entrepreneurapp.databinding.ActivityAnnouncementDetailsBinding
import com.example.entrepreneurapp.models.Announcement
import com.example.entrepreneurapp.models.Comment
import com.example.entrepreneurapp.repository.AnnouncementRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AnnouncementDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnnouncementDetailsBinding
    private val announcementRepository = AnnouncementRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var announcementId: String
    private var currentAnnouncement: Announcement? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get announcement ID from intent
        announcementId = intent.getStringExtra("ANNOUNCEMENT_ID") ?: run {
            Toast.makeText(this, "Error: Announcement not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupCommentsRecyclerView()
        setupClickListeners()
        loadAnnouncement()
        loadComments()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCommentsRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""

        commentsAdapter = CommentsAdapter(
            comments = emptyList(),
            currentUserId = currentUserId,
            onDeleteClick = { comment ->
                showDeleteCommentDialog(comment)
            }
        )

        binding.recyclerViewComments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewComments.adapter = commentsAdapter
    }

    private fun setupClickListeners() {
        binding.btnSendComment.setOnClickListener {
            addComment()
        }

        binding.btnLike.setOnClickListener {
            likeAnnouncement()
        }
    }

    private fun loadAnnouncement() {
        setLoading(true)

        lifecycleScope.launch {
            val result = announcementRepository.getAnnouncement(announcementId)

            setLoading(false)

            result.fold(
                onSuccess = { announcement ->
                    currentAnnouncement = announcement
                    displayAnnouncement(announcement)
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Error loading announcement: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            )
        }
    }

    private fun displayAnnouncement(announcement: Announcement) {
        // Set user info
        binding.tvUserAvatar.text = announcement.userAvatar
        binding.tvUserName.text = announcement.userName
        binding.tvTimeAgo.text = announcement.getTimeAgo()

        // Set category with color
        binding.tvCategoryEmoji.text = announcement.getCategoryEmoji()
        binding.tvCategory.text = announcement.category
        try {
            val categoryColor = Color.parseColor(announcement.getCategoryColor())
            binding.categoryBadge.setBackgroundColor(categoryColor)
        } catch (e: Exception) {
            // Fallback to default color
        }

        // Set content
        binding.tvTitle.text = announcement.title
        binding.tvContent.text = announcement.content

        // Set image (if available)
        if (announcement.imageUrl.isNotEmpty()) {
            binding.ivAnnouncementImage.visibility = View.VISIBLE
            // TODO: Load image with Glide/Picasso when implementing image upload
        } else {
            binding.ivAnnouncementImage.visibility = View.GONE
        }

        // Set like status
        val currentUserId = auth.currentUser?.uid ?: ""
        val isLiked = announcement.isLikedBy(currentUserId)
        binding.tvLikeIcon.text = if (isLiked) "❤️" else "🤍"
        binding.tvLikesCount.text = announcement.likesCount.toString()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            val result = announcementRepository.getComments(announcementId)

            result.fold(
                onSuccess = { comments ->
                    if (comments.isEmpty()) {
                        binding.layoutNoComments.visibility = View.VISIBLE
                        binding.recyclerViewComments.visibility = View.GONE
                    } else {
                        binding.layoutNoComments.visibility = View.GONE
                        binding.recyclerViewComments.visibility = View.VISIBLE
                        commentsAdapter.updateComments(comments)
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Error loading comments: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun addComment() {
        val commentText = binding.etComment.text.toString().trim()

        if (commentText.isEmpty()) {
            binding.tilComment.error = "Comment cannot be empty"
            return
        }

        if (commentText.length < 3) {
            binding.tilComment.error = "Comment must be at least 3 characters"
            return
        }

        binding.tilComment.error = null

        val comment = Comment(
            announcementId = announcementId,
            content = commentText
        )

        lifecycleScope.launch {
            val result = announcementRepository.addComment(comment)

            result.fold(
                onSuccess = {
                    binding.etComment.setText("")
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Comment added",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadComments()
                    loadAnnouncement() // Reload to update comment count
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun likeAnnouncement() {
        lifecycleScope.launch {
            val result = announcementRepository.likeAnnouncement(announcementId)

            result.fold(
                onSuccess = {
                    loadAnnouncement() // Reload to update like status
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun showDeleteCommentDialog(comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteComment(comment)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteComment(comment: Comment) {
        lifecycleScope.launch {
            val result = announcementRepository.deleteComment(comment.id, announcementId)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Comment deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadComments()
                    loadAnnouncement() // Reload to update comment count
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AnnouncementDetailsActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}