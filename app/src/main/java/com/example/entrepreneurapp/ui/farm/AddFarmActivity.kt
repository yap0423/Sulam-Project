package com.example.entrepreneurapp.ui.farm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.databinding.ActivityAddFarmBinding
import com.example.entrepreneurapp.models.Farm
import com.example.entrepreneurapp.models.PineappleVariety
import com.example.entrepreneurapp.repository.FarmRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddFarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFarmBinding
    private val farmRepository = FarmRepository()

    private val pineappleTypes = listOf("MD2", "Josapine", "N36", "Moris")
    private var varietyViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val farmerTypes = listOf("Select Farm Type", "Individual ", "Commercial")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, farmerTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFarmerType.adapter = spinnerAdapter


        setupToolbar()
        setupClickListeners()

        // Add first variety by default
        addVarietyField()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnAddVariety.setOnClickListener {
            addVarietyField()
        }

        binding.btnSave.setOnClickListener {
            saveFarm()
        }
    }

    private fun addVarietyField() {
        val varietyView = LayoutInflater.from(this)
            .inflate(R.layout.item_variety_input, binding.varietiesContainer, false)

        // Setup variety number
        val tvVarietyNumber = varietyView.findViewById<android.widget.TextView>(R.id.tvVarietyNumber)
        tvVarietyNumber.text = "Variety ${varietyViews.size + 1}"

        // Setup variety type dropdown
        val tilVarietyType = varietyView.findViewById<TextInputLayout>(R.id.tilVarietyType)
        val etVarietyType = varietyView.findViewById<AutoCompleteTextView>(R.id.etVarietyType)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pineappleTypes)
        etVarietyType.setAdapter(adapter)

        // Setup remove button
        val btnRemoveVariety = varietyView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveVariety)

        // First variety cannot be removed
        if (varietyViews.isEmpty()) {
            btnRemoveVariety.visibility = View.GONE
        } else {
            btnRemoveVariety.visibility = View.VISIBLE
            btnRemoveVariety.setOnClickListener {
                removeVarietyField(varietyView)
            }
        }

        // Add to container
        binding.varietiesContainer.addView(varietyView)
        varietyViews.add(varietyView)

        // Update variety numbers
        updateVarietyNumbers()
    }

    private fun removeVarietyField(view: View) {
        binding.varietiesContainer.removeView(view)
        varietyViews.remove(view)
        updateVarietyNumbers()
    }

    private fun updateVarietyNumbers() {
        varietyViews.forEachIndexed { index, view ->
            val tvVarietyNumber = view.findViewById<android.widget.TextView>(R.id.tvVarietyNumber)
            tvVarietyNumber.text = "Variety ${index + 1}"

            // First variety cannot be removed
            val btnRemoveVariety = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveVariety)
            btnRemoveVariety.visibility = if (index == 0) View.GONE else View.VISIBLE
        }
    }

    private fun saveFarm() {
        // Clear previous errors
        binding.tilFarmName.error = null
        binding.tilLocation.error = null
        binding.tilTotalSize.error = null
        binding.tilFarmerType.error = null

        // Get basic information
        val farmName = binding.etFarmName.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val latitude = binding.etLatitude.text.toString().trim()
        val longitude = binding.etLongitude.text.toString().trim()
        val totalSizeStr = binding.etTotalSize.text.toString().trim()
        val selectedFarmType = binding.spinnerFarmerType.selectedItem.toString()


        // Validate basic information
        if (farmName.isEmpty()) {
            binding.tilFarmName.error = "Farm name is required"
            binding.etFarmName.requestFocus()
            return
        }

        if (location.isEmpty()) {
            binding.tilLocation.error = "Location is required"
            binding.etLocation.requestFocus()
            return
        }

        if (totalSizeStr.isEmpty()) {
            binding.tilTotalSize.error = "Total farm size is required"
            binding.etTotalSize.requestFocus()
            return
        }

        val totalSize = totalSizeStr.toDoubleOrNull()
        if (totalSize == null || totalSize <= 0) {
            binding.tilTotalSize.error = "Please enter a valid size"
            binding.etTotalSize.requestFocus()
            return
        }

        if (selectedFarmType == "Select Farm Type") {
            binding.tilFarmerType.error = "Please select farm type"
            return
        }


        // Validate and collect varieties
        val varieties = mutableListOf<PineappleVariety>()
        var hasError = false
        var totalVarietyArea = 0.0

        varietyViews.forEach { view ->
            val tilVarietyType = view.findViewById<TextInputLayout>(R.id.tilVarietyType)
            val etVarietyType = view.findViewById<AutoCompleteTextView>(R.id.etVarietyType)
            val tilAreaSize = view.findViewById<TextInputLayout>(R.id.tilAreaSize)
            val etAreaSize = view.findViewById<TextInputEditText>(R.id.etAreaSize)

            // Clear errors
            tilVarietyType.error = null
            tilAreaSize.error = null

            val varietyType = etVarietyType.text.toString().trim()
            val areaSizeStr = etAreaSize.text.toString().trim()

            if (varietyType.isEmpty()) {
                tilVarietyType.error = "Select variety type"
                hasError = true
                return@forEach
            }

            if (areaSizeStr.isEmpty()) {
                tilAreaSize.error = "Enter area size"
                hasError = true
                return@forEach
            }

            val areaSize = areaSizeStr.toDoubleOrNull()
            if (areaSize == null || areaSize <= 0) {
                tilAreaSize.error = "Invalid area size"
                hasError = true
                return@forEach
            }

            totalVarietyArea += areaSize

            varieties.add(PineappleVariety(
                variety = varietyType,
                areaSize = areaSize
            ))
        }

        if (hasError) {
            Toast.makeText(this, "Please fix the errors", Toast.LENGTH_SHORT).show()
            return
        }

        if (varieties.isEmpty()) {
            Toast.makeText(this, "Please add at least one variety", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if total variety area exceeds farm size
        if (totalVarietyArea > totalSize) {
            Toast.makeText(
                this,
                "Total variety area ($totalVarietyArea acres) exceeds farm size ($totalSize acres)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Create farm object
        val farm = Farm(
            name = farmName,
            location = location,
            gpsLatitude = latitude,
            gpsLongitude = longitude,
            totalSize = totalSize,
            farmerType = selectedFarmType,
            varieties = varieties
        )

        // Save to Firebase
        setLoading(true)

        lifecycleScope.launch {
            val result = farmRepository.addFarm(farm)

            setLoading(false)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@AddFarmActivity,
                        "Farm added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AddFarmActivity,
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
        binding.btnAddVariety.isEnabled = !isLoading
        binding.etFarmName.isEnabled = !isLoading
        binding.etLocation.isEnabled = !isLoading
        binding.etLatitude.isEnabled = !isLoading
        binding.etLongitude.isEnabled = !isLoading
        binding.etTotalSize.isEnabled = !isLoading

        // Disable all variety fields
        varietyViews.forEach { view ->
            view.findViewById<AutoCompleteTextView>(R.id.etVarietyType)?.isEnabled = !isLoading
            view.findViewById<TextInputEditText>(R.id.etAreaSize)?.isEnabled = !isLoading
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveVariety)?.isEnabled = !isLoading
        }
    }
}