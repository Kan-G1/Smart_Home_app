package com.example.smarthomeapp

// Represents a smart device in Firestore
data class Device(
    val id: String = "",
    val name: String = "",
    val type: String = "Light",
    val state: Boolean = false,
    val brightness: Int = 50,
    val ownerId: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
