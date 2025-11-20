package com.example.smarthomeapp

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log2
import kotlin.random.Random

class DashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val dbManager = DatabaseManager()
    private lateinit var deviceAdapter: DashboardDeviceAdapter
    private var deviceList = listOf<Device>()

    private lateinit var powerUsageTextView: TextView
    private lateinit var powerProgressBar: ProgressBar
    private lateinit var staticMapImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Strong suggestion change #1: Ensure Maps SDK is initialized before any map-related operations
        try {
            MapsInitializer.initialize(requireContext())
        } catch (e: Exception) {
            Log.e("DashboardFragment", "MapsInitializer failed", e)
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        powerUsageTextView = view.findViewById(R.id.tv_power_usage)
        powerProgressBar = view.findViewById(R.id.power_progress_bar)
        staticMapImageView = view.findViewById(R.id.iv_static_map)

        val profileCard = view.findViewById<CardView>(R.id.profile_card)
        profileCard.setOnClickListener { navigateTo(R.id.nav_profile) }

        val gridIcon = view.findViewById<ImageView>(R.id.grid_icon)
        gridIcon.setOnClickListener { showNavigationDialog() }

        setupRecyclerView(view)
        loadStaticMap()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userDocRef = db.collection("users").document(currentUser.uid)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val user = document.toObject(User::class.java)
                        view.findViewById<TextView>(R.id.tvUsername).text =
                            "Hello, ${user?.displayName ?: user?.email}"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardFragment", "Error fetching user profile", e)
                }

            dbManager.getDevicesForUser(currentUser.uid, { snapshot ->
                if (snapshot != null) {
                    deviceList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Device::class.java)?.also {
                            it.id = doc.id
                            if (it.power == 0) {
                                it.power = when (it.type) {
                                    "Light" -> Random.nextInt(40, 100)
                                    "Camera" -> Random.nextInt(100, 200)
                                    else -> Random.nextInt(20, 50)
                                }
                                dbManager.updateDevicePower(it.id, it.power)
                            }
                        }
                    }
                    deviceAdapter.submitList(deviceList)
                    updatePowerUsage()
                }
            }, { e ->
                Log.e("DashboardFragment", "Error fetching devices", e)
            })
        }

        view.findViewById<TextView>(R.id.tvGreeting).text =
            SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        val startSimButton = view.findViewById<Button>(R.id.btn_start_simulation)
        val stopSimButton = view.findViewById<Button>(R.id.btn_stop_simulation)

        startSimButton.setOnClickListener {
            val intent = Intent(activity, SensorSimulationService::class.java)
            activity?.startService(intent)
        }

        stopSimButton.setOnClickListener {
            val intent = Intent(activity, SensorSimulationService::class.java)
            activity?.stopService(intent)
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.devices_recycler_view)
        deviceAdapter = DashboardDeviceAdapter { device, isChecked ->
            dbManager.updateDeviceState(device.id, isChecked)
            deviceList.find { it.id == device.id }?.state = isChecked
            updatePowerUsage()
        }
        recyclerView.adapter = deviceAdapter
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun updatePowerUsage() {
        val totalPower = deviceList.filter { it.state }.sumOf { it.power }
        val maxPower = deviceList.sumOf { it.power }.coerceAtLeast(1)

        powerUsageTextView.text = "Total Power: $totalPower W"
        powerProgressBar.max = maxPower
        powerProgressBar.progress = totalPower
    }

    private fun loadStaticMap() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs = activity?.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
                    ?: return@launch
                val lat = prefs.getFloat("lat", 0f).toDouble()
                val lon = prefs.getFloat("lon", 0f).toDouble()
                val radius = prefs.getInt("radius", 0).toFloat()

                if (lat == 0.0 || lon == 0.0 || radius == 0f) return@launch

                val apiKey = getMapsApiKey()
                if (apiKey.isNullOrEmpty()) {
                    Log.e("DashboardFragment", "API key is null or empty")
                    return@launch
                }

                val zoom = calculateZoomLevel(radius)
                val circlePath = createCirclePath(lat, lon, radius)

                val centerMarkerLat = String.format(Locale.US, "%.6f", lat)
                val centerMarkerLon = String.format(Locale.US, "%.6f", lon)

                val urlString =
                    "https://maps.googleapis.com/maps/api/staticmap?" +
                            "center=$centerMarkerLat,$centerMarkerLon" +
                            "&zoom=$zoom" +
                            "&size=600x400" +
                            "&maptype=roadmap" +
                            "&markers=color:blue%7Clabel:H%7C$centerMarkerLat,$centerMarkerLon" +
                            "&path=color:0x0000ff80|weight:5|fillcolor:0x0000ff30|$circlePath" +
                            "&key=$apiKey"

                val url = URL(urlString)
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())

                withContext(Dispatchers.Main) {
                    staticMapImageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error loading static map", e)
            }
        }
    }

    private fun getMapsApiKey(): String? {
        return try {
            val ai: ApplicationInfo? = context?.packageManager?.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            )
            ai?.metaData?.getString("com.google.android.geo.API_KEY").also {
                if (it.isNullOrEmpty()) {
                    Log.e("DashboardFragment", "API key missing in manifest meta-data")
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Failed to load meta-data: ${e.message}")
            null
        }
    }

    private fun calculateZoomLevel(radius: Float): Int {
        if (radius <= 0) return 15
        val zoom = 16 - log2(radius / 100)
        return floor(zoom).toInt().coerceIn(10, 20)
    }

    private fun createCirclePath(lat: Double, lon: Double, radiusMeters: Float): String {
        val earthRadius = 6378137.0
        val latA = Math.toRadians(lat)
        val lonA = Math.toRadians(lon)
        val angularDistance = radiusMeters / earthRadius
        val points = mutableListOf<String>()
        for (i in 0..360 step 45) {
            val bearing = Math.toRadians(i.toDouble())
            val latB =
                Math.asin(
                    Math.sin(latA) * Math.cos(angularDistance) +
                            Math.cos(latA) * Math.sin(angularDistance) * Math.cos(bearing)
                )
            var lonB = lonA + Math.atan2(
                Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(latA),
                Math.cos(angularDistance) - Math.sin(latA) * Math.sin(latB)
            )
            lonB = (lonB + 3 * Math.PI) % (2 * Math.PI) - Math.PI

            val latStr = String.format(Locale.US, "%.6f", Math.toDegrees(latB))
            val lonStr = String.format(Locale.US, "%.6f", Math.toDegrees(lonB))
            points.add("$latStr,$lonStr")
        }
        return points.joinToString("|")
    }

    private fun showNavigationDialog() {
        val navOptions = arrayOf("Dashboard", "Devices", "Profile")
        AlertDialog.Builder(requireContext())
            .setTitle("Navigate to")
            .setItems(navOptions) { _, which ->
                when (which) {
                    0 -> navigateTo(R.id.nav_dashboard)
                    1 -> navigateTo(R.id.nav_devices)
                    2 -> navigateTo(R.id.nav_profile)
                }
            }
            .show()
    }

    private fun navigateTo(navId: Int) {
        (activity?.findViewById<BottomNavigationView>(R.id.bottom_nav))?.selectedItemId =
            navId
    }
}
