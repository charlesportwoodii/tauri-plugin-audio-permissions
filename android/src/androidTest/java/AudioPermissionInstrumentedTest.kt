package com.alaydriem.bvc.plugin.audio_permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.ContextCompat

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AudioPermissionInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.alaydriem.bvc.plugin.audio_permissions", appContext.packageName)
    }

    @Test
    fun testAudioPermissionManifestEntry() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
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
    fun testAudioPermissionCheck() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
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
}
