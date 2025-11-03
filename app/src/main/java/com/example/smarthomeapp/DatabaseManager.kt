package com.example.smarthomeapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// "object" = singleton utility. Manages all Firestore calls.
object DatabaseManager {

    private val db = FirebaseFirestore.getInstance()

    // --- Add a device ---
    suspend fun addDevice(device: Device) {
        val ref = db.collection("devices").document()
        val deviceWithId = device.copy(id = ref.id)
        ref.set(deviceWithId).await()
    }

    // --- Get all devices for a specific user ---
    fun getDevicesForUser(userId: String): Query {
        return db.collection("devices")
            .whereEqualTo("ownerId", userId)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
    }

    // --- Update device state (on/off or brightness) ---
    suspend fun updateDeviceState(deviceId: String, newState: Boolean, brightness: Int? = null) {
        val data = hashMapOf<String, Any>(
            "state" to newState,
            "lastUpdated" to System.currentTimeMillis()
        )
        brightness?.let { data["brightness"] = it }
        db.collection("devices").document(deviceId).update(data).await()
    }

    // --- Delete device ---
    suspend fun deleteDevice(deviceId: String) {
        db.collection("devices").document(deviceId).delete().await()
    }
}
