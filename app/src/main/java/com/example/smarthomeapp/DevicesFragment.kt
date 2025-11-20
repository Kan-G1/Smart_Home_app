package com.example.smarthomeapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class DevicesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val dbManager = DatabaseManager()
    private lateinit var auth: FirebaseAuth
    private var devicesListener: ListenerRegistration? = null
    private val SPAN_COUNT = 2 // Number of columns in the grid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        auth = FirebaseAuth.getInstance()
        recyclerView = view.findViewById(R.id.rvDevicesList)

        // --- Adapter and LayoutManager Setup ---
        adapter = DeviceAdapter(
            mutableListOf(),
            onStateChanged = { device, newState -> dbManager.updateDeviceState(device.id, newState) },
            onBrightnessChanged = { device, value -> dbManager.updateBrightness(device.id, value) },
            onDeleteClicked = { device -> showDeleteConfirmationDialog(device) },
            onItemClicked = { device -> handleDeviceClick(device) }
        )

        val layoutManager = GridLayoutManager(requireContext(), SPAN_COUNT)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // If the item is a header, it should span all columns.
                return if (adapter.getItemViewType(position) == DeviceAdapter.TYPE_HEADER) SPAN_COUNT else 1
            }
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
        // -------------------------------------

        view.findViewById<FloatingActionButton>(R.id.fab_add_device).setOnClickListener { showAddDeviceDialog() }

        view.findViewById<Button>(R.id.btnViewAllClips).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ClipListFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        loadAndGroupDevices()
    }

    override fun onStop() {
        super.onStop()
        devicesListener?.remove()
    }

    private fun loadAndGroupDevices() {
        val currentUser = auth.currentUser ?: return

        devicesListener = dbManager.getDevicesForUser(currentUser.uid, {
            snapshot ->
            if (snapshot != null) {
                val devices = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Device::class.java)?.also { it.id = doc.id }
                }
                // Group devices by room and create a new list with headers
                val groupedItems = mutableListOf<Any>()
                devices.groupBy { it.room }.toSortedMap().forEach { (room, deviceList) ->
                    groupedItems.add(room) // Add room header
                    groupedItems.addAll(deviceList) // Add devices in that room
                }
                if (isAdded) {
                    adapter.updateList(groupedItems)
                }
            } else {
                Log.d("DevicesFragment", "Current data: null")
            }
        }, {
            e -> Log.e("DevicesFragment", "Listen failed.", e)
        })
    }

    private fun showDeleteConfirmationDialog(device: Device) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Device")
            .setMessage("Are you sure you want to delete '${device.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                dbManager.deleteDevice(device.id, {
                    Toast.makeText(context, "Device deleted", Toast.LENGTH_SHORT).show()
                }, {
                    Toast.makeText(context, "Error deleting device", Toast.LENGTH_SHORT).show()
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleDeviceClick(device: Device) {
        if (device.type == "Camera") {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CameraFragment.newInstance(device.id))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showAddDeviceDialog() {
        // This implementation remains largely the same as before
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_device, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.etDeviceName)
        val roomInput = dialogView.findViewById<EditText>(R.id.etRoomName)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerDeviceType)
        val brightnessSeek = dialogView.findViewById<SeekBar>(R.id.seekBrightness)
        val brightnessLabel = dialogView.findViewById<TextView>(R.id.tvBrightnessLabel)
        val backButton = dialogView.findViewById<ImageButton>(R.id.btnBack)
        val submitButton = dialogView.findViewById<Button>(R.id.btnSubmitDevice)

        val deviceTypes = arrayOf("Light", "Camera")
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, deviceTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val isLight = parent.getItemAtPosition(position).toString() == "Light"
                brightnessSeek.visibility = if (isLight) View.VISIBLE else View.GONE
                brightnessLabel.visibility = if (isLight) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        var currentBrightness = 50
        brightnessSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress
                brightnessLabel.text = "Brightness: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        backButton.setOnClickListener { dialog.dismiss() }
        submitButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val room = roomInput.text.toString().trim()
            val type = typeSpinner.selectedItem.toString()

            if (name.isEmpty() || room.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser ?: return@setOnClickListener
            val device = Device(
                name = name, room = room, type = type,
                brightness = if (type == "Light") currentBrightness else 0,
                ownerID = currentUser.uid
            )
            dbManager.addDevice(device, {
                Toast.makeText(context, "Device added!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }, {
                Toast.makeText(context, "Error adding device", Toast.LENGTH_SHORT).show()
            })
        }
        dialog.show()
    }
}
