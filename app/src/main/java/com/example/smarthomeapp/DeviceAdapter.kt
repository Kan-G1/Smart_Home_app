package com.example.smarthomeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private var devices: List<Device>,
    private val onStateChanged: (Device, Boolean) -> Unit,
    private val onBrightnessChanged: (Device, Int) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvDeviceName)
        val powerSwitch: Switch = view.findViewById(R.id.switchPower)
        val brightness: SeekBar = view.findViewById(R.id.seekValue)
        val valueText: TextView = view.findViewById(R.id.tvValue)
        val statusIcon: ImageView = view.findViewById(R.id.ivStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.name.text = device.name
        holder.powerSwitch.isChecked = device.state
        holder.brightness.progress = device.brightness
        holder.valueText.text = "Value: ${device.brightness}%"

        holder.statusIcon.setImageResource(
            if (device.state) android.R.drawable.presence_online
            else android.R.drawable.presence_offline
        )

        // Power switch toggle
        holder.powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            onStateChanged(device, isChecked)
        }

        // Brightness change
        holder.brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                holder.valueText.text = "Value: $progress%"
                if (fromUser) onBrightnessChanged(device, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun getItemCount(): Int = devices.size

    fun updateList(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}
