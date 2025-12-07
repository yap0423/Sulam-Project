package com.example.entrepreneurapp.ui.planner

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.databinding.ActivityAddHarvestBinding
import com.example.entrepreneurapp.models.HarvestSchedule
import com.example.entrepreneurapp.repository.AuthRepository
import com.example.entrepreneurapp.repository.HarvestRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddHarvestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHarvestBinding
    private val harvestRepository = HarvestRepository()
    private val authRepository = AuthRepository()

    private var plantedDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHarvestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinners()
        setupWeekSpinner()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Harvest Schedule"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinners() {
        val cropTypes = arrayOf("Pineapples")
        val cropAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cropTypes)
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCropType.adapter = cropAdapter

        val varieties = arrayOf("MD2", "Moris", "Josapine", "Yankee", "Other")
        val varietyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, varieties)
        varietyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVariety.adapter = varietyAdapter
    }

    private fun setupWeekSpinner() {
        val weekRanges = mutableListOf<String>()
        val calendar = Calendar.getInstance(Locale.getDefault())

        // Move calendar to next Monday
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        for (i in 0 until 52) { // next 52 weeks
            val startDate = calendar.time // Monday
            calendar.add(Calendar.DAY_OF_YEAR, 6) // Sunday
            val endDate = calendar.time

            weekRanges.add("${formatter.format(startDate)} - ${formatter.format(endDate)}")

            calendar.add(Calendar.DAY_OF_YEAR, 1) // move to next Monday
        }

        val weekAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weekRanges)
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHarvestWeek.adapter = weekAdapter
    }

    private fun setupClickListeners() {
        binding.btnPlantedDate.setOnClickListener {
            showDatePicker { date ->
                plantedDate = date
                binding.tvPlantedDate.text = formatDate(date)
                binding.tvPlantedDate.visibility = View.VISIBLE
            }
        }

        binding.btnSave.setOnClickListener {
            saveHarvest()
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    private fun saveHarvest() {
        val cropType = binding.spinnerCropType.selectedItem.toString()
        val variety = binding.spinnerVariety.selectedItem.toString()
        val yieldText = binding.etEstimatedYield.text.toString()
        val notes = binding.etNotes.text.toString()

        if (yieldText.isEmpty()) {
            binding.etEstimatedYield.error = "Required"
            return
        }

        val estimatedYield = yieldText.toDoubleOrNull() ?: 0.0

        val selectedWeekRange = binding.spinnerHarvestWeek.selectedItem.toString()
        val dates = selectedWeekRange.split(" - ")
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val harvestStart = formatter.parse(dates[0])
        val harvestEnd = formatter.parse(dates[1])

        if (harvestStart == null || harvestEnd == null) {
            Toast.makeText(this, "Invalid week selection", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.getUserProfile()
            result.onSuccess { user ->
                val harvest = HarvestSchedule(
                    userId = user.uid,
                    userName = user.name,
                    userAvatar = user.avatar,
                    cropType = cropType,
                    variety = variety,
                    plantedDate = plantedDate?.let { Timestamp(it) } ?: Timestamp.now(),
                    estimatedYield = estimatedYield,
                    harvestStartDate = Timestamp(harvestStart),
                    harvestEndDate = Timestamp(harvestEnd),
                    region = user.region,
                    status = "active",
                    notes = notes,
                    createdAt = Timestamp.now()
                )

                val saveResult = harvestRepository.addHarvest(harvest)
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true

                saveResult.onSuccess {
                    Toast.makeText(this@AddHarvestActivity, "Harvest schedule added", Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { exception ->
                    Toast.makeText(this@AddHarvestActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { exception ->
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this@AddHarvestActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
