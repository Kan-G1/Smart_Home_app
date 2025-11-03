package com.example.smarthomeapp

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SensorSimulationService : Service() {

    /**
     * This method is called when another component wants to bind with the service.
     * If you don't need to support binding, you can simply return null.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
