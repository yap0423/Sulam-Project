package com.example.entrepreneurapp.ui.business

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityEditBusinessBinding
import com.example.entrepreneurapp.models.Business
import com.example.entrepreneurapp.repository.BusinessRepository
import kotlinx.coroutines.launch

class EditBusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBusinessBinding
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

    private lateinit var businessId: String
    private var currentBusiness: Business? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get business ID from intent
        businessId = intent.getStringExtra("BUSINESS_ID") ?: run {
            Toast.makeText(this, "Error: Business not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupBusinessTypeDropdown()
        setupClickListeners()
        loadBusinessData()
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

    private fun loadBusinessData() {
        setLoading(true)

        lifecycleScope.launch {
            val result = businessRepository.getUserBusinesses()

            result.fold(
                onSuccess = { businesses ->
                    currentBusiness = businesses.find { it.id == businessId }

                    if (currentBusiness == null) {
                        Toast.makeText(
                            this@EditBusinessActivity,
                            "Business not found",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return@fold
                    }

                    // Populate fields
                    binding.etBusinessName.setText(currentBusiness?.name)
                    binding.etBusinessType.setText(currentBusiness?.type, false)
                    binding.etPhone.setText(currentBusiness?.phone)
                    binding.etOperatingHours.setText(currentBusiness?.operatingHours)
                    binding.etLocation.setText(currentBusiness?.location)
                    binding.etLatitude.setText(currentBusiness?.gpsLatitude)
                    binding.etLongitude.setText(currentBusiness?.gpsLongitude)
                    binding.etDescription.setText(currentBusiness?.description)

                    setLoading(false)
                },
                onFailure = { exception ->
                    setLoading(false)
                    Toast.makeText(
                        this@EditBusinessActivity,
                        "Error loading business: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            )
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

        // Update business object
        val updatedBusiness = Business(
            id = businessId,
            userId = currentBusiness?.userId ?: "",
            name = businessName,
            type = businessType,
            phone = phone,
            operatingHours = operatingHours,
            location = location,
            gpsLatitude = latitude,
            gpsLongitude = longitude,
            description = description,
            createdAt = currentBusiness?.createdAt ?: System.currentTimeMillis()
        )

        // Save to Firebase
        setLoading(true)

        lifecycleScope.launch {
            val result = businessRepository.updateBusiness(updatedBusiness)

            setLoading(false)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@EditBusinessActivity,
                        "Business updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@EditBusinessActivity,
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