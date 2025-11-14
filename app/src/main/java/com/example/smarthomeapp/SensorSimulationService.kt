package com.example.smarthomeapp

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.Random

class SensorSimulationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private val runnable = object : Runnable {
        override fun run() {
            val simulatedTemperature = 20 + random.nextInt(5) // Simulate temperature between 20 and 24
            Log.d("SensorSimulation", "Simulated temperature: $simulatedTemperature°C")
            handler.postDelayed(this, 5000) // Run every 5 seconds
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SensorSimulation", "Service started")
        handler.post(runnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SensorSimulation", "Service stopped")
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
