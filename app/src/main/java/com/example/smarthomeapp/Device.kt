package com.example.smarthomeapp

data class Device(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var brightness: Int = 0,
    var state: Boolean = false,
    var ownerID: String = "",
    var room: String = "", // New property for room name
    var lastVideoUrl: String? = null, // New property for video URL
    var isRecording: Boolean = false, // New property for recording state
    var power: Int = 0 // New property for power usage in watts
) {
    // Firestore requires a no-arg constructor
    constructor() : this("", "", "", 0, false, "", "", null, false, 0)
}
