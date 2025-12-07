package com.example.entrepreneurapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.databinding.ActivityEditProfileBinding
import com.example.entrepreneurapp.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val avatarEmojis = listOf(
        "👤", "🌾", "🥬", "🍍", "🌽", "🥕", "🍅",
        "🥦", "🫑", "🥒", "🧅", "🧄", "🥔", "🍠"
    )

    private val malaysiaRegions = listOf(
        "Kluang, Johor",
        "Kubang Pasu, Kedah",
        "Pasir Puteh, Kelantan",
        "Alor Gajah, Melaka",
        "Kuala Pilah, Negeri Sembilan",
        "Rompin, Pahang",
        "Seberang Perai Selatan, Pulau Pinang",
        "Perak Tengah, Perak",
        "Perlis, Perlis",
        "Kuala Langat, Selangor",
        "Setiu, Terengganu",
        "Tuaran, Sabah",
        "Samarahan, Sarawak"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRegionDropdown()
        loadCurrentProfile()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRegionDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, malaysiaRegions)
        binding.etRegion.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnChangeAvatar.setOnClickListener {
            showAvatarPicker()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadCurrentProfile() {
        val currentUser = authRepository.getCurrentUser() ?: return

        lifecycleScope.launch {
            try {
                val documentSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                // Load current values
                binding.etName.setText(documentSnapshot.getString("name") ?: "")
                binding.etEmail.setText(documentSnapshot.getString("email") ?: "")
                binding.etPhone.setText(documentSnapshot.getString("phone") ?: "")
                binding.etRegion.setText(documentSnapshot.getString("region") ?: "", false)

                val avatar = documentSnapshot.getString("avatar") ?: "👤"
                binding.tvAvatarPreview.text = avatar

            } catch (e: Exception) {
                Toast.makeText(
                    this@EditProfileActivity,
                    "Error loading profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showAvatarPicker() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Avatar")

        val items = avatarEmojis.toTypedArray()

        builder.setItems(items) { dialog, which ->
            binding.tvAvatarPreview.text = avatarEmojis[which]
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val region = binding.etRegion.text.toString().trim()
        val avatar = binding.tvAvatarPreview.text.toString()

        // Validate
        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilRegion.error = null

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }

        if (name.length < 3) {
            binding.tilName.error = "Name must be at least 3 characters"
            return
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            return
        }

        if (region.isEmpty()) {
            binding.tilRegion.error = "Please select a region"
            return
        }

        // Save to Firestore
        val currentUser = authRepository.getCurrentUser() ?: return

        setLoading(true)

        lifecycleScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "name" to name,
                    "phone" to phone,
                    "region" to region,
                    "avatar" to avatar
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .update(updates)
                    .await()

                setLoading(false)

                Toast.makeText(
                    this@EditProfileActivity,
                    "Profile updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                finish() // Go back to profile

            } catch (e: Exception) {
                setLoading(false)

                Toast.makeText(
                    this@EditProfileActivity,
                    "Error saving profile: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etRegion.isEnabled = !isLoading
        binding.btnChangeAvatar.isEnabled = !isLoading
    }


}