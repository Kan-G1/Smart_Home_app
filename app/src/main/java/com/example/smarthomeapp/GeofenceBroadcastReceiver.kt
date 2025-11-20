package com.example.smarthomeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.auth.FirebaseAuth

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"
    private val dbManager = DatabaseManager()

    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.createNotificationChannel()

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null, cannot process.")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence Error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Log.e(TAG, "User is not logged in. Cannot perform geofence actions.")
            return
        }

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            notificationHelper.sendNotification("Leaving Home", "Turning off lights and activating cameras.")
            handleExit(context, userId)
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            notificationHelper.sendNotification("Arriving Home", "Welcome back! Restoring your home's state.")
            handleEnter(context, userId)
        }
    }

    private fun handleExit(context: Context, userId: String) {
        Log.i(TAG, "User has exited the geofence. Turning off lights and turning on cameras.")
        dbManager.getDevicesForUserOnce(userId, { snapshot ->
            val lightsToTurnOff = mutableListOf<String>()
            val camerasToTurnOn = mutableListOf<String>()
            val prefs = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
            val lightsThatWereOn = mutableSetOf<String>()

            snapshot?.documents?.forEach { doc ->
                val device = doc.toObject(Device::class.java)
                if (device != null) {
                    device.id = doc.id
                    if (device.type == "Light") {
                        if (device.state) { // If the light is on
                            lightsThatWereOn.add(device.id)
                        }
                        lightsToTurnOff.add(device.id)
                    } else if (device.type == "Camera") {
                        camerasToTurnOn.add(device.id)
                    }
                }
            }

            // Save the state of the lights that were on
            prefs.edit().putStringSet("lights_on_before_exit", lightsThatWereOn).apply()

            // Perform actions
            lightsToTurnOff.forEach { dbManager.updateDeviceState(it, false) }
            camerasToTurnOn.forEach { dbManager.updateDeviceState(it, true) }

        }, { e -> Log.e(TAG, "Error fetching devices for exit event", e) })
    }

    private fun handleEnter(context: Context, userId: String) {
        Log.i(TAG, "User has entered the geofence. Restoring light states.")
        val prefs = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
        val lightsToTurnOn = prefs.getStringSet("lights_on_before_exit", emptySet()) ?: emptySet()

        if (lightsToTurnOn.isNotEmpty()) {
            lightsToTurnOn.forEach { lightId ->
                dbManager.updateDeviceState(lightId, true)
            }
            // Clear the saved state
            prefs.edit().remove("lights_on_before_exit").apply()
        }
    }
}
