package com.example.smarthomeapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivityNavigation"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Log.d(TAG, "User is not logged in. Redirecting to AuthActivity.")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        Log.d(TAG, "Activity content view set.")

        startService(Intent(this, SensorSimulationService::class.java))

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        Log.d(TAG, "BottomNavigationView found.")

        // This listener contains the logic to switch screens
        bottomNav.setOnItemSelectedListener { item ->
            Log.d(TAG, "Bottom nav item selected: ${item.title}")

            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_devices -> {
                    Log.d(TAG, "Switching to DevicesFragment.")
                    DevicesFragment()
                }
                R.id.nav_profile -> {
                    Log.d(TAG, "Switching to ProfileFragment.")
                    ProfileFragment()
                }
                else -> {
                    Log.d(TAG, "Switching to DashboardFragment.")
                    DashboardFragment()
                }
            }

            // This transaction replaces the content of the FrameLayout
            Log.d(TAG, "Replacing fragment in container.")
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()
            
            true // Return true to show the item as selected
        }

        // Set the default screen when the app first loads
        if (savedInstanceState == null) {
            Log.d(TAG, "Setting default fragment to Dashboard.")
            bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, SensorSimulationService::class.java))
    }
}
