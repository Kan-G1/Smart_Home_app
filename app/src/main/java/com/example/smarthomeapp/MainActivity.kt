package com.example.smarthomeapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private val TAG = "MainActivity"

    private val fineLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            checkAndRequestNotificationPermission()
        } else {
            showPermissionRationale("Fine Location", "Geofencing requires precise location to function.")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications are disabled. You can enable them in settings.", Toast.LENGTH_LONG).show()
        }
        // Proceed regardless of whether notification permission is granted
        checkAndRequestBackgroundLocation()
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            checkDeviceLocationSettingsAndAddGeofence()
        } else {
            showPermissionRationale("Background Location", "Geofencing requires background location access to work when the app is not open.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_devices -> DevicesFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> DashboardFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment).commit()
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }

    override fun onResume() {
        super.onResume()
        checkPlayServicesAndProceed()
    }

    private fun checkPlayServicesAndProceed() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            googleApiAvailability.getErrorDialog(this, resultCode, 0)?.show()
        } else {
            checkAndRequestPermissions()
        }
    }

    fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkAndRequestNotificationPermission()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAndRequestBackgroundLocation()
            }
        } else {
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                checkDeviceLocationSettingsAndAddGeofence()
            }
        } else {
            checkDeviceLocationSettingsAndAddGeofence()
        }
    }

    fun checkDeviceLocationSettingsAndAddGeofence() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this)
                .setTitle("Location Services Disabled")
                .setMessage("Geofencing requires Location Services to be enabled. Please turn on location services in your device settings.")
                .setPositiveButton("Go to Settings") { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton("Cancel", null)
                .show()
        } else if (arePermissionsGranted()) {
            addGeofence()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else { true }
        return fineLocation && backgroundLocation
    }
    
    private fun showPermissionRationale(permissionName: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle("$permissionName Required")
            .setMessage(message)
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        if (!arePermissionsGranted()) { // Redundant check, but safe
            Log.d(TAG, "Permissions not fully granted. Skipping addGeofence.")
            return
        }

        val prefs = getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 0f).toDouble()
        val lon = prefs.getFloat("lon", 0f).toDouble()
        val radius = prefs.getInt("radius", 0).toFloat()

        if (lat == 0.0 || lon == 0.0 || radius == 0f) {
            Log.d(TAG, "No geofence set in SharedPreferences. Skipping.")
            return
        }

        val geofence = geofenceHelper.getGeofence("HOME", lat, lon, radius)
        val geofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent = geofenceHelper.getPendingIntent()

        geofencingClient.removeGeofences(listOf("HOME")).run {
            addOnCompleteListener { 
                geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener { 
                        Toast.makeText(this@MainActivity, "Geofence added successfully!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Geofence added successfully.") 
                    }
                    .addOnFailureListener { e -> 
                        Toast.makeText(this@MainActivity, "Failed to add geofence: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Failed to add geofence.", e) 
                    }
            }
        }
    }
}