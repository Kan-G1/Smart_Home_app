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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvDisplayName: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val btnSetHomeLocation = view.findViewById<Button>(R.id.btnSetHomeLocation)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)

        loadUserProfile()

        tvDisplayName.setOnClickListener { showEditDisplayNameDialog() }

        btnSetHomeLocation.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapSettingsFragment())
                .addToBackStack(null)
                .commit()
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

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return
        view?.findViewById<TextView>(R.id.tvEmail)?.text = currentUser.email

        val userDocRef = db.collection("users").document(currentUser.uid)
        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && isAdded) {
                val user = document.toObject(User::class.java)
                tvDisplayName.text = user?.displayName ?: "Set Display Name"
            }
        }
    }

    private fun showEditDisplayNameDialog() {
        val context = context ?: return
        val currentUser = auth.currentUser ?: return

        val editText = EditText(context).apply {
            setText(tvDisplayName.text)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Display Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateDisplayName(currentUser.uid, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDisplayName(userId: String, newName: String) {
        db.collection("users").document(userId)
            .update("displayName", newName)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                loadUserProfile() // Refresh the user profile data
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
