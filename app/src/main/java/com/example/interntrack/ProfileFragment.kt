package com.example.interntrack

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProfileFragment : Fragment() {

    private lateinit var tvAvatar: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserUniversity: TextView
    private lateinit var tvStatsSaved: TextView
    private lateinit var tvStatsApplied: TextView
    private lateinit var tvStatsInterview: TextView
    private lateinit var tvStatsAccepted: TextView
    private lateinit var tvResumeStatus: TextView
    private lateinit var btnUploadResume: Button
    private lateinit var btnViewResume: Button
    private lateinit var btnReplaceResume: Button
    private lateinit var btnLogout: Button

    private lateinit var userPrefs: SharedPreferences  // For user data (name, email, etc.)
    private lateinit var appsPrefs: SharedPreferences   // For internship data
    private var hasResume: Boolean = false

    private val pickResumeLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            hasResume = true
            userPrefs.edit().putBoolean("has_resume", true).apply()
            userPrefs.edit().putString("resume_uri", uri.toString()).apply()
            tvResumeStatus.text = "Resume uploaded"
            Toast.makeText(requireContext(), "Resume uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAvatar = view.findViewById(R.id.tvAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvUserUniversity = view.findViewById(R.id.tvUserUniversity)
        tvStatsSaved = view.findViewById(R.id.tvStatsSaved)
        tvStatsApplied = view.findViewById(R.id.tvStatsApplied)
        tvStatsInterview = view.findViewById(R.id.tvStatsInterview)
        tvStatsAccepted = view.findViewById(R.id.tvStatsAccepted)
        tvResumeStatus = view.findViewById(R.id.tvResumeStatus)
        btnUploadResume = view.findViewById(R.id.btnUploadResume)
        btnViewResume = view.findViewById(R.id.btnViewResume)
        btnReplaceResume = view.findViewById(R.id.btnReplaceResume)
        btnLogout = view.findViewById(R.id.btnLogout)

        val settingEditProfile = view.findViewById<TextView>(R.id.settingEditProfile)
        val settingChangePassword = view.findViewById<TextView>(R.id.settingChangePassword)
        val settingPrivacy = view.findViewById<TextView>(R.id.settingPrivacy)
        val settingAbout = view.findViewById<TextView>(R.id.settingAbout)

        // Use two different SharedPreferences files
        userPrefs = requireActivity().getSharedPreferences("UserPrefs", AppCompatActivity.MODE_PRIVATE)
        appsPrefs = requireActivity().getSharedPreferences("ApplicationsPrefs", AppCompatActivity.MODE_PRIVATE)

        loadUserData()
        loadStatistics()
        loadResume()

        btnUploadResume.setOnClickListener {
            pickResumeLauncher.launch("application/pdf")
        }

        btnViewResume.setOnClickListener {
            if (!hasResume) {
                Toast.makeText(requireContext(), "No resume uploaded. Please upload a resume first.", Toast.LENGTH_LONG).show()
            } else {
                val uriString = userPrefs.getString("resume_uri", null)
                if (uriString != null) {
                    try {
                        val uri = Uri.parse(uriString)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        startActivity(Intent.createChooser(intent, "Open Resume"))
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Cannot open resume file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnReplaceResume.setOnClickListener {
            if (!hasResume) {
                Toast.makeText(requireContext(), "No resume uploaded. Please upload a resume first.", Toast.LENGTH_LONG).show()
            } else {
                pickResumeLauncher.launch("application/pdf")
            }
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    userPrefs.edit().clear().apply()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Settings click listeners
        settingEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        settingChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        settingPrivacy.setOnClickListener {
            showPrivacyPolicyDialog()
        }

        settingAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    // Refresh statistics when fragment becomes visible again
    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    private fun loadUserData() {
        val userName = userPrefs.getString("userName", "User") ?: "User"
        val userEmail = userPrefs.getString("userEmail", "user@example.com") ?: "user@example.com"
        val userUniversity = userPrefs.getString("userUniversity", "Add your university") ?: "Add your university"
        val userMajor = userPrefs.getString("userMajor", "") ?: ""

        tvUserName.text = userName
        tvUserEmail.text = userEmail

        val universityText = if (userMajor.isNotEmpty()) {
            "$userUniversity - $userMajor"
        } else {
            userUniversity
        }
        tvUserUniversity.text = universityText

        val firstLetter = if (userName.isNotEmpty()) userName[0].uppercase() else "U"
        tvAvatar.text = firstLetter
        tvAvatar.setBackgroundResource(R.drawable.avatar_background)
    }

    private fun loadStatistics() {
        // READ FROM THE CORRECT SharedPreferences - "ApplicationsPrefs"
        val savedInternshipsJson = appsPrefs.getString("saved_internships", "[]")
        val type = object : TypeToken<MutableList<InternshipDetailsActivity.SavedInternship>>() {}.type
        val savedInternships: MutableList<InternshipDetailsActivity.SavedInternship> = Gson().fromJson(savedInternshipsJson, type)

        var saved = savedInternships.size
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

        tvStatsSaved.text = saved.toString()
        tvStatsApplied.text = applied.toString()
        tvStatsInterview.text = interview.toString()
        tvStatsAccepted.text = accepted.toString()
    }

    private fun loadResume() {
        hasResume = userPrefs.getBoolean("has_resume", false)
        if (hasResume) {
            tvResumeStatus.text = "Resume uploaded"
        } else {
            tvResumeStatus.text = "No resume uploaded"
        }
        btnViewResume.isEnabled = true
        btnReplaceResume.isEnabled = true
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etUniversity = dialogView.findViewById<EditText>(R.id.etEditUniversity)
        val etMajor = dialogView.findViewById<EditText>(R.id.etEditMajor)

        etName.setText(userPrefs.getString("userName", ""))
        etUniversity.setText(userPrefs.getString("userUniversity", ""))
        etMajor.setText(userPrefs.getString("userMajor", ""))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                val newUniversity = etUniversity.text.toString().trim()
                val newMajor = etMajor.text.toString().trim()

                if (newName.isNotEmpty()) {
                    userPrefs.edit().putString("userName", newName).apply()
                }
                if (newUniversity.isNotEmpty()) {
                    userPrefs.edit().putString("userUniversity", newUniversity).apply()
                }
                userPrefs.edit().putString("userMajor", newMajor).apply()

                loadUserData()
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = dialogView.findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmNewPassword.text.toString().trim()

            val savedPassword = userPrefs.getString("userPassword", "")

            if (currentPassword != savedPassword) {
                Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 4) {
                Toast.makeText(requireContext(), "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userPrefs.edit().putString("userPassword", newPassword).apply()
            Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPrivacyPolicyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("📄 Privacy Policy")
            .setMessage(
                "• User information is stored locally on the device.\n\n" +
                        "• Resume files are only used within the application.\n\n" +
                        "• The application does not sell or share personal data.\n\n" +
                        "• Internet access is used only to retrieve internship-related information through APIs.\n\n" +
                        "• Users are responsible for the information they upload."
            )
            .setPositiveButton("Close", null)
            .create()
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About InternTrack")
            .setMessage("InternTrack is your personal internship management companion.\n\nVersion 1.0\n\nTrack applications, manage interviews, and stay organized throughout your internship journey.")
            .setPositiveButton("OK", null)
            .create()
            .show()
    }
}