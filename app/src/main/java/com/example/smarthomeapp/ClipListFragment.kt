package com.example.smarthomeapp

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ClipListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clip_list, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvClipsList)

        val videoDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SmartHome-Videos")
        val clips = videoDir.listFiles { _, name -> name.endsWith(".mp4") }?.toList() ?: emptyList()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ClipAdapter(clips) { clip ->
            val uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", clip)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }

        return view
    }
}
