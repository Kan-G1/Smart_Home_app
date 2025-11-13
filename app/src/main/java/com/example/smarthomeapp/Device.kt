package com.example.smarthomeapp

data class Device(
    val id: String,
    val name: String,
    val type: String,
    val brightness: Int,
    val state: Boolean
)
