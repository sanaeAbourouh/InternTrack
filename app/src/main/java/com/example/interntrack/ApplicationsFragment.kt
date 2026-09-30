package com.example.interntrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ApplicationsFragment : Fragment() {

    private lateinit var tvAppliedCount: TextView
    private lateinit var tvInterviewCount: TextView
    private lateinit var tvAcceptedCount: TextView
    private lateinit var tvRejectedCount: TextView
    private lateinit var savedApplicationsContainer: LinearLayout
    private lateinit var btnViewAll: Button
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_applications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAppliedCount = view.findViewById(R.id.tvAppliedCount)
        tvInterviewCount = view.findViewById(R.id.tvInterviewCount)
        tvAcceptedCount = view.findViewById(R.id.tvAcceptedCount)
        tvRejectedCount = view.findViewById(R.id.tvRejectedCount)
        savedApplicationsContainer = view.findViewById(R.id.savedApplicationsContainer)
        btnViewAll = view.findViewById(R.id.btnViewAll)

        sharedPref = requireActivity().getSharedPreferences("ApplicationsPrefs", AppCompatActivity.MODE_PRIVATE)

        loadStatistics()
        loadSavedApplications()

        btnViewAll.setOnClickListener {
            val intent = Intent(requireContext(), SavedApplicationsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadStatistics() {
        val savedInternshipsJson = sharedPref.getString("saved_internships", "[]")
        val type = object : TypeToken<MutableList<InternshipDetailsActivity.SavedInternship>>() {}.type
        val savedInternships: MutableList<InternshipDetailsActivity.SavedInternship> = Gson().fromJson(savedInternshipsJson, type)

        var applied = 0
        var interview = 0
        var accepted = 0
        var rejected = 0

        for (internship in savedInternships) {
            when (internship.status) {
                "Applied" -> applied++
                "Interview" -> interview++
                "Accepted" -> accepted++
                "Rejected" -> rejected++
            }
        }

        tvAppliedCount.text = applied.toString()
        tvInterviewCount.text = interview.toString()
        tvAcceptedCount.text = accepted.toString()
        tvRejectedCount.text = rejected.toString()
    }

    private fun saveInternships(internships: MutableList<InternshipDetailsActivity.SavedInternship>) {
        val internshipsJson = Gson().toJson(internships)
        sharedPref.edit().putString("saved_internships", internshipsJson).apply()
    }

    private fun loadSavedApplications() {
        savedApplicationsContainer.removeAllViews()

        val savedInternshipsJson = sharedPref.getString("saved_internships", "[]")
        val type = object : TypeToken<MutableList<InternshipDetailsActivity.SavedInternship>>() {}.type
        val savedInternships: MutableList<InternshipDetailsActivity.SavedInternship> = Gson().fromJson(savedInternshipsJson, type)

        if (savedInternships.isEmpty()) {
            val emptyText = TextView(requireContext())
            emptyText.text = "No saved internships yet."
            emptyText.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            emptyText.textSize = 14f
            savedApplicationsContainer.addView(emptyText)
            btnViewAll.visibility = View.GONE
        } else {
            // Show only first 2 saved internships
            val internshipsToShow = if (savedInternships.size > 2) {
                savedInternships.subList(0, 2)
            } else {
                savedInternships
            }

            for (internship in internshipsToShow) {
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
                tvStatus.text = internship.status

                // Remove button functionality
                btnRemove.setOnClickListener {
                    // Remove from the full list
                    val currentList = Gson().fromJson<MutableList<InternshipDetailsActivity.SavedInternship>>(
                        sharedPref.getString("saved_internships", "[]"),
                        type
                    )
                    currentList.removeAll { it.id == internship.id }
                    saveInternships(currentList)

                    // Refresh the UI
                    loadStatistics()
                    loadSavedApplications()

                    Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show()
                }

                savedApplicationsContainer.addView(itemView)
            }

            // Only show "View All" button if there are more than 2 saved internships
            btnViewAll.visibility = if (savedInternships.size > 2) View.VISIBLE else View.GONE
        }
    }
}