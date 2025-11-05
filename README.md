# Tauri Plugin audio_permissions

A Tauri v2 plugin that provides a comprehensive API for managing audio recording permissions and background audio sessions on mobile platforms. Built for use with Rodio/CPAL audio libraries.

## Features

- ✅ **Complete Permission Management** - Request and check microphone permissions
- ✅ **Background Recording Support** - Keep audio recording active when app is in background
- ✅ **Foreground Service Management** - Android foreground service with notifications
- ✅ **Audio Session Control** - iOS AVAudioSession management
- ✅ **Platform Parity** - Identical API across Android and iOS
- ✅ **Auto Configuration** - Automatic manifest/Info.plist injection
- ✅ **Desktop Support** - Returns granted by default (desktop platforms don't require explicit audio permissions)

## Installation

Add the plugin to your Tauri project's dependencies:

```toml
[dependencies]
tauri-plugin-audio-permissions = { path = "path/to/this/plugin" }
```

Initialize the plugin in your Tauri app:

```rust
fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_audio_permissions::init())
        .setup(|app| {
            // Your setup code
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

## Permissions Configuration

Add permissions to your `capabilities/*.json` file:

```json
{
  "permissions": [
    "audio-permissions:default"
  ]
}
```

**Available Permission Sets:**
- `audio-permissions:default` - Basic permission checking and requesting
- `audio-permissions:full-access` - All features including background service management
- `audio-permissions:read-only` - Permission checking only (no requesting)

## Implementation Guide

### Basic Pattern for Background Recording

Here's the recommended pattern for using this plugin with Rodio/CPAL:

#### Rust Backend

```rust
use tauri_plugin_audio_permissions::{AudioPermissionsExt, PermissionRequest, NotificationUpdate};

#[tauri::command]
async fn start_recording<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<bool, String> {
    // Check permission
    let permission = app.audio_permissions()
        .check_permission(PermissionRequest {})
        .map_err(|e| e.to_string())?;

    if !permission.granted {
        let response = app.audio_permissions()
            .request_permission(PermissionRequest {})
            .map_err(|e| e.to_string())?;

        if !response.granted {
            return Ok(false);
        }
    }

    // Start foreground service for background recording
    let service = app.audio_permissions()
        .start_foreground_service()
        .map_err(|e| e.to_string())?;

    if service.started.unwrap_or(false) {
        // Initialize your audio recording here (Rodio/CPAL)
        // let device = cpal::default_host().default_input_device()...

        Ok(true)
    } else {
        Ok(false)
    }
}

#[tauri::command]
async fn stop_recording<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<(), String> {
    // Stop your audio recording first
    // ...

    // Stop foreground service
    app.audio_permissions()
        .stop_foreground_service()
        .map_err(|e| e.to_string())?;

    Ok(())
}

#[tauri::command]
async fn update_recording_notification<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    elapsed: String
) -> Result<(), String> {
    app.audio_permissions()
        .update_notification(NotificationUpdate {
            title: Some("Recording Audio".to_string()),
            message: Some(format!("Recording time: {}", elapsed)),
        })
        .map_err(|e| e.to_string())?;

    Ok(())
}
```

#### TypeScript/JavaScript Frontend

```typescript
import {
  requestPermission,
  checkPermission,
  startForegroundService,
  stopForegroundService,
  updateNotification,
  isServiceRunning
} from 'tauri-plugin-audio-permissions-api';
import { info, error, warn, debug } from '@tauri-apps/plugin-log';

async function startRecording() {
  try {
    const permissionStatus = await checkPermission();

    if (!permissionStatus.granted) {
      const permissionResult = await requestPermission();

      if (!permissionResult.granted) {
        error('Permission denied');
        return false;
      }
    }

    const serviceResult = await startForegroundService();

    if (serviceResult.started) {
      info('Background audio session started');

      // Start your audio recording code here

      return true;
    } else {
      error('Failed to start foreground service');
      return false;
    }
  } catch (error) {
    error('Error starting recording:', error);
    return false;
  }
}

async function stopRecording() {
  try {
    // Stop your recording code

    const result = await stopForegroundService();

    if (result.stopped) {
      info('Background audio session stopped');
    }
  } catch (error) {
    error('Error stopping recording:', error);
  }
}

// Update notification during recording (Android only)
async function updateRecordingNotification(elapsed: string) {
  try {
    await updateNotification({
      title: 'Recording Audio',
      message: `Recording time: ${elapsed}`
    });
  } catch (error) {
    error('Error updating notification:', error);
  }
}

// Check if recording is active
async function checkRecordingStatus() {
  try {
    const status = await isServiceRunning();
    return status.running;
  } catch (error) {
    error('Error checking status:', error);
    return false;
  }
}
```

## Troubleshooting

### Android: Service not starting
- Ensure `RECORD_AUDIO` permission is granted before calling `start_foreground_service()`
- Check logcat for errors: `adb logcat | grep AudioPermission`

### iOS: Background audio stops
- Ensure you call `start_foreground_service()` before starting audio recording
- Verify `UIBackgroundModes` includes `"audio"` in Info.plist

### Desktop: Service commands not working
- Service management commands are no-ops on desktop - this is expected behavior
- Only use service commands conditionally on mobile platforms
