package com.example.entrepreneurapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        // Clear previous errors
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        // Get values
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validate
        if (currentPassword.isEmpty()) {
            binding.tilCurrentPassword.error = "Current password is required"
            binding.etCurrentPassword.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "New password is required"
            binding.etNewPassword.requestFocus()
            return
        }

        if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Password must be at least 6 characters"
            binding.etNewPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your new password"
            binding.etConfirmPassword.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return
        }

        if (currentPassword == newPassword) {
            binding.tilNewPassword.error = "New password must be different from current password"
            binding.etNewPassword.requestFocus()
            return
        }

        // Proceed with password change
        setLoading(true)

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser

                if (currentUser == null || currentUser.email == null) {
                    setLoading(false)
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Error: User not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Re-authenticate user with current password
                val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)

                try {
                    currentUser.reauthenticate(credential).await()

                    // If re-authentication successful, update password
                    currentUser.updatePassword(newPassword).await()

                    setLoading(false)

                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Password changed successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } catch (e: Exception) {
                    setLoading(false)

                    // Handle re-authentication errors
                    val errorMessage = when {
                        e.message?.contains("password is invalid") == true ->
                            "Current password is incorrect"
                        e.message?.contains("network") == true ->
                            "Network error. Please check your connection"
                        else ->
                            "Error: ${e.message}"
                    }

                    binding.tilCurrentPassword.error = errorMessage
                }

            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnChangePassword.isEnabled = !isLoading
        binding.etCurrentPassword.isEnabled = !isLoading
        binding.etNewPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }
}