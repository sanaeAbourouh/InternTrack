package com.example.interntrack

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class InternshipDetailsActivity : AppCompatActivity() {

    private lateinit var tvCompany: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDescription: TextView
    private lateinit var radioGroupStatus: RadioGroup
    private lateinit var btnSaveStatus: Button
    private lateinit var sharedPref: SharedPreferences

    private var currentInternshipId = ""
    private var currentCompany = ""
    private var currentTitle = ""
    private var currentLocation = ""
    private var currentDescription = ""
    private var currentApplyUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internship_details)

        tvCompany = findViewById(R.id.tvCompany)
        tvTitle = findViewById(R.id.tvTitle)
        tvLocation = findViewById(R.id.tvLocation)
        tvDescription = findViewById(R.id.tvDescription)
        radioGroupStatus = findViewById(R.id.radioGroupStatus)
        btnSaveStatus = findViewById(R.id.btnSaveStatus)

        // Get data from intent
        currentInternshipId = intent.getStringExtra("INTERNSHIP_ID") ?: System.currentTimeMillis().toString()
        currentCompany = intent.getStringExtra("COMPANY") ?: "Unknown Company"
        currentTitle = intent.getStringExtra("TITLE") ?: "Internship Position"
        currentLocation = intent.getStringExtra("LOCATION") ?: "Location not specified"
        currentDescription = intent.getStringExtra("DESCRIPTION") ?: "No description available"
        currentApplyUrl = intent.getStringExtra("APPLY_URL") ?: ""

        // Set data to views
        tvCompany.text = currentCompany
        tvTitle.text = currentTitle
        tvLocation.text = "📍 $currentLocation"
        tvDescription.text = currentDescription

        sharedPref = getSharedPreferences("ApplicationsPrefs", MODE_PRIVATE)

        // Load saved status if exists
        loadSavedStatus()

        btnSaveStatus.setOnClickListener {
            saveApplicationStatus()
        }
    }

    private fun loadSavedStatus() {
        val savedInternshipsJson = sharedPref.getString("saved_internships", "[]")
        val type = object : TypeToken<MutableList<SavedInternship>>() {}.type
        val savedInternships: MutableList<SavedInternship> = Gson().fromJson(savedInternshipsJson, type)

        val existing = savedInternships.find { it.id == currentInternshipId }
        if (existing != null) {
            when (existing.status) {
                "Interested" -> radioGroupStatus.check(R.id.radioInterested)
                "Applied" -> radioGroupStatus.check(R.id.radioApplied)
                "Interview" -> radioGroupStatus.check(R.id.radioInterview)
                "Rejected" -> radioGroupStatus.check(R.id.radioRejected)
                "Accepted" -> radioGroupStatus.check(R.id.radioAccepted)
            }
        }
    }

    private fun saveApplicationStatus() {
        val selectedId = radioGroupStatus.checkedRadioButtonId
        val status = when (selectedId) {
            R.id.radioInterested -> "Interested"
            R.id.radioApplied -> "Applied"
            R.id.radioInterview -> "Interview"
            R.id.radioRejected -> "Rejected"
            R.id.radioAccepted -> "Accepted"
            else -> {
                Toast.makeText(this, "Please select a status", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val savedInternshipsJson = sharedPref.getString("saved_internships", "[]")
        val type = object : TypeToken<MutableList<SavedInternship>>() {}.type
        val savedInternships: MutableList<SavedInternship> = Gson().fromJson(savedInternshipsJson, type)

        val existingIndex = savedInternships.indexOfFirst { it.id == currentInternshipId }
        val newInternship = SavedInternship(
            id = currentInternshipId,
            company = currentCompany,
            title = currentTitle,
            location = currentLocation,
            description = currentDescription,
            applyUrl = currentApplyUrl,
            status = status
        )

        if (existingIndex != -1) {
            savedInternships[existingIndex] = newInternship
        } else {
            savedInternships.add(newInternship)
        }

        val updatedJson = Gson().toJson(savedInternships)
        sharedPref.edit().putString("saved_internships", updatedJson).apply()

        Toast.makeText(this, "Status saved: $status", Toast.LENGTH_SHORT).show()
        finish()
    }

    data class SavedInternship(
        val id: String,
        val company: String,
        val title: String,
        val location: String,
        val description: String,
        val applyUrl: String,
        val status: String
    )
}