package com.charlesportwoodii.tauri.plugin.audio_permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NotificationPermission(private val activity: Activity) {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
        private const val TAG = "NotificationPermission"
    }

    fun checkPermission(): Boolean {
        // POST_NOTIFICATIONS is only required on Android 13+
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Checking POST_NOTIFICATIONS permission: $granted")
            granted
        } else {
            // Auto-granted on Android < 13
            Log.d(TAG, "POST_NOTIFICATIONS not required on Android < 13")
            true
        }
    }

    fun requestPermission(): Boolean {
        // Only request on Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "POST_NOTIFICATIONS not required on Android < 13, auto-granting")
            return true
        }

        if (checkPermission()) {
            Log.d(TAG, "POST_NOTIFICATIONS already granted")
            return true
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )

        Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
        return false
    }
}
