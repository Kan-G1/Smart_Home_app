package com.example.smarthomeapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val dbManager = DatabaseManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userDocRef = db.collection("users").document(currentUser.uid)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val user = document.toObject(User::class.java)
                        view.findViewById<TextView>(R.id.tvUsername).text = "Hello, ${user?.displayName ?: user?.email}"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardFragment", "Error fetching user profile", e)
                }

            val devicesContainer = view.findViewById<LinearLayout>(R.id.devices_container)
            dbManager.getDevicesForUser(currentUser.uid, {
                snapshot ->
                devicesContainer.removeAllViews()
                if (snapshot != null) {
                    for (document in snapshot.documents) {
                        val device = document.toObject(Device::class.java)
                        if (device != null) {
                            val deviceCard = inflater.inflate(R.layout.item_device_card, devicesContainer, false)
                            deviceCard.findViewById<TextView>(R.id.tvDeviceName).text = device.name
                            deviceCard.findViewById<TextView>(R.id.tvDeviceStatus).text = if (device.state) "On" else "Off"
                            deviceCard.findViewById<TextView>(R.id.tvDeviceStatusUnit).text = if (device.type == "Light") "%" else ""
                            deviceCard.findViewById<ImageView>(R.id.ivDeviceIcon).setImageResource(
                                when (device.type) {
                                    "Light" -> R.drawable.ic_light
                                    "AC" -> R.drawable.ic_ac
                                    else -> R.drawable.ic_fridge
                                }
                            )
                            devicesContainer.addView(deviceCard)
                        }
                    }
                }
            }, {
                e -> Log.e("DashboardFragment", "Error fetching devices", e)
            })
        }

        view.findViewById<TextView>(R.id.tvGreeting).text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())

        return view
    }
}
