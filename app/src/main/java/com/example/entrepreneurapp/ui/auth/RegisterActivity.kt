package com.example.entrepreneurapp.ui.auth

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.MainActivity
import com.example.entrepreneurapp.databinding.ActivityRegisterBinding
import com.example.entrepreneurapp.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRegionDropdown()
        setupClickListeners()

    }

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


    private fun setupRegionDropdown() {
        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, malaysiaRegions)
        binding.etRegion.setAdapter(adapter)
    }


    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val region = binding.etRegion.text.toString().trim()


            if (validateInput(name, email, phone,region, password, confirmPassword)) {
                performRegister(name, email, phone,region, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish() // Go back to login
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        region: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Reset errors
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        // Validate name
        if (name.isEmpty()) {
            binding.tilName.error = "Full name is required"
            return false
        }

        if (name.length < 3) {
            binding.tilName.error = "Name must be at least 3 characters"
            return false
        }

        // Validate email
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            return false
        }

        // Validate phone
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            return false
        }

        if (phone.length < 9) {
            binding.tilPhone.error = "Invalid phone number"
            return false
        }

        // Validate region
        if (region.isEmpty()) {
            binding.tilRegion.error = "Region is required"
            return false
        }

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            return false
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun performRegister(
        name: String,
        email: String,
        phone: String,
        region: String,
        password: String
    ) {
        // Show loading
        setLoading(true)

        // Format phone number
        val formattedPhone = "+60$phone"

        lifecycleScope.launch {
            val result = authRepository.register(name, email, formattedPhone, password,region)

            setLoading(false)

            result.fold(
                onSuccess = { user ->
                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created successfully! Welcome, ${user.name}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToHome()
                },
                onFailure = { exception ->
                    val errorMessage = when {
                        exception.message?.contains("already in use") == true ->
                            "This email is already registered"
                        exception.message?.contains("weak-password") == true ->
                            "Password is too weak. Use at least 6 characters"
                        exception.message?.contains("network") == true ->
                            "Network error. Please check your connection"
                        else -> "Registration failed: ${exception.message}"
                    }

                    Toast.makeText(
                        this@RegisterActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etRegion.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}