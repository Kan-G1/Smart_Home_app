package com.example.smarthomeapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class DatabaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val devicesCollection = db.collection("devices")

    fun getDevicesForUser(
        userId: String,
        onSnapshot: (QuerySnapshot?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        Log.d("DatabaseManager", "Fetching devices for user: $userId")
        return devicesCollection
            .whereEqualTo("ownerID", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("DatabaseManager", "Error listening for device updates", e)
                    onError(e)
                } else {
                    Log.d("DatabaseManager", "Successfully fetched ${snapshot?.size() ?: 0} devices")
                    onSnapshot(snapshot)
                }
            }
    }

    fun getDevicesForUserOnce(userId: String, onComplete: (QuerySnapshot?) -> Unit, onError: (Exception) -> Unit) {
        devicesCollection.whereEqualTo("ownerID", userId).get()
            .addOnSuccessListener { onComplete(it) }
            .addOnFailureListener { onError(it) }
    }

    fun addDevice(device: Device, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        Log.d("DatabaseManager", "Adding new device: ${device.name}")
        devicesCollection.add(device)
            .addOnSuccessListener {
                Log.d("DatabaseManager", "Device added successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("DatabaseManager", "Error adding device", e)
                onError(e)
            }
    }

    fun updateDeviceState(deviceId: String, newState: Boolean) {
        Log.d("DatabaseManager", "Updating state for device $deviceId to $newState")
        devicesCollection.document(deviceId).update("state", newState)
            .addOnFailureListener { e -> Log.e("DatabaseManager", "Error updating device state", e) }
    }

    fun updateDevicePower(deviceId: String, power: Int) {
        Log.d("DatabaseManager", "Updating power for device $deviceId to $power W")
        devicesCollection.document(deviceId).update("power", power)
            .addOnFailureListener { e -> Log.e("DatabaseManager", "Error updating device power", e) }
    }

    fun updateBrightness(deviceId: String, brightness: Int) {
        Log.d("DatabaseManager", "Updating brightness for device $deviceId to $brightness")
        devicesCollection.document(deviceId).update("brightness", brightness)
            .addOnFailureListener { e -> Log.e("DatabaseManager", "Error updating brightness", e) }
    }

    fun deleteDevice(deviceId: String, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        Log.d("DatabaseManager", "Deleting device: $deviceId")
        devicesCollection.document(deviceId).delete()
            .addOnSuccessListener {
                Log.d("DatabaseManager", "Device deleted successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("DatabaseManager", "Error deleting device", e)
                onError(e)
            }
    }
}
