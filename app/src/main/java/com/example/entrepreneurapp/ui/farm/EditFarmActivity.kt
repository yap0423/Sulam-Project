package com.example.entrepreneurapp.ui.farm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.databinding.ActivityEditFarmBinding
import com.example.entrepreneurapp.models.Farm
import com.example.entrepreneurapp.models.PineappleVariety
import com.example.entrepreneurapp.repository.FarmRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class EditFarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditFarmBinding
    private val farmRepository = FarmRepository()

    private val farmerTypes = listOf("Individual", "Commercial")
    private val pineappleTypes = listOf("MD2", "Josapine", "N36", "Moris")
    private var varietyViews = mutableListOf<View>()

    private lateinit var farmId: String
    private var currentFarm: Farm? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditFarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get farm ID from intent
        farmId = intent.getStringExtra("FARM_ID") ?: run {
            Toast.makeText(this, "Error: Farm not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupClickListeners()
        setupFarmerTypeSpinner()
        loadFarmData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnAddVariety.setOnClickListener { addVarietyField() }
        binding.btnSave.setOnClickListener { saveFarm() }
    }

    private fun setupFarmerTypeSpinner() {
        val spinner: Spinner = binding.spinnerFarmerType
        val adapter = ArrayAdapter(
            this@EditFarmActivity,
            android.R.layout.simple_spinner_item,
            farmerTypes.toMutableList()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun loadFarmData() {
        setLoading(true)
        lifecycleScope.launch {
            val result = farmRepository.getUserFarms()
            result.fold(
                onSuccess = { farms ->
                    currentFarm = farms.find { it.id == farmId }

                    if (currentFarm == null) {
                        Toast.makeText(this@EditFarmActivity, "Farm not found", Toast.LENGTH_SHORT).show()
                        finish()
                        return@fold
                    }

                    // Populate fields
                    binding.etFarmName.setText(currentFarm?.name)
                    binding.etLocation.setText(currentFarm?.location)
                    binding.etLatitude.setText(currentFarm?.gpsLatitude)
                    binding.etLongitude.setText(currentFarm?.gpsLongitude)
                    binding.etTotalSize.setText(currentFarm?.totalSize.toString())

                    // Set Farmer Type
                    currentFarm?.farmerType?.let { type ->
                        val position = farmerTypes.indexOf(type)
                        if (position >= 0) binding.spinnerFarmerType.setSelection(position)
                    }

                    // Load varieties
                    currentFarm?.varieties?.forEach { addVarietyField(it) }
                    if (currentFarm?.varieties?.isEmpty() == true) addVarietyField()

                    setLoading(false)
                },
                onFailure = { exception ->
                    setLoading(false)
                    Toast.makeText(this@EditFarmActivity, "Error loading farm: ${exception.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        }
    }

    private fun addVarietyField(variety: PineappleVariety? = null) {
        val varietyView = LayoutInflater.from(this).inflate(R.layout.item_variety_input, binding.varietiesContainer, false)
        val tvVarietyNumber = varietyView.findViewById<android.widget.TextView>(R.id.tvVarietyNumber)
        tvVarietyNumber.text = "Variety ${varietyViews.size + 1}"

        val etVarietyType = varietyView.findViewById<android.widget.AutoCompleteTextView>(R.id.etVarietyType)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pineappleTypes)
        etVarietyType.setAdapter(adapter)

        variety?.let {
            etVarietyType.setText(it.variety, false)
            varietyView.findViewById<TextInputEditText>(R.id.etAreaSize).setText(it.areaSize.toString())
        }

        val btnRemoveVariety = varietyView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveVariety)
        btnRemoveVariety.visibility = if (varietyViews.isEmpty()) View.GONE else View.VISIBLE
        btnRemoveVariety.setOnClickListener { removeVarietyField(varietyView) }

        binding.varietiesContainer.addView(varietyView)
        varietyViews.add(varietyView)
        updateVarietyNumbers()
    }

    private fun removeVarietyField(view: View) {
        binding.varietiesContainer.removeView(view)
        varietyViews.remove(view)
        updateVarietyNumbers()
    }

    private fun updateVarietyNumbers() {
        varietyViews.forEachIndexed { index, view ->
            view.findViewById<android.widget.TextView>(R.id.tvVarietyNumber).text = "Variety ${index + 1}"
            val btnRemoveVariety = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveVariety)
            btnRemoveVariety.visibility = if (index == 0) View.GONE else View.VISIBLE
        }
    }

    private fun saveFarm() {
        binding.tilFarmName.error = null
        binding.tilLocation.error = null
        binding.tilTotalSize.error = null

        val farmName = binding.etFarmName.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val latitude = binding.etLatitude.text.toString().trim()
        val longitude = binding.etLongitude.text.toString().trim()
        val totalSizeStr = binding.etTotalSize.text.toString().trim()

        if (farmName.isEmpty()) { binding.tilFarmName.error = "Farm name is required"; return }
        if (location.isEmpty()) { binding.tilLocation.error = "Location is required"; return }
        if (totalSizeStr.isEmpty()) { binding.tilTotalSize.error = "Total farm size is required"; return }

        val totalSize = totalSizeStr.toDoubleOrNull()
        if (totalSize == null || totalSize <= 0) { binding.tilTotalSize.error = "Please enter a valid size"; return }

        val varieties = mutableListOf<PineappleVariety>()
        var hasError = false
        var totalVarietyArea = 0.0

        varietyViews.forEach { view ->
            val etVarietyType = view.findViewById<android.widget.AutoCompleteTextView>(R.id.etVarietyType)
            val etAreaSize = view.findViewById<TextInputEditText>(R.id.etAreaSize)
            val tilVarietyType = view.findViewById<TextInputLayout>(R.id.tilVarietyType)
            val tilAreaSize = view.findViewById<TextInputLayout>(R.id.tilAreaSize)

            tilVarietyType.error = null
            tilAreaSize.error = null

            val varietyType = etVarietyType.text.toString().trim()
            val areaSizeStr = etAreaSize.text.toString().trim()

            if (varietyType.isEmpty()) { tilVarietyType.error = "Select variety type"; hasError = true; return@forEach }
            if (areaSizeStr.isEmpty()) { tilAreaSize.error = "Enter area size"; hasError = true; return@forEach }

            val areaSize = areaSizeStr.toDoubleOrNull()
            if (areaSize == null || areaSize <= 0) { tilAreaSize.error = "Invalid area size"; hasError = true; return@forEach }

            totalVarietyArea += areaSize
            varieties.add(PineappleVariety(varietyType, areaSize))
        }

        if (hasError) { Toast.makeText(this, "Please fix the errors", Toast.LENGTH_SHORT).show(); return }
        if (varieties.isEmpty()) { Toast.makeText(this, "Please add at least one variety", Toast.LENGTH_SHORT).show(); return }
        if (totalVarietyArea > totalSize) { Toast.makeText(this, "Total variety area ($totalVarietyArea) exceeds farm size ($totalSize)", Toast.LENGTH_LONG).show(); return }

        val farmerType = binding.spinnerFarmerType.selectedItem as String

        val updatedFarm = Farm(
            id = farmId,
            userId = currentFarm?.userId ?: "",
            name = farmName,
            location = location,
            gpsLatitude = latitude,
            gpsLongitude = longitude,
            totalSize = totalSize,
            varieties = varieties,
            farmerType = farmerType,
            createdAt = currentFarm?.createdAt ?: System.currentTimeMillis()
        )

        setLoading(true)
        lifecycleScope.launch {
            val result = farmRepository.updateFarm(updatedFarm)
            setLoading(false)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@EditFarmActivity, "Farm updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { exception ->
                    Toast.makeText(this@EditFarmActivity, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
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

        varietyViews.forEach { view ->
            view.findViewById<android.widget.AutoCompleteTextView>(R.id.etVarietyType)?.isEnabled = !isLoading
            view.findViewById<TextInputEditText>(R.id.etAreaSize)?.isEnabled = !isLoading
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveVariety)?.isEnabled = !isLoading
        }
    }
}
