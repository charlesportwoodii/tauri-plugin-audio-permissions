package com.alaydriem.bvc.plugin.audio_permissions

import android.app.Activity
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

@TauriPlugin
class AudioPermissionPlugin(private val activity: Activity): Plugin(activity) {
    private val implementation = AudioPermission(activity)

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
}
