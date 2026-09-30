package com.example.interntrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var tvTogglePassword: TextView
    private lateinit var tvToggleConfirmPassword: TextView
    private lateinit var sharedPref: SharedPreferences

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        tvTogglePassword = findViewById(R.id.tvTogglePassword)
        tvToggleConfirmPassword = findViewById(R.id.tvToggleConfirmPassword)

        sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Show/Hide Password Toggle
        tvTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvTogglePassword.text = "Show"
                isPasswordVisible = false
            } else {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvTogglePassword.text = "Hide"
                isPasswordVisible = true
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Show/Hide Confirm Password Toggle
        tvToggleConfirmPassword.setOnClickListener {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvToggleConfirmPassword.text = "Show"
                isConfirmPasswordVisible = false
            } else {
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvToggleConfirmPassword.text = "Hide"
                isConfirmPasswordVisible = true
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            when {
                fullName.isEmpty() -> Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                email.isEmpty() -> Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                password.isEmpty() -> Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                password != confirmPassword -> Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                password.length < 4 -> Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                else -> {
                    // Save user data with multiple keys to ensure it's saved
                    sharedPref.edit().putString("userName", fullName).apply()
                    sharedPref.edit().putString("user_name", fullName).apply()
                    sharedPref.edit().putString("name", fullName).apply()
                    sharedPref.edit().putString("fullName", fullName).apply()
                    sharedPref.edit().putString("userEmail", email).apply()
                    sharedPref.edit().putString("userPassword", password).apply()

                    Toast.makeText(this, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}