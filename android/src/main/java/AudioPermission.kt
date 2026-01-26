package com.charlesportwoodii.tauri.plugin.audio_permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AudioPermission(private val activity: Activity) {
    companion object {
        private const val AUDIO_PERMISSION_REQUEST_CODE = 1001
    }

    fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(): Boolean {
        if (checkPermission()) {
            return true
        }
        
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_REQUEST_CODE
        )
        
        return false
    }
}
