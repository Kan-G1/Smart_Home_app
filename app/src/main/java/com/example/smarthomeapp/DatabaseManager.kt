package com.example.smarthomeapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class DatabaseManager {
    private val db = FirebaseFirestore.getInstance()

    // 🔹 Get all devices for a specific user in real-time
    fun getDevicesForUser(
        userId: String,
        onSnapshot: (QuerySnapshot?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        // You can filter devices that belong to a user (if you add "ownerID" field)
        return db.collection("devices")
            // .whereEqualTo("ownerID", userId)  ← Uncomment when you add per-user devices
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("DatabaseManager", "Error listening for updates", e)
                    onError(e)
                } else {
                    onSnapshot(snapshot)
                }
            }
    }


    // 🔹 Add a demo device
    fun addDemoDevice(onResult: (Boolean) -> Unit) {
        val newDevice = hashMapOf(
            "name" to "Living Room Light",
            "type" to "Light",
            "brightness" to 75,
            "state" to true
        )

        db.collection("devices")
            .add(newDevice)
            .addOnSuccessListener {
                Log.d("DatabaseManager", "Device added with ID: ${it.id}")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("DatabaseManager", "Error adding device", e)
                onResult(false)
            }
    }

    // 🔹 Get all devices
    fun getDevices(onResult: (List<Device>) -> Unit) {
        db.collection("devices")
            .get()
            .addOnSuccessListener { result ->
                val devices = result.mapNotNull { doc -> docToDevice(doc) }
                onResult(devices)
            }
            .addOnFailureListener { e ->
                Log.e("DatabaseManager", "Error fetching devices", e)
                onResult(emptyList())
            }
    }

    // 🔹 Convert document to Device object
    private fun docToDevice(doc: QueryDocumentSnapshot): Device? {
        return try {
            Device(
                id = doc.id,
                name = doc.getString("name") ?: "Unknown",
                type = doc.getString("type") ?: "Unknown",
                brightness = (doc.getLong("brightness") ?: 0L).toInt(),
                state = doc.getBoolean("state") ?: false
            )
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error converting document", e)
            null
        }
    }

    // 🔹 Update device state
    fun updateDeviceState(id: String, newState: Boolean) {
        db.collection("devices").document(id)
            .update("state", newState)
    }

    // 🔹 Update brightness
    fun updateBrightness(id: String, value: Int) {
        db.collection("devices").document(id)
            .update("brightness", value)
    }
}
