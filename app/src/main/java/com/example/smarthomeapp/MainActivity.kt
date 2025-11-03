package com.example.smarthomeapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        //  bottom navigation icons
        bottomNav.setOnItemSelectedListener { item ->
            // Determine which fragment to show based on the item tapped
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                else -> DashboardFragment() // A safe default
            }
            // Load the selected fragment.
            loadFragment(selectedFragment)
            true // Return true to indicate the selection was handled
        }
    }

    // helper function to make switching fragments smoother
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
