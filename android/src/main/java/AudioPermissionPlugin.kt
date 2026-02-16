package com.charlesportwoodii.tauri.plugin.audio_permissions

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
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

@InvokeArg
class StartServiceArgs {
  var onPermissionRevoked: app.tauri.plugin.Channel? = null
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

    // Atomic pairing of permission type + invoke to prevent race conditions
    // when concurrent permission requests arrive before callbacks fire
    private data class PendingPermission(val type: String, val invoke: Invoke)

    @Volatile
    private var pendingPermission: PendingPermission? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Channel for emitting permission-revoked events to the frontend
    private var permissionRevokedChannel: app.tauri.plugin.Channel? = null

    companion object {
        private const val TAG = "AudioPermissionPlugin"
    }

    private fun isAppInForeground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
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
                        pendingPermission = PendingPermission("notification", invoke)
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
                    pendingPermission = PendingPermission("audio", invoke)
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
        // Callback fired normally — read and clear the pending request atomically
        val pending = pendingPermission
        pendingPermission = null

        // Determine which permission was requested
        val requestedPermission = pending?.type ?: "audio"

        // Map permission alias to actual Android permission string
        val permissionString = when (requestedPermission) {
            "notification" -> android.Manifest.permission.POST_NOTIFICATIONS
            else -> android.Manifest.permission.RECORD_AUDIO
        }

        // Check permission directly from Android system (not from SharedPreferences)
        val granted = ContextCompat.checkSelfPermission(
            activity,
            permissionString
        ) == PackageManager.PERMISSION_GRANTED

        // Resolve using the pending invoke if available, otherwise the callback invoke
        val resolveInvoke = pending?.invoke ?: invoke
        val ret = JSObject().apply { put("granted", granted) }
        resolveInvoke.resolve(ret)

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
            // Parse optional args (channel for permission revocation events)
            val args = invoke.parseArgs(StartServiceArgs::class.java)
            permissionRevokedChannel = args.onPermissionRevoked

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

            // On Android 11+, starting a foreground service from the background can fail
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isAppInForeground()) {
                invoke.reject("Cannot start foreground service from background on Android 11+")
                Log.e(TAG, "Attempted to start foreground service from background on Android 11+")
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
            val errorMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is android.app.ForegroundServiceStartNotAllowedException) {
                "Cannot start foreground service from background on Android 12+: ${e.message}"
            } else {
                "Failed to start foreground service: ${e.message}"
            }
            invoke.reject(errorMessage)
            Log.e(TAG, errorMessage, e)
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

            // Clear the revocation channel since service is stopping intentionally
            permissionRevokedChannel = null

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

    @Command
    fun isMicrophoneAvailable(invoke: Invoke) {
        try {
            val ret = JSObject()
            // Android 12+ has a device-wide microphone toggle in Settings > Privacy.
            // The toggle state requires android.permission.OBSERVE_SENSOR_PRIVACY
            // (a system-level permission), so third-party apps cannot query it.
            // We report whether the toggle feature exists so the frontend can
            // warn users to check their privacy settings if they get silent audio.
            ret.put("available", audioPermission.checkPermission())
            ret.put("toggleSupported", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            invoke.resolve(ret)
        } catch (e: Exception) {
            invoke.reject("Failed to check microphone availability: ${e.message}")
            Log.e(TAG, "Error checking microphone availability", e)
        }
    }

    override fun onResume() {
        // Fallback: if Tauri's ActivityResult permission callback didn't fire,
        // resolve the pending invoke when the activity resumes after the dialog.
        // A short delay gives the ActivityResult callback priority if it does fire.
        if (pendingPermission != null) {
            mainHandler.postDelayed({
                val pending = pendingPermission ?: return@postDelayed
                pendingPermission = null

                val permissionString = when (pending.type) {
                    "notification" -> android.Manifest.permission.POST_NOTIFICATIONS
                    else -> android.Manifest.permission.RECORD_AUDIO
                }
                val granted = ContextCompat.checkSelfPermission(
                    activity,
                    permissionString
                ) == PackageManager.PERMISSION_GRANTED

                val ret = JSObject().apply { put("granted", granted) }
                pending.invoke.resolve(ret)
                Log.d(TAG, "Fallback permission resolution for '${pending.type}': granted=$granted")
            }, 500)
        }

        // Permission monitoring: if service is running but permission was revoked
        // (one-time permission expired, manual revoke in settings, auto-reset),
        // auto-stop the service and notify the frontend.
        if (isBound && audioService?.isRecordingActive() == true) {
            if (!audioPermission.checkPermission()) {
                Log.w(TAG, "Audio permission revoked while service was running — auto-stopping")

                val stopIntent = Intent(activity, AudioRecordingService::class.java).apply {
                    action = AudioRecordingService.ACTION_STOP_RECORDING
                }
                activity.startService(stopIntent)

                try {
                    activity.unbindService(serviceConnection)
                } catch (e: Exception) {
                    Log.w(TAG, "Error unbinding service during auto-stop", e)
                }
                isBound = false

                // Emit event to frontend (if channel was provided via startForegroundService)
                permissionRevokedChannel?.send(JSObject().apply {
                    put("permissionType", "audio")
                    put("granted", false)
                })
                permissionRevokedChannel = null
            }
        }
    }

    override fun onDestroy() {
        permissionRevokedChannel = null
        if (isBound) {
            activity.unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
