package com.example.smarthomeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private var items: List<Any>,
    private val onStateChanged: (Device, Boolean) -> Unit,
    private val onBrightnessChanged: (Device, Int) -> Unit,
    private val onDeleteClicked: (Device) -> Unit,
    private val onItemClicked: (Device) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        internal const val TYPE_HEADER = 0
        internal const val TYPE_DEVICE = 1
    }

    // ViewHolder for the room header
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val header: TextView = view.findViewById(R.id.tvRoomHeader)
    }

    // ViewHolder for the device item
    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvDeviceName)
        val powerSwitch: Switch = view.findViewById(R.id.switchPower)
        val brightness: SeekBar = view.findViewById(R.id.seekValue)
        val valueText: TextView = view.findViewById(R.id.tvValue)
        val statusIcon: ImageView = view.findViewById(R.id.ivStatus)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> TYPE_HEADER
            is Device -> TYPE_DEVICE
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            DeviceViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is String) {
            holder.header.text = item
        } else if (holder is DeviceViewHolder && item is Device) {
            bindDeviceViewHolder(holder, item)
        }
    }

    private fun bindDeviceViewHolder(holder: DeviceViewHolder, device: Device) {
        holder.name.text = device.name
        holder.powerSwitch.isChecked = device.state

        if (device.type == "Light") {
            holder.brightness.visibility = View.VISIBLE
            holder.valueText.visibility = View.VISIBLE
            holder.brightness.progress = device.brightness
            holder.valueText.text = "Value: ${device.brightness}%"
        } else {
            holder.brightness.visibility = View.GONE
            holder.valueText.visibility = View.GONE
        }

        holder.statusIcon.setImageResource(
            if (device.state) android.R.drawable.presence_online
            else android.R.drawable.presence_offline
        )

        holder.itemView.setOnClickListener { onItemClicked(device) }
        holder.powerSwitch.setOnCheckedChangeListener { _, isChecked -> onStateChanged(device, isChecked) }
        holder.brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                holder.valueText.text = "Value: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onBrightnessChanged(device, holder.brightness.progress)
            }
        })
        holder.deleteButton.setOnClickListener { onDeleteClicked(device) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }
}
