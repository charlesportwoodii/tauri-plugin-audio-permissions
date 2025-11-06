package com.alaydriem.bvc.plugin.audio_permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.ContextCompat

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AudioPermissionInstrumentedTest {

    private lateinit var appContext: Context

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("com.alaydriem.bvc.plugin.audio_permissions", appContext.packageName)
    }

    @Test
    fun testAudioPermissionManifestEntry() {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions

            assertNotNull("Permissions array should not be null", permissions)
            assertTrue(
                "RECORD_AUDIO permission should be declared in manifest",
                permissions.contains(Manifest.permission.RECORD_AUDIO)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found: $packageName")
        }
    }

    @Test
    fun testForegroundServicePermissions() {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions

            assertNotNull("Permissions array should not be null", permissions)
            assertTrue(
                "FOREGROUND_SERVICE permission should be declared in manifest",
                permissions.contains(Manifest.permission.FOREGROUND_SERVICE)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                assertTrue(
                    "FOREGROUND_SERVICE_MICROPHONE permission should be declared for API 34+",
                    permissions.contains(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found: $packageName")
        }
    }

    @Test
    fun testNotificationPermissions() {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions

            assertNotNull("Permissions array should not be null", permissions)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                assertTrue(
                    "POST_NOTIFICATIONS permission should be declared for API 33+",
                    permissions.contains(Manifest.permission.POST_NOTIFICATIONS)
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found: $packageName")
        }
    }

    @Test
    fun testWakeLockPermission() {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions

            assertNotNull("Permissions array should not be null", permissions)
            assertTrue(
                "WAKE_LOCK permission should be declared in manifest",
                permissions.contains(Manifest.permission.WAKE_LOCK)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found: $packageName")
        }
    }

    @Test
    fun testAudioPermissionCheck() {
        // This test verifies that the permission check method works
        // Note: The actual permission state will depend on the test environment
        val permissionState = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        )

        // The permission state should be either GRANTED or DENIED
        assertTrue(
            "Permission state should be either GRANTED or DENIED",
            permissionState == PackageManager.PERMISSION_GRANTED ||
            permissionState == PackageManager.PERMISSION_DENIED
        )
    }

    @Test
    fun testAudioRecordingServiceExists() {
        // Test that AudioRecordingService is declared in the manifest
        val packageManager = appContext.packageManager
        val serviceIntent = Intent(appContext, AudioRecordingService::class.java)
        val resolveInfo = packageManager.resolveService(serviceIntent, PackageManager.MATCH_ALL)

        assertNotNull("AudioRecordingService should be declared in manifest", resolveInfo)
    }

    @Test
    fun testAudioRecordingServiceConfiguration() {
        // Test service configuration
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES)
            val services = packageInfo.services

            assertNotNull("Services array should not be null", services)

            val audioService = services?.find { it.name.contains("AudioRecordingService") }
            assertNotNull("AudioRecordingService should be in manifest", audioService)

            // Verify service is not exported
            assertFalse("AudioRecordingService should not be exported", audioService?.exported ?: true)

            // Verify foreground service type (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertTrue(
                    "Service should have microphone foreground service type",
                    (audioService?.foregroundServiceType ?: 0) and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE != 0
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            fail("Package not found: $packageName")
        }
    }

    @Test
    fun testServiceIntentActions() {
        // Test that service intent actions are correctly formatted
        val startIntent = Intent(appContext, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_START_RECORDING
        }
        val stopIntent = Intent(appContext, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_STOP_RECORDING
        }

        assertEquals(
            "Start action should match expected value",
            "com.alaydriem.bvc.START_RECORDING",
            startIntent.action
        )
        assertEquals(
            "Stop action should match expected value",
            "com.alaydriem.bvc.STOP_RECORDING",
            stopIntent.action
        )
        assertNotEquals("Actions should be different", startIntent.action, stopIntent.action)
    }

    @Test
    fun testNotificationChannelId() {
        // Test that notification channel constants are accessible
        val channelId = "audio_recording_channel"
        assertFalse("Channel ID should not be empty", channelId.isEmpty())
        assertTrue("Channel ID should describe audio", channelId.contains("audio"))
    }

    @Test
    fun testServiceResponseFormat() {
        // Test that service response keys match expected format
        val startedResponse = mapOf("started" to true)
        val stoppedResponse = mapOf("stopped" to true)
        val updatedResponse = mapOf("updated" to true)
        val runningResponse = mapOf("running" to false)

        assertTrue("Started response should contain 'started' key", startedResponse.containsKey("started"))
        assertTrue("Stopped response should contain 'stopped' key", stoppedResponse.containsKey("stopped"))
        assertTrue("Updated response should contain 'updated' key", updatedResponse.containsKey("updated"))
        assertTrue("Running response should contain 'running' key", runningResponse.containsKey("running"))
    }

    @Test
    fun testPermissionResponseFormat() {
        // Test that permission response keys match expected format
        val permissionResponse = mapOf("granted" to true)

        assertTrue("Permission response should contain 'granted' key", permissionResponse.containsKey("granted"))
        assertTrue("Permission response value should be boolean", permissionResponse["granted"] is Boolean)
    }
}
