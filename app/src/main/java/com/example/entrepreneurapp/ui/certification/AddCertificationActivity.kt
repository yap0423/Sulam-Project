package com.example.entrepreneurapp.ui.certification

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityAddCertificationBinding
import com.example.entrepreneurapp.models.Certification
import com.example.entrepreneurapp.repository.CertificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddCertificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCertificationBinding
    private val certificationRepository = CertificationRepository()

    private val certificationTypes = listOf(
        "MyGAP - Malaysian Good Agricultural Practice",
        "Organic Certification",
        "HACCP - Hazard Analysis Critical Control Point",
        "Halal Certification",
        "ISO 22000 - Food Safety Management",
        "GlobalGAP - International Standard"
    )

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var issuedDateMillis: Long = 0L
    private var expiryDateMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCertificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCertificationTypeDropdown()
        setupDatePickers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCertificationTypeDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, certificationTypes)
        binding.etCertificationType.setAdapter(adapter)
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        // Issued Date Picker
        binding.etIssuedDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    issuedDateMillis = calendar.timeInMillis
                    binding.etIssuedDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Expiry Date Picker
        binding.etExpiryDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    expiryDateMillis = calendar.timeInMillis
                    binding.etExpiryDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            // Set minimum date to today
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveCertification()
        }
    }

    private fun saveCertification() {
        // Clear previous errors
        binding.tilCertificationType.error = null
        binding.tilCertificateNumber.error = null
        binding.tilIssuingBody.error = null
        binding.tilIssuedDate.error = null
        binding.tilExpiryDate.error = null

        // Get values
        val certificationType = binding.etCertificationType.text.toString().trim()
        val certificateNumber = binding.etCertificateNumber.text.toString().trim()
        val issuingBody = binding.etIssuingBody.text.toString().trim()
        val issuedDate = binding.etIssuedDate.text.toString().trim()
        val expiryDate = binding.etExpiryDate.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        // Validate
        if (certificationType.isEmpty()) {
            binding.tilCertificationType.error = "Certification type is required"
            binding.etCertificationType.requestFocus()
            return
        }

        if (certificateNumber.isEmpty()) {
            binding.tilCertificateNumber.error = "Certificate number is required"
            binding.etCertificateNumber.requestFocus()
            return
        }

        if (issuingBody.isEmpty()) {
            binding.tilIssuingBody.error = "Issuing body is required"
            binding.etIssuingBody.requestFocus()
            return
        }

        if (issuedDate.isEmpty()) {
            binding.tilIssuedDate.error = "Issued date is required"
            binding.etIssuedDate.requestFocus()
            return
        }

        if (expiryDate.isEmpty()) {
            binding.tilExpiryDate.error = "Expiry date is required"
            binding.etExpiryDate.requestFocus()
            return
        }

        // Validate dates
        if (expiryDateMillis <= issuedDateMillis) {
            binding.tilExpiryDate.error = "Expiry date must be after issued date"
            return
        }

        // Create certification object
        val certification = Certification(
            type = certificationType,
            certificateNumber = certificateNumber,
            issuingBody = issuingBody,
            issuedDate = issuedDateMillis,
            expiryDate = expiryDateMillis,
            notes = notes
        )

        // Save to Firebase
        setLoading(true)

        lifecycleScope.launch {
            val result = certificationRepository.addCertification(certification)

            setLoading(false)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@AddCertificationActivity,
                        "Certification added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@AddCertificationActivity,
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
        binding.etCertificationType.isEnabled = !isLoading
        binding.etCertificateNumber.isEnabled = !isLoading
        binding.etIssuingBody.isEnabled = !isLoading
        binding.etIssuedDate.isEnabled = !isLoading
        binding.etExpiryDate.isEnabled = !isLoading
        binding.etNotes.isEnabled = !isLoading
    }
}