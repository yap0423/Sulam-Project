package com.example.entrepreneurapp.ui.announcements

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityCreateAnnouncementBinding
import com.example.entrepreneurapp.models.Announcement
import com.example.entrepreneurapp.repository.AnnouncementRepository
import kotlinx.coroutines.launch

class CreateAnnouncementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAnnouncementBinding
    private val announcementRepository = AnnouncementRepository()

    private val categories = listOf(
        "General Updates",
        "Market Prices",
        "Equipment & Tools",
        "Growing Tips",
        "Collaboration",
        "Events & Workshops",
        "Alerts & Warnings",
        "Success Stories"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAnnouncementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.etCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnPost.setOnClickListener {
            createAnnouncement()
        }
    }

    private fun createAnnouncement() {
        // Clear previous errors
        binding.tilCategory.error = null
        binding.tilTitle.error = null
        binding.tilContent.error = null

        // Get values
        val category = binding.etCategory.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        // Validate
        if (category.isEmpty()) {
            binding.tilCategory.error = "Category is required"
            binding.etCategory.requestFocus()
            return
        }

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            binding.etTitle.requestFocus()
            return
        }

        if (title.length < 10) {
            binding.tilTitle.error = "Title must be at least 10 characters"
            binding.etTitle.requestFocus()
            return
        }

        if (content.isEmpty()) {
            binding.tilContent.error = "Content is required"
            binding.etContent.requestFocus()
            return
        }

        if (content.length < 20) {
            binding.tilContent.error = "Content must be at least 20 characters"
            binding.etContent.requestFocus()
            return
        }

        // Create announcement object
        val announcement = Announcement(
            title = title,
            content = content,
            category = category,
            imageUrl = "" // No image for now
        )

        // Save to Firebase
        setLoading(true)

        lifecycleScope.launch {
            val result = announcementRepository.createAnnouncement(announcement)

            setLoading(false)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@CreateAnnouncementActivity,
                        "Announcement posted successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@CreateAnnouncementActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnPost.isEnabled = !isLoading
        binding.etCategory.isEnabled = !isLoading
        binding.etTitle.isEnabled = !isLoading
        binding.etContent.isEnabled = !isLoading
    }
}