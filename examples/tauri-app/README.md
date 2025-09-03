# Audio Permissions Plugin - Example App

This example application demonstrates the Tauri Audio Permissions plugin functionality across different platforms.

## Features

### Automatic Permission Management
- **Auto-check on launch**: Automatically checks audio permission status when the app starts
- **Auto-request if needed**: If permission is not granted, automatically requests it from the user
- **Real-time status**: Shows current permission status with clear visual indicators

### Manual Controls
- **Check Permission**: Manually verify current permission status
- **Request Permission**: Manually trigger permission request dialog

### Technical Logging
- **Detailed logs**: Shows all API calls and responses for debugging
- **Error handling**: Displays technical error messages for troubleshooting
- **Timestamps**: All operations are logged with precise timing

## How It Works

### 1. App Launch Sequence
```typescript
1. App loads → onMount() triggered
2. Auto-initialization starts
3. Check current permission status
4. If not granted → Request permission automatically  
5. Display final status
```

### 2. Permission States
- **Unknown** (🔄): Initial state or checking in progress
- **Granted** (✅): User has granted audio recording permission
- **Denied** (❌): User has denied audio recording permission  
- **Error** (⚠️): Technical error occurred during permission operations

### 3. Platform Behavior
- **Android**: Shows system permission dialog for RECORD_AUDIO
- **iOS**: Shows system permission dialog for microphone access
- **Desktop**: Typically grants permission automatically

## Running the Example

### Prerequisites
```bash
# Install dependencies
npm install

# Build the plugin API
cd ../../ && npm run build
```

### Development
```bash
# Run in development mode
npm run tauri dev

# Or with specific platform
npm run tauri dev -- --target android
npm run tauri dev -- --target ios
```

### Building
```bash
# Build for current platform
npm run tauri build

# Build for specific platform
npm run tauri build -- --target android
npm run tauri build -- --target ios
```

## Configuration

### Permissions
The app uses the default audio permissions configuration in `src-tauri/capabilities/default.json`:

```json
{
  "permissions": [
    "core:default",
    "audio-permissions:default"
  ]
}
```

This grants both:
- `allow-check-permission`: Check permission status
- `allow-request-permission`: Request permissions from user

### Tauri Plugin Integration
The plugin is initialized in `src-tauri/src/lib.rs`:

```rust
tauri::Builder::default()
    .plugin(tauri_plugin_audio_permissions::init())
    .run(tauri::generate_context!())
```

## Testing Scenarios

### Mobile Testing
1. **Fresh Install**: Install app on device that has never granted permission
2. **Permission Denied**: Deny permission and observe app behavior
3. **Permission Granted**: Grant permission and verify functionality
4. **Settings Change**: Change permission in device settings and restart app

### Desktop Testing
1. **First Run**: Verify permission is granted automatically
2. **Manual Operations**: Test manual check/request buttons
3. **Error Simulation**: Test error handling (disconnect network, etc.)

## Troubleshooting

### Common Issues
- **"Permission not found"**: Check that plugin is properly installed and permissions configured
- **"Command not allowed"**: Verify capabilities configuration includes audio-permissions
- **Mobile permission dialog not showing**: Ensure proper platform-specific setup

### Debug Information
The app provides detailed technical logs showing:
- Exact API calls being made
- JSON responses from permission operations
- Error messages with technical details
- Timing information for performance analysis

### Platform-Specific Notes
- **Android**: Requires RECORD_AUDIO permission in AndroidManifest.xml (automatically included)
- **iOS**: Requires NSMicrophoneUsageDescription in Info.plist (add manually)
- **Desktop**: No special requirements, permissions typically granted by default

## API Usage Examples

The example demonstrates all plugin API methods:

```typescript
import { requestPermission, checkPermission } from 'tauri-plugin-audio-permissions-api';

// Check permission status
const status = await checkPermission();
console.log('Granted:', status.granted);

// Request permission
const result = await requestPermission();
console.log('User granted:', result.granted);
```

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Svelte](https://marketplace.visualstudio.com/items?itemName=svelte.svelte-vscode) + [Tauri](https://marketplace.visualstudio.com/items?itemName=tauri-apps.tauri-vscode) + [rust-analyzer](https://marketplace.visualstudio.com/items?itemName=rust-lang.rust-analyzer).

