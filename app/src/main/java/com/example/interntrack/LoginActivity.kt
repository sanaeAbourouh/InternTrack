package com.example.interntrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)


        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedEmail = sharedPref.getString("userEmail", "")
            val savedPassword = sharedPref.getString("userPassword", "")
            val savedName = sharedPref.getString("userName", "")

            // Also check for other possible keys with safe handling
            val savedNameAlt = sharedPref.getString("user_name", "")
            val savedNameAlt2 = sharedPref.getString("name", "")
            val savedNameAlt3 = sharedPref.getString("fullName", "")

            // Determine the final name safely
            val finalName = when {
                !savedName.isNullOrEmpty() -> savedName
                !savedNameAlt.isNullOrEmpty() -> savedNameAlt
                !savedNameAlt2.isNullOrEmpty() -> savedNameAlt2
                !savedNameAlt3.isNullOrEmpty() -> savedNameAlt3
                else -> "User"
            }

            if (email == savedEmail && password == savedPassword) {
                sharedPref.edit().putBoolean("isLoggedIn", true).apply()

                Toast.makeText(this, "Welcome back, $finalName!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Contact support to reset your password", Toast.LENGTH_SHORT).show()
        }
    }
}