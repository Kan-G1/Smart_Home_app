package com.example.smarthomeapp

data class Device(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var brightness: Int = 0,
    var state: Boolean = false,
    var ownerID: String = ""
) {
    // Firestore requires a no-arg constructor
    constructor() : this("", "", "", 0, false, "")
}
