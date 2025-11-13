package com.example.smarthomeapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        adapter = DeviceAdapter(
            emptyList(),
            onStateChanged = { device, newState -> dbManager.updateDeviceState(device.id, newState) },
            onBrightnessChanged = { device, value -> dbManager.updateBrightness(device.id, value) }
        )

        recyclerView.adapter = adapter

        // Add Device button
        val btnAdd = view.findViewById<Button>(R.id.btnAddDevice)
        btnAdd.setOnClickListener { showAddDeviceDialog() }

        loadDevices()
        return view
    }

    // --- Load all devices from Firebase ---
    private fun loadDevices() {
        dbManager.getDevices { devices ->
            if (isAdded) adapter.updateList(devices)
        }
    }

    // --- Show Add Device Dialog ---
    private fun showAddDeviceDialog() {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_device, null)

        val nameInput = dialogView.findViewById<EditText>(R.id.etDeviceName)
        val typeInput = dialogView.findViewById<EditText>(R.id.etDeviceType)
        val ownerInput = dialogView.findViewById<EditText>(R.id.etOwnerID)
        val brightnessSeek = dialogView.findViewById<SeekBar>(R.id.seekBrightness)
        val brightnessLabel = dialogView.findViewById<TextView>(R.id.tvBrightnessLabel)
        val backButton = dialogView.findViewById<ImageButton>(R.id.btnBack)
        val submitButton = dialogView.findViewById<Button>(R.id.btnSubmitDevice)

        var currentBrightness = 50
        brightnessSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress
                brightnessLabel.text = "Brightness: $progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        backButton.setOnClickListener { dialog.dismiss() }

        submitButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val type = typeInput.text.toString().trim()
            val owner = ownerInput.text.toString().trim()

            if (name.isEmpty() || type.isEmpty() || owner.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbManager.addDevice(
                name = name,
                type = type,
                brightness = currentBrightness,
                ownerID = owner
            ) {
                Toast.makeText(context, "Device added!", Toast.LENGTH_SHORT).show()
                if (isAdded) loadDevices()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
