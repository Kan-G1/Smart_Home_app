package com.example.smarthomeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DevicesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        // Example: add a new device when this fragment loads
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val newDevice = Device(
                    name = "Living Room Light",
                    type = "Light",
                    ownerId = "demoUser123"
                )
                DatabaseManager.addDevice(newDevice)
                println("Device added to Firestore!")
            } catch (e: Exception) {
                println("Error adding device: ${e.message}")
            }
        }

        return view
    }
}
