package com.alaydriem.bvc.plugin.audio_permissions

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AudioRecordingService : Service() {
    private val binder = AudioRecordingBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRecording = false

    companion object {
        private const val TAG = "AudioRecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_recording_channel"
        private const val CHANNEL_NAME = "Audio Recording"

        // Actions for service control
        const val ACTION_START_RECORDING = "com.alaydriem.bvc.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.alaydriem.bvc.STOP_RECORDING"
    }

    inner class AudioRecordingBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startForegroundService()
                isRecording = true
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }

        // Restart service if killed by system
        return START_STICKY
    }

    private fun startForegroundService() {
        Log.d(TAG, "Starting foreground service")
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopRecording() {
        Log.d(TAG, "Stopping recording")
        isRecording = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing audio recording notification"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Create an intent to open your app when notification is tapped
        val pendingIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Audio")
            .setContentText("Audio recording is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Replace with your app icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$TAG::AudioRecordingWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes timeout for safety
        }
        Log.d(TAG, "Wake lock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }

    fun updateNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun isRecordingActive(): Boolean = isRecording

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        releaseWakeLock()
        isRecording = false
        super.onDestroy()
    }
}
