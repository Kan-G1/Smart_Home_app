package com.example.smarthomeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ClipAdapter(
    private val clips: List<File>,
    private val onItemClicked: (File) -> Unit
) : RecyclerView.Adapter<ClipAdapter.ClipViewHolder>() {

    class ClipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ClipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        val clip = clips[position]
        holder.name.text = clip.name
        holder.itemView.setOnClickListener { onItemClicked(clip) }
    }

    override fun getItemCount(): Int = clips.size
}
