package com.example.smarthomeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ktx.toObject

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Temporary fake user ID (replace later with Firebase Auth UID)
        val userId = "demoUser123"

        // Listen for devices belonging to that user
        DatabaseManager.getDevicesForUser(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Firestore error: ${error.message}")
                    return@addSnapshotListener
                }

                val devices = snapshot?.documents?.mapNotNull { it.toObject(Device::class.java) }
                println("✅ Devices loaded:")
                devices?.forEach { println("${it.name} | State: ${it.state}") }
            }

        return view
    }
}
