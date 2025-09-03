# Tauri Plugin audio_permissions

A Tauri v2 plugin that provides a lightweight shim layer for managing audio recording permissions on mobile platforms.

## Features

- **Android**: Implements `ActivityCompat.requestPermissions` for `RECORD_AUDIO` permission
- **iOS**: Ready for iOS audio permission implementation
- **Desktop**: Returns granted by default (desktop platforms typically don't require explicit audio permissions)

## Installation

Add the plugin to your Tauri project's dependencies:

```toml
[dependencies]
tauri-plugin-audio-permissions = { path = "path/to/this/plugin" }
```

## Permissions

This plugin follows Tauri's permission system. See [PERMISSIONS.md](./PERMISSIONS.md) for detailed security configuration.

**Basic Configuration:**
```json
{
  "permissions": ["audio-permissions:default"]
}
```

## Usage

### Rust (Backend)

```rust
use tauri_plugin_audio_permissions::{AudioPermissionsExt, PermissionRequest};

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_audio_permissions::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

// In your Tauri commands
#[tauri::command]
async fn request_audio_permission(app: tauri::AppHandle) -> Result<bool, String> {
    let response = app.audio_permissions()
        .request_permission(PermissionRequest {})
        .map_err(|e| e.to_string())?;
    Ok(response.granted)
}
```

### JavaScript/TypeScript (Frontend)

```typescript
import { requestPermission, checkPermission } from 'tauri-plugin-audio-permissions-api';

// Request audio permission
const response = await requestPermission();
if (response.granted) {
    console.log('Audio permission granted');
} else {
    console.log('Audio permission denied');
}

// Check current permission status
const status = await checkPermission();
console.log('Permission status:', status.granted);
```

## API

### Commands

- `request_permission()` - Requests audio recording permission from the user
- `check_permission()` - Checks the current audio recording permission status

### Response Format

Both commands return a `PermissionResponse`:

```typescript
interface PermissionResponse {
  granted: boolean;
}
```

## Example Application

A complete example app is available in `examples/tauri-app/` demonstrating:
- Automatic permission checking and requesting on app launch
- Real-time permission status display
- Manual permission controls for testing
- Technical logging for debugging
- Cross-platform behavior

```bash
cd examples/tauri-app
npm install
npm run tauri dev
```

See [examples/tauri-app/README.md](./examples/tauri-app/README.md) for detailed usage instructions.

## Platform Behavior

- **Android**: Uses `ActivityCompat.requestPermissions` with `RECORD_AUDIO` permission
- **iOS**: Ready for implementation (currently stub)
- **Desktop**: Always returns `granted: true` as desktop platforms typically don't require explicit audio permissions

## Android Manifest

The plugin automatically includes the required Android permission in your app's manifest:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
