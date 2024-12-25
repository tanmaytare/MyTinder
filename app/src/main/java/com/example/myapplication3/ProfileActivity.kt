package com.example.myapplication3

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.util.Log

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Retrieve the user name from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("user_name", "User") ?: "User" // Default to "User" if name is null

        // Find the TextView by ID and set the name
        val nameTextView = findViewById<TextView>(R.id.profile_name)
        nameTextView.text = "Welcome, $userName!" // Display the saved name

        // Initialize Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_profile // Set Profile as selected item

        // Handle Bottom Navigation item selection
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("ProfileActivity", "Home selected")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Close current activity
                    true
                }
                R.id.nav_favorite -> {
                    Log.d("ProfileActivity", "Favorite selected")
                    // Add your favorite activity here if you have one
                    true
                }
                R.id.nav_profile -> {
                    Log.d("ProfileActivity", "Profile selected")
                    true
                }
                else -> false
            }
        }
    }
}