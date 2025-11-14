package com.example.smarthomeapp

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = ""
) {
    // Required for Firestore
    constructor() : this("", "", "")
}
