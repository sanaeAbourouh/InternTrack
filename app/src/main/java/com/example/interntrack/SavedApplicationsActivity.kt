package com.example.interntrack

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SavedApplicationsActivity : AppCompatActivity() {

    private lateinit var savedApplicationsContainer: LinearLayout
    private lateinit var tvSavedCount: TextView
    private lateinit var etSearch: EditText
    private lateinit var sharedPref: SharedPreferences

    private var allSavedInternships = mutableListOf<SavedInternship>()
    private val gson = Gson()

    // Local data class for this page only
    data class SavedInternship(
        val id: String,
        val company: String,
        val title: String,
        val location: String,
        val description: String,
        val applyUrl: String,
        var status: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_applications)

        savedApplicationsContainer = findViewById(R.id.savedApplicationsContainer)
        tvSavedCount = findViewById(R.id.tvSavedCount)
        etSearch = findViewById(R.id.etSearchSaved)
        sharedPref = getSharedPreferences("ApplicationsPrefs", MODE_PRIVATE)

        loadSavedInternships()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterInternships(s.toString())
            }
        })
    }

    private fun loadSavedInternships() {
        val savedInternshipsJson = sharedPref.getString("saved_internships", "[]")
        val type = object : TypeToken<MutableList<InternshipDetailsActivity.SavedInternship>>() {}.type
        val originalInternships: MutableList<InternshipDetailsActivity.SavedInternship> = gson.fromJson(savedInternshipsJson, type)

        allSavedInternships.clear()

        for (internship in originalInternships) {
            allSavedInternships.add(
                SavedInternship(
                    id = internship.id,
                    company = internship.company,
                    title = internship.title,
                    location = internship.location,
                    description = internship.description,
                    applyUrl = internship.applyUrl,
                    status = internship.status
                )
            )
        }

        displayInternships(allSavedInternships)
    }

    private fun saveInternships() {
        // Convert back to original format and save
        val originalInternships = allSavedInternships.map {
            com.example.interntrack.InternshipDetailsActivity.SavedInternship(
                id = it.id,
                company = it.company,
                title = it.title,
                location = it.location,
                description = it.description,
                applyUrl = it.applyUrl,
                status = it.status
            )
        }
        val savedInternshipsJson = gson.toJson(originalInternships)
        sharedPref.edit().putString("saved_internships", savedInternshipsJson).apply()
    }

    private fun filterInternships(query: String) {
        if (query.isEmpty()) {
            displayInternships(allSavedInternships)
        } else {
            val filtered = allSavedInternships.filter {
                it.title.lowercase().contains(query.lowercase()) ||
                        it.company.lowercase().contains(query.lowercase())
            }
            displayInternships(filtered)
        }
    }

    private fun displayInternships(internships: List<SavedInternship>) {
        savedApplicationsContainer.removeAllViews()

        tvSavedCount.text = "${internships.size} internship${if (internships.size != 1) "s" else ""} saved"

        if (internships.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "No saved internships yet.\n\nTap the heart icon on any internship to save it."
            emptyText.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            emptyText.textSize = 14f
            emptyText.gravity = android.view.Gravity.CENTER
            emptyText.setPadding(40, 60, 40, 60)
            savedApplicationsContainer.addView(emptyText)
            return
        }

        for (internship in internships) {
            val itemView = layoutInflater.inflate(R.layout.item_saved_application, savedApplicationsContainer, false)

            val tvTitle = itemView.findViewById<TextView>(R.id.tvSavedTitle)
            val tvCompany = itemView.findViewById<TextView>(R.id.tvSavedCompany)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvSavedStatus)
            val btnRemove = itemView.findViewById<TextView>(R.id.btnRemove)

            tvTitle.text = internship.title
            tvCompany.text = internship.company

            // Set status badge color
            val backgroundColor = when (internship.status) {
                "Interested" -> R.drawable.status_badge_purple
                "Applied" -> R.drawable.status_badge_blue
                "Interview" -> R.drawable.status_badge_orange
                "Accepted" -> R.drawable.status_badge_green
                "Rejected" -> R.drawable.status_badge_red
                else -> R.drawable.status_badge_gray
            }
            tvStatus.setBackgroundResource(backgroundColor)
            tvStatus.setTextColor(resources.getColor(android.R.color.white, null))
            tvStatus.text = internship.status

            // Make status clickable to change
            tvStatus.setOnClickListener {
                showStatusDialog(internship)
            }

            // Remove button
            btnRemove.setOnClickListener {
                allSavedInternships.remove(internship)
                saveInternships()
                loadSavedInternships()
                Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show()
            }

            // Make the entire card clickable to open internship details
            itemView.setOnClickListener {
                openInternshipDetails(internship)
            }

            savedApplicationsContainer.addView(itemView)
        }
    }

    private fun openInternshipDetails(internship: SavedInternship) {
        val intent = Intent(this, InternshipDetailsActivity::class.java)
        intent.putExtra("INTERNSHIP_ID", internship.id)
        intent.putExtra("COMPANY", internship.company)
        intent.putExtra("TITLE", internship.title)
        intent.putExtra("LOCATION", internship.location)
        intent.putExtra("DESCRIPTION", internship.description)
        intent.putExtra("APPLY_URL", internship.applyUrl)
        startActivity(intent)
    }

    private fun showStatusDialog(internship: SavedInternship) {
        val statuses = arrayOf("Interested", "Applied", "Interview", "Accepted", "Rejected")

        AlertDialog.Builder(this)
            .setTitle("Update Application Status")
            .setItems(statuses) { _, which ->
                val newStatus = statuses[which]
                internship.status = newStatus
                saveInternships()
                displayInternships(allSavedInternships)
                Toast.makeText(this, "Status updated to: $newStatus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}