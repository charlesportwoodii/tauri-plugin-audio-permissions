package com.alaydriem.bvc.plugin.audio_permissions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke

@InvokeArg
class PermissionArgs {
  // No arguments needed for permission request
}

@InvokeArg
class ServiceArgs {
  var title: String? = null
  var message: String? = null
}

@TauriPlugin
class AudioPermissionPlugin(private val activity: Activity): Plugin(activity) {
    private val implementation = AudioPermission(activity)
    private var audioService: AudioRecordingService? = null
    private var isBound = false

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
            val hasPermission = implementation.requestPermission()
            val ret = JSObject()
            ret.put("granted", hasPermission)
            invoke.resolve(ret)
        } catch (e: Exception) {
            invoke.reject("Failed to request audio permission: ${e.message}")
        }
    }

    @Command
    fun checkPermission(invoke: Invoke) {
        try {
            val hasPermission = implementation.checkPermission()
            val ret = JSObject()
            ret.put("granted", hasPermission)
            invoke.resolve(ret)
        } catch (e: Exception) {
            invoke.reject("Failed to check audio permission: ${e.message}")
        }
    }

    @Command
    fun startForegroundService(invoke: Invoke) {
        try {
            if (!implementation.checkPermission()) {
                invoke.reject("Audio permission not granted")
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
