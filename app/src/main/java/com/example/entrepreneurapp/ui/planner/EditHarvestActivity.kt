package com.example.entrepreneurapp.ui.planner

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityEditHarvestBinding
import com.example.entrepreneurapp.models.HarvestSchedule
import com.example.entrepreneurapp.repository.HarvestRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditHarvestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditHarvestBinding
    private val harvestRepository = HarvestRepository()

    private var harvestId = ""
    private var currentHarvest: HarvestSchedule? = null
    private var plantedDate: Date? = null
    private var harvestStartDate: Date? = null
    private var harvestEndDate: Date? = null
    private val weekRanges = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditHarvestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        harvestId = intent.getStringExtra("harvestId") ?: ""

        setupToolbar()
        setupSpinners()
        setupWeekSpinner()
        setupClickListeners()
        loadHarvestData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Harvest Schedule"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinners() {
        val cropTypes = arrayOf("Pineapples", "Watermelons", "Corn", "Vegetables", "Other")
        val cropAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cropTypes)
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCropType.adapter = cropAdapter

        val varieties = arrayOf("MD2", "Moris", "Josapine", "Yankee", "Other")
        val varietyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, varieties)
        varietyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVariety.adapter = varietyAdapter
    }

    private fun setupWeekSpinner() {
        weekRanges.clear() // use class-level list
        val calendar = Calendar.getInstance(Locale.getDefault())

        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        for (i in 0 until 52) {
            val startDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val endDate = calendar.time
            weekRanges.add("${formatter.format(startDate)} - ${formatter.format(endDate)}")
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val weekAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weekRanges)
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHarvestWeek.adapter = weekAdapter

        // Add listener to update dates
        binding.spinnerHarvestWeek.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedRange = weekRanges[position]
                val dates = selectedRange.split(" - ")
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                harvestStartDate = formatter.parse(dates[0])
                harvestEndDate = formatter.parse(dates[1])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }



    private fun setupClickListeners() {
        binding.btnPlantedDate.setOnClickListener {
            showDatePicker { date ->
                plantedDate = date
                binding.tvPlantedDate.text = formatDate(date)
            }
        }

        binding.btnUpdate.setOnClickListener {
            updateHarvest()
        }
    }

    private fun loadHarvestData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = harvestRepository.getHarvestById(harvestId)
            binding.progressBar.visibility = View.GONE

            result.onSuccess { harvest ->
                currentHarvest = harvest
                populateFields(harvest)
            }.onFailure { exception ->
                Toast.makeText(this@EditHarvestActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateFields(harvest: HarvestSchedule) {
        // Set spinners
        val cropTypes = arrayOf("Pineapples", "Watermelons", "Corn", "Vegetables", "Other")
        binding.spinnerCropType.setSelection(cropTypes.indexOf(harvest.cropType).coerceAtLeast(0))

        val varieties = arrayOf("MD2", "Moris", "Josapine", "Yankee", "Other")
        binding.spinnerVariety.setSelection(varieties.indexOf(harvest.variety).coerceAtLeast(0))

        // Set planted date
        plantedDate = harvest.plantedDate.toDate()
        binding.tvPlantedDate.text = formatDate(plantedDate!!)
        binding.tvPlantedDate.visibility = View.VISIBLE

        // Set original week range and spinner selection
        val currentRange = "${formatDate(harvest.harvestStartDate.toDate())} - ${formatDate(harvest.harvestEndDate.toDate())}"
        binding.tvOriginalWeekRange.text = currentRange
        val selectedIndex = weekRanges.indexOf(currentRange)
        if (selectedIndex >= 0) binding.spinnerHarvestWeek.setSelection(selectedIndex)
        harvestStartDate = harvest.harvestStartDate.toDate()
        harvestEndDate = harvest.harvestEndDate.toDate()

        // Set other fields
        binding.etEstimatedYield.setText(harvest.estimatedYield.toInt().toString())
        binding.etNotes.setText(harvest.notes)
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

    private fun updateHarvest() {
        val cropType = binding.spinnerCropType.selectedItem.toString()
        val variety = binding.spinnerVariety.selectedItem.toString()
        val yieldText = binding.etEstimatedYield.text.toString()
        val notes = binding.etNotes.text.toString()

        if (yieldText.isEmpty()) {
            binding.etEstimatedYield.error = "Required"
            return
        }

        if (plantedDate == null || harvestStartDate == null || harvestEndDate == null) {
            Toast.makeText(this, "Please select planted date and week", Toast.LENGTH_SHORT).show()
            return
        }

        val estimatedYield = yieldText.toDoubleOrNull() ?: 0.0

        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdate.isEnabled = false

        lifecycleScope.launch {
            val updatedHarvest = currentHarvest!!.copy(
                cropType = cropType,
                variety = variety,
                plantedDate = Timestamp(plantedDate!!),
                estimatedYield = estimatedYield,
                harvestStartDate = Timestamp(harvestStartDate!!),
                harvestEndDate = Timestamp(harvestEndDate!!),
                notes = notes
            )

            val result = harvestRepository.updateHarvest(updatedHarvest)
            binding.progressBar.visibility = View.GONE
            binding.btnUpdate.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@EditHarvestActivity, "Harvest updated", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { exception ->
                Toast.makeText(this@EditHarvestActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
