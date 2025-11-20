package com.example.smarthomeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class DashboardDeviceAdapter(
    private val onDeviceStateChanged: (Device, Boolean) -> Unit
) : ListAdapter<Device, DashboardDeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_card, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val iconImageView: ImageView = itemView.findViewById(R.id.ivDeviceIcon)
        private val statusTextView: TextView = itemView.findViewById(R.id.tvDeviceStatus)
        private val stateSwitch: Switch = itemView.findViewById(R.id.switchDeviceState)

        fun bind(device: Device) {
            nameTextView.text = device.name
            statusTextView.text = if (device.state) "On" else "Off"
            iconImageView.setImageResource(
                when (device.type) {
                    "Light" -> R.drawable.ic_light
                    "AC" -> R.drawable.ic_ac
                    else -> R.drawable.ic_fridge
                }
            )

            // Set switch state without triggering the listener, to prevent infinite loops
            stateSwitch.setOnCheckedChangeListener(null)
            stateSwitch.isChecked = device.state
            stateSwitch.setOnCheckedChangeListener { _, isChecked ->
                // When the user interacts with the switch, call the callback
                onDeviceStateChanged(device, isChecked)
            }
        }
    }
}

class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
    override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
        // Only redraw the item if its state or name has changed
        return oldItem.state == newItem.state && oldItem.name == newItem.name
    }
}
