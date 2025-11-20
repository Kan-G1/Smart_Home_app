package com.example.smarthomeapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapSettingsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var homeMarker: Marker? = null
    private var geofenceCircle: Circle? = null
    private lateinit var radiusLabel: TextView
    private lateinit var radiusSeekBar: SeekBar
    private lateinit var saveButton: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map_settings, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        prefs = requireActivity().getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)

        radiusLabel = view.findViewById(R.id.tvRadiusLabel)
        radiusSeekBar = view.findViewById(R.id.seekRadius)
        saveButton = view.findViewById(R.id.btnSaveLocation)

        saveButton.setOnClickListener { saveGeofenceSettings() }

        return view
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
        loadGeofenceSettings()

        mMap.setOnMyLocationButtonClickListener {
            if (!isLocationPermissionGranted()) {
                (activity as? MainActivity)?.checkAndRequestPermissions()
                // Return true to consume the event and prevent default behavior until permission is granted
                true 
            } else {
                // Default behavior (zoom to current location) is desired if permission is granted
                false 
            }
        }

        mMap.setOnMapLongClickListener { latLng ->
            placeHomeMarker(latLng)
            drawGeofenceCircle(latLng)
        }

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                geofenceCircle?.radius = progress.toDouble()
                radiusLabel.text = "Geofence Radius: ${progress}m"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && homeMarker == null) { // Only move camera if no home is set
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }
        } else {
            mMap.isMyLocationEnabled = false
            mMap.uiSettings.isMyLocationButtonEnabled = true // Keep the button visible
        }
    }

    private fun placeHomeMarker(location: LatLng) {
        homeMarker?.remove()
        homeMarker = mMap.addMarker(MarkerOptions().position(location).title("Home"))
    }

    private fun drawGeofenceCircle(location: LatLng) {
        geofenceCircle?.remove()
        geofenceCircle = mMap.addCircle(
            CircleOptions()
                .center(location)
                .radius(radiusSeekBar.progress.toDouble())
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5f)
        )
    }

    private fun saveGeofenceSettings() {
        if (homeMarker == null) {
            Toast.makeText(context, "Long-press on the map to set a home location first.", Toast.LENGTH_SHORT).show()
            return
        }

        val editor = prefs.edit()
        editor.putFloat("lat", homeMarker!!.position.latitude.toFloat())
        editor.putFloat("lon", homeMarker!!.position.longitude.toFloat())
        editor.putInt("radius", radiusSeekBar.progress)
        editor.commit() 

        Toast.makeText(context, "Geofence saved!", Toast.LENGTH_SHORT).show()
        // Trigger MainActivity to re-evaluate geofence
        (activity as? MainActivity)?.checkDeviceLocationSettingsAndAddGeofence()
    }

    private fun loadGeofenceSettings() {
        val lat = prefs.getFloat("lat", 0f).toDouble()
        val lon = prefs.getFloat("lon", 0f).toDouble()
        val radius = prefs.getInt("radius", 150)

        if (lat != 0.0 && lon != 0.0) {
            val homeLocation = LatLng(lat, lon)
            placeHomeMarker(homeLocation)
            drawGeofenceCircle(homeLocation)
            radiusSeekBar.progress = radius
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLocation, 15f))
        }
    }
}
