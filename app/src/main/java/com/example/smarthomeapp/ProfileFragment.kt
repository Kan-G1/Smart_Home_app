package com.example.smarthomeapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val etDisplayName = view.findViewById<EditText>(R.id.etDisplayName)
        val btnUpdateProfile = view.findViewById<Button>(R.id.btnUpdateProfile)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)

        if (currentUser != null) {
            tvEmail.text = "Email: ${currentUser.email}"
            val userDocRef = db.collection("users").document(currentUser.uid)
            userDocRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.toObject(User::class.java)
                    etDisplayName.setText(user?.displayName)
                }
            }
        }

        btnUpdateProfile.setOnClickListener {
            val displayName = etDisplayName.text.toString().trim()
            if (displayName.isNotEmpty()) {
                if (currentUser != null) {
                    val userDocRef = db.collection("users").document(currentUser.uid)
                    userDocRef.update("displayName", displayName)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
}
