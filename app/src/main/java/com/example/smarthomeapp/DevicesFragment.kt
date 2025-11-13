package com.example.smarthomeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DevicesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val dbManager = DatabaseManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        recyclerView = view.findViewById(R.id.rvDevicesList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = DeviceAdapter(emptyList(),
            onStateChanged = { device, newState -> dbManager.updateDeviceState(device.id, newState) },
            onBrightnessChanged = { device, value -> dbManager.updateBrightness(device.id, value) }
        )

        recyclerView.adapter = adapter

        val btnAdd = view.findViewById<Button>(R.id.btnAddDevice)
        btnAdd.setOnClickListener {
            dbManager.addDemoDevice { loadDevices() }
        }

        loadDevices()
        return view
    }

    private fun loadDevices() {
        dbManager.getDevices { devices ->
            adapter.updateList(devices)
        }
    }
}
