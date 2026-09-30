package com.example.interntrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPref: SharedPreferences

    // Keep references to fragments
    private lateinit var homeFragment: HomeFragment
    private lateinit var applicationsFragment: ApplicationsFragment
    private lateinit var remindersFragment: RemindersFragment
    private lateinit var profileFragment: ProfileFragment

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Check if user is logged in
        if (!sharedPref.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize fragments once
        homeFragment = HomeFragment()
        applicationsFragment = ApplicationsFragment()
        remindersFragment = RemindersFragment()
        profileFragment = ProfileFragment()

        // Load Home fragment by default
        loadFragment(homeFragment)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(homeFragment)
                    true
                }
                R.id.nav_applications -> {
                    loadFragment(applicationsFragment)
                    true
                }
                R.id.nav_reminders -> {
                    loadFragment(remindersFragment)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        if (currentFragment != fragment) {
            val transaction = supportFragmentManager.beginTransaction()
            if (currentFragment != null) {
                transaction.hide(currentFragment!!)
            }
            if (!fragment.isAdded) {
                transaction.add(R.id.container, fragment, fragment.javaClass.simpleName)
            } else {
                transaction.show(fragment)
            }
            transaction.commit()
            currentFragment = fragment
        }
    }
}