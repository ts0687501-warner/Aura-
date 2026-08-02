package com.aura.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Application entry point. Phase 1 uses manual wiring (no Hilt yet) to keep
 * the module boundary explicit while the architecture stabilizes. DI framework
 * (Hilt) is planned for Phase 3 once core-automation and core-plugins land.
 */
class AuraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createVoiceNotificationChannel()
    }

    private fun createVoiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VOICE_CHANNEL_ID,
                "AURA Voice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indicates AURA is listening"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val VOICE_CHANNEL_ID = "aura_voice_channel"
    }
}
