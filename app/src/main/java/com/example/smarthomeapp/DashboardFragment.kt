package com.example.smarthomeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment() {

    private val dbManager = DatabaseManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Temporary fake user ID
        val userId = "demoUser123"

        dbManager.getDevicesForUser(
            userId,
            onSnapshot = { snapshot ->
                if (snapshot != null) {
                    val devices = snapshot.documents.mapNotNull { it.toObject(Device::class.java) }

                    // Example output for debugging
                    println("Devices loaded (${devices.size}):")
                    devices.forEach { println("${it.name} | State: ${it.state}") }

                    // TODO: Update dashboard UI here (e.g., power usage summary)
                }
            },
            onError = { e ->
                println("Firestore error: ${e.message}")
            }
        )

        return view
    }
}
