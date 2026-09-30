package com.example.interntrack

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RemindersFragment : Fragment() {

    private lateinit var remindersContainer: LinearLayout
    private lateinit var fabAddReminder: FloatingActionButton
    private lateinit var sharedPref: SharedPreferences

    private val reminders = mutableListOf<Reminder>()
    private val gson = Gson()

    data class Reminder(
        val id: String,
        var title: String,
        var date: String,
        var note: String,
        var isCompleted: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reminders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        remindersContainer = view.findViewById(R.id.remindersContainer)
        fabAddReminder = view.findViewById(R.id.fabAddReminder)

        sharedPref = requireActivity().getSharedPreferences("RemindersPrefs", AppCompatActivity.MODE_PRIVATE)

        loadReminders()
        displayReminders()

        fabAddReminder.setOnClickListener {
            showAddReminderDialog(null)
        }
    }

    private fun loadReminders() {
        reminders.clear()
        val remindersJson = sharedPref.getString("reminders", "[]")
        val type = object : TypeToken<MutableList<Reminder>>() {}.type
        val savedReminders: MutableList<Reminder> = gson.fromJson(remindersJson, type)
        reminders.addAll(savedReminders)
    }

    private fun saveReminders() {
        val remindersJson = gson.toJson(reminders)
        sharedPref.edit().putString("reminders", remindersJson).apply()
    }

    private fun calculateCountdown(reminderDate: String): String {
        return try {
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val targetDate = dateFormat.parse(reminderDate)

            if (targetDate == null) return "Invalid date"

            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val target = Calendar.getInstance()
            target.time = targetDate
            target.set(Calendar.HOUR_OF_DAY, 0)
            target.set(Calendar.MINUTE, 0)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)

            val diffInMillis = target.timeInMillis - today.timeInMillis
            val diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)

            when {
                diffInDays < 0 -> "⚠️ Overdue"
                diffInDays == 0L -> "🔴 Today"
                diffInDays == 1L -> "🟡 Tomorrow"
                diffInDays <= 7 -> "🟢 $diffInDays days remaining"
                diffInDays <= 14 -> "📅 1 week remaining"
                diffInDays <= 30 -> "📅 ${diffInDays / 7} weeks remaining"
                else -> "📅 ${diffInDays} days remaining"
            }
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    private fun getCountdownColor(countdownText: String): Int {
        return when {
            countdownText.contains("Overdue") -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            countdownText.contains("Today") -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            countdownText.contains("Tomorrow") -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            countdownText.contains("days remaining") && countdownText.contains("1") -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        }
    }

    private fun displayReminders() {
        remindersContainer.removeAllViews()

        if (reminders.isEmpty()) {
            val emptyText = TextView(requireContext())
            emptyText.text = "No reminders yet.\n\nTap the + button to create your first reminder."
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            emptyText.textSize = 14f
            emptyText.gravity = android.view.Gravity.CENTER
            emptyText.setPadding(40, 60, 40, 60)
            remindersContainer.addView(emptyText)
            return
        }

        for (reminder in reminders) {
            val reminderView = layoutInflater.inflate(R.layout.item_reminder, remindersContainer, false)

            val tvTitle = reminderView.findViewById<TextView>(R.id.tvReminderTitle)
            val tvNote = reminderView.findViewById<TextView>(R.id.tvReminderNote)
            val tvDate = reminderView.findViewById<TextView>(R.id.tvReminderDate)
            val tvCountdown = reminderView.findViewById<TextView>(R.id.tvReminderCountdown)
            val tvStatus = reminderView.findViewById<TextView>(R.id.tvReminderStatus)
            val btnRemove = reminderView.findViewById<TextView>(R.id.btnRemoveReminder)

            tvTitle.text = reminder.title
            tvDate.text = reminder.date

            // Calculate and display countdown
            val countdownText = calculateCountdown(reminder.date)
            tvCountdown.text = countdownText
            tvCountdown.setTextColor(getCountdownColor(countdownText))

            if (reminder.note.isNotEmpty()) {
                tvNote.text = reminder.note
                tvNote.visibility = View.VISIBLE
            } else {
                tvNote.visibility = View.GONE
            }

            if (reminder.isCompleted) {
                tvStatus.text = "Completed"
                tvStatus.setBackgroundResource(R.drawable.status_badge_completed)
            } else {
                tvStatus.text = "Pending"
                tvStatus.setBackgroundResource(R.drawable.status_badge_pending)
            }

            tvStatus.setOnClickListener {
                reminder.isCompleted = !reminder.isCompleted
                saveReminders()
                displayReminders()
                val status = if (reminder.isCompleted) "completed" else "pending"
                Toast.makeText(requireContext(), "Reminder marked as $status", Toast.LENGTH_SHORT).show()
            }

            btnRemove.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to delete this reminder?")
                    .setPositiveButton("Delete") { _, _ ->
                        reminders.remove(reminder)
                        saveReminders()
                        displayReminders()
                        Toast.makeText(requireContext(), "Reminder deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            remindersContainer.addView(reminderView)
        }
    }

    private fun showAddReminderDialog(existingReminder: Reminder?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etReminderTitle)
        val tvReminderDate = dialogView.findViewById<TextView>(R.id.tvReminderDate)
        val llDatePicker = dialogView.findViewById<LinearLayout>(R.id.llDatePicker)
        val etNote = dialogView.findViewById<EditText>(R.id.etReminderNote)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        // Set default date (today)
        if (existingReminder != null) {
            etTitle.setText(existingReminder.title)
            tvReminderDate.text = existingReminder.date
            etNote.setText(existingReminder.note)
        } else {
            tvReminderDate.text = dateFormat.format(calendar.time)
        }

        // DatePicker click listener
        llDatePicker.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                tvReminderDate.text = dateFormat.format(selectedDate.time)
            }, year, month, day).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnAdd.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val date = tvReminderDate.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date.isEmpty() || date == "Select Date") {
                Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (existingReminder != null) {
                existingReminder.title = title
                existingReminder.date = date
                existingReminder.note = note
            } else {
                val newReminder = Reminder(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    date = date,
                    note = note,
                    isCompleted = false
                )
                reminders.add(newReminder)
            }
            saveReminders()
            displayReminders()
            dialog.dismiss()
            Toast.makeText(requireContext(), if (existingReminder != null) "Reminder updated" else "Reminder added", Toast.LENGTH_SHORT).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}