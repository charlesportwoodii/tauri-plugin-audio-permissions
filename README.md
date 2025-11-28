# Tauri Plugin audio_permissions

A Tauri v2 plugin that provides a comprehensive API for managing audio recording permissions and background audio sessions on mobile platforms. Built for use with Rodio/CPAL audio libraries.

## Platform Support

| Platform | Support |
|----------|---------|
| Linux    | X       |
| Windows  | ✓       |
| macOS    | ✓       |
| Android  | ✓       |
| iOS      | ✓       |

## Features

- ✅ **Complete Permission Management** - Request and check microphone and notification permissions
- ✅ **Background Recording Support** - Keep audio recording active when app is in background
- ✅ **Foreground Service Management** - Android foreground service with notifications
- ✅ **Audio Session Control** - iOS AVAudioSession management
- ✅ **Platform Parity** - Identical API across Android and iOS
- ✅ **Unified Permission API** - Single API for multiple permission types (audio, notification)

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
use tauri_plugin_audio_permissions::{AudioPermissionsExt, PermissionRequest, PermissionType, NotificationUpdate};

#[tauri::command]
async fn start_recording<R: tauri::Runtime>(app: tauri::AppHandle<R>) -> Result<bool, String> {
    // Check audio permission
    let permission = app.audio_permissions()
        .check_permission(PermissionRequest {
            permission_type: PermissionType::Audio,
        })
        .map_err(|e| e.to_string())?;

    if !permission.granted {
        let response = app.audio_permissions()
            .request_permission(PermissionRequest {
                permission_type: PermissionType::Audio,
            })
            .map_err(|e| e.to_string())?;

        if !response.granted {
            return Ok(false);
        }
    }

    // Request notification permission (Android 13+ requires this for foreground service notifications)
    let notif_permission = app.audio_permissions()
        .request_permission(PermissionRequest {
            permission_type: PermissionType::Notification,
        })
        .map_err(|e| e.to_string())?;

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
  isServiceRunning,
  PermissionType
} from 'tauri-plugin-audio-permissions';
import { info, error } from '@tauri-apps/plugin-log';

async function startRecording() {
  try {
    // Check audio permission
    const permissionStatus = await checkPermission({
      permissionType: PermissionType.Audio
    });

    if (!permissionStatus.granted) {
      const permissionResult = await requestPermission({
        permissionType: PermissionType.Audio
      });

      if (!permissionResult.granted) {
        error('Audio permission denied');
        return false;
      }
    }

    // Request notification permission (Android 13+ requires this)
    const notifResult = await requestPermission({
      permissionType: PermissionType.Notification
    });

    const serviceResult = await startForegroundService();

    if (serviceResult.started) {
      info('Background audio session started');

      // Start your audio recording code here

      return true;
    } else {
      error('Failed to start foreground service');
      return false;
    }
  } catch (err) {
    error('Error starting recording:', err);
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
  } catch (err) {
    error('Error stopping recording:', err);
  }
}

// Update notification during recording (Android only)
async function updateRecordingNotification(elapsed: string) {
  try {
    await updateNotification({
      title: 'Recording Audio',
      message: `Recording time: ${elapsed}`
    });
  } catch (err) {
    error('Error updating notification:', err);
  }
}

// Check if recording is active
async function checkRecordingStatus() {
  try {
    const status = await isServiceRunning();
    return status.running;
  } catch (err) {
    error('Error checking status:', err);
    return false;
  }
}
```

## API Reference

### TypeScript API

#### Permission Management

```typescript
// PermissionType enum
const PermissionType = {
  Audio: 'audio',
  Notification: 'notification',
} as const;

// Request audio permission (default)
const audioResult = await requestPermission();
// or explicitly
const audioResult = await requestPermission({ permissionType: PermissionType.Audio });

// Request notification permission
const notifResult = await requestPermission({ permissionType: PermissionType.Notification });

// Check permission
const status = await checkPermission({ permissionType: PermissionType.Audio });
```

#### Background Service Management

```typescript
// Start foreground service/audio session
const result = await startForegroundService();
// result.started: boolean

// Stop foreground service/audio session
const result = await stopForegroundService();
// result.stopped: boolean

// Update notification (Android only, no-op on iOS)
await updateNotification({
  title: 'Recording',
  message: 'Duration: 00:05:23'
});

// Check if service is running
const status = await isServiceRunning();
// status.running: boolean
```