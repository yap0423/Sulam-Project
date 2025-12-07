package com.example.entrepreneurapp.ui.business

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityAddBusinessBinding
import com.example.entrepreneurapp.models.Business
import com.example.entrepreneurapp.repository.BusinessRepository
import kotlinx.coroutines.launch

class AddBusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBusinessBinding
    private val businessRepository = BusinessRepository()

    private val businessTypes = listOf(
        "Fresh Fruit Shop",
        "Market Stall",
        "Retail Outlet",
        "Processing Plant",
        "Wholesale Distributor",
        "Online Store",
        "Restaurant/Cafe",
        "Food Manufacturing"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBusinessTypeDropdown()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupBusinessTypeDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, businessTypes)
        binding.etBusinessType.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveBusiness()
        }
    }

    private fun saveBusiness() {
        // Clear previous errors
        binding.tilBusinessName.error = null
        binding.tilBusinessType.error = null
        binding.tilPhone.error = null
        binding.tilLocation.error = null

        // Get values
        val businessName = binding.etBusinessName.text.toString().trim()
        val businessType = binding.etBusinessType.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val operatingHours = binding.etOperatingHours.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val latitude = binding.etLatitude.text.toString().trim()
        val longitude = binding.etLongitude.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Validate
        if (businessName.isEmpty()) {
            binding.tilBusinessName.error = "Business name is required"
            binding.etBusinessName.requestFocus()
            return
        }

        if (businessType.isEmpty()) {
            binding.tilBusinessType.error = "Business type is required"
            binding.etBusinessType.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            binding.etPhone.requestFocus()
            return
        }

        if (location.isEmpty()) {
            binding.tilLocation.error = "Location is required"
            binding.etLocation.requestFocus()
            return
        }

        // Create business object
        val business = Business(
            name = businessName,
            type = businessType,
            phone = phone,
            operatingHours = operatingHours,
            location = location,
            gpsLatitude = latitude,
            gpsLongitude = longitude,
            description = description
        )

        // Save to Firebase
        setLoading(true)

        lifecycleScope.launch {
            val result = businessRepository.addBusiness(business)

            setLoading(false)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@AddBusinessActivity,
                        "Business added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AddBusinessActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.etBusinessName.isEnabled = !isLoading
        binding.etBusinessType.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etOperatingHours.isEnabled = !isLoading
        binding.etLocation.isEnabled = !isLoading
        binding.etLatitude.isEnabled = !isLoading
        binding.etLongitude.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading
    }
}