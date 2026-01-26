package com.charlesportwoodii.tauri.plugin.audio_permissions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import app.tauri.PermissionState
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.Permission
import app.tauri.annotation.PermissionCallback
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke

@InvokeArg
class PermissionArgs {
  var permissionType: String? = "audio" // Default to audio
}

@InvokeArg
class ServiceArgs {
  var title: String? = null
  var message: String? = null
}

@TauriPlugin(
  permissions = [
    Permission(strings = [android.Manifest.permission.RECORD_AUDIO], alias = "audio"),
    Permission(strings = [android.Manifest.permission.POST_NOTIFICATIONS], alias = "notification")
  ]
)
class AudioPermissionPlugin(private val activity: Activity): Plugin(activity) {
    private val audioPermission = AudioPermission(activity)
    private val notificationPermission = NotificationPermission(activity)
    private var audioService: AudioRecordingService? = null
    private var isBound = false

    // Track which permission type is being requested for the callback
    @Volatile
    private var currentPermissionRequest: String? = null

    companion object {
        private const val TAG = "AudioPermissionPlugin"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as AudioRecordingService.AudioRecordingBinder
            audioService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            audioService = null
            isBound = false
        }
    }

    @Command
    fun requestPermission(invoke: Invoke) {
        try {
            val args = invoke.parseArgs(PermissionArgs::class.java)
            val permissionType = args.permissionType ?: "audio"

            when (permissionType.lowercase()) {
                "notification" -> {
                    // Handle notification permission request (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Track which permission we're requesting
                        currentPermissionRequest = "notification"
                        // Use Tauri's async permission system - it will call handlePermissionResult when user responds
                        requestPermissionForAlias("notification", invoke, "handlePermissionResult")
                        Log.d(TAG, "Requesting POST_NOTIFICATIONS permission (async via Tauri)")
                    } else {
                        // Not needed on Android < 13
                        val ret = JSObject().apply { put("granted", true) }
                        invoke.resolve(ret)
                        Log.d(TAG, "POST_NOTIFICATIONS not required on Android < 13")
                    }
                }
                else -> {
                    // Track which permission we're requesting
                    currentPermissionRequest = "audio"
                    // Default: audio permission - Use Tauri's async permission system
                    requestPermissionForAlias("audio", invoke, "handlePermissionResult")
                    Log.d(TAG, "Requesting RECORD_AUDIO permission (async via Tauri)")
                }
            }
        } catch (e: Exception) {
            invoke.reject("Failed to request permission: ${e.message}")
        }
    }

    @PermissionCallback
    fun handlePermissionResult(invoke: Invoke) {
        // Called by Tauri's permission system after user responds
        // Get the permission type that was requested
        val requestedPermission = currentPermissionRequest ?: "audio"

        // Map permission alias to actual Android permission string
        val permissionString = when (requestedPermission) {
            "notification" -> android.Manifest.permission.POST_NOTIFICATIONS
            else -> android.Manifest.permission.RECORD_AUDIO
        }

        // Check permission directly from Android system (not from SharedPreferences)
        // This avoids race condition with SharedPreferences async writes
        val granted = ContextCompat.checkSelfPermission(
            activity,
            permissionString
        ) == PackageManager.PERMISSION_GRANTED

        val ret = JSObject().apply { put("granted", granted) }
        invoke.resolve(ret)

        // Clear the current request
        currentPermissionRequest = null

        Log.d(TAG, "Permission callback for '$requestedPermission' ($permissionString): granted=$granted")
    }

    @Command
    fun checkPermission(invoke: Invoke) {
        try {
            val args = invoke.parseArgs(PermissionArgs::class.java)
            val permissionType = args.permissionType ?: "audio"

            val hasPermission = when (permissionType.lowercase()) {
                "notification" -> notificationPermission.checkPermission()
                else -> audioPermission.checkPermission()
            }

            val ret = JSObject()
            ret.put("granted", hasPermission)
            invoke.resolve(ret)
            Log.d(TAG, "Checked $permissionType permission: $hasPermission")
        } catch (e: Exception) {
            invoke.reject("Failed to check permission: ${e.message}")
        }
    }

    @Command
    fun startForegroundService(invoke: Invoke) {
        try {
            // Check audio permission using our AudioPermission class
            if (!audioPermission.checkPermission()) {
                invoke.reject("Audio permission not granted")
                return
            }

            // Check notification permission using our NotificationPermission class
            if (!notificationPermission.checkPermission()) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted - notification may not be visible")
                invoke.reject("Notification permission not granted")
                return
            }

            val intent = Intent(activity, AudioRecordingService::class.java).apply {
                action = AudioRecordingService.ACTION_START_RECORDING
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
            }

            // Bind to the service to get reference
            activity.bindService(
                Intent(activity, AudioRecordingService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            val ret = JSObject()
            ret.put("started", true)
            invoke.resolve(ret)
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            invoke.reject("Failed to start foreground service: ${e.message}")
            Log.e(TAG, "Error starting service", e)
        }
    }

    @Command
    fun stopForegroundService(invoke: Invoke) {
        try {
            val intent = Intent(activity, AudioRecordingService::class.java).apply {
                action = AudioRecordingService.ACTION_STOP_RECORDING
            }
            activity.startService(intent)

            if (isBound) {
                activity.unbindService(serviceConnection)
                isBound = false
            }

            val ret = JSObject()
            ret.put("stopped", true)
            invoke.resolve(ret)
            Log.d(TAG, "Foreground service stopped")
        } catch (e: Exception) {
            invoke.reject("Failed to stop foreground service: ${e.message}")
            Log.e(TAG, "Error stopping service", e)
        }
    }

    @Command
    fun updateNotification(invoke: Invoke) {
        try {
            val args = invoke.parseArgs(ServiceArgs::class.java)
            val title = args.title ?: "Recording Audio"
            val message = args.message ?: "Audio recording is active"

            audioService?.updateNotification(title, message)

            val ret = JSObject()
            ret.put("updated", true)
            invoke.resolve(ret)
        } catch (e: Exception) {
            invoke.reject("Failed to update notification: ${e.message}")
        }
    }

    @Command
    fun isServiceRunning(invoke: Invoke) {
        try {
            val isRunning = audioService?.isRecordingActive() ?: false
            val ret = JSObject()
            ret.put("running", isRunning)
            invoke.resolve(ret)
        } catch (e: Exception) {
            invoke.reject("Failed to check service status: ${e.message}")
        }
    }

    override fun onDestroy() {
        if (isBound) {
            activity.unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
