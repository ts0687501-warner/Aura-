package com.aura.voice

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Required by Android policy for any app that captures microphone audio while
 * not in the foreground UI. Shows a persistent low-priority notification so
 * the user always knows AURA can hear. This is what makes "always listening
 * for the wake word" legal and Play-Store-compliant rather than a hidden
 * background mic capture.
 */
class VoiceForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "com.aura.app.aura_voice_channel"
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AURA is listening")
            .setContentText("Say \"AURA\" to talk")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
